<template>
  <!-- 悬浮球 -->
  <div v-if="showBall" class="ai-float-widget" :style="widgetStyle">
    <div
      v-if="!visible"
      class="ai-float-ball"
      @mousedown.prevent="onDragStart"
      @touchstart.prevent="onDragStart"
      @click="openChat"
    >
      <span class="ball-icon">🤖</span>
      <span class="ball-badge" v-if="unreadCount > 0">{{ unreadCount }}</span>
    </div>
  </div>

  <!-- 聊天弹窗 -->
  <Teleport to="body">
    <div v-if="visible" class="ai-float-dialog" @click.stop>
      <!-- 头部 -->
      <div class="float-header">
        <div class="float-header-left">
          <span class="header-icon">🤖</span>
          <div>
            <div class="header-title">AI 智能助手</div>
            <div class="header-sub" @click="showSidebar = !showSidebar" style="cursor:pointer">{{ currentTitle }}</div>
          </div>
        </div>
        <div class="float-header-right">
          <button
            class="rag-toggle-btn"
            :class="{ active: ragEnabled }"
            @click="ragEnabled = !ragEnabled"
            :title="ragEnabled ? 'RAG 已开启：AI 将检索商品知识库' : 'RAG 已关闭：点击开启商品知识库检索'"
          >
            <span class="rag-icon">🧠</span>
            <span class="rag-label">RAG</span>
            <span class="rag-dot" :class="{ on: ragEnabled }"></span>
          </button>
          <button
            class="rag-toggle-btn v2-mode-btn"
            :class="{ active: useV2 }"
            @click="useV2 = !useV2"
            :title="useV2 ? 'V2 模式：6 步编排 + 12 个 MCP 工具（购物车/商品/订单/售后）' : 'V1 模式：基础对话'"
          >
            <span class="rag-icon">⚡</span>
            <span class="rag-label">V2</span>
            <span class="rag-dot" :class="{ on: useV2 }"></span>
          </button>
          <button class="float-btn" @click="showSidebar = !showSidebar" title="会话列表">☰</button>
          <button class="float-btn" @click="newChat" title="新对话">＋</button>
          <button class="float-btn" v-if="messages.length > 0" @click="clearMessages" title="清空当前对话">🗘</button>
          <button class="float-close" @click="closeChat">✕</button>
        </div>
      </div>

      <!-- 主体：左右布局 -->
      <div class="float-main">
        <!-- 左侧会话列表 -->
        <div class="session-sidebar" v-show="showSidebar">
          <div class="session-search">
            <el-input v-model="sessionSearch" placeholder="搜索会话..." size="small" prefix-icon="Search" clearable />
          </div>
          <div class="session-list">
            <div
              v-for="s in filteredSessions"
              :key="s.id"
              :class="['session-item', { active: sessionId === s.id }]"
              @click="switchSession(s.id)"
            >
              <div class="session-info">
                <span class="session-title">{{ s.title || '新对话' }}</span>
                <span class="session-time">{{ formatTime(s.lastTime || s.createTime) }}</span>
              </div>
              <div class="session-actions">
                <button class="session-btn" @click.stop="showRenameDialog(s)" title="重命名">✎</button>
                <button class="session-btn" @click.stop="removeSession(s.id)" title="删除">🗑</button>
              </div>
            </div>
            <div v-if="filteredSessions.length === 0" class="session-empty">{{ sessionSearch ? '未找到匹配的会话' : '暂无历史对话' }}</div>
          </div>
        </div>

        <!-- 右侧聊天区域 -->
        <div class="chat-area">
          <!-- 消息区 -->
          <div class="float-body" ref="chatBodyRef" @scroll="onScroll">
            <div v-if="historyLoading" class="history-loading">
              <span class="loading-dots">加载历史消息</span>
            </div>
            <div v-if="historyAllLoaded && messages.length > 0" class="history-loaded-all">— 已加载全部历史消息 —</div>

            <div v-if="messages.length === 0" class="float-empty">
              <div class="empty-icon">🤖</div>
              <p>{{ useV2 ? '你好！我是 V2 智能购物助手，支持搜索商品、智能推荐、查询订单、售后进度、管理购物车' : '你好！我是智能购物助手，可以帮你推荐商品、查询订单、解答购物问题' }}</p>
              <div class="quick-questions">
                <span class="quick-tag" v-for="q in quickQuestions" :key="q" @click="sendQuick(q)">{{ q }}</span>
              </div>
            </div>

            <div v-for="(msg, idx) in messages" :key="idx" :class="['float-msg', msg.role]">
              <div class="msg-avatar">{{ msg.role === 'user' ? '👤' : '🤖' }}</div>
              <div class="msg-content">
                <div class="msg-header">
                  <span class="msg-role-name">{{ msg.role === 'user' ? '我' : 'AI 助手' }}</span>
                  <span class="msg-time">{{ formatTimeDetail(msg.createTime) }}</span>
                </div>
                <div class="msg-bubble" v-html="renderContent(msg.content)"></div>

                <!-- 图片消息 -->
                <div v-if="msg.imgUrl" class="msg-image">
                  <img
                    :src="msg.imgUrl"
                    alt="图片"
                    @click="previewImage(msg.imgUrl)"
                    @error="onImageError($event)"
                  />
                </div>

                <!-- AI 回复操作按钮 -->
                <div v-if="msg.role === 'assistant'" class="msg-actions">
                  <button class="msg-action-btn" @click="copyText(msg.content)" title="复制">
                    <span v-if="copiedIdx === idx">✓</span><span v-else>📋</span>
                  </button>
                  <button class="msg-action-btn" @click="regenerate(idx)" title="重新生成" :disabled="loading">🔄</button>
                  <button class="msg-action-btn" @click="toggleFavorite(idx)" title="收藏">
                    <span :class="{ favorited: msg.favorited }">{{ msg.favorited ? '⭐' : '☆' }}</span>
                  </button>
                </div>
              </div>
            </div>

            <!-- 流式打字中 -->
            <div v-if="streaming" class="float-msg assistant">
              <div class="msg-avatar">🤖</div>
              <div class="msg-content">
                <div class="msg-header">
                  <span class="msg-role-name">AI 助手</span>
                  <span class="msg-time">正在输入...</span>
                </div>
                <div class="msg-bubble streaming-text" v-html="renderContent(streamingText)"></div>
                <span class="cursor-blink">|</span>
              </div>
            </div>

            <!-- 加载/思考状态 -->
            <div v-if="loading && !streaming" class="float-msg assistant">
              <div class="msg-avatar">🤖</div>
              <div class="msg-content">
                <div class="msg-header">
                  <span class="msg-role-name">AI 助手</span>
                  <span class="msg-time">思考中...</span>
                </div>
                <div class="msg-bubble thinking">
                  <span class="thinking-text">🤔 正在思考</span>
                  <span class="typing-dots"><span></span><span></span><span></span></span>
                </div>
              </div>
            </div>
          </div>

          <!-- 停止生成按钮 -->
          <div v-if="streaming" class="stop-bar">
            <button class="stop-btn" @click="stopGeneration">■ 停止生成</button>
          </div>

          <!-- 敏感词警告 -->
          <div v-if="showSensitiveWarning" class="sensitive-warning-bar">
            ⚠️ {{ sensitiveWarningText }}
            <button class="error-close" @click="showSensitiveWarning = false">✕</button>
          </div>

          <!-- 图片预览区 -->
          <div v-if="uploadedImage" class="image-preview-bar">
            <div class="preview-thumb">
              <img :src="uploadedImage" alt="preview" @error="uploadedImage = ''" />
              <button class="preview-remove" @click="removeImage">✕</button>
            </div>
          </div>

          <!-- 输入区 -->
          <div class="float-input-area">
            <div class="input-row">
              <label class="upload-btn" :class="{ uploading: imageUploading }" title="上传图片">
                📷
                <input type="file" accept="image/*" @change="onImageUpload" hidden ref="fileInputRef" />
              </label>
              <div class="input-wrapper">
                <el-input
                  v-model="inputText"
                  :placeholder="loading ? '请等待回复...' : '输入您的问题...'"
                  @keyup.enter="sendMessage"
                  :disabled="loading"
                  clearable
                  size="large"
                  class="chat-input"
                  maxlength="500"
                  @input="onInputChange"
                >
                  <template #append>
                    <el-button
                      type="primary"
                      @click="sendMessage"
                      :disabled="!canSend"
                      :loading="loading"
                    >
                      发送
                    </el-button>
                  </template>
                </el-input>
                <div class="input-footer">
                  <span class="char-count" :class="{ warn: inputText.length > 450 }">{{ inputText.length }}/500</span>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 网络异常提示条 -->
      <div v-if="showNetworkError" class="network-error-bar">
        ⚠️ 网络连接异常，请检查网络后重试
        <button class="error-close" @click="showNetworkError = false">✕</button>
      </div>
    </div>
  </Teleport>

  <!-- 图片预览弹窗 -->
  <Teleport to="body">
    <div v-if="previewVisible" class="image-preview-modal" @click="previewVisible = false">
      <img :src="previewSrc" alt="preview" @error="previewVisible = false" />
      <span class="preview-close-btn" @click="previewVisible = false">✕</span>
    </div>
  </Teleport>
</template>

<script setup lang="ts">
import { ref, computed, nextTick, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  streamChat,
  streamChatV2,
  getMessages,
  createSession,
  getSessions,
  deleteSession,
  renameSession,
  uploadChatImage,
  sendMessage as apiSend,
  getSessionMessagesPage,
  type ChatMessage,
  type AISession,
} from '../../api/aiChat'

interface ChatMsg {
  role: string
  content: string
  imgUrl?: string
  createTime?: string
  favorited?: boolean
}

const route = useRoute()
const router = useRouter()

const MAX_CHARS = 500
const SESSION_PAGE_SIZE = 20

/* ── 悬浮球 ── */
const showBall = computed(() => {
  const path = route.path
  if (path === '/login') return false
  if (path.startsWith('/merchant')) return false
  if (path.startsWith('/admin')) return false
  return true
})

const visible = ref(false)
const ragEnabled = ref(false)
const useV2 = ref(true)      // V2 模式（6 步编排 + MCP 工具调用）
const toolEnabled = ref(true) // 工具调用开关
const unreadCount = ref(0)
const showSidebar = ref(false)
const sessionSearch = ref('')
const showNetworkError = ref(false)

/* ── 会话管理 ── */
const sessionId = ref<number | undefined>(undefined)
const sessions = ref<AISession[]>([])
const currentTitle = computed(() => {
  const s = sessions.value.find((s) => s.id === sessionId.value)
  return s?.title || '新对话'
})
const filteredSessions = computed(() => {
  if (!sessionSearch.value) return sessions.value
  return sessions.value.filter((s) =>
    (s.title || '').toLowerCase().includes(sessionSearch.value.toLowerCase()),
  )
})

/* ── 消息 ── */
const messages = ref<ChatMsg[]>([])
const inputText = ref('')
const loading = ref(false)
const streaming = ref(false)
const streamingText = ref('')
const chatBodyRef = ref<HTMLElement | null>(null)
const imageUploading = ref(false)
const copiedIdx = ref(-1)
let abortController: AbortController | null = null

/* ── 图片上传 ── */
const uploadedImage = ref('')
const fileInputRef = ref<HTMLInputElement | null>(null)
const previewVisible = ref(false)
const previewSrc = ref('')

/* ── 分页加载 ── */
const historyPage = ref(1)
const historyTotal = ref(0)
const historyLoading = ref(false)
const historyAllLoaded = ref(false)

/* ── 敏感词列表 ── */
const sensitiveWords = [
  '赌博', '色情', '暴力', '毒品', '枪支', '走私', '诈骗',
  '反动', '分裂', '邪教', '恐怖', '裸聊', '刷单', '传销',
  '违禁品', '假钞', '发票', '代考', '代写',
]
const showSensitiveWarning = ref(false)
const sensitiveWarningText = ref('')

/* ── 悬浮球拖拽 ── */
const rawX = Number(localStorage.getItem('ai_ball_x'))
const rawY = Number(localStorage.getItem('ai_ball_y'))
const hasPos = localStorage.getItem('ai_ball_x') !== null
const posX = ref(0)
const posY = ref(0)
const isDragging = ref(false)
const hasDraggedBefore = ref(false)
let dragStartX = 0
let dragStartY = 0
let startPosX = 0
let startPosY = 0

const BALL_SIZE = 56
const MARGIN = 16

function validatePos(x: number, y: number, vw: number, vh: number) {
  return x >= -MARGIN && x <= vw - BALL_SIZE - MARGIN && y >= -MARGIN && y <= vh - BALL_SIZE - MARGIN
}

const widgetStyle = computed(() => {
  if (!hasDraggedBefore.value) {
    return { position: 'fixed', zIndex: 9999, bottom: '80px', right: '24px' } as any
  }
  return { position: 'fixed', zIndex: 9999, left: posX.value + 'px', top: posY.value + 'px' } as any
})

const canSend = computed(() => {
  return inputText.value.trim().length > 0 && !loading.value && !imageUploading.value
})

const quickQuestions = [
  '帮我推荐一款手机',
  '有什么优惠活动',
  '帮我查一下最近的订单',
  '如何申请退款',
  '购物车有什么',
  '帮我找蓝牙耳机',
]

/* ── 初始化 ── */
onMounted(async () => {
  const vw = window.innerWidth
  const vh = window.innerHeight
  if (hasPos && !Number.isNaN(rawX) && !Number.isNaN(rawY) && validatePos(rawX, rawY, vw, vh)) {
    posX.value = rawX
    posY.value = rawY
    hasDraggedBefore.value = true
  }
  await loadSessions()
  const sid = localStorage.getItem('ai_session_id')
  if (sid) {
    await switchSession(Number(sid))
  }
  window.addEventListener('online', () => { showNetworkError.value = false })
  window.addEventListener('offline', () => { showNetworkError.value = true })
})

async function loadSessions() {
  try {
    const res = await getSessions()
    if (res.data) {
      sessions.value = res.data
    }
  } catch {
    // 忽略
  }
}

/* ── 会话操作 ── */
async function newChat() {
  stopGeneration()
  try {
    const res = await createSession()
    sessionId.value = res.data.id
    messages.value = []
    localStorage.setItem('ai_session_id', String(res.data.id))
    await loadSessions()
  } catch {
    ElMessage.error('创建会话失败')
  }
}

async function switchSession(id: number) {
  if (sessionId.value === id) return
  stopGeneration()
  sessionId.value = id
  localStorage.setItem('ai_session_id', String(id))
  // 重置分页状态
  historyPage.value = 1
  historyTotal.value = 0
  historyAllLoaded.value = false
  try {
    const res = await getMessages(id)
    if (res.data) {
      messages.value = res.data.map((msg: ChatMessage) => ({
        role: msg.role,
        content: msg.content,
        imgUrl: msg.imgUrl || '',
        createTime: msg.createTime,
        favorited: false,
      }))
    }
    scrollToBottom()
  } catch {
    messages.value = []
  }
}

async function removeSession(id: number) {
  try {
    await deleteSession(id)
    await loadSessions()
    if (sessionId.value === id) {
      sessionId.value = undefined
      messages.value = []
      localStorage.removeItem('ai_session_id')
    }
    ElMessage.success('已删除')
  } catch {
    ElMessage.error('删除失败')
  }
}

function showRenameDialog(s: AISession) {
  ElMessageBox.prompt('请输入新的会话名称', '重命名会话', {
    inputValue: s.title || '',
    inputValidator: (val: string) => !!val.trim() || '名称不能为空',
    inputErrorMessage: '名称不能为空',
  }).then(async ({ value }) => {
    try {
      await renameSession(s.id, value.trim())
      await loadSessions()
      ElMessage.success('重命名成功')
    } catch {
      ElMessage.error('重命名失败')
    }
  }).catch(() => {})
}

function clearMessages() {
  messages.value = []
  scrollToBottom()
}

/* ── 消息发送 ── */
function sendQuick(q: string) {
  inputText.value = q
  sendMessage()
}

/** 检查敏感词 */
function checkSensitive(text: string): boolean {
  for (const w of sensitiveWords) {
    if (text.includes(w)) {
      sensitiveWarningText.value = `输入内容包含敏感词"${w}"，请修改后重试`
      showSensitiveWarning.value = true
      setTimeout(() => { showSensitiveWarning.value = false }, 3000)
      return false
    }
  }
  showSensitiveWarning.value = false
  return true
}

async function sendMessage() {
  const text = inputText.value.trim()
  if (!canSend.value || !text) return

  // 敏感词拦截
  if (!checkSensitive(text)) return

  if (!sessionId.value) {
    await newChat()
    if (!sessionId.value) return
  }

  historyAllLoaded.value = false

  const now = new Date().toISOString()
  messages.value.push({
    role: 'user',
    content: text,
    imgUrl: uploadedImage.value || undefined,
    createTime: now,
  })
  inputText.value = ''
  const img = uploadedImage.value
  uploadedImage.value = ''
  scrollToBottom()

  loading.value = true
  streaming.value = false
  streamingText.value = ''

  abortController = (useV2.value ? streamChatV2 : streamChat)(
    text,
    sessionId.value,
    img || undefined,
    ragEnabled.value,
    ...(useV2.value ? [toolEnabled.value] : []),
    (chunk: string) => {
      streaming.value = true
      streamingText.value += chunk
      scrollToBottom()
    },
    (sid: number) => {
      sessionId.value = sid
      localStorage.setItem('ai_session_id', String(sid))
      loadSessions()
    },
    () => {
      const finalText = streamingText.value
      if (finalText) {
        const now2 = new Date().toISOString()
        messages.value.push({ role: 'assistant', content: finalText, createTime: now2, favorited: false })
        // 自动更新会话标题（取第一条用户消息）
        autoUpdateTitle(text)
      }
      streaming.value = false
      streamingText.value = ''
      loading.value = false
      abortController = null
      scrollToBottom()
    },
    (err: string) => {
      streaming.value = false
      loading.value = false
      abortController = null
      showNetworkError.value = true
      ElMessage.error('AI 回复失败: ' + err)
      fallbackSend(text, img)
    },
  )
}

function stopGeneration() {
  if (abortController) {
    abortController.abort()
    abortController = null
  }
  streaming.value = false
  streamingText.value = ''
  loading.value = false
}

async function autoUpdateTitle(firstMsg: string) {
  if (!sessionId.value) return
  const s = sessions.value.find((s) => s.id === sessionId.value)
  if (s && s.title && s.title !== '新对话') return // 已有自定义标题
  const title = firstMsg.length > 20 ? firstMsg.slice(0, 20) + '...' : firstMsg
  try {
    await renameSession(sessionId.value, title)
    await loadSessions()
  } catch {
    // 忽略
  }
}

/** 非流式回退 */
async function fallbackSend(text: string, img: string | undefined) {
  loading.value = true
  try {
    const res = await apiSend({
      sessionId: sessionId.value,
      message: text,
      imgUrl: img || undefined,
    })
    if (res.data) {
      sessionId.value = res.data.sessionId
      localStorage.setItem('ai_session_id', String(res.data.sessionId))
      const now = new Date().toISOString()
      messages.value.push({ role: 'assistant', content: res.data.reply, createTime: now, favorited: false })
      autoUpdateTitle(text)
      loadSessions()
    }
  } catch (e: any) {
    ElMessage.error(e.message || '发送失败')
  } finally {
    loading.value = false
    scrollToBottom()
  }
}

/* ── AI 回复操作 ── */
function copyText(text: string) {
  navigator.clipboard.writeText(text).then(() => {
    ElMessage.success('已复制')
  }).catch(() => {
    // fallback
    const ta = document.createElement('textarea')
    ta.value = text
    document.body.appendChild(ta)
    ta.select()
    document.execCommand('copy')
    document.body.removeChild(ta)
    ElMessage.success('已复制')
  })
}

function regenerate(idx: number) {
  // 找到前一条用户消息
  for (let i = idx - 1; i >= 0; i--) {
    if (messages.value[i].role === 'user') {
      const userMsg = messages.value[i]
      messages.value.splice(idx, 1) // 删除当前 AI 回复
      inputText.value = userMsg.content
      uploadedImage.value = userMsg.imgUrl || ''
      sendMessage()
      return
    }
  }
  ElMessage.warning('未找到对应的用户消息')
}

function toggleFavorite(idx: number) {
  messages.value[idx].favorited = !messages.value[idx].favorited
  ElMessage.success(messages.value[idx].favorited ? '已收藏' : '已取消收藏')
}

/* ── 图片上传 ── */
async function onImageUpload(e: Event) {
  const target = e.target as HTMLInputElement
  if (!target.files?.length) return
  const file = target.files[0]
  imageUploading.value = true
  try {
    const res = await uploadChatImage(file)
    uploadedImage.value = res.data
    ElMessage.success('图片上传成功')
  } catch {
    ElMessage.error('图片上传失败')
  } finally {
    imageUploading.value = false
  }
  target.value = ''
}

function removeImage() {
  uploadedImage.value = ''
}

function previewImage(url: string) {
  previewSrc.value = url
  previewVisible.value = true
}

function onImageError(e: Event) {
  const img = e.target as HTMLImageElement
  img.style.display = 'none'
  const fallback = img.parentElement?.querySelector('.img-error-placeholder') as HTMLElement
  if (fallback) {
    fallback.style.display = 'flex'
  } else {
    const div = document.createElement('div')
    div.className = 'img-error-placeholder'
    div.textContent = '🖼️ 图片加载失败'
    img.parentElement?.appendChild(div)
  }
}

/* ── 商品解析 ── */
interface ProductInfo {
  name: string
  price: string
}

function parseProducts(text: string): ProductInfo[] {
  const products: ProductInfo[] = []
  const lines = text.split('\n')
  for (const line of lines) {
    // 匹配商品推荐模式: 商品名 - ¥价格 或 商品名 ￥价格
    const match = line.match(/(?:推荐|介绍|看看)?(.+?)[（(]?[¥￥](\d+(?:\.\d+)?)[)）]?/)
    if (match) {
      products.push({ name: match[1].trim(), price: match[2] })
    }
    // 也匹配简单列表项: 1. 商品名 价格
    const match2 = line.match(/^\d+[.、]\s*(.+?)\s*[¥￥]?\s*(\d+(?:\.\d+)?)\s*元?/)
    if (match2 && !match) {
      products.push({ name: match2[1].trim(), price: match2[2] })
    }
  }
  return products.slice(0, 5)
}

function goToProduct(prod: ProductInfo) {
  // 跳转到商品搜索页面，展示搜索结果
  const query = encodeURIComponent(prod.name)
  window.open(`/#/hotsale?keyword=${query}`, '_blank')
}

/* ── 工具函数 ── */
function onInputChange() {
  if (inputText.value.length > MAX_CHARS) {
    inputText.value = inputText.value.slice(0, MAX_CHARS)
  }
}

function scrollToBottom() {
  nextTick(() => {
    if (chatBodyRef.value) {
      chatBodyRef.value.scrollTop = chatBodyRef.value.scrollHeight
    }
  })
}

function onScroll() {
  const el = chatBodyRef.value
  if (!el) return
  // 下拉加载更早历史问答
  if (el.scrollTop < 50 && !historyLoading.value && !historyAllLoaded.value && messages.value.length > 0) {
    loadMoreHistory()
  }
}

/** 加载历史消息 */
async function loadMoreHistory() {
  if (!sessionId.value || historyLoading.value || historyAllLoaded.value) return
  historyLoading.value = true
  try {
    const page = historyPage.value + 1
    const res = await getSessionMessagesPage(sessionId.value, page, 20)
    if (res.data) {
      const data = res.data
      const oldRecords = data.records || []
      if (oldRecords.length === 0 || data.current >= data.pages) {
        historyAllLoaded.value = true
      }
      if (oldRecords.length > 0) {
        // 反转：后端是倒序，要正序追加到头部
        const sorted = [...oldRecords].reverse()
        const newMsgs: ChatMsg[] = sorted.map((msg: ChatMessage) => ({
          role: msg.role,
          content: msg.content,
          imgUrl: msg.imgUrl || '',
          createTime: msg.createTime,
          favorited: false,
        }))
        // 保持滚动位置
        const oldHeight = chatBodyRef.value?.scrollHeight || 0
        messages.value = [...newMsgs, ...messages.value]
        historyPage.value = page
        historyTotal.value = data.total
        nextTick(() => {
          if (chatBodyRef.value) {
            chatBodyRef.value.scrollTop = chatBodyRef.value.scrollHeight - oldHeight + 50
          }
        })
      } else {
        historyAllLoaded.value = true
      }
    }
  } catch {
    ElMessage.warning('历史消息加载失败')
  } finally {
    historyLoading.value = false
  }
}

function renderContent(text: string): string {
  if (!text) return ''
  // 换行转 <br>
  let html = text.replace(/\n/g, '<br/>')
  // 加粗 **text**
  html = html.replace(/\*\*(.+?)\*\*/g, '<strong>$1</strong>')
  return html
}

function formatTime(timeStr: string): string {
  if (!timeStr) return ''
  const d = new Date(timeStr)
  const now = new Date()
  const diff = now.getTime() - d.getTime()
  if (diff < 60000) return '刚刚'
  if (diff < 3600000) return Math.floor(diff / 60000) + '分钟前'
  if (diff < 86400000) return Math.floor(diff / 3600000) + '小时前'
  return d.toLocaleDateString()
}

function formatTimeDetail(timeStr?: string): string {
  if (!timeStr) return ''
  const d = new Date(timeStr)
  const h = d.getHours().toString().padStart(2, '0')
  const m = d.getMinutes().toString().padStart(2, '0')
  return `${h}:${m}`
}

function openChat() {
  if (isDragging.value) return
  visible.value = true
  unreadCount.value = 0
  scrollToBottom()
}

function closeChat() {
  visible.value = false
}

/* ── 拖拽逻辑 ── */
function onDragStart(e: MouseEvent | TouchEvent) {
  isDragging.value = false
  const clientX = 'touches' in e ? e.touches[0].clientX : e.clientX
  const clientY = 'touches' in e ? e.touches[0].clientY : e.clientY
  if (!hasDraggedBefore.value) {
    const rect = (e.currentTarget as HTMLElement).parentElement!.getBoundingClientRect()
    posX.value = rect.left
    posY.value = rect.top
    hasDraggedBefore.value = true
  }
  dragStartX = clientX
  dragStartY = clientY
  startPosX = posX.value
  startPosY = posY.value
  document.addEventListener('mousemove', onDragMove)
  document.addEventListener('mouseup', onDragEnd)
  document.addEventListener('touchmove', onDragMove, { passive: false })
  document.addEventListener('touchend', onDragEnd)
}

function onDragMove(e: MouseEvent | TouchEvent) {
  isDragging.value = true
  const clientX = 'touches' in e ? e.touches[0].clientX : e.clientX
  const clientY = 'touches' in e ? e.touches[0].clientY : e.clientY
  posX.value = startPosX + clientX - dragStartX
  posY.value = startPosY + clientY - dragStartY
}

function onDragEnd() {
  if (isDragging.value) {
    const vw = window.innerWidth
    const vh = window.innerHeight
    posX.value = Math.max(-MARGIN, Math.min(vw - BALL_SIZE - MARGIN, posX.value))
    posY.value = Math.max(-MARGIN, Math.min(vh - BALL_SIZE - MARGIN, posY.value))
    localStorage.setItem('ai_ball_x', String(Math.round(posX.value)))
    localStorage.setItem('ai_ball_y', String(Math.round(posY.value)))
  }
  document.removeEventListener('mousemove', onDragMove)
  document.removeEventListener('mouseup', onDragEnd)
  document.removeEventListener('touchmove', onDragMove)
  document.removeEventListener('touchend', onDragEnd)
}
</script>

<style scoped>
.ai-float-widget {
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
}

/* ===== 悬浮球 ===== */
.ai-float-ball {
  width: 56px;
  height: 56px;
  border-radius: 50%;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  box-shadow: 0 4px 16px rgba(102, 126, 234, 0.4);
  cursor: grab;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: transform 0.2s, box-shadow 0.2s;
  position: relative;
  user-select: none;
  touch-action: none;
}
.ai-float-ball:hover {
  transform: scale(1.1);
  box-shadow: 0 6px 24px rgba(102, 126, 234, 0.5);
}
.ball-icon { font-size: 28px; line-height: 1; }
.ball-badge {
  position: absolute;
  top: -4px; right: -4px;
  min-width: 20px; height: 20px;
  border-radius: 10px;
  background: #e4393c;
  color: #fff;
  font-size: 11px; line-height: 20px;
  text-align: center;
  padding: 0 5px;
  font-weight: 600;
}

/* ===== 聊天弹窗 ===== */
.ai-float-dialog {
  position: fixed;
  bottom: 80px; right: 24px;
  width: 780px; height: 600px;
  background: #fff;
  border-radius: 16px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.18);
  display: flex;
  flex-direction: column;
  overflow: hidden;
  animation: floatSlideUp 0.25s ease-out;
  z-index: 10000;
}
@keyframes floatSlideUp {
  from { opacity: 0; transform: translateY(20px) scale(0.95); }
  to { opacity: 1; transform: translateY(0) scale(1); }
}

/* 头部 */
.float-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: #fff;
  flex-shrink: 0;
}
.float-header-left { display: flex; align-items: center; gap: 10px; min-width: 0; }
.float-header-right { display: flex; align-items: center; gap: 6px; flex-shrink: 0; }
.header-icon { font-size: 24px; }
.header-title { font-size: 15px; font-weight: 600; }
.header-sub { font-size: 11px; opacity: 0.8; max-width: 180px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.float-btn {
  width: 28px; height: 28px;
  border: none;
  background: rgba(255,255,255,0.2);
  color: #fff;
  border-radius: 50%;
  cursor: pointer;
  font-size: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: background 0.2s;
  padding: 0;
  line-height: 1;
}
.float-btn:hover { background: rgba(255,255,255,0.35); }
.float-btn:disabled { opacity: 0.4; cursor: not-allowed; }

/* RAG 开关按钮 */
.rag-toggle-btn {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 2px 8px;
  border: 1px solid rgba(255,255,255,0.3);
  border-radius: 14px;
  background: rgba(255,255,255,0.1);
  color: #fff;
  cursor: pointer;
  font-size: 11px;
  transition: all 0.25s;
  white-space: nowrap;
  margin-right: 2px;
}
.rag-toggle-btn:hover { background: rgba(255,255,255,0.25); }
.rag-toggle-btn.active {
  background: rgba(255,255,255,0.25);
  border-color: rgba(255,255,255,0.6);
  box-shadow: 0 0 8px rgba(255,255,255,0.2);
}
.rag-icon { font-size: 13px; line-height: 1; }
.rag-label { font-weight: 600; letter-spacing: 0.5px; }
.rag-dot {
  width: 6px; height: 6px;
  border-radius: 50%;
  background: rgba(255,255,255,0.3);
  transition: all 0.25s;
}
.rag-dot.on {
  background: #4eff4e;
  box-shadow: 0 0 4px #4eff4e;
}

/* V2 模式按钮特殊样式 */
.v2-mode-btn.active {
  background: rgba(255,255,255,0.3);
  border-color: rgba(255,255,255,0.7);
  box-shadow: 0 0 10px rgba(102,255,102,0.3);
}
.v2-mode-btn .rag-dot.on {
  background: #66ff66;
  box-shadow: 0 0 6px #66ff66;
}

.float-close {
  width: 28px; height: 28px;
  border: none;
  background: rgba(255,255,255,0.2);
  color: #fff;
  border-radius: 50%;
  cursor: pointer;
  font-size: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
}
.float-close:hover { background: rgba(255,255,255,0.35); }

/* 网络异常提示条 */
.network-error-bar {
  padding: 6px 16px;
  background: #fff3cd;
  color: #856404;
  font-size: 12px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-shrink: 0;
}
.error-close {
  background: transparent;
  border: none;
  cursor: pointer;
  font-size: 14px;
  color: #856404;
  padding: 0 4px;
}

/* 敏感词警告 */
.sensitive-warning-bar {
  padding: 6px 16px;
  background: #fff0f0;
  color: #e4393c;
  font-size: 12px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-top: 1px solid #ffd5d5;
  flex-shrink: 0;
}

/* 历史加载 & 全部加载提示 */
.history-loading {
  text-align: center;
  padding: 12px;
  color: #999;
  font-size: 12px;
}
.loading-dots::after {
  content: '';
  animation: dotAnim 1.5s infinite;
}
@keyframes dotAnim {
  0% { content: ''; }
  25% { content: ' .'; }
  50% { content: ' ..'; }
  75% { content: ' ...'; }
  100% { content: ''; }
}
.history-loaded-all {
  text-align: center;
  padding: 10px;
  color: #ccc;
  font-size: 11px;
}

/* 主体 */
.float-main {
  flex: 1;
  display: flex;
  overflow: hidden;
  min-height: 0;
}

/* 左侧会话列表 */
.session-sidebar {
  width: 220px;
  border-right: 1px solid #eee;
  background: #fafafa;
  display: flex;
  flex-direction: column;
  flex-shrink: 0;
}
.session-search { padding: 8px; }
.session-list {
  flex: 1;
  overflow-y: auto;
  padding: 0 8px 8px;
}
.session-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 10px;
  border-radius: 8px;
  cursor: pointer;
  transition: background 0.15s;
  margin-bottom: 2px;
}
.session-item:hover { background: #e8e8ff; }
.session-item.active { background: #e0e0ff; }
.session-info { flex: 1; min-width: 0; display: flex; flex-direction: column; gap: 1px; }
.session-title {
  font-size: 13px;
  color: #333;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.session-time { font-size: 10px; color: #999; }
.session-actions {
  display: flex;
  gap: 2px;
  opacity: 0;
  transition: opacity 0.15s;
  flex-shrink: 0;
}
.session-item:hover .session-actions { opacity: 0.7; }
.session-btn {
  width: 20px; height: 20px;
  border: none;
  background: transparent;
  cursor: pointer;
  font-size: 11px;
  border-radius: 4px;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0;
}
.session-btn:hover { background: #ddd; opacity: 1 !important; }
.session-empty {
  text-align: center;
  padding: 20px;
  color: #999;
  font-size: 12px;
}

/* 聊天区域 */
.chat-area {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
}

/* 消息区 */
.float-body {
  flex: 1;
  overflow-y: auto;
  padding: 16px;
  background: #f7f8fa;
}
.float-empty {
  text-align: center;
  padding: 40px 12px;
  color: #999;
}
.float-empty .empty-icon { font-size: 40px; margin-bottom: 12px; }
.float-empty p { font-size: 13px; line-height: 1.6; margin: 0; }
.quick-questions {
  margin-top: 16px;
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  justify-content: center;
}
.quick-tag {
  padding: 6px 14px;
  border-radius: 20px;
  background: #fff;
  border: 1px solid #e0e0e0;
  font-size: 12px;
  color: #666;
  cursor: pointer;
  transition: all 0.2s;
}
.quick-tag:hover {
  border-color: #667eea;
  color: #667eea;
  background: #f0f0ff;
}

/* 消息 */
.float-msg {
  display: flex;
  gap: 8px;
  margin-bottom: 16px;
  animation: msgIn 0.2s ease-out;
}
@keyframes msgIn {
  from { opacity: 0; transform: translateY(8px); }
  to { opacity: 1; transform: translateY(0); }
}
.float-msg.user { flex-direction: row-reverse; }
.msg-avatar {
  width: 32px; height: 32px;
  border-radius: 50%;
  background: #e8e8e8;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
  flex-shrink: 0;
  margin-top: 4px;
}
.float-msg.user .msg-avatar { background: #e8f0ff; }
.msg-content {
  max-width: 75%;
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.msg-header {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 0 2px;
}
.msg-role-name {
  font-size: 12px;
  font-weight: 600;
  color: #666;
}
.msg-time {
  font-size: 10px;
  color: #aaa;
}
.msg-bubble {
  padding: 10px 14px;
  border-radius: 12px;
  font-size: 13px;
  line-height: 1.5;
  word-break: break-word;
}
.float-msg.assistant .msg-bubble {
  background: #fff;
  border: 1px solid #e8e8e8;
  color: #333;
  border-bottom-left-radius: 4px;
}
.float-msg.user .msg-bubble {
  background: #667eea;
  color: #fff;
  border-bottom-right-radius: 4px;
}
.msg-image { margin-top: 4px; }
.msg-image img {
  max-width: 200px;
  max-height: 200px;
  border-radius: 8px;
  cursor: pointer;
  border: 1px solid #eee;
  display: block;
}
.float-msg.user .msg-image { display: flex; justify-content: flex-end; }

/* 图片加载失败兜底 */
.img-error-placeholder {
  display: none;
  width: 100px;
  height: 80px;
  border-radius: 8px;
  background: #f0f0f0;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  color: #999;
}

/* 推荐商品卡片 */
.msg-products {
  display: flex;
  flex-direction: column;
  gap: 6px;
  margin-top: 8px;
}
.products-header {
  font-size: 12px;
  color: #888;
  font-weight: 500;
  padding: 0 2px;
}
.product-card {
  border: 1px solid #e8e8ff;
  border-radius: 10px;
  background: #f8f8ff;
  cursor: pointer;
  transition: all 0.2s;
  overflow: hidden;
}
.product-card:hover {
  border-color: #667eea;
  box-shadow: 0 2px 12px rgba(102, 126, 234, 0.18);
  transform: translateY(-1px);
}
.product-card-body {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 12px 6px;
}
.product-icon {
  width: 36px;
  height: 36px;
  border-radius: 8px;
  background: #eef0ff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  flex-shrink: 0;
}
.product-info {
  flex: 1;
  min-width: 0;
}
.product-name {
  font-size: 13px;
  color: #333;
  font-weight: 500;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.product-price {
  font-size: 15px;
  color: #e4393c;
  font-weight: 700;
  margin-top: 2px;
}
.product-card-footer {
  padding: 4px 12px 8px;
  display: flex;
  justify-content: flex-end;
}
.product-action {
  font-size: 11px;
  color: #667eea;
  font-weight: 500;
}

/* 消息操作按钮 */
.msg-actions {
  display: flex;
  gap: 4px;
  padding: 2px 0;
  opacity: 0;
  transition: opacity 0.15s;
}
.msg-content:hover .msg-actions { opacity: 1; }
.msg-action-btn {
  width: 24px; height: 24px;
  border: none;
  background: #f0f0f0;
  border-radius: 4px;
  cursor: pointer;
  font-size: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: background 0.15s;
  padding: 0;
  line-height: 1;
}
.msg-action-btn:hover { background: #e0e0ff; }
.msg-action-btn:disabled { opacity: 0.4; cursor: not-allowed; }
.favorited { color: #f5a623; }

/* 流式打字光标 */
.streaming-text { display: inline; }
.cursor-blink {
  display: inline-block;
  width: 2px;
  height: 14px;
  background: #667eea;
  margin-left: 2px;
  animation: blink 0.8s infinite;
  vertical-align: middle;
}
@keyframes blink {
  0%, 50% { opacity: 1; }
  51%, 100% { opacity: 0; }
}

/* 思考动画 */
.thinking {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 16px !important;
}
.thinking-text { font-size: 13px; color: #999; }
.typing-dots { display: flex; gap: 3px; align-items: center; }
.typing-dots span {
  width: 5px; height: 5px;
  border-radius: 50%;
  background: #999;
  animation: typingDot 1.4s infinite ease-in-out;
}
.typing-dots span:nth-child(2) { animation-delay: 0.2s; }
.typing-dots span:nth-child(3) { animation-delay: 0.4s; }
@keyframes typingDot {
  0%, 60%, 100% { opacity: 0.3; transform: scale(0.8); }
  30% { opacity: 1; transform: scale(1); }
}

/* 停止生成 */
.stop-bar {
  padding: 6px;
  display: flex;
  justify-content: center;
  background: #fff;
  border-top: 1px solid #eee;
}
.stop-btn {
  padding: 4px 16px;
  border: 1px solid #e4393c;
  border-radius: 20px;
  background: #fff;
  color: #e4393c;
  font-size: 12px;
  cursor: pointer;
  transition: all 0.2s;
}
.stop-btn:hover { background: #e4393c; color: #fff; }

/* 图片预览栏 */
.image-preview-bar {
  padding: 8px 16px;
  border-top: 1px solid #eee;
  background: #fff;
  display: flex;
  gap: 8px;
}
.preview-thumb {
  position: relative;
  width: 60px; height: 60px;
  border-radius: 6px;
  overflow: hidden;
  border: 1px solid #ddd;
}
.preview-thumb img { width: 100%; height: 100%; object-fit: cover; }
.preview-remove {
  position: absolute;
  top: 2px; right: 2px;
  width: 18px; height: 18px;
  border: none;
  background: rgba(0,0,0,0.5);
  color: #fff;
  border-radius: 50%;
  cursor: pointer;
  font-size: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0;
}

/* 输入区 */
.float-input-area {
  padding: 8px 16px 10px;
  border-top: 1px solid #eee;
  background: #fff;
  flex-shrink: 0;
}
.input-row {
  display: flex;
  align-items: flex-start;
  gap: 8px;
}
.upload-btn {
  width: 36px; height: 36px;
  border-radius: 8px;
  background: #f5f5f5;
  cursor: pointer;
  font-size: 18px;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: background 0.2s;
  flex-shrink: 0;
  margin-top: 2px;
}
.upload-btn:hover { background: #e8e8ff; }
.upload-btn.uploading { opacity: 0.5; pointer-events: none; }
.upload-btn input { display: none; }
.input-wrapper { flex: 1; display: flex; flex-direction: column; }
.input-footer {
  display: flex;
  justify-content: flex-end;
  padding: 2px 4px 0;
}
.char-count {
  font-size: 11px;
  color: #ccc;
}
.char-count.warn { color: #e6a23c; }

.float-input-area :deep(.el-input-group__append) {
  background-color: #667eea;
  border-color: #667eea;
}
.float-input-area :deep(.el-input-group__append .el-button) {
  color: #fff;
  background: transparent;
  border: none;
}
.float-input-area :deep(.el-input-group__append .el-button.is-disabled) {
  color: rgba(255,255,255,0.5);
}

/* 图片预览模态框 */
.image-preview-modal {
  position: fixed;
  inset: 0;
  background: rgba(0,0,0,0.7);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 99999;
  cursor: pointer;
}
.image-preview-modal img {
  max-width: 90vw;
  max-height: 90vh;
  border-radius: 8px;
}
.preview-close-btn {
  position: fixed;
  top: 20px; right: 20px;
  width: 36px; height: 36px;
  border-radius: 50%;
  background: rgba(0,0,0,0.5);
  color: #fff;
  font-size: 18px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
}
</style>