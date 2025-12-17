<template>
  <div>
    <div class="card">
      <div class="title">销售额分析</div>
      <div class="subtitle">本年度月度销售额与订单数量</div>
      <div ref="amountChart" style="width:100%;height:260px;"></div>
      <div ref="countChart" style="width:100%;height:260px;margin-top:16px;"></div>
    </div>
    <div class="card" style="margin-top:16px;">
      <div class="title">商品销售额排行</div>
      <div class="subtitle">本年度各商品销售额（Top 20）</div>
      <div ref="productSalesChart" style="width:100%;height:400px;"></div>
    </div>
    <div class="card" style="margin-top:16px;">
      <div class="title">商品销售量排行</div>
      <div class="subtitle">本年度各商品销售量（Top 20）</div>
      <div ref="productQuantityChart" style="width:100%;height:400px;"></div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, nextTick } from 'vue'
import api from '@/utils/api'
import { ensureEchartsLoaded } from '@/utils/echarts'
import { bufferToString } from '@/utils/constants'

const amountChart = ref(null)
const countChart = ref(null)
const productSalesChart = ref(null)
const productQuantityChart = ref(null)

onMounted(async () => {
  await loadData()
})

async function loadData() {
  try {
    await ensureEchartsLoaded()
    const res = await api.get('/admin/shop/analytics')
    const data = res.data.data
    
    await nextTick()
    
    const labels = Array.from({ length: 12 }, (_, i) => `${i + 1}月`)
    
    const amountChartInstance = window.echarts.init(amountChart.value)
    const countChartInstance = window.echarts.init(countChart.value)
    const productSalesChartInstance = window.echarts.init(productSalesChart.value)
    const productQuantityChartInstance = window.echarts.init(productQuantityChart.value)
    
    const lineOption = (title, seriesData, color) => ({
      color: [color],
      tooltip: { trigger: 'axis' },
      xAxis: { type: 'category', data: labels },
      yAxis: { type: 'value', minInterval: 1 },
      grid: { left: '6%', right: '4%', bottom: 20, top: 30, containLabel: true },
      series: [
        {
          type: 'line',
          data: seriesData,
          smooth: true,
          areaStyle: { opacity: 0.1 },
          label: { show: true, position: 'top' }
        }
      ],
      title: { text: title, left: 'center', textStyle: { fontSize: 14, fontWeight: 600 } }
    })
    
    amountChartInstance.setOption(lineOption('本年月度销售额（元）', data.amount, '#5ba8ff'))
    countChartInstance.setOption(lineOption('本年订单数量（单）', data.count, '#ff914d'))
    
    // 商品销售额柱状图
    if (data.productSales && data.productSales.length > 0) {
      const productNames = data.productSales.map(p => {
        const name = bufferToString(p.name)
        return name.length > 10 ? name.substring(0, 10) + '...' : name
      })
      const productSalesData = data.productSales.map(p => Number(p.sales || 0))
      productSalesChartInstance.setOption({
        tooltip: {
          trigger: 'axis',
          axisPointer: { type: 'shadow' },
          formatter: (params) => {
            const param = params[0]
            const idx = param.dataIndex
            const fullName = bufferToString(data.productSales[idx].name)
            return `${fullName}<br/>销售额: ￥${param.value.toFixed(2)}`
          }
        },
        grid: { left: '8%', right: '4%', bottom: '15%', top: '10%', containLabel: true },
        xAxis: {
          type: 'category',
          data: productNames,
          axisLabel: { rotate: 45, fontSize: 11 }
        },
        yAxis: {
          type: 'value',
          axisLabel: { formatter: (value) => `￥${value.toFixed(0)}` }
        },
        series: [
          {
            type: 'bar',
            data: productSalesData,
            itemStyle: { color: '#5ba8ff' },
            label: {
              show: true,
              position: 'top',
              formatter: (params) => {
                const value = params.value
                if (value >= 10000) return `￥${(value / 10000).toFixed(1)}万`
                return `￥${value.toFixed(0)}`
              },
              fontSize: 10
            }
          }
        ],
        title: {
          text: '商品销售额排行（元）',
          left: 'center',
          textStyle: { fontSize: 14, fontWeight: 600 }
        }
      })
    } else {
      productSalesChartInstance.setOption({
        title: {
          text: '暂无数据',
          left: 'center',
          top: 'middle',
          textStyle: { fontSize: 14, color: '#999' }
        }
      })
    }
    
    // 商品销售量柱状图
    if (data.productQuantities && data.productQuantities.length > 0) {
      const productQuantityNames = data.productQuantities.map(p => {
        const name = bufferToString(p.name)
        return name.length > 10 ? name.substring(0, 10) + '...' : name
      })
      const productQuantityData = data.productQuantities.map(p => Number(p.quantity || 0))
      productQuantityChartInstance.setOption({
        tooltip: {
          trigger: 'axis',
          axisPointer: { type: 'shadow' },
          formatter: (params) => {
            const param = params[0]
            const idx = param.dataIndex
            const fullName = bufferToString(data.productQuantities[idx].name)
            return `${fullName}<br/>销售量: ${param.value} 件`
          }
        },
        grid: { left: '8%', right: '4%', bottom: '15%', top: '10%', containLabel: true },
        xAxis: {
          type: 'category',
          data: productQuantityNames,
          axisLabel: { rotate: 45, fontSize: 11 }
        },
        yAxis: {
          type: 'value',
          axisLabel: { formatter: (value) => `${value} 件` }
        },
        series: [
          {
            type: 'bar',
            data: productQuantityData,
            itemStyle: { color: '#ff914d' },
            label: {
              show: true,
              position: 'top',
              formatter: (params) => {
                const value = params.value
                if (value >= 10000) return `${(value / 10000).toFixed(1)}万`
                return `${value}`
              },
              fontSize: 10
            }
          }
        ],
        title: {
          text: '商品销售量排行（件）',
          left: 'center',
          textStyle: { fontSize: 14, fontWeight: 600 }
        }
      })
    } else {
      productQuantityChartInstance.setOption({
        title: {
          text: '暂无数据',
          left: 'center',
          top: 'middle',
          textStyle: { fontSize: 14, color: '#999' }
        }
      })
    }
    
    window.addEventListener('resize', () => {
      amountChartInstance.resize()
      countChartInstance.resize()
      productSalesChartInstance.resize()
      productQuantityChartInstance.resize()
    })
  } catch (error) {
    console.error('加载失败:', error)
  }
}
</script>
