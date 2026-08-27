<template>
  <div class="pc-page">
    <div class="pc-window">
      <div class="pc-header">
        <div class="pc-header-left">
          <span class="pc-icon">🛡️</span>
          <div>
            <span>平台官方客服</span>
            <span :class="['pc-status-tag', adminOnline ? 'online' : 'offline']">
              <span class="pc-status-dot"></span>
              {{ adminOnline ? '客服在线' : '客服离线' }}
            </span>
          </div>
        </div>
        <span class="pc-close" @click="goBack">✕</span>
      </div>

      <div class="pc-body" ref="msgRef">
        <div v-if="!adminOnline && !connecting" class="pc-offline-tip">
          <span class="pc-offline-icon">⏳</span> 平台客服离线，消息已留存，工作人员上线后回复
        </div>
        <div v-if="messages.length === 0 && !connecting" class="pc-empty">
          <p>🛡️ 平台官方客服</p>
          <p class="pc-empty-sub">平台规则、商家纠纷、退款仲裁、账号问题</p>
        </div>
        <template v-for="(item, idx) in timedMessages" :key="'m-' + idx">
            <div v-if="item.type === 'time'" class="pc-time-separator">{{ item.label }}</div>
            <div v-else-if="item.role === 'system'" class="pc-system-msg">{{ item.content }}</div>
            <div v-else :class="['pc-msg', item.role === 'user' ? 'right' : 'left']">
              <div class="pc-msg-avatar">{{ item.role === 'user' ? '👤' : '🛡️' }}</div>
              <div class="pc-msg-bubble">
                <div class="pc-msg-content">{{ item.content }}</div>
              </div>
            </div>
          </template>
        <div v-if="connecting" class="pc-connecting">
          <span class="pc-connecting-dot"></span> 连接中...
        </div>
      </div>

      <div class="pc-input-area">
        <el-input
          v-model="inputText"
          :placeholder="adminOnline ? '输入消息...' : '客服离线，可留言...'"
          size="medium"
          @keyup.enter="sendMessage"
        />
        <el-button type="primary" size="medium" @click="sendMessage" :disabled="!inputText.trim()">
          发送
        </el-button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, nextTick, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()

interface ChatMsg {
  role: string
  content: string
  time?: string
}

const messages = ref<ChatMsg[]>([])
const inputText = ref('')
const msgRef = ref<HTMLElement | null>(null)
const connecting = ref(false)
const adminOnline = ref(false)
const sessionId = ref<number | null>(null)
const wsConnected = ref(false)

// QQ/微信风格时间分隔
const timedMessages = computed(() => {
  const result: any[] = []
  for (let i = 0; i < messages.value.length; i++) {
    const msg = messages.value[i]
    if (msg.role === 'system') {
      result.push({ type: 'msg', role: 'system', content: msg.content })
      continue
    }
    const shouldShowTime = i === 0 || (() => {
      const prev = messages.value[i - 1]
      if (prev.role === 'system') return true
      if (!prev.time || !msg.time) return true
      try {
        return new Date(msg.time).getTime() - new Date(prev.time).getTime() > 5 * 60 * 1000
      } catch { return true }
    })()
    if (shouldShowTime && msg.time) {
      result.push({ type: 'time', label: formatFullTime(msg.time) })
    }
    result.push({ type: 'msg', role: msg.role, content: msg.content })
  }
  return result
})

let ws: WebSocket | null = null
let reconnectTimer: ReturnType<typeof setTimeout> | null = null
let reconnectAttempts = 0
const MAX_RECONNECT = 5

const auth = useAuthStore()

function getUserId(): number | null {
  const user = auth.user || {}
  return user?.id || null
}

onMounted(() => { connectWs() })

onUnmounted(() => {
  disconnect()
  if (reconnectTimer) clearTimeout(reconnectTimer)
})

function goBack() {
  disconnect()
  router.back()
}

function connectWs() {
  const uid = getUserId()
  if (!uid) return

  if (ws && ws.readyState === WebSocket.OPEN && wsConnected.value) return

  connecting.value = true
  const baseUrl = import.meta.env.VITE_WS_URL || `ws://${location.host}`
  const wsUrl = `${baseUrl}/api/ws/platform-cs?uid=${uid}&type=user`

  try {
    ws = new WebSocket(wsUrl)
    ws.onopen = () => {
      connecting.value = false
      wsConnected.value = true
      reconnectAttempts = 0
    }
    ws.onmessage = (event) => {
      try {
        const data = JSON.parse(event.data)
        handleWsMessage(data)
      } catch { /* ignore */ }
    }
    ws.onclose = () => {
      wsConnected.value = false
      connecting.value = false
      adminOnline.value = false
      scheduleReconnect()
    }
    ws.onerror = () => {
      wsConnected.value = false
      connecting.value = false
    }
  } catch {
    connecting.value = false
    wsConnected.value = false
  }
}

function scheduleReconnect() {
  if (reconnectTimer) return
  if (reconnectAttempts >= MAX_RECONNECT) return
  reconnectAttempts++
  reconnectTimer = setTimeout(() => {
    reconnectTimer = null
    connectWs()
  }, 3000)
}

function handleWsMessage(data: any) {
  switch (data.type) {
    case 'connected':
      sessionId.value = data.sessionId
      adminOnline.value = data.online === true
      if (data.message) {
        messages.value.push({ role: 'system', content: data.message })
      }
      scrollBottom()
      break
    case 'history':
      if (data.messages) {
        for (const m of data.messages) {
          messages.value.push({
            role: m.sendType === 1 ? 'user' : 'admin',
            content: m.content,
            time: m.createTime,
          })
        }
      }
      scrollBottom()
      break
    case 'message':
      if (data.sendType === 2) {
        // 管理员发来的消息
        messages.value.push({
          role: 'admin',
          content: data.content,
          time: formatTime(data.createTime),
        })
        scrollBottom()
      }
      break
    case 'admin_online':
      adminOnline.value = true
      break
    case 'admin_offline':
      adminOnline.value = false
      break
  }
}

function disconnect() {
  if (ws) {
    ws.onclose = null
    ws.close()
    ws = null
  }
  wsConnected.value = false
  adminOnline.value = false
  connecting.value = false
}

function sendMessage() {
  const text = inputText.value.trim()
  if (!text) return

  messages.value.push({ role: 'user', content: text, time: new Date().toISOString() })
  inputText.value = ''
  scrollBottom()

  if (ws && ws.readyState === WebSocket.OPEN && sessionId.value) {
    ws.send(JSON.stringify({ type: 'message', sessionId: sessionId.value, content: text }))
  }
}

function scrollBottom() {
  nextTick(() => { if (msgRef.value) msgRef.value.scrollTop = msgRef.value.scrollHeight })
}

function formatTime(t: string): string {
  try {
    const d = new Date(t)
    if (isNaN(d.getTime())) return ''
    return `${d.getHours().toString().padStart(2, '0')}:${d.getMinutes().toString().padStart(2, '0')}`
  } catch { return '' }
}

function formatFullTime(t: string): string {
  try {
    const d = new Date(t)
    if (isNaN(d.getTime())) return ''
    const pad = (n: number) => String(n).padStart(2, '0')
    const now = new Date()
    const today = new Date(now.getFullYear(), now.getMonth(), now.getDate())
    const date = new Date(d.getFullYear(), d.getMonth(), d.getDate())
    const diffDays = (today.getTime() - date.getTime()) / (24 * 60 * 60 * 1000)
    const time = `${pad(d.getHours())}:${pad(d.getMinutes())}`
    if (diffDays < 1) return time
    if (diffDays < 2) return `昨天 ${time}`
    if (diffDays < 7) return `${pad(d.getMonth() + 1)}/${pad(d.getDate())} ${time}`
    return `${d.getFullYear()}/${pad(d.getMonth() + 1)}/${pad(d.getDate())} ${time}`
  } catch { return '' }
}
</script>

<style scoped>
.pc-page {
  width: 100%;
  height: calc(100vh - 60px);
  display: flex;
  flex-direction: column;
  background: #f5f5f5;
  box-sizing: border-box;
}
.pc-window {
  width: 100%;
  height: 100%;
  background: #fff;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}
.pc-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 20px;
  background: linear-gradient(135deg, #409eff, #337ecc);
  color: #fff;
  font-size: 15px;
  font-weight: 600;
  flex-shrink: 0;
}
.pc-header-left { display: flex; align-items: center; gap: 10px; }
.pc-icon {
  width: 30px; height: 30px;
  background: rgba(255,255,255,0.2);
  border-radius: 8px;
  display: flex; align-items: center; justify-content: center;
  font-size: 16px;
  flex-shrink: 0;
}
.pc-status-tag {
  display: inline-flex; align-items: center; gap: 4px;
  font-size: 11px; font-weight: 400;
  padding: 2px 8px; border-radius: 10px;
  margin-left: 8px;
}
.pc-status-tag.online { color: #67c23a; background: rgba(103,194,58,0.15); }
.pc-status-tag.offline { color: #909399; background: rgba(144,147,153,0.15); }
.pc-status-dot { width: 6px; height: 6px; border-radius: 50%; display: inline-block; }
.pc-status-tag.online .pc-status-dot { background: #67c23a; }
.pc-status-tag.offline .pc-status-dot { background: #909399; }
.pc-close { cursor: pointer; font-size: 18px; opacity: 0.7; }
.pc-close:hover { opacity: 1; }
.pc-body { flex: 1; overflow-y: auto; padding: 16px; background: #f7f8fa; }
.pc-offline-tip {
  text-align: center; padding: 10px 12px; margin-bottom: 12px;
  background: #fff7e6; border: 1px solid #ffd591; border-radius: 6px;
  font-size: 12px; color: #d46b08;
  display: flex; align-items: center; justify-content: center; gap: 6px;
}
.pc-offline-icon { font-size: 14px; }
.pc-empty { text-align: center; padding: 40px 20px; color: #999; font-size: 13px; }
.pc-empty-sub { font-size: 12px; color: #bbb; margin-top: 4px; }
.pc-msg { display: flex; gap: 8px; margin-bottom: 14px; align-items: flex-start; }
.pc-msg.right { flex-direction: row-reverse; }
.pc-msg.left { flex-direction: row; }
.pc-msg-avatar { width: 32px; height: 32px; border-radius: 50%; background: #e8eaed; display: flex; align-items: center; justify-content: center; font-size: 16px; flex-shrink: 0; }
.pc-msg-bubble { max-width: 75%; padding: 8px 14px; border-radius: 12px; font-size: 13px; line-height: 1.5; word-break: break-word; }
.pc-msg.right .pc-msg-bubble { background: #409eff; color: #fff; border-radius: 12px 12px 4px 12px; }
.pc-msg.left .pc-msg-bubble { background: #fff; border: 1px solid #e8eaed; border-radius: 12px 12px 12px 4px; }
.pc-msg-content { white-space: pre-wrap; }
.pc-time-separator {
  text-align: center;
  padding: 8px 0;
  margin: 4px 0;
  font-size: 11px;
  color: #b2b2b2;
  user-select: none;
}
.pc-system-msg {
  text-align: center;
  padding: 6px 0;
  font-size: 12px;
  color: #999;
}
.pc-connecting {
  text-align: center; padding: 20px; color: #999; font-size: 13px;
  display: flex; align-items: center; justify-content: center; gap: 6px;
}
.pc-connecting-dot {
  width: 8px; height: 8px; border-radius: 50%;
  background: #409eff;
  animation: dot-blink 1.2s infinite;
}
@keyframes dot-blink {
  0%, 100% { opacity: 0.3; }
  50% { opacity: 1; }
}
.pc-input-area { display: flex; gap: 8px; padding: 12px 16px; border-top: 1px solid #eee; flex-shrink: 0; }
.pc-input-area .el-input { flex: 1; }
</style>