import { describe, expect, it } from 'vitest'
import { publicReadPolicy } from './storage.service'

describe('politique du bucket', () => {
  it('ouvre la seule lecture d objets, ni ecriture ni listing', () => {
    const [stmt] = publicReadPolicy('ecoswitch-media').Statement
    expect(stmt.Action).toEqual(['s3:GetObject'])
    expect(stmt.Resource).toEqual(['arn:aws:s3:::ecoswitch-media/*'])
    expect(stmt.Effect).toBe('Allow')
  })
})
