import request from './request'

/** 发送客服消息（AI/商家客服） */
export function sendCustomerMessage(data: {
  sessionId?: number
  message: string
  msgType?: number
}) {
  return request.post('/customer-chat/send', data)
}

/** 获取客服会话列表 */
export function getCustomerSessions() {
  return request.get('/customer-chat/sessions')
}

/** 获取会话消息 */
export function getCustomerMessages(sessionId: number, type: string = 'merchant') {
  return request.get(`/customer-chat/messages/${sessionId}`, { params: { type } })
}

/** 获取客服聊天历史记录 */
export function getChatHistory() {
  return request.get('/customer-chat/history')
}