import {
  BadRequestException,
  Controller,
  Get,
  HttpCode,
  NotFoundException,
  Param,
  Post,
  Res,
  UploadedFile,
  UseInterceptors,
  Body,
  Query,
} from '@nestjs/common'
import { FileInterceptor } from '@nestjs/platform-express'
import type { Response } from 'express'
import { Public, Roles } from '../auth/decorators'
import { PrismaService } from '../common/prisma/prisma.service'
import { extensionOf, MAX_UPLOAD_BYTES, mimeFor, safeFolder, uniqueName } from './media.rules'
import { StorageService } from './storage.service'

@Controller()
export class MediaController {
  constructor(
    private readonly storage: StorageService,
    private readonly prisma: PrismaService,
  ) {}

  /**
   * Reserve aux ADMIN (le front ne l'appelle que depuis le gestionnaire de
   * catalogue). Le Java l'exposait a tous, sans limite de taille.
   * Reponse inchangee : `{url}` — desormais une URL absolue vers le bucket.
   */
  @Roles('ADMIN')
  @Post('api/v1/uploads/image')
  @HttpCode(200)
  @UseInterceptors(FileInterceptor('file', { limits: { fileSize: MAX_UPLOAD_BYTES, files: 1 } }))
  async upload(
    @UploadedFile() file: Express.Multer.File | undefined,
    // Le front envoie le dossier en champ de formulaire, le seeder en query
    // string : Spring (@RequestParam) acceptait les deux.
    @Body('folder') bodyFolder?: string,
    @Query('folder') queryFolder?: string,
  ) {
    const folder = bodyFolder || queryFolder
    if (!file || file.size === 0) throw new BadRequestException('Le fichier envoyé est vide.')
    const ext = extensionOf(file.originalname)
    const contentType = mimeFor(ext)
    const key = `${safeFolder(folder)}/${uniqueName(ext)}`
    return { url: await this.storage.put(key, file.buffer, contentType) }
  }

  /**
   * Anciennes URL relatives `/uploads/...`. Elles peuvent survivre apres
   * l'extraction vers le bucket, notamment dans les simulations sauvegardees
   * (blob JSON opaque, jamais reecrit). Ordre : base historique, puis bucket.
   */
  @Public()
  @Get('uploads/:folder/:fileName')
  serve(@Param('folder') folder: string, @Param('fileName') fileName: string, @Res() res: Response) {
    return this.serveLegacy(folder, fileName, res)
  }

  @Public()
  @Get('uploads/:fileName')
  serveRoot(@Param('fileName') fileName: string, @Res() res: Response) {
    return this.serveLegacy('general', fileName, res)
  }

  private async serveLegacy(folder: string, fileName: string, res: Response): Promise<void> {
    const media =
      (await this.prisma.mediaFile.findUnique({ where: { folder_fileName: { folder, fileName } } })) ??
      (folder !== 'general'
        ? await this.prisma.mediaFile.findUnique({ where: { folder_fileName: { folder: 'general', fileName } } })
        : null)

    if (media) {
      res.set({
        'Content-Type': media.contentType || 'application/octet-stream',
        'Cache-Control': 'public, max-age=31536000, immutable',
        // Un SVG peut contenir du script : servi depuis le domaine de l'API, il
        // y serait execute (XSS stocke, present cote Java). On l'interdit.
        'Content-Security-Policy': "default-src 'none'; style-src 'unsafe-inline'; sandbox",
        'X-Content-Type-Options': 'nosniff',
      })
      res.send(Buffer.from(media.data))
      return
    }

    const url = this.storage.publicUrl(`${folder}/${fileName}`)
    if (url === null) throw new NotFoundException()
    res.redirect(301, url)
  }
}
