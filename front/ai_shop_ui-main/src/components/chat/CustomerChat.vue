<template>
  <div class="cc-page">
    <HeaderUser />
    <div class="cc-window">
      <div class="cc-layout">
        <!-- 左侧会话列表 -->
        <div class="cc-sidebar">
          <div class="cc-sidebar-header">
            <span>我的聊天</span>
            <span class="cc-refresh" @click="loadSessions">↻</span>
          </div>
          <div class="cc-session-list">
            <div class="cc-group-label">商家对话</div>
            <template v-for="s in sessions" :key="s.id">
              <div
                :class="['cc-session-item', activeSession && activeSession.id === s.id ? 'active' : '']"
                @click="selectSession(s)"
              >
                <div class="cc-session-icon">{{ getIcon(s) }}</div>
                <div class="cc-session-info">
                  <div class="cc-session-title">{{ s.shopName || '店铺客服' }}</div>
                  <div class="cc-session-last">{{ s.lastMessage || '点击开始聊天' }}</div>
                </div>
              </div>
            </template>

            <div v-if="sessions.length === 0" class="cc-empty-sessions">
              暂无聊天记录
            </div>
          </div>
        </div>

        <!-- 右侧对话框 -->
        <div class="cc-main">
          <div v-if="activeSession" class="cc-main-header">
            <span class="cc-main-icon">{{ getIcon(activeSession) }}</span>
            <span>{{ activeSession.shopName || '聊天' }}</span>
            <span :class="['cc-status-tag', targetOnline ? 'online' : 'offline']">
              <span class="cc-status-dot"></span>
              {{ targetOnline ? '在线' : '离线' }}
            </span>
          </div>

          <div v-if="!activeSession" class="cc-no-session">
            <div class="cc-no-session-icon">💬</div>
            <div>选择左侧会话开始聊天</div>
          </div>

          <div v-else class="cc-body" ref="msgRef">
            <div
              v-for="(msg, idx) in timedMessages"
              :key="idx"
            >
              <div v-if="msg.type === 'time'" class="cc-time-separator">{{ msg.label }}</div>
              <div v-else :class="['cc-msg', msg.role === 'user' ? 'right' : 'left']">
                <div class="cc-msg-avatar">{{ msg.role === 'user' ? '👤' : getTargetIcon() }}</div>
                <div class="cc-msg-body">
                  <div class="cc-msg-bubble">
                    <div class="cc-msg-content">{{ msg.content }}</div>
                  </div>
                </div>
              </div>
            </div>
            <div v-if="connecting" class="cc-connecting">
              <span class="cc-connecting-dot"></span> 连接中...
            </div>
          </div>

          <div v-if="activeSession" class="cc-input-area">
            <el-input
              v-model="inputText"
              placeholder="输入消息..."
              size="default"
              @keyup.enter="sendMessage"
              :disabled="!wsConnected"
            />
            <el-button type="primary" size="default" @click="sendMessage" :disabled="!inputText.trim() || !wsConnected">
              发送
            </el-button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, nextTick, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import request from '../../api/request'
import { useWebSocket } from '../../composables/useWebSocket'
import HeaderUser from '../layout/HeaderUser.vue'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()

// ===== WebSocket 连接管理 =====
const { connected: wsConnected, connecting, connect, disconnect, send, onMessage } = useWebSocket()

interface SessionItem {
  id: number
  shopId?: number
  merchantId?: number
  shopName?: string
  status?: number
  createTime?: string
  lastMessage?: string
}

interface ChatMsg {
  id?: number
  sessionId?: number
  sendType: number
  sendId?: number
  content: string
  createTime?: string
}

const sessions = ref<SessionItem[]>([])
const activeSession = ref<SessionItem | null>(null)
const activeMessages = ref<ChatMsg[]>([])
const inputText = ref('')
const targetOnline = ref(false)
const sessionId = ref<number | null>(null)
const msgRef = ref<HTMLElement | null>(null)

const timedMessages = computed(() => {
  const result: any[] = []
  for (let i = 0; i < activeMessages.value.length; i++) {
    const msg = activeMessages.value[i]
    const shouldShowTime = i === 0 || (() => {
      const prev = activeMessages.value[i - 1]
      if (!prev.createTime || !msg.createTime) return true
      try {
        return new Date(msg.createTime).getTime() - new Date(prev.createTime).getTime() > 5 * 60 * 1000
      } catch { return true }
    })()
    if (shouldShowTime && msg.createTime) {
      result.push({ type: 'time', label: formatFullTime(msg.createTime) })
    }
    result.push({
      type: 'msg',
      role: msg.sendType === 1 ? 'user' : 'target',
      content: msg.content,
    })
  }
  return result
})

const auth = useAuthStore()

function getUserId(): number | null {
  const user = auth.user || {}
  return user?.id || null
}

function getIcon(s: SessionItem): string {
  return s.shopName?.charAt(0) || 'S'
}

function getTargetIcon(): string {
  return '👩‍💼'
}

// 注册 WS 消息回调
onMessage((data) => handleWsMessage(data))

// WS 断开时重置商家在线状态
watch(wsConnected, (val) => {
  if (!val) targetOnline.value = false
})

onMounted(async () => {
  const shopId = router.currentRoute.value.query.shopId
  const shopName = router.currentRoute.value.query.shopName
  const merchantId = router.currentRoute.value.query.merchantId

  await loadSessions()

  if (shopId && shopName) {
    const targetMerchantId = merchantId ? Number(merchantId) : Number(shopId)
    const matched = sessions.value.find(s => s.merchantId === targetMerchantId)
    if (matched) {
      selectSession(matched)
    } else {
      const tempSession: SessionItem = {
        id: -1,
        shopId: Number(shopId),
        merchantId: targetMerchantId,
        shopName: String(shopName),
      }
      activeSession.value = tempSession
      connectWs(tempSession)
    }
  } else {
    if (sessions.value.length > 0) {
      selectSession(sessions.value[0])
    }
  }
})

onUnmounted(() => {
  disconnect()
})

async function loadSessions() {
  try {
    const res = await request.get<any>('/customer-chat/history')
    sessions.value = (res.data || []) as SessionItem[]
  } catch (e) {
    console.error('加载会话失败:', e)
  }
}

function selectSession(s: SessionItem) {
  if (activeSession.value?.id === s.id) {
    return
  }
  // 断开之前的连接
  disconnect()
  activeSession.value = s
  activeMessages.value = []
  targetOnline.value = false
  wsConnected.value = false
  sessionId.value = null

  // 加载历史消息
  loadSessionMessages(s)

  // 建立 WebSocket
  connectWs(s)
}

async function loadSessionMessages(s: SessionItem) {
  if (!s || s.id <= 0) return
  try {
    const res = await request.get<any>(`/customer-chat/messages/${s.id}`)
    activeMessages.value = (res.data || []) as ChatMsg[]
    scrollBottom()
  } catch (e) {
    console.error('加载消息失败:', e)
  }
}

function connectWs(s: SessionItem) {
  const uid = getUserId()
  if (!uid) return

  const baseUrl = import.meta.env.VITE_WS_URL || `ws://${location.host}`
  const merchantId = s.merchantId || s.shopId
  const wsUrl = `${baseUrl}/api/ws/merchant?uid=${uid}&type=user&targetMerchantId=${merchantId}`
  connect(wsUrl)
}

function handleWsMessage(data: any) {
  switch (data.type) {
    case 'connected':
      sessionId.value = data.sessionId
      targetOnline.value = data.merchantOnline === true
      // 如果是临时会话(id=-1)，更新为真实 sessionId 并重新加载列表
      if (activeSession.value && activeSession.value.id === -1 && data.sessionId) {
        activeSession.value.id = data.sessionId
        loadSessions()
      }
      scrollBottom()
      break
    case 'history':
      if (data.messages) {
        activeMessages.value = []
        for (const m of data.messages) {
          activeMessages.value.push({
            id: m.id,
            sessionId: m.sessionId,
            sendType: m.sendType,
            sendId: m.sendId,
            content: m.content,
            createTime: m.createTime,
          })
        }
      }
      scrollBottom()
      break
    case 'message':
      if (data.sendType === 2) {
        activeMessages.value.push({
          sendType: 2,
          content: data.content,
          createTime: data.createTime,
        })
        scrollBottom()
      }
      break
    case 'merchant_online':
      targetOnline.value = true
      break
    case 'merchant_offline':
      targetOnline.value = false
      break
  }
}

function sendMessage() {
  const text = inputText.value.trim()
  if (!text) return
  if (!wsConnected.value) return

  activeMessages.value.push({
    sendType: 1,
    content: text,
    createTime: new Date().toISOString(),
  })
  inputText.value = ''
  scrollBottom()

  if (sessionId.value) {
    send(JSON.stringify({ type: 'message', sessionId: sessionId.value, content: text }))
  }
}

function scrollBottom() {
  nextTick(() => {
    if (msgRef.value) {
      msgRef.value.scrollTop = msgRef.value.scrollHeight
    }
  })
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
.cc-page {
  width: 100%;
  height: 100vh;
  display: flex;
  flex-direction: column;
  background: #f5f5f5;
  box-sizing: border-box;
}
.cc-window {
  width: 100%;
  height: 100%;
  background: #fff;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}
.cc-layout {
  display: flex;
  flex: 1 1 auto;
  min-height: 0;
  min-width: 0;
}
.cc-sidebar {
  width: 260px;
  border-right: 1px solid #eee;
  display: flex;
  flex-direction: column;
  background: #fafafa;
  flex-shrink: 0;
  min-height: 0;
}
.cc-sidebar-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  font-size: 14px;
  font-weight: 600;
  color: #333;
  background: linear-gradient(135deg, #667eea, #764ba2);
  color: #fff;
  flex-shrink: 0;
}
.cc-refresh {
  cursor: pointer;
  font-size: 16px;
  opacity: 0.85;
  user-select: none;
}
.cc-refresh:hover { opacity: 1; }
.cc-session-list {
  flex: 1 1 auto;
  overflow-y: auto;
  min-height: 0;
}
.cc-group-label {
  padding: 8px 14px 4px;
  font-size: 11px;
  font-weight: 600;
  color: #999;
  text-transform: uppercase;
  letter-spacing: 0.5px;
  background: #f5f5f5;
  border-bottom: 1px solid #eee;
  position: sticky;
  top: 0;
  z-index: 1;
}
.cc-session-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 14px;
  cursor: pointer;
  border-bottom: 1px solid #f0f0f0;
  transition: background 0.15s;
}
.cc-session-item:hover { background: #f0f0f0; }
.cc-session-item.active {
  background: #fff;
  border-left: 3px solid #667eea;
}
.cc-session-icon {
  width: 36px;
  height: 36px;
  border-radius: 8px;
  background: linear-gradient(135deg, #667eea, #764ba2);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
  font-weight: 700;
  flex-shrink: 0;
}
.cc-session-info {
  flex: 1;
  min-width: 0;
  overflow: hidden;
}
.cc-session-title {
  font-size: 13px;
  font-weight: 600;
  color: #333;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.cc-session-last {
  font-size: 12px;
  color: #999;
  margin-top: 4px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.cc-empty-sessions {
  padding: 40px 20px;
  text-align: center;
  color: #bbb;
  font-size: 13px;
}
.cc-main {
  flex: 1 1 auto;
  display: flex;
  flex-direction: column;
  min-width: 0;
  min-height: 0;
}
.cc-main-header {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 14px 20px;
  font-size: 15px;
  font-weight: 600;
  color: #333;
  background: #fff;
  border-bottom: 1px solid #eee;
  flex-shrink: 0;
}
.cc-main-icon {
  width: 32px;
  height: 32px;
  border-radius: 8px;
  background: linear-gradient(135deg, #667eea, #764ba2);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  font-weight: 700;
}
.cc-status-tag {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 11px;
  font-weight: 400;
  padding: 2px 8px;
  border-radius: 10px;
  margin-left: 8px;
}
.cc-status-tag.online { color: #67c23a; background: rgba(103,194,58,0.15); }
.cc-status-tag.offline { color: #909399; background: rgba(144,147,153,0.15); }
.cc-status-dot { width: 6px; height: 6px; border-radius: 50%; display: inline-block; }
.cc-status-tag.online .cc-status-dot { background: #67c23a; }
.cc-status-tag.offline .cc-status-dot { background: #909399; }
.cc-no-session {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: #bbb;
  font-size: 14px;
  padding: 60px 20px;
}
.cc-no-session-icon { font-size: 48px; margin-bottom: 12px; opacity: 0.5; }
.cc-body {
  flex: 1 1 auto;
  overflow-y: auto;
  padding: 16px 20px;
  background: #f7f8fa;
  min-height: 0;
}
.cc-time-separator {
  text-align: center;
  padding: 8px 0;
  margin: 4px 0 8px;
  font-size: 11px;
  color: #b2b2b2;
  user-select: none;
}
.cc-msg {
  display: flex;
  width: 100%;
  gap: 8px;
  margin-bottom: 16px;
  align-items: flex-start;
  box-sizing: border-box;
}
.cc-msg.right { flex-direction: row-reverse; justify-content: flex-start; }
.cc-msg.left { flex-direction: row; justify-content: flex-start; }
.cc-msg-avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: #e8eaed;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
  flex-shrink: 0;
  border: 1px solid #ddd;
  box-sizing: border-box;
}
/* Wrapper for bubble + time – limits max-width */
.cc-msg-body {
  max-width: 70%;
  display: flex;
  flex-direction: column;
  min-width: 0;
}
.cc-msg.right .cc-msg-body { align-items: flex-end; }
.cc-msg.left .cc-msg-body { align-items: flex-start; }

/* ── QQ 风格气泡 ── */
.cc-msg-bubble {
  position: relative;
  padding: 10px 14px;
  font-size: 13px;
  line-height: 1.5;
  word-break: break-word;
  white-space: pre-wrap;
}
/* 右侧用户气泡（紫色） */
.cc-msg.right .cc-msg-bubble {
  background: linear-gradient(135deg, #667eea, #764ba2);
  color: #fff;
  border-radius: 12px 4px 12px 12px;
}
.cc-msg.right .cc-msg-bubble::after {
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
/* 左侧目标气泡（白色） */
.cc-msg.left .cc-msg-bubble {
  background: #fff;
  color: #333;
  border: 1px solid #e8e8e8;
  border-radius: 4px 12px 12px 12px;
}
.cc-msg.left .cc-msg-bubble::before {
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
.cc-msg.left .cc-msg-bubble::after {
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
/* 气泡下方时间占位（保留结构不显示） */
.cc-msg-time { display: none; }
.cc-connecting {
  text-align: center;
  padding: 20px;
  color: #999;
  font-size: 13px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
}
.cc-connecting-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #667eea;
  animation: dot-blink 1.2s infinite;
}
@keyframes dot-blink {
  0%, 100% { opacity: 0.3; }
  50% { opacity: 1; }
}
.cc-input-area {
  display: flex;
  gap: 8px;
  padding: 12px 20px;
  border-top: 1px solid #eee;
  flex-shrink: 0;
  background: #fff;
}
.cc-input-area .el-input { flex: 1; }
</style>
