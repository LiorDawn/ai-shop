<template>
  <Teleport to="body">
    <Transition name="pc-fade">
      <div v-if="visible" class="pc-overlay" @click.self="closeChat">
        <div class="pc-window">
          <div class="pc-header">
            <span>平台官方客服</span>
            <span class="pc-close" @click="closeChat">✕</span>
          </div>

          <div class="pc-status-bar">
            <span :class="['pc-dot', adminOnline ? 'green' : 'gray']"></span>
            {{ adminOnline ? '平台客服在线' : '平台客服离线，消息已留存，工作人员上线后回复' }}
          </div>

          <div class="pc-body" ref="msgRef">
            <div v-if="messages.length === 0 && !connecting" class="pc-empty">
              <p>您好，欢迎联系平台官方客服</p>
              <p style="font-size:12px;color:#bbb;margin-top:4px;">平台规则 · 商家纠纷 · 退款仲裁 · 账号问题</p>
            </div>

            <div v-for="(item, idx) in timedMessages" :key="idx">
              <div v-if="item.type === 'time'" class="pc-time-separator">{{ item.label }}</div>
              <div v-else :class="['pc-msg', item.sendType === 1 ? 'right' : 'left']">
                <div class="pc-msg-avatar">{{ item.sendType === 1 ? '👤' : '🛡️' }}</div>
                <div class="pc-msg-body">
                  <div class="pc-msg-bubble">
                    <div class="pc-msg-content">{{ item.content }}</div>
                  </div>
                </div>
              </div>
            </div>

            <div v-if="connecting" class="pc-connecting">连接平台客服中...</div>
          </div>

          <div class="pc-input-area">
            <el-input
              v-model="inputText"
              placeholder="输入消息..."
              size="medium"
              @keyup.enter="sendMessage"
              :disabled="false"
            />
            <el-button type="primary" size="medium" @click="sendMessage"
              :disabled="!inputText.trim()">发送</el-button>
          </div>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted, onUnmounted, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useWebSocket } from '../../composables/useWebSocket'
import { useAuthStore } from '@/stores/auth'

interface Msg {
  id?: number
  sessionId?: number
  sendType: number
  sendId: number
  content: string
  createTime?: string
}

const visible = ref(false)
const messages = ref<Msg[]>([])
const inputText = ref('')
const adminOnline = ref(false)
const sessionId = ref<number | undefined>(undefined)
const msgRef = ref<HTMLElement | null>(null)
const pendingMessages = ref<string[]>([])

// ===== WebSocket 连接管理 =====
const { connected: wsConnected, connecting, connect, disconnect, send, onMessage } = useWebSocket()

const timedMessages = computed(() => {
  const result: any[] = []
  for (let i = 0; i < messages.value.length; i++) {
    const msg = messages.value[i]
    const shouldShowTime = i === 0 || (() => {
      const prev = messages.value[i - 1]
      if (!prev.createTime || !msg.createTime) return true
      try {
        return new Date(msg.createTime).getTime() - new Date(prev.createTime).getTime() > 5 * 60 * 1000
      } catch { return true }
    })()
    if (shouldShowTime && msg.createTime) {
      result.push({ type: 'time', label: formatFullTime(msg.createTime) })
    }
    result.push({ type: 'msg', sendType: msg.sendType, content: msg.content })
  }
  return result
})

const router = useRouter()

onMounted(() => {
  window.addEventListener('open-platform-cs', onOpenEvent)
  // 注册 WS 消息回调
  onMessage((data) => handleMessage(data))
})

onUnmounted(() => {
  disconnect()
  window.removeEventListener('open-platform-cs', onOpenEvent)
})

function onOpenEvent() {
  open()
}

const auth = useAuthStore()

function getUserId(): number | null {
  const user = auth.user || {}
  return user?.id || null
}

function open() {
  const uid = getUserId()
  if (!uid) {
    ElMessage.warning('请先登录')
    router.push('/login')
    return
  }
  visible.value = true
  connectWs()
}

function closeChat() {
  visible.value = false
  disconnect()
  if (sessionId.value) {
    send(JSON.stringify({ type: 'close_session', sessionId: sessionId.value }))
  }
}

function connectWs() {
  const uid = getUserId()
  if (!uid) return

  const baseUrl = import.meta.env.VITE_WS_URL || `ws://${location.host}`
  const wsUrl = `${baseUrl}/api/ws/platform-cs?uid=${uid}&type=user`
  connect(wsUrl)
}

function handleMessage(data: any) {
  switch (data.type) {
    case 'connected':
      sessionId.value = data.sessionId
      adminOnline.value = data.online === true
      if (data.message) {
        messages.value.push({ sendType: 0, sendId: 0, content: data.message, createTime: new Date().toISOString() })
      }
      flushPendingMessages()
      scrollBottom()
      break
    case 'history':
      if (data.messages) {
        messages.value = data.messages.map((m: any) => ({
          id: m.id, sessionId: m.sessionId, sendType: m.sendType,
          sendId: m.sendId, content: m.content, createTime: m.createTime,
        }))
      }
      scrollBottom()
      break
    case 'message':
      // 跳过服务器回显的自己发的消息（避免重复），只添加客服回复
      if (data.sendType !== 1) {
        messages.value.push({
          id: data.messageId, sessionId: data.sessionId, sendType: data.sendType,
          sendId: data.sendId, content: data.content, createTime: data.createTime || new Date().toISOString(),
        })
      }
      scrollBottom()
      break
    case 'error':
      ElMessage.error(data.message || '连接异常')
      break
    case 'admin_offline':
      adminOnline.value = false
      messages.value.push({ sendType: 0, sendId: 0, content: data.message || '平台客服已下线，消息已留存', createTime: new Date().toISOString() })
      scrollBottom()
      break
    case 'admin_online':
      adminOnline.value = true
      messages.value.push({ sendType: 0, sendId: 0, content: data.message || '平台客服已上线，可以继续咨询', createTime: new Date().toISOString() })
      scrollBottom()
      break
  }
}

function sendMessage() {
  const text = inputText.value.trim()
  if (!text) return

  messages.value.push({ sendType: 1, sendId: 0, content: text, createTime: new Date().toISOString() })
  inputText.value = ''
  scrollBottom()

  if (!wsConnected.value || !sessionId.value) {
    pendingMessages.value.push(text)
    return
  }

  send(JSON.stringify({ type: 'message', sessionId: sessionId.value, content: text, msgType: 1 }))
}

function flushPendingMessages() {
  if (pendingMessages.value.length === 0) return
  const queue = pendingMessages.value.slice()
  pendingMessages.value = []
  for (const text of queue) {
    if (wsConnected.value && sessionId.value) {
      send(JSON.stringify({ type: 'message', sessionId: sessionId.value, content: text, msgType: 1 }))
    } else {
      pendingMessages.value.push(text)
      break
    }
  }
}

function scrollBottom() {
  nextTick(() => { if (msgRef.value) msgRef.value.scrollTop = msgRef.value.scrollHeight })
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

function formatTime(t: string | undefined | null): string {
  if (!t) return ''
  try {
    const d = new Date(t)
    if (isNaN(d.getTime())) return ''
    const pad = (n: number) => String(n).padStart(2, '0')
    const now = new Date()
    if (d.toDateString() === now.toDateString()) return `${pad(d.getHours())}:${pad(d.getMinutes())}`
    return `${pad(d.getMonth() + 1)}/${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
  } catch { return '' }
}
</script>

<style scoped>
.pc-overlay {
  position: fixed; inset: 0;
  background: rgba(0,0,0,0.35);
  z-index: 10001;
  display: flex;
  align-items: center;
  justify-content: center;
}
.pc-window {
  width: 400px; height: 520px;
  background: #fff;
  border-radius: 12px;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  box-shadow: 0 8px 40px rgba(0,0,0,0.18);
}
.pc-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 20px;
  background: linear-gradient(135deg, #434343, #1a1a2e);
  color: #fff;
  font-size: 15px;
  font-weight: 600;
}
.pc-close { cursor: pointer; font-size: 18px; opacity: 0.7; }
.pc-close:hover { opacity: 1; }
.pc-status-bar {
  padding: 6px 16px;
  font-size: 12px;
  background: #f7f8fa;
  color: #666;
  display: flex;
  align-items: center;
  gap: 6px;
}
.pc-dot {
  width: 8px; height: 8px;
  border-radius: 50%;
  display: inline-block;
}
.pc-dot.green { background: #67c23a; }
.pc-dot.gray { background: #bfbfbf; }
.pc-body {
  flex: 1;
  overflow-y: auto;
  padding: 16px;
  background: #f7f8fa;
}
.pc-empty { text-align: center; padding: 60px 20px; color: #999; font-size: 13px; }
.pc-msg { display: flex; width: 100%; gap: 8px; margin-bottom: 16px; align-items: flex-start; box-sizing: border-box; }
.pc-msg.right { flex-direction: row-reverse; justify-content: flex-start; }
.pc-msg.left { flex-direction: row; justify-content: flex-start; }
.pc-msg-avatar {
  width: 36px; height: 36px;
  border-radius: 50%;
  display: flex; align-items: center; justify-content: center;
  font-size: 16px; flex-shrink: 0; background: #e8eaed;
  border: 1px solid #ddd;
  box-sizing: border-box;
}
.pc-msg-body {
  max-width: 72%;
  display: flex;
  flex-direction: column;
  min-width: 0;
}
.pc-msg.right .pc-msg-body { align-items: flex-end; }
.pc-msg.left .pc-msg-body { align-items: flex-start; }

/* ── QQ 风格气泡 ── */
.pc-msg-bubble {
  position: relative;
  padding: 10px 14px;
  font-size: 13px;
  line-height: 1.5;
  word-break: break-word;
  white-space: pre-wrap;
}
.pc-msg.right .pc-msg-bubble {
  background: linear-gradient(135deg, #667eea, #764ba2);
  color: #fff;
  border-radius: 12px 4px 12px 12px;
}
.pc-msg.right .pc-msg-bubble::after {
  content: '';
  position: absolute;
  top: 12px;
  right: -6px;
  width: 0;
  height: 0;
  border: 6px solid transparent;
  border-left-color: #764ba2;
  border-right: 0;
}
.pc-msg.left .pc-msg-bubble {
  background: #fff;
  color: #333;
  border: 1px solid #e8e8e8;
  border-radius: 4px 12px 12px 12px;
}
.pc-msg.left .pc-msg-bubble::before {
  content: '';
  position: absolute;
  top: 12px;
  left: -7px;
  width: 0;
  height: 0;
  border: 6px solid transparent;
  border-right-color: #e8e8e8;
  border-left: 0;
}
.pc-msg.left .pc-msg-bubble::after {
  content: '';
  position: absolute;
  top: 13px;
  left: -5px;
  width: 0;
  height: 0;
  border: 5px solid transparent;
  border-right-color: #fff;
  border-left: 0;
}
.pc-msg-content { padding: 0; font-size: 13px; line-height: 1.5; word-break: break-word; white-space: pre-wrap; }
.pc-msg-time { display: none; }
.pc-time-separator {
  text-align: center;
  padding: 8px 0;
  margin: 4px 0 8px;
  font-size: 11px;
  color: #b2b2b2;
  user-select: none;
}
.pc-connecting { text-align: center; color: #999; font-size: 12px; padding: 8px; }
.pc-input-area {
  display: flex; gap: 8px;
  padding: 10px 16px;
  border-top: 1px solid #eee;
  background: #fff;
}
.pc-input-area .el-input { flex: 1; }

.pc-fade-enter-active { transition: opacity 0.2s; }
.pc-fade-enter-active .pc-window { animation: pc-in 0.25s ease; }
.pc-fade-leave-active { transition: opacity 0.2s; }
.pc-fade-leave-active .pc-window { animation: pc-in 0.2s ease reverse; }
.pc-fade-enter-from, .pc-fade-leave-to { opacity: 0; }
@keyframes pc-in {
  from { transform: translateY(30px) scale(0.95); opacity: 0; }
  to { transform: translateY(0) scale(1); opacity: 1; }
}
</style>