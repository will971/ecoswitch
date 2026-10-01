import { Logger, ValidationPipe } from '@nestjs/common'
import { NestFactory } from '@nestjs/core'
import type { NestExpressApplication } from '@nestjs/platform-express'
import { AppModule } from './app.module'
import { installBigIntJsonSupport } from './common/bigint'
import { AllExceptionsFilter } from './common/filters/http-exception.filter'

async function bootstrap(): Promise<void> {
  installBigIntJsonSupport()

  const app = await NestFactory.create<NestExpressApplication>(AppModule, { bufferLogs: true })

  // Derriere Nginx : l'IP client vient de X-Forwarded-For. Sans cela, la
  // limitation de debit verrait tout le trafic venir du proxy.
  app.set('trust proxy', 1)
  app.disable('x-powered-by')

  app.useGlobalFilters(new AllExceptionsFilter())
  app.useGlobalPipes(
    new ValidationPipe({
      transform: true,
      whitelist: true,
      // PAS de forbidNonWhitelisted : UserProfileModal.vue renvoie l'objet
      // complet (id, userEmail inclus) sur un PUT. Rejeter les champs en trop
      // casserait la mise a jour d'un profil de garage.
      forbidNonWhitelisted: false,
    }),
  )

  const origins = process.env.CORS_ALLOWED_ORIGINS ?? '*'
  app.enableCors({
    origin: origins === '*' ? true : origins.split(',').map((o) => o.trim()),
    credentials: true,
  })

  const port = Number(process.env.PORT ?? 8080)
  await app.listen(port, '0.0.0.0')
  new Logger('Bootstrap').log(`EcoSwitch API en ecoute sur le port ${port}`)
}

void bootstrap()
