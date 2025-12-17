<template>
  <div>
    <div class="card">
      <div class="title">订单管理</div>
      <div style="display:flex;align-items:center;gap:12px;margin-bottom:12px;">
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
        <div style="display:flex;align-items:center;gap:6px;margin-bottom:16px;margin-left:20px;">
          <label style="font-size:13px;color:#666;">筛选月份：</label>
          <input
            v-model="selectedMonth"
            type="month"
            style="padding:4px 8px;border:1px solid #ddd;border-radius:4px;font-size:13px;"
            @change="loadOrders()"
          />
          <button class="btn secondary" @click="clearMonthFilter" style="font-size:12px;padding:4px 8px;">清除</button>
        </div>
      </div>
      <div>
        <div v-for="order in orders" :key="order.id" class="card" style="padding:12px;margin-bottom:12px;">
          <div style="display:flex;justify-content:space-between;align-items:center;">
            <div>
              <strong>{{ order.orderNo || order.order_no }}</strong> | 下单用户：{{ order.username }} | {{ formatDateTime(order.createdAt || order.created_at) }}
            </div>
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
          <div style="font-size:13px;color:#666;margin-bottom:6px;">
            收货信息：{{ order.receiverName || order.receiver_name }}，{{ order.receiverPhone || order.receiver_phone }}，{{ order.receiverAddress || order.receiver_address }}
          </div>
          <div style="display:flex;justify-content:space-between;align-items:center;">
            <div style="font-size:13px;color:#666;">
              <template v-if="order.status === '已发货' || order.status === '已收货' || order.status === '已完成'">
                快递：{{ order.expressCompany || order.express_company || '-' }} | 物流单号：{{ order.expressNo || order.express_no || '-' }}
              </template>
              <span v-else>未发货</span>
            </div>
            <div style="font-size:15px;">合计：<span style="color:#d33;font-weight:700;">￥{{ Number(order.totalAmount || order.total_amount).toFixed(2) }}</span></div>
          </div>
          <div style="text-align:right;margin-top:8px;display:flex;gap:8px;justify-content:flex-end;">
            <button
              v-if="order.status === '待发货'"
              class="btn"
              @click="showShipDialog(order.id)"
            >
              发货
            </button>
            <button
              v-if="order.status === '已收货'"
              class="btn secondary"
              @click="completeOrder(order.id)"
            >
              完成订单
            </button>
          </div>
        </div>
        <div v-if="!orders.length" style="color:#999;">暂无订单</div>
      </div>
    </div>

    <!-- 发货弹窗 -->
    <div v-if="showShip" class="modal-overlay" @click.self="handleShipOverlayClick">
      <div class="modal-card" style="width:420px;">
        <div class="title" style="font-size:20px;">发货</div>
        <div class="form-field">
          <label>快递公司</label>
          <select v-model="shipForm.company">
            <option value="逆丰快递">逆丰快递</option>
            <option value="京西快递">京西快递</option>
            <option value="方通快递">方通快递</option>
            <option value="闪电急送">闪电急送</option>
          </select>
        </div>
        <div class="form-field">
          <label>物流单号</label>
          <input v-model="shipForm.expressNo" />
        </div>
        <div style="text-align:right;display:flex;gap:8px;justify-content:flex-end;">
          <button class="btn secondary" @click="showShip = false">取消</button>
          <button class="btn" @click="confirmShip">确定</button>
        </div>
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
const selectedMonth = ref('')
const showShip = ref(false)
const currentOrderId = ref(null)
const shipForm = ref({
  company: '逆丰快递',
  expressNo: ''
})
const statuses = ['全部', '待发货', '已发货', '已收货', '已完成']

onMounted(async () => {
  await loadOrders()
})

async function loadOrders() {
  try {
    let params = activeStatus.value === '全部' ? '' : `?status=${encodeURIComponent(activeStatus.value)}`
    if (selectedMonth.value) {
      params += (params ? '&' : '?') + `month=${encodeURIComponent(selectedMonth.value)}`
    }
    const res = await api.get(`/admin/shop/orders${params}`)
    orders.value = res.data.data
  } catch (error) {
    console.error('加载失败:', error)
  }
}

function clearMonthFilter() {
  selectedMonth.value = ''
  loadOrders()
}

function showShipDialog(orderId) {
  currentOrderId.value = orderId
  shipForm.value = {
    company: '逆丰快递',
    expressNo: ''
  }
  showShip.value = true
}

function handleShipOverlayClick(e) {
  // 阻止点击外部关闭
  if (e.target === e.currentTarget) {
    return
  }
}

async function confirmShip() {
  const express_no = shipForm.value.expressNo.trim()
  if (!express_no) {
    alert('请填写单号')
    return
  }
  try {
    await api.post(`/admin/shop/orders/${currentOrderId.value}/ship`, {
      company: shipForm.value.company,
      express_no
    })
    showShip.value = false
    await loadOrders()
  } catch (error) {
    alert(error.response?.data?.message || '发货失败')
  }
}

async function completeOrder(id) {
  if (!confirm('确认完成订单？')) return
  try {
    await api.post(`/admin/shop/orders/${id}/complete`)
    await loadOrders()
  } catch (error) {
    alert(error.response?.data?.message || '操作失败')
  }
}
</script>
