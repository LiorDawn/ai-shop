<template>
  <div class="aw-panel">
    <div class="aw-layout">
      <div class="aw-sidebar">
        <div class="aw-sidebar-header">
          <h3>用户咨询</h3>
          <div class="aw-header-right">
            <span class="aw-count">进行中 {{ sessions.length }}</span>
            <span :class="['aw-ws-dot', wsConnected ? 'online' : 'offline']"></span>
          </div>
        </div>
        <div class="aw-session-list">
          <div
            v-for="s in sessions"
            :key="s.id"
            :class="['aw-session-item', { active: activeSession?.id === s.id }]"
            @click="selectSession(s)"
          >
            <div class="aw-session-info">
              <span class="aw-session-user">{{ getUserLabel(s) }}</span>
              <span class="aw-session-time">{{ formatTime(s.createTime) }}</span>
            </div>
            <div class="aw-session-bottom">
              <span class="aw-session-preview">{{ s.lastMessage || '暂无消息' }}</span>
              <span v-if="s.unreadCount && s.unreadCount > 0 && activeSession?.id !== s.id" class="aw-unread-badge">{{ s.unreadCount > 99 ? '99+' : s.unreadCount }}</span>
            </div>
          </div>
          <div v-if="sessions.length === 0" class="aw-empty"><p>暂无用户咨询</p></div>
        </div>
      </div>

      <div class="aw-main">
        <template v-if="activeSession">
          <div class="aw-main-header">
            <span class="aw-main-title">与 {{ getUserLabel(activeSession) }} 对话中</span>
            <div class="aw-main-header-right">
              <span :class="['aw-online-tag', userOnline ? 'online' : 'offline']">
                <span class="aw-dot"></span>
                {{ userOnline ? '用户在线' : '用户离线' }}
              </span>
              <el-button size="small" type="danger" plain @click="closeCurrentSession">结束会话</el-button>
            </div>
          </div>

          <div class="aw-messages" ref="msgRef">
            <template v-for="(item, idx) in timedMessages" :key="item.id || idx">
              <div v-if="item.type === 'time'" class="aw-time-separator">{{ item.label }}</div>
              <div v-else :class="['aw-msg', item.sendType === 2 ? 'self' : 'other']">
                <div class="aw-msg-avatar">{{ item.sendType === 2 ? '👨‍💼' : '👤' }}</div>
                <div class="aw-msg-body">
                  <div class="aw-msg-bubble">{{ item.content }}</div>
                </div>
              </div>
            </template>
            <div v-if="currentMessages.length === 0" class="aw-msg-empty">暂无消息，等待用户发送...</div>
          </div>

          <div class="aw-input-area">
            <el-input
              v-model="inputText"
              placeholder="输入回复内容..."
              @keyup.enter="sendMessage"
              :disabled="!wsConnected"
              maxlength="2000"
            />
            <el-button type="primary" @click="sendMessage" :disabled="!wsConnected || !inputText.trim()">发送</el-button>
          </div>
        </template>
        <div v-else class="aw-no-session"><p>请从左侧选择一个会话</p></div>
      </div>
    </div>

    <div :class="['aw-status-bar', wsConnected ? 'connected' : 'disconnected']">
      <span>{{ wsConnected ? '客服通道已连接' : '客服通道未连接' }}</span>
      <el-button v-if="!wsConnected" size="small" type="text" @click="manualReconnect">重新连接</el-button>
    </div>
    <div v-for="(popup, idx) in newMsgPopups" :key="idx" class="aw-msg-popup" @click="handlePopupClick(popup)">
      <div class="aw-popup-header">{{ popup.userName }} 发来消息</div>
      <div class="aw-popup-body">{{ popup.content }}</div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, nextTick } from 'vue'
import { ElMessage, ElMessageBox, ElNotification } from 'element-plus'
import request from '../../api/request'
import { useAuthStore } from '@/stores/auth'

interface PlatformSessionVO {
  id: number
  userId: number
  adminId?: number
  status: number
  createTime: string
  endTime?: string
  unreadCount?: number
  lastMessage?: string
  nickname?: string
}

interface PlatformMsgVO {
  id: number
  sessionId: number
  sendType: number
  sendId: number
  receiveId: number
  msgType: number
  content: string
  createTime: string
}

interface MsgPopup { sessionId: number; userId: number; userName: string; content: string }

const sessions = ref<PlatformSessionVO[]>([])
const activeSession = ref<PlatformSessionVO | null>(null)
const currentMessages = ref<PlatformMsgVO[]>([])
const timedMessages = computed(() => {
  const result: any[] = []
  for (let i = 0; i < currentMessages.value.length; i++) {
    const msg = currentMessages.value[i]
    const shouldShowTime = i === 0 || (() => {
      const prev = currentMessages.value[i - 1]
      if (!prev.createTime || !msg.createTime) return true
      try {
        return new Date(msg.createTime).getTime() - new Date(prev.createTime).getTime() > 5 * 60 * 1000
      } catch { return true }
    })()
    if (shouldShowTime && msg.createTime) {
      result.push({ type: 'time', label: formatTime(msg.createTime) })
    }
    result.push({ type: 'msg', ...msg })
  }
  return result
})
const inputText = ref('')
const msgRef = ref<HTMLElement | null>(null)
const wsConnected = ref(false)
const userOnline = ref(false)
const newMsgPopups = ref<MsgPopup[]>([])
const userCache = ref<Record<number, { name: string; online: boolean }>>({})

let ws: WebSocket | null = null
let reconnectTimer: ReturnType<typeof setTimeout> | null = null

onMounted(() => { loadSessions(); connectWs() })
onUnmounted(() => {
  if (ws) { ws.onclose = null; ws.close(); ws = null }
  if (reconnectTimer) clearTimeout(reconnectTimer)
})

async function loadSessions() {
  try {
    const res = await request.get<any>('/admin/chat/sessions')
    if (res.data) {
      sessions.value = res.data
      // 用 sessions 接口返回的 nickname 填充 userCache（无需额外请求）
      for (const s of sessions.value) {
        if (!userCache.value[s.userId]) {
          const name = s.nickname || '用户' + s.userId
          userCache.value[s.userId] = { name, online: false }
        }
      }
      // 自动选中第一个有未读消息的会话
      const unreadSession = res.data.find((s: any) => s.unreadCount && s.unreadCount > 0)
      if (unreadSession && !activeSession.value) {
        selectSession(unreadSession)
      } else if (!activeSession.value && res.data.length > 0) {
        selectSession(res.data[0])
      }
    }
  } catch { /* ignore */ }
}

async function loadUserLabel(userId: number) {
  try {
    const res = await request.get<any>(`/admin/chat/user-info/${userId}`)
    if (res.data) {
      userCache.value[userId] = { name: res.data.displayName || res.data.nickname || res.data.username || '用户' + userId, online: false }
    }
  } catch { userCache.value[userId] = { name: '用户' + userId, online: false } }
}

function getUserLabel(s: PlatformSessionVO): string {
  if (s.nickname) return s.nickname
  return userCache.value[s.userId]?.name || '用户' + s.userId
}

async function selectSession(session: PlatformSessionVO) {
  activeSession.value = session
  userOnline.value = userCache.value[session.userId]?.online || false
  try {
    const res = await request.get<any>(`/admin/chat/messages/${session.id}`)
    if (res.data) currentMessages.value = res.data
    scrollToBottom()
    if (ws && ws.readyState === WebSocket.OPEN) {
      ws.send(JSON.stringify({ type: 'mark_read', sessionId: session.id }))
    }
    session.unreadCount = 0
  } catch { /* ignore */ }
}

const auth = useAuthStore()

function connectWs() {
  const user = auth.user
  if (!user) return
  const uid = user?.id
  if (!uid) return

  const baseUrl = import.meta.env.VITE_WS_URL || `ws://${location.host}`
  const wsUrl = `${baseUrl}/api/ws/platform-cs?uid=${uid}&type=admin`

  try {
    ws = new WebSocket(wsUrl)
    ws.onopen = () => { wsConnected.value = true; loadSessions() }
    ws.onmessage = (event) => {
      try { handleMessage(JSON.parse(event.data)) } catch { /* ignore */ }
    }
    ws.onclose = () => { wsConnected.value = false; scheduleReconnect() }
    ws.onerror = () => { wsConnected.value = false }
  } catch { wsConnected.value = false }
}

function handleMessage(data: any) {
  switch (data.type) {
    case 'connected':
      if (data.messages?.length) ElMessage.success(`收到 ${data.messages.length} 条离线消息`)
      loadSessions()
      break
    case 'new_session':
      ElNotification({ title: '新用户咨询', message: '有新用户进入咨询', type: 'success', duration: 4000 })
      loadSessions()
      break
    case 'message':
      handleNewMessage(data)
      break
    case 'user_online':
      if (activeSession.value?.userId === data.userId) userOnline.value = true
      if (userCache.value[data.userId]) userCache.value[data.userId].online = true
      break
    case 'user_offline':
      if (activeSession.value?.userId === data.userId) userOnline.value = false
      if (userCache.value[data.userId]) userCache.value[data.userId].online = false
      break
    case 'session_closed':
      if (activeSession.value?.id === data.sessionId) { activeSession.value = null; currentMessages.value = [] }
      loadSessions()
      ElMessage.info('用户已关闭会话')
      break
    case 'error':
      ElMessage.error(data.message || '连接异常')
      break
  }
}

function handleNewMessage(data: any) {
  const msg: PlatformMsgVO = {
    id: data.messageId || Date.now(),
    sessionId: data.sessionId,
    sendType: data.sendType || 1,
    sendId: data.sendId,
    receiveId: data.receiveId,
    msgType: data.msgType || 1,
    content: data.content || '',
    createTime: data.createTime || new Date().toISOString(),
  }
  if (activeSession.value?.id === data.sessionId) {
    currentMessages.value.push(msg)
    scrollToBottom()
  }
  const session = sessions.value.find(s => s.id === data.sessionId)
  if (session) {
    session.lastMessage = data.content
    if (activeSession.value?.id !== data.sessionId) session.unreadCount = (session.unreadCount || 0) + 1
  }
  if (activeSession.value?.id !== data.sessionId) {
    const userName = userCache.value[data.sendId]?.name || '用户' + data.sendId
    showPopup(data.sessionId, data.sendId, userName, data.content || '[消息]')
  }
}

function sendMessage() {
  const text = inputText.value.trim()
  if (!text || !ws || ws.readyState !== WebSocket.OPEN || !activeSession.value) return
  ws.send(JSON.stringify({ type: 'message', sessionId: activeSession.value.id, toUid: activeSession.value.userId, content: text, msgType: 1 }))
  currentMessages.value.push({ id: Date.now(), sessionId: activeSession.value.id, sendType: 2, sendId: 0, receiveId: activeSession.value.userId, msgType: 1, content: text, createTime: new Date().toISOString() })
  inputText.value = ''
  scrollToBottom()
}

async function closeCurrentSession() {
  if (!activeSession.value) return
  try {
    await ElMessageBox.confirm('确定结束该会话吗？', '提示', { type: 'warning' })
    if (ws && ws.readyState === WebSocket.OPEN) {
      ws.send(JSON.stringify({ type: 'close_session', sessionId: activeSession.value.id }))
    }
    activeSession.value = null; currentMessages.value = []; loadSessions()
  } catch { /* cancel */ }
}

function showPopup(sessionId: number, userId: number, userName: string, content: string) {
  const popup: MsgPopup = { sessionId, userId, userName, content }
  newMsgPopups.value.push(popup)
  setTimeout(() => { const i = newMsgPopups.value.indexOf(popup); if (i !== -1) newMsgPopups.value.splice(i, 1) }, 5000)
  if (newMsgPopups.value.length > 3) newMsgPopups.value.shift()
}

function handlePopupClick(popup: MsgPopup) {
  const s = sessions.value.find(s => s.id === popup.sessionId)
  if (s) selectSession(s)
  const i = newMsgPopups.value.indexOf(popup); if (i !== -1) newMsgPopups.value.splice(i, 1)
}

function manualReconnect() { if (reconnectTimer) { clearTimeout(reconnectTimer); reconnectTimer = null } connectWs() }
function scheduleReconnect() { if (reconnectTimer) return; reconnectTimer = setTimeout(() => { reconnectTimer = null; if (!wsConnected.value) connectWs() }, 3000) }
function scrollToBottom() { nextTick(() => { if (msgRef.value) msgRef.value.scrollTop = msgRef.value.scrollHeight }) }

function formatTime(time: string | undefined | null): string {
  if (!time) return ''
  try {
    const d = new Date(time)
    if (isNaN(d.getTime())) return ''
    const now = new Date()
    const pad = (n: number) => String(n).padStart(2, '0')
    if (d.toDateString() === now.toDateString()) return `${pad(d.getHours())}:${pad(d.getMinutes())}`
    return `${pad(d.getMonth() + 1)}/${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
  } catch { return '' }
}
</script>

<style scoped>
.aw-panel { height: calc(100vh - 120px); max-height: calc(100vh - 120px); display: flex; flex-direction: column; background: #fff; border-radius: 8px; box-shadow: 0 2px 12px rgba(0,0,0,0.06); overflow: hidden; position: relative; }
.aw-layout { display: flex; flex: 1 1 auto; min-height: 0; overflow: hidden; }
.aw-sidebar { width: 300px; border-right: 1px solid #eee; display: flex; flex-direction: column; min-height: 0; }
.aw-sidebar-header { padding: 16px; border-bottom: 1px solid #eee; display: flex; justify-content: space-between; align-items: center; flex-shrink: 0; }
.aw-sidebar-header h3 { margin: 0; font-size: 16px; }
.aw-header-right { display: flex; align-items: center; gap: 8px; }
.aw-count { font-size: 12px; color: #999; }
.aw-ws-dot { width: 8px; height: 8px; border-radius: 50%; display: inline-block; }
.aw-ws-dot.online { background: #67c23a; }
.aw-ws-dot.offline { background: #909399; }
.aw-session-list { flex: 1 1 auto; overflow-y: auto; min-height: 0; }
.aw-session-item { padding: 14px 16px; cursor: pointer; border-bottom: 1px solid #f5f5f5; transition: background 0.2s; }
.aw-session-item:hover { background: #f7f8fa; }
.aw-session-item.active { background: #eef1ff; border-left: 3px solid #409eff; }
.aw-session-info { display: flex; justify-content: space-between; margin-bottom: 6px; }
.aw-session-user { font-size: 14px; font-weight: 600; color: #333; }
.aw-session-time { font-size: 12px; color: #999; }
.aw-session-bottom { display: flex; justify-content: space-between; align-items: center; }
.aw-session-preview { font-size: 13px; color: #999; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; max-width: 200px; }
.aw-unread-badge { min-width: 18px; height: 18px; padding: 0 5px; background: #e4393c; color: #fff; font-size: 11px; line-height: 18px; text-align: center; border-radius: 9px; flex-shrink: 0; }
.aw-empty { padding: 40px 16px; text-align: center; color: #999; }
.aw-main { flex: 1 1 auto; display: flex; flex-direction: column; min-width: 0; min-height: 0; }
.aw-main-header { padding: 12px 20px; border-bottom: 1px solid #eee; display: flex; justify-content: space-between; align-items: center; flex-shrink: 0; }
.aw-main-title { font-size: 15px; font-weight: 600; }
.aw-main-header-right { display: flex; align-items: center; gap: 12px; }
.aw-online-tag { font-size: 12px; display: flex; align-items: center; gap: 4px; padding: 2px 8px; border-radius: 10px; }
.aw-online-tag.online { color: #67c23a; background: #f0f9eb; }
.aw-online-tag.offline { color: #909399; background: #f5f5f5; }
.aw-dot { width: 6px; height: 6px; border-radius: 50%; display: inline-block; }
.aw-online-tag.online .aw-dot { background: #67c23a; }
.aw-online-tag.offline .aw-dot { background: #909399; }
.aw-messages { flex: 1 1 auto; overflow-y: auto; padding: 20px; background: #f7f8fa; min-height: 0; }
.aw-msg { display: flex; width: 100%; gap: 8px; margin-bottom: 16px; align-items: flex-start; box-sizing: border-box; }
.aw-msg.other { flex-direction: row; }
.aw-msg.self { flex-direction: row-reverse; }
.aw-msg-avatar {
  width: 36px; height: 36px;
  border-radius: 50%;
  display: flex; align-items: center; justify-content: center;
  font-size: 16px; flex-shrink: 0; background: #e8eaed;
  border: 1px solid #ddd; box-sizing: border-box;
}
.aw-msg-body { max-width: 65%; display: flex; flex-direction: column; min-width: 0; }
.aw-msg.self .aw-msg-body { align-items: flex-end; }
.aw-msg.other .aw-msg-body { align-items: flex-start; }

/* ── QQ 风格气泡 ── */
.aw-msg-bubble {
  position: relative;
  padding: 10px 14px; border-radius: 12px;
  font-size: 14px; line-height: 1.5; word-break: break-word; white-space: pre-wrap;
}
.aw-msg.other .aw-msg-bubble {
  background: #fff; color: #333;
  border: 1px solid #e8e8e8;
  border-radius: 4px 12px 12px 12px;
}
.aw-msg.other .aw-msg-bubble::before {
  content: '';
  position: absolute; top: 12px; left: -7px;
  width: 0; height: 0;
  border: 6px solid transparent;
  border-right-color: #e8e8e8; border-left: 0;
}
.aw-msg.other .aw-msg-bubble::after {
  content: '';
  position: absolute; top: 13px; left: -5px;
  width: 0; height: 0;
  border: 5px solid transparent;
  border-right-color: #fff; border-left: 0;
}
.aw-msg.self .aw-msg-bubble {
  background: linear-gradient(135deg, #667eea, #764ba2);
  color: #fff;
  border-radius: 12px 4px 12px 12px;
}
.aw-msg.self .aw-msg-bubble::after {
  content: '';
  position: absolute; top: 12px; right: -6px;
  width: 0; height: 0;
  border: 6px solid transparent;
  border-left-color: #764ba2; border-right: 0;
}
.aw-time-separator {
  text-align: center;
  padding: 6px 0;
  margin: 2px 0;
  font-size: 11px;
  color: #b2b2b2;
  user-select: none;
}
.aw-msg-empty { text-align: center; color: #999; padding: 40px 0; }
.aw-msg-time { display: none; }
.aw-input-area { display: flex; gap: 10px; padding: 12px 20px; border-top: 1px solid #eee; background: #fff; flex-shrink: 0; }
.aw-input-area .el-input { flex: 1; }
.aw-no-session { flex: 1 1 auto; display: flex; align-items: center; justify-content: center; color: #999; font-size: 15px; }
.aw-status-bar { padding: 6px 16px; font-size: 12px; display: flex; align-items: center; gap: 8px; border-top: 1px solid #eee; flex-shrink: 0; }
.aw-status-bar.connected { color: #67c23a; background: #f0f9eb; }
.aw-status-bar.disconnected { color: #909399; background: #f5f5f5; }
.aw-msg-popup { position: fixed; bottom: 20px; right: 20px; width: 300px; background: #fff; border-radius: 8px; box-shadow: 0 4px 16px rgba(0,0,0,0.15); padding: 12px; cursor: pointer; z-index: 9999; border-left: 4px solid #409eff; animation: popup-slide 0.3s ease-out; }
@keyframes popup-slide { from { transform: translateX(100%); opacity: 0; } to { transform: translateX(0); opacity: 1; } }
.aw-popup-header { font-size: 13px; font-weight: 600; color: #333; margin-bottom: 4px; }
.aw-popup-body { font-size: 13px; color: #666; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
</style>