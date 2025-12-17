<template>
  <div>
    <div class="card">
      <div class="title">我的工作信息</div>
      <div class="subtitle">查看当前工作区域、所属部门、薪资待遇及工作状态</div>
      <p>姓名：{{ profile.name }}</p>
      <p>工作区域：{{ profile.workArea || profile.work_area || '-' }}</p>
      <p>所属部门：{{ profile.department || '-' }}</p>
      <p>薪资待遇：{{ profile.salary != null ? profile.salary + ' 豆币/月' : '待管理员设置' }}</p>
      <p>工作状态：{{ getEmploymentStatus(profile) }}</p>
      <p style="margin-top:16px;margin-bottom:8px;">薪资变化：</p>
      <div style="padding:12px;background:#f5f8ff;border-radius:8px;min-height:220px;">
        <div ref="salaryChart" style="width:100%;height:200px;"></div>
        <div v-if="!salaryHistory.length" style="text-align:center;color:#999;padding:10px;">暂无数据</div>
      </div>
    </div>

    <div class="card">
      <div class="title">我的申请记录</div>
      <div class="subtitle">查看已提交的涨薪 / 休假 / 离职申请及审批状态</div>
      <div>
        <table v-if="applications.length" style="width:100%;font-size:13px;border-collapse:collapse;text-align:center;">
          <thead>
            <tr>
              <th style="padding:8px;border-bottom:2px solid #e0e7f5;">类型</th>
              <th style="padding:8px;border-bottom:2px solid #e0e7f5;">说明</th>
              <th style="padding:8px;border-bottom:2px solid #e0e7f5;">状态</th>
              <th style="padding:8px;border-bottom:2px solid #e0e7f5;">提交时间</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="a in applications" :key="a.id" style="border-bottom:1px solid #f1f1f1;">
              <td style="padding:8px;">{{ a.type }}</td>
              <td style="padding:8px;">{{ bufferToString(a.reason) || '-' }}</td>
              <td style="padding:8px;">{{ a.status }}</td>
              <td style="padding:8px;">{{ formatDateTime(a.createdAt || a.created_at || '').slice(0, 16) }}</td>
            </tr>
          </tbody>
        </table>
        <div v-else style="font-size:12px;color:#999;">暂无申请记录</div>
      </div>
    </div>

    <div class="card">
      <div class="title">申请中心</div>
      <div class="subtitle">可提交涨薪申请、休假申请、离职申请</div>
      <div class="form-field">
        <label>申请类型</label>
        <select v-model="applyForm.type">
          <option value="涨薪申请">涨薪申请</option>
          <option value="休假申请">休假申请</option>
          <option value="离职申请">离职申请</option>
        </select>
      </div>
      <div class="form-field">
        <label>申请说明</label>
        <textarea v-model="applyForm.reason" rows="3"></textarea>
      </div>
      <button class="btn" @click="submitApplication">提交申请</button>
      <div v-if="applyAlert" :class="'alert ' + applyAlert.type" style="margin-top:10px;">{{ applyAlert.message }}</div>
    </div>

    <div class="card">
      <div class="title">休假请求</div>
      <div class="subtitle">查看管理员的结束休假请求或主动结束休假</div>
      <div>
        <table v-if="leaveRequests.length" style="width:100%;font-size:13px;border-collapse:collapse;text-align:center;">
          <thead>
            <tr>
              <th style="padding:8px;border-bottom:2px solid #e0e7f5;">发起方</th>
              <th style="padding:8px;border-bottom:2px solid #e0e7f5;">状态</th>
              <th style="padding:8px;border-bottom:2px solid #e0e7f5;">时间</th>
              <th style="padding:8px;border-bottom:2px solid #e0e7f5;">操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="r in leaveRequests" :key="r.id" style="border-bottom:1px solid #f1f1f1;">
              <td style="padding:8px;">{{ r.initiator === 'admin' ? '管理员' : '本人' }}</td>
              <td style="padding:8px;">{{ r.status }}</td>
              <td style="padding:8px;">{{ formatDateTime(r.createdAt || r.created_at || '').slice(0, 16) }}</td>
              <td style="padding:8px;">
                <template v-if="r.initiator === 'admin' && r.status === '待员工确认'">
                  <button class="btn" @click="respondLeave(r.id, true)" style="font-size:12px;padding:4px 8px;">同意</button>
                  <button class="btn secondary" @click="respondLeave(r.id, false)" style="font-size:12px;padding:4px 8px;">拒绝</button>
                </template>
                <span v-else>-</span>
              </td>
            </tr>
          </tbody>
        </table>
        <div v-else style="font-size:12px;color:#999;">暂无休假请求</div>
      </div>
      <div class="btn-group" style="margin-top:8px;">
        <button
          v-if="profile.employmentStatus === '休假' || profile.employment_status === '休假'"
          class="btn"
          @click="endLeaveSelf"
        >
          主动结束休假
        </button>
        <span v-else style="font-size:12px;color:#666;">当前不在休假状态</span>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, nextTick } from 'vue'
import api from '@/utils/api'
import { ensureEchartsLoaded } from '@/utils/echarts'
import { formatDateTime, bufferToString } from '@/utils/constants'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()
const profile = ref({})
const salaryHistory = ref([])
const applications = ref([])
const leaveRequests = ref([])
const salaryChart = ref(null)
const applyForm = ref({ type: '涨薪申请', reason: '' })
const applyAlert = ref(null)

function getEmploymentStatus(profile) {
  const status = profile?.employmentStatus || profile?.employment_status
  if (!status || status === '未知' || status.trim() === '') {
    return '-'
  }
  return status
}

onMounted(async () => {
  await loadData()
})

async function loadData() {
  try {
    const [profileRes, historyRes, appsRes, leaveRes] = await Promise.all([
      api.get('/worker/profile'),
      api.get('/worker/salary-history'),
      api.get('/worker/applications'),
      api.get('/worker/leave-requests')
    ])
    profile.value = profileRes.data.data
    salaryHistory.value = historyRes.data.data
    applications.value = appsRes.data.data
    leaveRequests.value = leaveRes.data.data
    
    // 更新 userStore 中的用户信息
    if (profileRes.data.data) {
      const updatedUser = { ...userStore.user }
      updatedUser.employmentStatus = profileRes.data.data.employmentStatus || profileRes.data.data.employment_status
      updatedUser.employment_status = profileRes.data.data.employmentStatus || profileRes.data.data.employment_status
      updatedUser.salary = profileRes.data.data.salary
      updatedUser.workArea = profileRes.data.data.workArea || profileRes.data.data.work_area
      updatedUser.work_area = profileRes.data.data.workArea || profileRes.data.data.work_area
      updatedUser.department = profileRes.data.data.department
      userStore.setUser(updatedUser)
    }
    
    if (salaryHistory.value.length) {
      await nextTick()
      renderSalaryChart()
    }
  } catch (error) {
    console.error('加载失败:', error)
  }
}

async function renderSalaryChart() {
  try {
    await ensureEchartsLoaded()
    const chart = window.echarts.init(salaryChart.value)
    const labels = salaryHistory.value.map(h => formatDateTime(h.createdAt || h.time || h.created_at || '').slice(5, 10))
    const option = {
      color: ['#cfe6ff'],
      tooltip: {
        trigger: 'axis',
        formatter: (params) => {
          if (!params || !params.length) return ''
          const p = params[0]
          const marker = '<span style="display:inline-block;margin-right:6px;width:10px;height:10px;border-radius:50%;background:#5a8ccf;"></span>'
          return `${p.axisValue}<br/>${marker}薪资：${p.value} 豆币`
        }
      },
      grid: { left: '6%', right: '4%', bottom: 0, top: 28, containLabel: true },
      xAxis: {
        type: 'category',
        data: labels,
        axisLabel: { color: '#666' },
        axisLine: { lineStyle: { color: '#d4e2ff' } }
      },
      yAxis: {
        type: 'value',
        minInterval: 1,
        axisLabel: { color: '#666' },
        splitLine: { lineStyle: { color: '#e7eef7' } }
      },
      series: [{
        type: 'bar',
        data: salaryHistory.value.map(h => ({
          value: h.value,
          itemStyle: {
            color: new window.echarts.graphic.LinearGradient(0, 0, 0, 1, [
              { offset: 0, color: '#ebf6ff' },
              { offset: 1, color: '#c7e3ff' }
            ])
          },
          emphasis: {
            itemStyle: {
              color: new window.echarts.graphic.LinearGradient(0, 0, 0, 1, [
                { offset: 0, color: '#d7e9ff' },
                { offset: 1, color: '#9fc8ff' }
              ])
            }
          }
        })),
        barMaxWidth: 50,
        showBackground: true,
        backgroundStyle: { color: 'rgba(0,0,0,0.04)' },
        label: {
          show: true,
          position: 'top',
          distance: 6,
          color: '#145a7b',
          fontWeight: '700'
        }
      }]
    }
    chart.setOption(option)
    window.addEventListener('resize', () => chart.resize())
  } catch (err) {
    if (salaryChart.value) {
      salaryChart.value.innerHTML = '<div style="text-align:center;color:#c00;">图表加载失败，请检查 echarts.js 是否可访问。</div>'
    }
  }
}

async function submitApplication() {
  const type = applyForm.value.type
  const reason = applyForm.value.reason.trim()
  try {
    await api.post('/worker/apply', { type, reason })
    applyAlert.value = { type: 'alert-success', message: '申请已提交，请等待管理员审批。' }
    applyForm.value.reason = ''
    await loadData()
  } catch (error) {
    applyAlert.value = { type: 'alert-error', message: error.response?.data?.message || '提交失败' }
  }
}

async function respondLeave(id, agree) {
  try {
    await api.post(`/worker/leave-requests/${id}/respond`, { agree })
    alert('已处理')
    await loadData()
    // loadData 已经更新了 userStore，这里再刷新一次确保同步
    await userStore.refreshUser()
  } catch (error) {
    alert(error.response?.data?.message || '处理失败')
  }
}

async function endLeaveSelf() {
  try {
    await api.post('/worker/leave-requests', {})
    alert('已结束休假，状态已更新为在职')
    await loadData()
    // loadData 已经更新了 userStore，这里再刷新一次确保同步
    await userStore.refreshUser()
  } catch (error) {
    alert(error.response?.data?.message || '操作失败')
  }
}
</script>
