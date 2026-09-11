/**
 * 采购域日期处理工具（B 模块内部使用）
 *
 * 背景（yudao 全局序列化契约，见 YudaoJacksonAutoConfiguration）：
 * - LocalDateTime ↔ 毫秒时间戳（数字）。因此表单提交必须给数字（Element Plus 用 value-format="x"），
 *   传字符串会被 getValueAsLong() 静默解析为 0（即 1970-01-01）。
 * - LocalDate / LocalTime 使用 Jackson 默认序列化 → 响应里是数组形式，例如 [2026,9,11]，
 *   直接渲染会显示成 "2026,9,11"，必须格式化后再展示。
 */

/** 把后端返回的 LocalDate（数组 [y,m,d] 或字符串）格式化为 YYYY-MM-DD */
export function formatLocalDate(value?: number[] | string | null): string {
  if (value === undefined || value === null || value === '') {
    return ''
  }
  if (Array.isArray(value)) {
    const [year, month, day] = value
    return `${year}-${String(month).padStart(2, '0')}-${String(day).padStart(2, '0')}`
  }
  return String(value)
}

/** 把 LocalDate（数组）转换为 el-date-picker 的绑定值（YYYY-MM-DD 字符串） */
export function toLocalDateString(value?: number[] | string | null): string {
  return formatLocalDate(value)
}

/** Element Plus el-table 的 formatter：用于 LocalDate 列 */
export function localDateFormatter(
  _row: unknown,
  _column: unknown,
  cellValue: number[] | string | null
): string {
  return formatLocalDate(cellValue)
}
