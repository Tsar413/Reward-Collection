export function imageError(file?: File) {
  if (!file) return '请选择图片'
  if (
    !['image/jpeg', 'image/png', 'image/webp'].includes(file.type) ||
    !/\.(jpe?g|png|webp)$/i.test(file.name)
  )
    return '只支持 JPEG、PNG、WebP 图片'
  if (file.size === 0) return '图片不能为空'
  if (file.size > 5 * 1024 * 1024) return '图片不能超过 5MiB'
  return ''
}
export function validDate(value: string) {
  if (!/^\d{4}-\d{2}-\d{2}$/.test(value)) return false
  const [y, m, d] = value.split('-').map(Number) as [number, number, number]
  const leap = y % 4 === 0 && (y % 100 !== 0 || y % 400 === 0)
  return (
    y > 0 &&
    m >= 1 &&
    m <= 12 &&
    d >= 1 &&
    d <= ([31, leap ? 29 : 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31][m - 1] || 0)
  )
}
export const awardFields = [
  ['awardName', '奖状名称', 200],
  ['issuingOrganization', '颁发单位', 200],
  ['awardDate', '获奖日期', 10],
  ['activityName', '活动名称', 200],
  ['awardCategory', '奖项类别', 50],
  ['awardLevel', '奖项级别', 50],
  ['awardGrade', '获奖等级', 50],
  ['certificateNo', '证书编号', 100],
  ['description', '奖状说明', 5000],
] as const
