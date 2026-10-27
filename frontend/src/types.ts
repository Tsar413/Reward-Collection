export type Role = 'ADMIN' | 'TEACHER' | 'STUDENT'
// Resource fields are dynamic at the table/form boundary; outgoing DTOs use explicit allowlists.
export interface Entity {
  id: number
  [key: string]: any
}
export interface User extends Entity {
  orgId: number
  classId: number | null
  userNo: string
  realName: string
  username: string
  role: Role
  status: number
  remark?: string
}
export interface ImportReport {
  total: number
  successCount: number
  skippedCount: number
  failedCount: number
  details: { rowNumber: number; userNo: string; status: string; message: string }[]
}
