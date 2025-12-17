<template>
  <div>
    <div class="card">
      <div class="title">我的车票订单</div>
      <div class="subtitle">包括已退票和已出行的车票。</div>
      <div>
        <div v-for="order in orders" :key="order.id" style="border-bottom:1px solid #f1f1f1;padding:6px 0;font-size:13px;display:flex;justify-content:space-between;align-items:center;gap:8px;">
          <div>
            <div><strong>{{ order.trainCode || order.train_code }}</strong> {{ order.origin }} → {{ order.destination }}</div>
            <div>出发：{{ formatDateTime(order.departTime || order.depart_time) }} | 票价：{{ order.price }} 豆币</div>
            <div>状态：<span :style="getStatusStyle(order.status)">{{ order.status }}</span></div>
          </div>
          <div>
            <button
              v-if="order.status === '已支付'"
              class="btn secondary"
              @click="refundTicket(order.id)"
            >
              退票
            </button>
          </div>
        </div>
        <div v-if="!orders.length" style="font-size:12px;color:#999;">暂无订单</div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import api from '@/utils/api'
import { formatDateTime } from '@/utils/constants'

const orders = ref([])

onMounted(async () => {
  await loadOrders()
})

async function loadOrders() {
  try {
    const res = await api.get('/tickets/my-orders')
    orders.value = res.data.data
  } catch (error) {
    console.error('加载失败:', error)
  }
}

function getStatusStyle(status) {
  if (status === '已退票') {
    return 'display:inline-block;padding:2px 8px;border-radius:12px;font-size:12px;background:#ffe8e5;color:#c93a2f;'
  } else if (status === '已出行') {
    return 'display:inline-block;padding:2px 8px;border-radius:12px;font-size:12px;background:#e4f7ec;color:#2f8f4c;'
  } else {
    return 'display:inline-block;padding:2px 8px;border-radius:12px;font-size:12px;background:#e0e7f5;color:#38527a;'
  }
}

async function refundTicket(id) {
  if (!confirm('确认退票？')) return
  try {
    await api.post(`/tickets/orders/${id}/refund`)
    alert('退票成功')
    await loadOrders()
  } catch (error) {
    alert(error.response?.data?.message || '退票失败')
  }
}
</script>
