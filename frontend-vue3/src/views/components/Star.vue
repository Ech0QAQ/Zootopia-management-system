<template>
  <div>
    <div v-if="top3.length" class="card" style="margin-bottom:16px;background:linear-gradient(135deg,#fff7e5,#ffe1f0);">
      <div class="subtitle" style="font-size:15px;font-weight:600;margin-bottom:12px;">当前票数前三名</div>
      <div>
        <div style="display:flex;align-items:flex-end;justify-content:center;gap:20px;flex-wrap:wrap;">
          <div v-if="top3.length >= 2" style="text-align:center;order:1;">
            <img :src="`/photo/${top3[1].photo}`" style="width:140px;height:140px;object-fit:cover;border-radius:12px;margin-bottom:8px;border:3px solid #c0c0c0;" />
            <div style="font-size:16px;font-weight:600;color:#145a7b;">NO.2：{{ top3[1].name }}</div>
            <div style="font-size:14px;color:#666;">{{ top3[1].count }}票</div>
          </div>
          <div v-if="top3.length >= 1" style="text-align:center;order:2;">
            <img :src="`/photo/${top3[0].photo}`" style="width:180px;height:180px;object-fit:cover;border-radius:12px;margin-bottom:8px;border:3px solid #ffd700;" />
            <div style="font-size:18px;font-weight:600;color:#145a7b;">NO.1：{{ top3[0].name }}</div>
            <div style="font-size:15px;color:#666;">{{ top3[0].count }}票</div>
          </div>
          <div v-if="top3.length >= 3" style="text-align:center;order:3;">
            <img :src="`/photo/${top3[2].photo}`" style="width:120px;height:120px;object-fit:cover;border-radius:12px;margin-bottom:8px;border:3px solid #cd7f32;" />
            <div style="font-size:14px;font-weight:600;color:#145a7b;">NO.3：{{ top3[2].name }}</div>
            <div style="font-size:13px;color:#666;">{{ top3[2].count }}票</div>
          </div>
        </div>
      </div>
    </div>
    <div v-else style="font-size:14px;color:#999;text-align:center;padding:20px;">暂无投票</div>

    <div style="margin-bottom:16px;">
      <div v-if="hasVoted" class="alert alert-success" style="font-size:15px;padding:12px;">
        <strong>您已投票！</strong> 您投给了：<strong style="color:#145a7b;">{{ votedCandidate }}</strong>
      </div>
    </div>

    <div class="card">
      <div style="font-size:15px;font-weight:600;color:#555;margin-bottom:16px;">全部候选人{{ hasVoted ? '票数（柱状图）' : '投票' }}</div>
      <div style="display:flex;flex-direction:column;gap:12px;">
        <div v-if="!hasVoted">
          <div
            v-for="candidate in totalVotes"
            :key="candidate.id"
            style="display:flex;align-items:center;gap:12px;padding:12px;border:1px solid #e0e7f5;border-radius:8px;transition:background 0.2s;"
            @mouseenter="$event.target.style.background='#f5f8ff'"
            @mouseleave="$event.target.style.background='transparent'"
          >
            <img :src="`/photo/${candidate.photo}`" style="width:60px;height:60px;object-fit:cover;border-radius:6px;" />
            <span style="font-size:15px;font-weight:500;flex:1;">{{ candidate.name }}</span>
            <button class="btn secondary" @click="vote(candidate.id)" style="font-size:13px;">投票给：{{ candidate.name }}</button>
          </div>
        </div>
        <div v-else>
          <div v-for="candidate in totalVotes" :key="candidate.id">
            <div style="display:flex;align-items:center;gap:12px;margin-bottom:4px;">
              <img :src="`/photo/${candidate.photo}`" style="width:50px;height:50px;object-fit:cover;border-radius:6px;" />
              <span style="font-size:14px;font-weight:500;min-width:60px;">{{ candidate.name }}</span>
              <div style="flex:1;background:#e0e7f5;height:32px;border-radius:6px;position:relative;overflow:hidden;">
                <div
                  :style="{
                    background: 'linear-gradient(90deg,#145a7b,#1a7ba8)',
                    height: '100%',
                    width: `${(candidate.count / maxVotes) * 100}%`,
                    transition: 'width 0.3s',
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'flex-end',
                    paddingRight: '8px'
                  }"
                >
                  <span style="color:#fff;font-size:13px;font-weight:600;">{{ candidate.count }}票</span>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import api from '@/utils/api'

const candidates = ref([])
const stats = ref([])
const myVote = ref(null)

const statsMap = computed(() => {
  const map = {}
  stats.value.forEach(s => { map[s.id] = s })
  return map
})

const totalVotes = computed(() => {
  return candidates.value.map(c => ({
    id: c.id,
    name: c.name,
    photo: c.photo,
    count: statsMap.value[c.id]?.votes || 0
  })).sort((a, b) => b.count - a.count)
})

const top3 = computed(() => totalVotes.value.slice(0, 3))
const maxVotes = computed(() => Math.max(...totalVotes.value.map(v => v.count), 1))
const hasVoted = computed(() => !!myVote.value)
const votedCandidate = computed(() => myVote.value?.name || null)

onMounted(async () => {
  await loadData()
})

async function loadData() {
  try {
    const [candidatesRes, statsRes, voteRes] = await Promise.all([
      api.get('/star/candidates'),
      api.get('/star/stats'),
      api.get('/star/my-vote')
    ])
    candidates.value = candidatesRes.data.data
    stats.value = statsRes.data.data
    myVote.value = voteRes.data.data
  } catch (error) {
    console.error('加载失败:', error)
  }
}

async function vote(candidateId) {
  try {
    await api.post('/star/vote', { candidate_id: candidateId })
    await loadData()
  } catch (error) {
    alert(error.response?.data?.message || '投票失败')
  }
}
</script>
