<template>
  <div class="chat-history-page">
    <div class="page-header">
      <div class="container">
        <h2>客服聊天记录</h2>
      </div>
    </div>

    <div class="container history-body">
      <div v-if="loading" class="loading-state">
        <el-skeleton :rows="4" animated />
      </div>

      <div v-else-if="historyList.length === 0" class="empty-state">
        <el-empty description="暂无聊天记录" />
      </div>

      <div v-else class="history-list">
        <div
          v-for="(item, idx) in historyList"
          :key="item.id || idx"
          class="history-card"
          @click="expandSession(item)"
        >
          <div class="history-header">
            <span class="history-type">{{ item.shopName || '平台客服' }}</span>
            <span class="history-time">{{ formatDate(item.createTime) }}</span>
          </div>
          <div class="history-preview">{{ item.lastMessage || item.content || '暂无消息内容' }}</div>
          <div class="history-footer">
            <el-tag size="small" :type="item.status === 1 ? 'success' : 'info'">
              {{ item.status === 1 ? '进行中' : '已结束' }}
            </el-tag>
          </div>
        </div>
      </div>

      <!-- 会话详情对话框 -->
      <el-dialog
        v-model="detailVisible"
        :title="'与 ' + (currentSession?.shopName || '平台客服') + ' 的对话'"
        width="500px"
        top="5vh"
      >
        <div class="dialog-messages" ref="dialogMsgRef">
          <div
            v-for="(msg, idx) in sessionMessages"
            :key="msg.id || idx"
            :class="['dm-msg', msg.sendType === 1 ? 'self' : 'other']"
          >
            <div class="dm-msg-avatar">{{ msg.sendType === 1 ? '👤' : '🛡️' }}</div>
            <div class="dm-msg-content">
              <div class="dm-msg-bubble">{{ msg.content }}</div>
              <div class="dm-msg-time">{{ formatTime(msg.createTime) }}</div>
            </div>
          </div>
          <div v-if="sessionMessages.length === 0" class="dm-empty">暂无消息记录</div>
        </div>
      </el-dialog>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getChatHistory, getCustomerMessages } from '../../api/customerchat'

interface HistoryItem {
  id: number
  type: string
  shopId?: number
  shopName?: string
  content?: string
  lastMessage?: string
  status: number
  createTime: string
}

interface MsgItem {
  id: number
  sendType: number
  content: string
  createTime: string
}

const loading = ref(true)
const historyList = ref<HistoryItem[]>([])
const detailVisible = ref(false)
const currentSession = ref<HistoryItem | null>(null)
const sessionMessages = ref<MsgItem[]>([])
const dialogMsgRef = ref<HTMLElement | null>(null)

onMounted(async () => {
  try {
    const res: any = await getChatHistory()
    historyList.value = Array.isArray(res.data) ? res.data : []
  } catch {
    ElMessage.error('加载聊天记录失败')
  } finally {
    loading.value = false
  }
})

async function expandSession(item: HistoryItem) {
  currentSession.value = item
  detailVisible.value = true
  try {
    const res: any = await getCustomerMessages(item.id, item.type || 'merchant')
    sessionMessages.value = Array.isArray(res.data) ? res.data : []
  } catch {
    sessionMessages.value = []
  }
  setTimeout(() => {
    if (dialogMsgRef.value) dialogMsgRef.value.scrollTop = dialogMsgRef.value.scrollHeight
  }, 100)
}

function formatDate(t: string): string {
  if (!t) return ''
  try {
    const d = new Date(t)
    return `${d.getFullYear()}-${(d.getMonth() + 1).toString().padStart(2, '0')}-${d.getDate().toString().padStart(2, '0')} ${d.getHours().toString().padStart(2, '0')}:${d.getMinutes().toString().padStart(2, '0')}`
  } catch { return t }
}

function formatTime(t: string): string {
  if (!t) return ''
  try {
    const d = new Date(t)
    return `${d.getHours().toString().padStart(2, '0')}:${d.getMinutes().toString().padStart(2, '0')}`
  } catch { return '' }
}
</script>

<style scoped>
.container { width: 100%; max-width: 800px; margin: 0 auto; padding: 0 24px; box-sizing: border-box; }
.chat-history-page { min-height: 100vh; background: #f5f5f5; padding-bottom: 60px; }
.page-header { background: #fff; border-bottom: 1px solid #e8e8e8; }
.page-header .container { display: flex; align-items: center; height: 56px; }
.page-header h2 { margin: 0; font-size: 18px; }
.history-body { margin-top: 20px; }
.loading-state { background: #fff; border-radius: 8px; padding: 40px; }
.empty-state { background: #fff; border-radius: 8px; padding: 60px 0; }
.history-list { display: flex; flex-direction: column; gap: 12px; }
.history-card { background: #fff; border-radius: 8px; padding: 16px 20px; cursor: pointer; transition: all 0.2s; box-shadow: 0 1px 4px rgba(0,0,0,0.04); }
.history-card:hover { box-shadow: 0 2px 12px rgba(0,0,0,0.08); transform: translateY(-1px); }
.history-header { display: flex; justify-content: space-between; margin-bottom: 8px; }
.history-type { font-size: 14px; font-weight: 600; color: #333; }
.history-time { font-size: 12px; color: #999; }
.history-preview { font-size: 13px; color: #666; margin-bottom: 8px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.history-footer { display: flex; gap: 8px; }
.dialog-messages { max-height: 400px; overflow-y: auto; padding: 10px 0; }
.dm-msg { display: flex; gap: 8px; margin-bottom: 14px; align-items: flex-start; }
.dm-msg.self { flex-direction: row-reverse; }
.dm-msg.other { flex-direction: row; }
.dm-msg-avatar { width: 28px; height: 28px; border-radius: 50%; display: flex; align-items: center; justify-content: center; font-size: 14px; flex-shrink: 0; background: #e8eaed; }
.dm-msg-content { max-width: 72%; }
.dm-msg.self .dm-msg-bubble { background: #409eff; color: #fff; border-radius: 12px 12px 4px 12px; }
.dm-msg.other .dm-msg-bubble { background: #f0f0f0; color: #333; border-radius: 12px 12px 12px 4px; }
.dm-msg-bubble { padding: 8px 14px; font-size: 13px; line-height: 1.5; word-break: break-word; }
.dm-msg-time { font-size: 10px; color: #bbb; margin-top: 2px; text-align: right; }
.dm-empty { text-align: center; padding: 30px; color: #999; }
</style>