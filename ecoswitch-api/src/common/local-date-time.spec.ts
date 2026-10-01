import { describe, expect, it } from 'vitest'
import { toJavaLocalDateTimeString } from './local-date-time'

describe('toJavaLocalDateTimeString', () => {
  it('rend millisecondes, sans Z ni decalage', () => {
    expect(toJavaLocalDateTimeString(new Date('2026-09-30T14:23:11.123Z'))).toBe('2026-09-30T14:23:11.123')
  })
  it('omet la fraction quand elle est nulle', () => {
    expect(toJavaLocalDateTimeString(new Date('2026-09-30T14:23:11.000Z'))).toBe('2026-09-30T14:23:11')
  })
  it('omet secondes et fraction quand elles sont nulles, comme Java', () => {
    expect(toJavaLocalDateTimeString(new Date('2026-09-30T14:23:00.000Z'))).toBe('2026-09-30T14:23')
  })
  it('garde les zeros significatifs de la fraction', () => {
    expect(toJavaLocalDateTimeString(new Date('2026-01-05T03:04:05.007Z'))).toBe('2026-01-05T03:04:05.007')
  })
})
