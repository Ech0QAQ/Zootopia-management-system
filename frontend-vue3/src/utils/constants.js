// 枚举数据
export const ANIMAL_TYPES = ['兔科', '犬科', '猫科', '鼬科', '啮齿目', '偶蹄目', '奇蹄目', '爬行纲', '其他']
export const AREAS = ['冰川镇', '雨林区', '撒哈拉广场', '湿地市场', '小型啮齿动物区', '稀树草原中心']
export const HOUSEHOLDS = [
  '动物城', '兔窝镇', '鹿岭', '狐尾原', '熊溪谷', '龟背岛',
  '羊蹄山', '鼠洞巷', '狼嚎谷', '牛角屯', '象牙港', '猪鼻巷', '长颈坡', '虎啸林'
]
export const DEPARTMENTS = ['市政厅', '警察局', '交通署', '气候中心', '档案馆', '食品安全局', '城市规划署']
export const STAR_CANDIDATES = ['朱迪', '尼克', '夏奇羊', '盖瑞', '宝伯特', '闪电', '豹警官', '牛局长']
export const STATIONS = [
  '冰川镇', '雨林区', '撒哈拉广场', '湿地市场', '小型啮齿动物区', '稀树草原中心',
  '兔窝镇', '鹿岭', '狐尾原', '熊溪谷', '龟背岛', '羊蹄山', '鼠洞巷',
  '狼嚎谷', '牛角屯', '象牙港', '猪鼻巷', '长颈坡', '虎啸林'
]

// 格式化日期时间
export function formatDateTime(dateTimeStr) {
  if (!dateTimeStr) return '-'
  try {
    const date = new Date(dateTimeStr)
    const year = date.getFullYear()
    const month = String(date.getMonth() + 1).padStart(2, '0')
    const day = String(date.getDate()).padStart(2, '0')
    const hours = String(date.getHours()).padStart(2, '0')
    const minutes = String(date.getMinutes()).padStart(2, '0')
    return `${year}-${month}-${day} ${hours}:${minutes}`
  } catch {
    return dateTimeStr
  }
}

// Buffer转字符串
export function bufferToString(value) {
  if (!value) return ''
  if (typeof value === 'string') return value
  if (typeof value === 'object' && value.type === 'Buffer' && Array.isArray(value.data)) {
    try {
      const uint8Array = new Uint8Array(value.data)
      return new TextDecoder('utf-8').decode(uint8Array)
    } catch (e) {
      console.error('Buffer 转换失败:', e, value)
      return ''
    }
  }
  return String(value)
}

