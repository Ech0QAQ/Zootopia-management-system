<template>
  <div>
    <div class="card">
      <div class="title">铁路32106</div>
      <div class="subtitle">查询车次、购买车票，工作人员可新增车次并查看售票情况。</div>
    </div>

    <div v-if="isStaff" class="card">
      <div class="subtitle">新增车次</div>
      <div class="form-row">
        <div class="form-field">
          <label>始发站</label>
          <select v-model="trainForm.origin">
            <option value="">请选择</option>
            <option v-for="station in stations" :key="station" :value="station">{{ station }}</option>
          </select>
        </div>
        <div class="form-field">
          <label>终点站</label>
          <select v-model="trainForm.destination">
            <option value="">请选择</option>
            <option v-for="station in stations" :key="station" :value="station">{{ station }}</option>
          </select>
        </div>
      </div>
      <div class="form-row">
        <div class="form-field">
          <label>出发时间</label>
          <input v-model="trainForm.departTime" type="datetime-local" :min="minDateTime" />
        </div>
        <div class="form-field">
          <label>到达时间</label>
          <input v-model="trainForm.arriveTime" type="datetime-local" :min="minDateTime" />
        </div>
      </div>
      <div class="form-row">
        <div class="form-field">
          <label>车次号</label>
          <input v-model="trainForm.code" />
        </div>
        <div class="form-field">
          <label>票价</label>
          <input v-model.number="trainForm.price" type="number" />
        </div>
        <div class="form-field">
          <label>售票数目</label>
          <input v-model.number="trainForm.capacity" type="number" />
        </div>
      </div>
      <button class="btn" @click="addTrain">添加车次</button>
    </div>

    <div class="card">
      <div class="subtitle">查询车次</div>
      <div class="form-row">
        <div class="form-field">
          <label>始发地</label>
          <select v-model="searchForm.origin">
            <option value="">全部</option>
            <option v-for="station in stations" :key="station" :value="station">{{ station }}</option>
          </select>
        </div>
        <div class="form-field">
          <label>目的地</label>
          <select v-model="searchForm.destination">
            <option value="">全部</option>
            <option v-for="station in stations" :key="station" :value="station">{{ station }}</option>
          </select>
        </div>
        <div class="form-field">
          <label>出行日期（可选）</label>
          <input v-model="searchForm.date" type="date" />
        </div>
      </div>
      <button class="btn" @click="searchTrains">筛选</button>
      <div style="margin-top:10px;">
        <div v-for="train in trains" :key="train.id" :style="getTrainStyle(train)">
          <div>
            <strong>{{ train.code }}</strong>
            <span v-if="isPast(train.departTime)" style="color:#ff4d4f;font-weight:600;"> [已发车]</span>
            <span :style="getStatusColor(train)">{{ train.origin }} → {{ train.destination }}</span>
          </div>
          <div>出发：{{ formatDateTime(train.departTime) }} | 到达：{{ formatDateTime(train.arriveTime) }} | 行驶时间：{{ train.duration }}</div>
          <div>票价：{{ train.price }} 豆币 | 已售：{{ train.sold }}/{{ train.capacity }}</div>
          <button
            v-if="userRole === 'resident' && !isPast(train.departTime)"
            class="btn"
            @click="buyTicket(train.id)"
          >
            购买车票
          </button>
          <button
            v-if="isStaff"
            class="btn secondary"
            @click="viewOrders(train.id)"
          >
            查看购票名单
          </button>
          <span v-if="isPast(train.departTime) && userRole !== 'resident' && !isStaff" style="color:#999;font-size:12px;">已发车，无法购买</span>
        </div>
        <div v-if="!trains.length" style="font-size:12px;color:#999;">暂无符合条件的车次</div>
      </div>
    </div>

    <!-- 购票名单对话框 -->
    <div v-if="showOrders" class="modal-overlay" @click.self="showOrders = false">
      <div class="modal-card">
        <div class="modal-header">
          <div class="title">购票名单</div>
          <button class="modal-close" @click="showOrders = false">×</button>
        </div>
        <table style="width:100%;font-size:13px;border-collapse:collapse;text-align:center;">
          <thead>
            <tr>
              <th style="padding:8px;border-bottom:2px solid #e0e7f5;">姓名</th>
              <th style="padding:8px;border-bottom:2px solid #e0e7f5;">动物种类</th>
              <th style="padding:8px;border-bottom:2px solid #e0e7f5;">居住区域</th>
              <th style="padding:8px;border-bottom:2px solid #e0e7f5;">订单状态</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="order in orderList" :key="order.id" style="border-bottom:1px solid #f1f1f1;">
              <td style="padding:8px;">{{ order.name || '-' }}</td>
              <td style="padding:8px;">{{ order.animalType || order.animal_type || '-' }}</td>
              <td style="padding:8px;">{{ order.liveArea || order.live_area || '-' }}</td>
              <td style="padding:8px;">
                <span :class="order.status === '已退票' ? 'tag' : 'tag'" :style="order.status === '已退票' ? 'background:#ff4d4f;color:#fff;' : 'background:#52c41a;color:#fff;'">
                  {{ order.status || '-' }}
                </span>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { useUserStore } from '@/stores/user'
import api from '@/utils/api'
import { STATIONS, formatDateTime } from '@/utils/constants'

const userStore = useUserStore()
const stations = STATIONS
const trains = ref([])
const orderList = ref([])
const showOrders = ref(false)
const trainForm = ref({
  code: '',
  origin: '',
  destination: '',
  departTime: '',
  arriveTime: '',
  price: 0,
  capacity: 0
})
const searchForm = ref({
  origin: '',
  destination: '',
  date: ''
})

const isStaff = computed(() => {
  return userStore.user?.role === 'admin' || userStore.user?.role === 'worker'
})
const userRole = computed(() => userStore.user?.role)

const minDateTime = computed(() => {
  const now = new Date()
  const pad = (n) => (n < 10 ? '0' + n : n)
  return `${now.getFullYear()}-${pad(now.getMonth() + 1)}-${pad(now.getDate())}T${pad(now.getHours())}:${pad(now.getMinutes())}`
})

onMounted(async () => {
  await searchTrains()
})

function isPast(departTime) {
  return new Date(departTime) <= new Date()
}

function getTrainStyle(train) {
  const isPastTrain = isPast(train.departTime)
  return {
    borderBottom: '1px solid #f1f1f1',
    padding: '6px 0',
    fontSize: '13px',
    background: isPastTrain ? '#f8f8f8' : ''
  }
}

function getStatusColor(train) {
  const isPastTrain = isPast(train.departTime)
  return isPastTrain ? 'color:#ff4d4f;font-weight:600;' : ''
}

async function addTrain() {
  if (!trainForm.value.code || !trainForm.value.origin || !trainForm.value.destination ||
      !trainForm.value.departTime || !trainForm.value.arriveTime ||
      !trainForm.value.price || !trainForm.value.capacity) {
    alert('请填写完整信息')
    return
  }
  
  const departDt = new Date(trainForm.value.departTime)
  const arriveDt = new Date(trainForm.value.arriveTime)
  const nowDt = new Date()
  
  if (Number.isNaN(departDt.getTime()) || Number.isNaN(arriveDt.getTime())) {
    alert('时间格式不正确')
    return
  }
  
  if (departDt <= nowDt || arriveDt <= nowDt) {
    alert('出发时间和到达时间必须是未来时间')
    return
  }
  
  if (arriveDt <= departDt) {
    alert('到达时间必须晚于出发时间')
    return
  }
  
  try {
    await api.post('/trains', {
      code: trainForm.value.code,
      origin: trainForm.value.origin,
      destination: trainForm.value.destination,
      depart_time: trainForm.value.departTime,
      arrive_time: trainForm.value.arriveTime,
      price: trainForm.value.price,
      capacity: trainForm.value.capacity
    })
    alert('车次添加成功')
    trainForm.value = {
      code: '',
      origin: '',
      destination: '',
      departTime: '',
      arriveTime: '',
      price: 0,
      capacity: 0
    }
    await searchTrains()
  } catch (error) {
    alert(error.response?.data?.message || '添加失败')
  }
}

async function searchTrains() {
  try {
    const params = new URLSearchParams()
    if (searchForm.value.origin) params.append('origin', searchForm.value.origin)
    if (searchForm.value.destination) params.append('destination', searchForm.value.destination)
    if (searchForm.value.date) params.append('date', searchForm.value.date)
    
    const res = await api.get(`/trains?${params.toString()}`)
    trains.value = res.data.data
  } catch (error) {
    alert('查询失败')
  }
}

async function buyTicket(trainId) {
  try {
    await api.post('/tickets/orders', { train_id: trainId })
    alert('购票成功，可在"我的车票"中查看。')
    await searchTrains()
  } catch (error) {
    alert(error.response?.data?.message || '购票失败')
  }
}

async function viewOrders(trainId) {
  try {
    const res = await api.get(`/tickets/train/${trainId}/orders`)
    orderList.value = res.data.data
    showOrders.value = true
  } catch (error) {
    alert('加载失败')
  }
}
</script>
