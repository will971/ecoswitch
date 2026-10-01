import { Module } from '@nestjs/common'
import { AiAdvisorService } from './ai-advisor.service'

@Module({ providers: [AiAdvisorService], exports: [AiAdvisorService] })
export class AiAdvisorModule {}
