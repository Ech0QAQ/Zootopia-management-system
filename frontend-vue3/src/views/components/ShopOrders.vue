<template>
  <div>
    <div class="card">
      <div class="title">我的订单</div>
      <div class="nav-tabs">
        <button
          v-for="status in statuses"
          :key="status"
          class="nav-tab"
          :class="{ active: activeStatus === status }"
          @click="activeStatus = status; loadOrders()"
        >
          {{ status === '已发货' ? '待收货' : status }}
        </button>
      </div>
      <div>
        <div v-for="order in orders" :key="order.id" class="card" style="padding:12px;margin-bottom:12px;">
          <div style="display:flex;justify-content:space-between;align-items:center;">
            <div><strong>{{ order.orderNo || order.order_no }}</strong> | {{ formatDateTime(order.createdAt || order.created_at) }}</div>
            <div style="font-weight:700;">{{ order.status === '已发货' ? '待收货' : order.status }}</div>
          </div>
          <div style="margin:8px 0;">
            <div
              v-for="item in order.items"
              :key="item.id"
              style="display:flex;justify-content:space-between;font-size:13px;border-bottom:1px dashed #eee;padding:4px 0;"
            >
              <span>{{ item.productName || item.product_name }} x {{ item.quantity }}</span>
              <span>￥{{ (item.price * item.quantity).toFixed(2) }}</span>
            </div>
          </div>
          <div style="display:flex;justify-content:space-between;align-items:center;">
            <div style="font-size:13px;color:#666;">
              <template v-if="order.status === '已发货' || order.status === '已收货' || order.status === '已完成'">
                快递：{{ order.expressCompany || order.express_company || '-' }} | 物流单号：{{ order.expressNo || order.express_no || '-' }}
              </template>
              <span v-else>暂未发货</span>
            </div>
            <div style="font-size:15px;">合计：<span style="color:#d33;font-weight:700;">￥{{ Number(order.totalAmount || order.total_amount).toFixed(2) }}</span></div>
          </div>
          <div v-if="order.status === '已发货'" style="text-align:right;margin-top:8px;">
            <button class="btn" @click="receiveOrder(order.id)">确认收货</button>
          </div>
        </div>
        <div v-if="!orders.length" style="color:#999;">暂无订单</div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import api from '@/utils/api'
import { formatDateTime } from '@/utils/constants'

const orders = ref([])
const activeStatus = ref('全部')
const statuses = ['全部', '待发货', '已发货', '已收货', '已完成']

onMounted(async () => {
  await loadOrders()
})

async function loadOrders() {
  try {
    const params = activeStatus.value === '全部' ? '' : `?status=${encodeURIComponent(activeStatus.value)}`
    const res = await api.get(`/shop/orders/my${params}`)
    orders.value = res.data.data
  } catch (error) {
    console.error('加载失败:', error)
  }
}

async function receiveOrder(id) {
  try {
    await api.post(`/shop/orders/${id}/receive`)
    alert('已确认收货')
    await loadOrders()
  } catch (error) {
    alert(error.response?.data?.message || '操作失败')
  }
}
</script>
