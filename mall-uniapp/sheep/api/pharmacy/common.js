// =====================================================
// 药店小程序公共转换层（Mock 与真实适配器共用）
// -----------------------------------------------------
// 页面只消费「客户端契约」，不直接依赖后端原始结构；
// 金额统一为「分」，日期统一为本地可读文本，状态统一为客户端状态机。
// =====================================================

/** 分 → 元字符串（两位小数） */
export const money = (cents = 0) => (Number(cents) / 100).toFixed(2);

/** 元（后端 Decimal） → 分 */
export const fen = (yuan) => Math.round(Number(yuan || 0) * 100);

/** 分 → 元（给后端试算接口用，后端按元计算） */
export const yuan = (cents) => Number(Number(cents || 0) / 100).toFixed(2);

/** 后端 LocalDateTime（如 2026-09-19T10:30:00） → 本地文本 */
export const formatDate = (value) => {
  if (!value) return '';
  const text = String(value).replace('T', ' ').replace(/\..*$/, '');
  return text.length >= 16 ? text.slice(0, 16) : text;
};

export const statusNames = {
  unpaid: '待支付',
  review: '待审核',
  ready: '待自提 / 待配送',
  completed: '已完成',
  cancelled: '已取消',
};

/** 处方审核状态文案（后端 0 待审 / 1 通过 / 2 驳回） */
export const reviewStatusNames = {
  0: '处方已提交，请等待药师审核',
  1: '处方审核已通过',
  2: '处方审核未通过，请联系药师',
};

/**
 * 后端 AppDrugRespVO → 客户端 product 契约。
 * @param raw 后端药品 VO（含 imageUrl/approvalNo）
 * @param availability 库存投影 { stock, image, approval, active }
 */
export function normalizeDrug(raw, availability = {}) {
  return {
    id: raw.id,
    name: raw.tradeName || raw.genericName,
    genericName: raw.genericName,
    specification: raw.specification,
    manufacturer: raw.manufacturer,
    category: raw.categoryId,
    barcode: availability.barcode || '',
    approval: availability.approval || '商品档案暂未提供',
    image: availability.image || '',
    // 会员价优先（与后端下单取价口径一致），单位为分
    price: Math.round(Number(raw.memberPrice ?? (raw.retailPrice || 0)) * 100),
    stock: Math.max(0, Math.floor(Number(availability.stock) || 0)),
    rx: Number(raw.isRx) === 1,
    device: Number(raw.drugType) === 6,
    active: availability.active !== false,
  };
}
