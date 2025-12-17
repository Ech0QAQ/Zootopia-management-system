<template>
  <div>
    <div class="card">
      <div class="title">工作人员管理</div>
      <div class="subtitle">审批工作人员、调整工作区域/部门/薪资、开除员工</div>
      <div style="margin-bottom:16px;display:flex;gap:12px;flex-wrap:wrap;align-items:flex-end;">
        <div class="form-field" style="flex:1;min-width:120px;">
          <label>搜索姓名</label>
          <input v-model="filters.name" placeholder="输入员工姓名..." />
        </div>
        <div class="form-field" style="flex:1;min-width:120px;">
          <label>动物种类</label>
          <select v-model="filters.animalType">
            <option value="">全部</option>
            <option v-for="type in animalTypes" :key="type" :value="type">{{ type }}</option>
          </select>
        </div>
        <div class="form-field" style="flex:1;min-width:120px;">
          <label>工作区域</label>
          <select v-model="filters.workArea">
            <option value="">全部</option>
            <option v-for="area in areas" :key="area" :value="area">{{ area }}</option>
          </select>
        </div>
        <div class="form-field" style="flex:1;min-width:120px;">
          <label>所属部门</label>
          <select v-model="filters.department">
            <option value="">全部</option>
            <option v-for="dept in departments" :key="dept" :value="dept">{{ dept }}</option>
          </select>
        </div>
        <div class="form-field" style="flex:1;min-width:120px;">
          <label>工作状态</label>
          <select v-model="filters.status">
            <option value="">全部</option>
            <option value="在职">在职</option>
            <option value="休假">休假</option>
            <option value="已离职">已离职</option>
            <option value="待审批">待审批</option>
          </select>
        </div>
      </div>
      <div>
        <table style="width:100%;font-size:13px;border-collapse:collapse;text-align:center;">
          <thead>
            <tr>
              <th style="padding:8px;border-bottom:2px solid #e0e7f5;">姓名</th>
              <th style="padding:8px;border-bottom:2px solid #e0e7f5;">账号</th>
              <th style="padding:8px;border-bottom:2px solid #e0e7f5;">动物种类</th>
              <th style="padding:8px;border-bottom:2px solid #e0e7f5;">工作区域</th>
              <th style="padding:8px;border-bottom:2px solid #e0e7f5;">所属部门</th>
              <th style="padding:8px;border-bottom:2px solid #e0e7f5;">薪资</th>
              <th style="padding:8px;border-bottom:2px solid #e0e7f5;">状态</th>
              <th style="padding:8px;border-bottom:2px solid #e0e7f5;">操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="w in filteredWorkers" :key="w.id" style="border-bottom:1px solid #f1f1f1;">
              <td style="padding:8px;">{{ w.name }}</td>
              <td style="padding:8px;">{{ w.username }}</td>
              <td style="padding:8px;">{{ w.animalType || w.animal_type }}</td>
              <td style="padding:8px;" data-field="work_area">
                <span v-if="!editing[w.id]" class="field-value">{{ w.workArea || w.work_area || '-' }}</span>
                <select v-else v-model="editData[w.id].workArea" class="field-edit" style="width:100%;">
                  <option value="">请选择</option>
                  <option v-for="area in areas" :key="area" :value="area">{{ area }}</option>
                </select>
              </td>
              <td style="padding:8px;" data-field="department">
                <span v-if="!editing[w.id]" class="field-value">{{ w.department || '-' }}</span>
                <select v-else v-model="editData[w.id].department" class="field-edit" style="width:100%;">
                  <option value="">请选择</option>
                  <option v-for="dept in departments" :key="dept" :value="dept">{{ dept }}</option>
                </select>
              </td>
              <td style="padding:8px;" data-field="salary">
                <span v-if="!editing[w.id]" class="field-value">{{ w.salary != null ? w.salary : '-' }}</span>
                <input
                  v-else
                  v-model.number="editData[w.id].salary"
                  type="number"
                  class="field-edit"
                  style="width:100%;padding:2px;"
                  placeholder="输入薪资"
                />
              </td>
              <td style="padding:8px;">{{ w.employmentStatus || w.employment_status || '-' }}</td>
              <td style="padding:8px;">
                <template v-if="!editing[w.id]">
                  <button
                    v-if="w.employmentStatus !== '已离职' && w.employment_status !== '已离职'"
                    class="btn secondary"
                    @click="startEdit(w)"
                    style="font-size:12px;padding:4px 8px;margin-right:4px;"
                  >
                    编辑
                  </button>
                  <button
                    v-if="w.employmentStatus === '待审批' || w.employment_status === '待审批'"
                    class="btn"
                    @click="approveWorker(w)"
                    style="font-size:12px;padding:4px 8px;margin-right:4px;"
                  >
                    审批通过
                  </button>
                  <button
                    v-if="w.employmentStatus !== '已离职' && w.employment_status !== '已离职'"
                    class="btn secondary"
                    @click="fireWorker(w)"
                    style="font-size:12px;padding:4px 8px;"
                  >
                    开除
                  </button>
                  <button
                    v-if="(w.employmentStatus === '休假' || w.employment_status === '休假') && !hasPendingLeaveRequest(w.id)"
                    class="btn"
                    @click="endLeave(w)"
                    style="font-size:12px;padding:4px 8px;margin-left:4px;"
                  >
                    要求结束休假
                  </button>
                  <button
                    v-if="(w.employmentStatus === '休假' || w.employment_status === '休假') && hasPendingLeaveRequest(w.id)"
                    class="btn secondary"
                    disabled
                    style="font-size:12px;padding:4px 8px;margin-left:4px;"
                  >
                    已通知
                  </button>
                </template>
                <template v-else>
                  <button class="btn" @click="saveEdit(w)" style="font-size:12px;padding:4px 8px;margin-right:4px;">保存</button>
                  <button class="btn secondary" @click="cancelEdit(w)" style="font-size:12px;padding:4px 8px;margin-right:4px;">取消</button>
                </template>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
      <div style="margin-top:16px;text-align:right;">
        <button class="btn secondary" @click="showApplications = true" style="font-size:13px;">查看员工申请</button>
      </div>
    </div>

    <!-- 员工申请列表弹窗 -->
    <div v-if="showApplications" class="modal-overlay" @click.self="showApplications = false">
      <div class="modal-card">
        <div class="modal-header">
          <div class="title" style="font-size:18px;">员工申请列表</div>
          <button class="modal-close" @click="showApplications = false">&times;</button>
        </div>
        <div>
          <table v-if="applications.length" style="width:100%;font-size:13px;border-collapse:collapse;text-align:center;">
            <thead>
              <tr>
                <th style="padding:8px;border-bottom:2px solid #e0e7f5;">员工</th>
                <th style="padding:8px;border-bottom:2px solid #e0e7f5;">类型</th>
                <th style="padding:8px;border-bottom:2px solid #e0e7f5;">说明</th>
                <th style="padding:8px;border-bottom:2px solid #e0e7f5;">状态</th>
                <th style="padding:8px;border-bottom:2px solid #e0e7f5;">操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="a in applications" :key="a.id" style="border-bottom:1px solid #f1f1f1;">
                <td style="padding:8px;">{{ a.name || a.username || a.workerId || a.worker_id }}</td>
                <td style="padding:8px;">{{ a.type }}</td>
                <td style="padding:8px;">{{ bufferToString(a.reason) || '-' }}</td>
                <td style="padding:8px;">{{ a.status }}</td>
                <td style="padding:8px;">
                  <template v-if="a.status === '待审批'">
                    <button class="btn" @click="approveApplication(a)" style="font-size:12px;padding:4px 8px;">通过</button>
                    <button class="btn secondary" @click="rejectApplication(a)" style="font-size:12px;padding:4px 8px;">拒绝</button>
                  </template>
                  <span v-else>-</span>
                </td>
              </tr>
            </tbody>
          </table>
          <div v-else style="color:#999;text-align:center;padding:20px;">暂无申请</div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import api from '@/utils/api'
import { ANIMAL_TYPES, AREAS, DEPARTMENTS, bufferToString } from '@/utils/constants'

const workers = ref([])
const applications = ref([])
const leaveRequests = ref([])
const filters = ref({
  name: '',
  animalType: '',
  workArea: '',
  department: '',
  status: ''
})
const editing = ref({})
const editData = ref({})
const showApplications = ref(false)

const animalTypes = ANIMAL_TYPES
const areas = AREAS
const departments = DEPARTMENTS

const filteredWorkers = computed(() => {
  return workers.value.filter(w => {
    const matchName = !filters.value.name || (w.name || '').toLowerCase().includes(filters.value.name.toLowerCase())
    const matchAnimal = !filters.value.animalType || (w.animalType || w.animal_type) === filters.value.animalType
    const matchArea = !filters.value.workArea || (w.workArea || w.work_area) === filters.value.workArea
    const matchDept = !filters.value.department || w.department === filters.value.department
    const matchStatus = !filters.value.status || (w.employmentStatus || w.employment_status) === filters.value.status
    return matchName && matchAnimal && matchArea && matchDept && matchStatus
  })
})

onMounted(async () => {
  await loadData()
})

watch(showApplications, async (val) => {
  if (val) {
    await loadApplications()
  }
})

async function loadData() {
  try {
    const [workersRes, appsRes, leaveRes] = await Promise.all([
      api.get('/admin/workers'),
      api.get('/admin/worker-applications'),
      api.get('/admin/leave-requests')
    ])
    workers.value = workersRes.data.data
    applications.value = appsRes.data.data
    leaveRequests.value = leaveRes.data.data
  } catch (error) {
    console.error('加载失败:', error)
  }
}

async function loadApplications() {
  try {
    const res = await api.get('/admin/worker-applications')
    applications.value = res.data.data
  } catch (error) {
    console.error('加载申请失败:', error)
  }
}

function hasPendingLeaveRequest(workerId) {
  // 只检查当前员工状态为"休假"时的待处理请求
  const worker = workers.value.find(w => (w.id === workerId))
  if (!worker || (worker.employmentStatus !== '休假' && worker.employment_status !== '休假')) {
    return false
  }
  // 检查是否有管理员发起的、状态为"待员工确认"的请求
  return leaveRequests.value.some(r => 
    (r.workerId === workerId || r.worker_id === workerId) &&
    r.initiator === 'admin' && 
    r.status === '待员工确认'
  )
}

function startEdit(worker) {
  editing.value[worker.id] = true
  editData.value[worker.id] = {
    workArea: worker.workArea || worker.work_area || '',
    department: worker.department || '',
    salary: worker.salary
  }
  // 禁用其他行的编辑按钮
  Object.keys(editing.value).forEach(id => {
    if (id !== String(worker.id)) {
      editing.value[id] = false
    }
  })
}

function cancelEdit(worker) {
  editing.value[worker.id] = false
  delete editData.value[worker.id]
}

async function saveEdit(worker) {
  const data = editData.value[worker.id]
  const updateData = {}
  if (data.workArea !== '') updateData.work_area = data.workArea
  if (data.department !== '') updateData.department = data.department
  if (data.salary !== null && data.salary !== undefined) updateData.salary = data.salary
  
  if (Object.keys(updateData).length === 0) {
    alert('没有需要更新的信息')
    return
  }
  
  try {
    await api.post(`/admin/workers/${worker.id}/update`, updateData)
    editing.value[worker.id] = false
    await loadData()
  } catch (error) {
    alert('更新失败：' + (error.response?.data?.message || '未知错误'))
  }
}

async function approveWorker(worker) {
  const salary = prompt('请输入该员工的初始薪资（豆币/月）：', '5000')
  if (!salary) return
  try {
    await api.post(`/admin/workers/${worker.id}/approve`, { salary: parseFloat(salary) || 0 })
    await loadData()
  } catch (error) {
    alert(error.response?.data?.message || '审批失败')
  }
}

async function fireWorker(worker) {
  if (!confirm('确认开除该员工？')) return
  try {
    await api.post(`/admin/workers/${worker.id}/fire`)
    await loadData()
  } catch (error) {
    alert(error.response?.data?.message || '操作失败')
  }
}

async function endLeave(worker) {
  try {
    await api.post(`/admin/leave-requests/${worker.id}`)
    alert('已通知该员工结束休假')
    await loadData()
  } catch (error) {
    alert(error.response?.data?.message || '操作失败')
  }
}

async function approveApplication(app) {
  try {
    let salary = null
    if (app.type === '涨薪申请') {
      const newSalary = prompt('请输入新的薪资（豆币/月）：', bufferToString(app.reason) || '')
      if (!newSalary) return
      salary = parseFloat(newSalary) || null
      await api.post(`/admin/workers/${app.workerId || app.worker_id}/update`, { salary })
    } else if (app.type === '休假申请') {
      await api.post(`/admin/workers/${app.workerId || app.worker_id}/update`, { employment_status: '休假' })
    } else if (app.type === '离职申请') {
      await api.post(`/admin/workers/${app.workerId || app.worker_id}/fire`)
    }
    await api.post(`/admin/worker-applications/${app.id}/approve`, {})
    await loadApplications()
    await loadData()
  } catch (error) {
    alert(error.response?.data?.message || '操作失败')
  }
}

async function rejectApplication(app) {
  try {
    await api.post(`/admin/worker-applications/${app.id}/reject`, {})
    await loadApplications()
    await loadData()
  } catch (error) {
    alert(error.response?.data?.message || '操作失败')
  }
}
</script>
