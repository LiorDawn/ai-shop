import request from './request'
import { useAuthStore } from '../stores/auth'

export interface ChatRequest {
  sessionId?: number
  message: string
  imgUrl?: string
}

export interface ChatResponse {
  sessionId: number
  reply: string
}

export interface ChatMessage {
  id: number
  sessionId: number
  content: string
  imgUrl: string
  role: string
  createTime: string
}

export interface AISession {
  id: number
  userId: number
  title: string
  createTime: string
  lastTime: string
}

/** 发送消息获取AI回复（非流式，兼容旧接口） */
export function sendMessage(data: ChatRequest) {
  return request.post<ChatResponse>('/chat/send', data, {
    timeout: 120000,
  })
}

// ==================== SSE 流式问答 ====================

/**
 * SSE 流式问答
 * 使用 fetch + ReadableStream 读取 SSE 流，实时回调
 */
export function streamChat(
  message: string,
  sessionId: number | undefined,
  imgUrl: string | undefined,
  ragEnabled: boolean,
  onMessage: (chunk: string) => void,
  onSession: (sessionId: number) => void,
  onDone: () => void,
  onError: (err: string) => void,
): AbortController {
  const controller = new AbortController()
  const params = new URLSearchParams()
  params.set('message', message)
  if (sessionId) params.set('sessionId', String(sessionId))
  if (imgUrl) params.set('imgUrl', imgUrl)
  if (ragEnabled) params.set('ragEnabled', 'true')

  const token = useAuthStore().token || ''
  const url = `/api/chat/stream?${params.toString()}`

  fetch(url, {
    headers: { token },
    signal: controller.signal,
  })
    .then(async (response) => {
      if (!response.ok) {
        throw new Error(`HTTP ${response.status}`)
      }
      const reader = response.body?.getReader()
      if (!reader) throw new Error('No reader')

      const decoder = new TextDecoder()
      let buffer = ''

      let currentEvent = ''

      // 完成标记：保证无论流正常收到 event:done，还是被服务端/网络掐断，
      // 都只复位一次 UI，避免永远卡在“思考中”。
      let finished = false
      const doneOnce = () => {
        if (!finished) {
          finished = true
          onDone()
        }
      }

      while (true) {
        const { done, value } = await reader.read()
        if (done) break

        buffer += decoder.decode(value, { stream: true })
        const lines = buffer.split('\n')
        buffer = lines.pop() || ''

        for (const line of lines) {
          if (line.startsWith('event:')) {
            currentEvent = line.slice(6).trim()
            continue
          }
          if (line.startsWith('data:')) {
            const data = line.slice(5).trim()
            handleSSEEvent(currentEvent, data, onMessage, onSession, doneOnce)
          }
        }
      }

      // 流被非正常掐断（未收到 event:done）时，强制复位 UI
      doneOnce()
    })
    .catch((err) => {
      if (err.name === 'AbortError') return
      onError(err.message || '连接失败')
    })

  return controller
}

/** 根据事件名处理 SSE 数据 */
function handleSSEEvent(
  eventName: string,
  data: string,
  onMessage: (chunk: string) => void,
  onSession: (sessionId: number) => void,
  onDone: () => void,
) {
  switch (eventName) {
    case 'session': {
      const num = Number(data)
      if (!isNaN(num) && num > 0) {
        onSession(num)
      }
      break
    }
    case 'msg': {
      if (data) {
        onMessage(data)
      }
      break
    }
    case 'done': {
      onDone()
      break
    }
    case 'process': {
      // 中间状态事件（tool:xxx / thinking:xxx / rag:xxx），不显示在回复中
      break
    }
    default: {
      // 兼容无事件名的情况：根据内容判断
      if (!data) return
      const num = Number(data)
      if (!isNaN(num) && num > 0) {
        onSession(num)
      } else {
        onMessage(data)
      }
    }
  }
}

// ==================== 会话管理 ====================

/** 创建新会话 */
export function createSession() {
  return request.post<AISession>('/chat/session')
}

/** 获取会话列表 */
export function getSessions() {
  return request.get<AISession[]>('/chat/sessions')
}

/** 删除会话 */
export function deleteSession(sessionId: number) {
  return request.delete(`/chat/session/${sessionId}`)
}

/** 重命名会话 */
export function renameSession(sessionId: number, title: string) {
  return request.put(`/chat/session/${sessionId}/rename`, { title })
}

/** 获取用户最近一次会话 */
export function getLatestSession() {
  return request.get<AISession>('/chat/latest-session')
}

// ==================== 历史消息 ====================

/** 获取会话历史消息 */
export function getMessages(sessionId: number) {
  return request.get<ChatMessage[]>(`/chat/messages/${sessionId}`)
}

/** 获取会话详情（含消息） */
export function getSessionMessages(sessionId: number) {
  return request.get<ChatMessage[]>(`/chat/session/${sessionId}/messages`)
}

/** 分页获取会话历史消息（倒序，最新的在前） */
export function getSessionMessagesPage(sessionId: number, page: number, size: number = 20) {
  return request.get<{
    records: ChatMessage[]
    total: number
    current: number
    size: number
    pages: number
  }>(`/chat/session/${sessionId}/messages/page`, { params: { page, size } })
}

// ==================== 图片上传 ====================

/** 上传 AI 对话图片 */
export function uploadChatImage(file: File) {
  const formData = new FormData()
  formData.append('file', file)
  return request.post<string>('/chat/upload-image', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  })
}

// ==================== V2 SSE 流式问答（6 步编排 + MCP 工具调用） ====================

/**
 * V2 SSE 流式问答
 * 端点: /ai/v2/chat/stream
 * 核心流程: 会话构建 → LLM 工具决策 → Schema 校验 → MCP 工具执行 → 循环判断 → 二次 LLM 生成回复
 *
 * 支持 12 个 MCP 工具：购物车(5) + 商品查询(3) + 订单(2) + 售后(2)
 * 事件格式与 V1 兼容：session / msg / done
 */
export function streamChatV2(
  message: string,
  sessionId: number | undefined,
  imgUrl: string | undefined,
  ragEnabled: boolean,
  toolEnabled: boolean,
  onMessage: (chunk: string) => void,
  onSession: (sessionId: number) => void,
  onDone: () => void,
  onError: (err: string) => void,
): AbortController {
  const controller = new AbortController()
  const params = new URLSearchParams()
  params.set('message', message)
  if (sessionId) params.set('sessionId', String(sessionId))
  if (imgUrl) params.set('imgUrl', imgUrl)
  params.set('ragEnabled', String(ragEnabled))
  params.set('toolEnabled', String(toolEnabled))

  const token = useAuthStore().token || ''
  const url = `/api/ai/v2/chat/stream?${params.toString()}`

  fetch(url, {
    headers: { token },
    signal: controller.signal,
  })
    .then(async (response) => {
      if (!response.ok) {
        throw new Error(`HTTP ${response.status}`)
      }
      const reader = response.body?.getReader()
      if (!reader) throw new Error('No reader')

      const decoder = new TextDecoder()
      let buffer = ''

      let currentEvent = ''

      // 完成标记：保证无论流正常收到 event:done，还是被服务端/网络掐断，
      // 都只复位一次 UI，避免永远卡在“思考中”。
      let finished = false
      const doneOnce = () => {
        if (!finished) {
          finished = true
          onDone()
        }
      }

      while (true) {
        const { done, value } = await reader.read()
        if (done) break

        buffer += decoder.decode(value, { stream: true })
        const lines = buffer.split('\n')
        buffer = lines.pop() || ''

        for (const line of lines) {
          if (line.startsWith('event:')) {
            currentEvent = line.slice(6).trim()
            continue
          }
          if (line.startsWith('data:')) {
            const data = line.slice(5).trim()
            handleSSEEvent(currentEvent, data, onMessage, onSession, doneOnce)
          }
        }
      }

      // 流被非正常掐断（未收到 event:done）时，强制复位 UI
      doneOnce()
    })
    .catch((err) => {
      if (err.name === 'AbortError') return
      onError(err.message || '连接失败')
    })

  return controller
}

/** 获取完整图片 URL */
export function getImageUrl(path: string): string {
  if (!path) return ''
  if (path.startsWith('http')) return path
  return path
}