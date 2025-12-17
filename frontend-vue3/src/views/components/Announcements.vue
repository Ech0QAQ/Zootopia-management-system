<template>
  <div>
    <div class="card">
      <div class="title">市政公告</div>
      <div class="subtitle">交通 / 活动 / 政策，可按区域推送。紧急广播会悬浮在所有页面上方。</div>
    </div>

    <div v-if="canEdit" class="card">
      <div class="subtitle">发布公告</div>
      <div id="ann-alert"></div>
      <div class="form-row">
        <div class="form-field">
          <label>标题</label>
          <input v-model="announcementForm.title" id="ann-title" />
        </div>
        <div class="form-field">
          <label>栏目</label>
          <select v-model="announcementForm.category" id="ann-category">
            <option value="交通">交通</option>
            <option value="活动">活动</option>
            <option value="政策">政策</option>
          </select>
        </div>
      </div>
      <div class="form-row">
        <div class="form-field">
          <label>推送区域</label>
          <select v-model="announcementForm.area" id="ann-area">
            <option value="ALL">全城</option>
            <option v-for="area in areas" :key="area" :value="area">{{ area }}</option>
          </select>
        </div>
        <div class="form-field">
          <label>是否紧急广播</label>
          <select v-model="announcementForm.emergency" id="ann-emergency">
            <option :value="false">否</option>
            <option :value="true">是</option>
          </select>
        </div>
      </div>
      <div class="form-field">
        <label>内容</label>
        <textarea v-model="announcementForm.content" id="ann-content" rows="4"></textarea>
      </div>
      <button class="btn" @click="publishAnnouncement">发布</button>
    </div>

    <div class="card">
      <div class="subtitle">公告列表</div>
      <div v-if="loading" style="padding:20px;text-align:center;color:#999">加载中...</div>
      <div v-else-if="error" class="alert alert-error">{{ error }}</div>
      <div v-else>
        <div v-for="ann in filteredAnnouncements" :key="ann.id" style="border-bottom:1px solid #f1f1f1;padding:8px 0">
          <div>
            <strong>{{ ann.title }}</strong>
            <span class="tag">{{ ann.category }}</span>
            <span v-if="ann.emergency" class="tag" style="background:#ff4d4f;color:#fff;">紧急</span>
            <span v-if="ann.expired" class="tag" style="background:#999;color:#fff;">已过期</span>
          </div>
          <div style="font-size:13px;color:#666;margin-top:4px;" v-html="ann.content.replace(/\n/g, '<br>')"></div>
          <div style="font-size:12px;color:#999;margin-top:4px;">
            推送区域：{{ ann.area === 'ALL' ? '全城' : ann.area }} | 发布人：{{ ann.createdBy }}
          </div>
          
          <div v-if="canEdit" class="btn-group" style="margin-top:8px;">
            <button class="btn secondary" @click="toggleExpired(ann)">
              {{ ann.expired ? '设为未过期' : '设为已过期' }}
            </button>
            <button class="btn secondary" @click="deleteAnnouncement(ann)">删除</button>
          </div>

          <div v-if="!ann.emergency || ann.expired" style="margin-top:6px;">
            <div style="font-size:13px;color:#555;margin-bottom:4px;">评论：</div>
            <div v-if="comments[ann.id] && comments[ann.id].length > 0">
              <div v-for="comment in comments[ann.id]" :key="comment.id" style="font-size:13px;border-bottom:1px dashed #eee;padding:2px 0;">
                <strong>{{ comment.anonymous ? '匿名居民' : (comment.name || '未知用户') }}</strong>：{{ comment.content || '' }}
              </div>
            </div>
            <div v-else style="font-size:12px;color:#999;">暂无评论</div>
            <div v-if="userStore.user?.role === 'resident'" class="form-row" style="margin-top:4px;align-items:center;">
              <div class="form-field">
                <input v-model="commentInputs[ann.id]" placeholder="说点什么..." />
              </div>
              <div class="form-field" style="margin:0;display:flex;align-items:center;gap:6px;flex:0 0 auto;min-width:auto;width:auto;">
                <input type="checkbox" v-model="anonymousInputs[ann.id]" style="margin:0;" />
                <span style="font-size:12px;white-space:nowrap;">匿名</span>
              </div>
              <button class="btn" @click="submitComment(ann.id)">评论</button>
            </div>
          </div>
        </div>
        <div v-if="!filteredAnnouncements.length" style="font-size:12px;color:#999;">暂无符合条件的公告</div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { useUserStore } from '@/stores/user'
import api from '@/utils/api'
import { AREAS, bufferToString } from '@/utils/constants'

const userStore = useUserStore()
const announcements = ref([])
const comments = ref({})
const commentInputs = ref({})
const anonymousInputs = ref({})
const loading = ref(false)
const error = ref('')
const areas = AREAS
const announcementForm = ref({
  title: '',
  content: '',
  category: '交通',
  area: 'ALL',
  emergency: false
})

const canEdit = computed(() => {
  return userStore.user?.role === 'admin' || userStore.user?.role === 'worker'
})

const filteredAnnouncements = computed(() => {
  const userArea = userStore.user?.liveArea || 'ALL'
  return announcements.value.filter(a => {
    if (canEdit.value) return true
    return a.area === 'ALL' || a.area === userArea
  })
})

onMounted(async () => {
  await loadAnnouncements()
})

async function loadAnnouncements() {
  try {
    loading.value = true
    error.value = ''
    const res = await api.get('/announcements')
    announcements.value = res.data.data.map(a => ({
      ...a,
      title: bufferToString(a.title),
      content: bufferToString(a.content),
      category: bufferToString(a.category),
      createdBy: bufferToString(a.createdBy)
    }))
    
    // 加载所有公告的评论（非紧急公告或已过期的紧急公告）
    for (const ann of announcements.value) {
      if (!ann.emergency || ann.expired) {
        try {
          const commentsRes = await api.get(`/announcements/${ann.id}/comments`)
          if (commentsRes.data && commentsRes.data.data) {
            comments.value[ann.id] = commentsRes.data.data.map(c => ({
              ...c,
              name: bufferToString(c.name || ''),
              content: bufferToString(c.content || '')
            }))
          } else {
            comments.value[ann.id] = []
          }
        } catch (e) {
          console.error(`加载公告 ${ann.id} 的评论失败:`, e)
          comments.value[ann.id] = []
        }
      } else {
        // 紧急且未过期的公告不加载评论
        comments.value[ann.id] = []
      }
    }
  } catch (err) {
    error.value = err.message || '加载失败'
    console.error('加载公告失败:', err)
  } finally {
    loading.value = false
  }
}

async function publishAnnouncement() {
  const alertEl = document.getElementById('ann-alert')
  if (alertEl) alertEl.innerHTML = ''
  
  if (!announcementForm.value.title?.trim() || !announcementForm.value.content?.trim()) {
    if (alertEl) alertEl.innerHTML = '<div class="alert alert-error">请填写完整信息</div>'
    return
  }
  
  try {
    await api.post('/announcements', {
      ...announcementForm.value,
      emergency: announcementForm.value.emergency === true || announcementForm.value.emergency === 'true'
    })
    if (alertEl) alertEl.innerHTML = '<div class="alert alert-success">发布成功</div>'
    announcementForm.value = {
      title: '',
      content: '',
      category: '交通',
      area: 'ALL',
      emergency: false
    }
    await loadAnnouncements()
  } catch (err) {
    if (alertEl) alertEl.innerHTML = `<div class="alert alert-error">${err.message || '发布失败'}</div>`
  }
}

async function toggleExpired(ann) {
  try {
    await api.put(`/announcements/${ann.id}`, { expired: !ann.expired })
    await loadAnnouncements()
  } catch (err) {
    alert(err.message || '更新失败')
  }
}

async function deleteAnnouncement(ann) {
  if (!confirm('确定要删除这条公告吗？')) return
  try {
    await api.delete(`/announcements/${ann.id}`)
    await loadAnnouncements()
  } catch (err) {
    alert(err.message || '删除失败')
  }
}

async function submitComment(annId) {
  const content = commentInputs.value[annId]?.trim()
  if (!content) return
  try {
    await api.post(`/announcements/${annId}/comments`, {
      content,
      anonymous: anonymousInputs.value[annId] || false
    })
    commentInputs.value[annId] = ''
    anonymousInputs.value[annId] = false
    await loadAnnouncements()
  } catch (err) {
    alert(err.message || '评论失败')
  }
}
</script>
