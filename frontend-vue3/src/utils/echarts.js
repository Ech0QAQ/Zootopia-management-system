// 动态加载echarts
export async function ensureEchartsLoaded() {
  if (window.echarts) return Promise.resolve()
  if (window.__echartsLoading) return window.__echartsLoading
  
  // 优先尝试使用 npm 包
  try {
    const echartsModule = await import('echarts')
    // echarts 5.x 版本导出方式
    const echarts = echartsModule.default || echartsModule
    window.echarts = echarts
    return Promise.resolve()
  } catch (e) {
    console.warn('从 npm 包加载 echarts 失败，尝试从 public 目录加载:', e)
    // 如果 npm 包加载失败，尝试从 public 目录加载
    const candidates = ['/echarts.js']
    window.__echartsLoading = new Promise((resolve, reject) => {
      const tryLoad = (idx) => {
        if (idx >= candidates.length) {
          reject(new Error('echarts.js 未找到，请确保 echarts.js 文件在 public 目录下'))
          return
        }
        const script = document.createElement('script')
        script.src = candidates[idx]
        script.onload = () => {
          if (!window.echarts) {
            reject(new Error('echarts.js 加载失败'))
            return
          }
          resolve()
        }
        script.onerror = () => {
          script.remove()
          tryLoad(idx + 1)
        }
        document.body.appendChild(script)
      }
      tryLoad(0)
    })
    return window.__echartsLoading
  }
}

