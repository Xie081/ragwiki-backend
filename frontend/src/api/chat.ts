import api from './index'
import type { ChatMessage } from '@/types'

export interface RemoteMessage {
  role: string
  content: string
  sources?: string
  timestamp: string
}

export function loadHistory(kbId: number) {
  return api.get<RemoteMessage[]>(`/chat/history/${kbId}`)
}

export function syncHistory(kbId: number, messages: RemoteMessage[]) {
  return api.post(`/chat/history/${kbId}`, messages)
}

/**
 * SSE 流式问答。返回 AbortController 用于取消。
 */
export function streamQuestion(
  knowledgeBaseId: number,
  question: string,
  onToken: (token: string) => void,
  onSources: (sources: Array<{ documentTitle: string; snippet: string }>) => void,
  onError: (err: Event) => void,
  onComplete: () => void,
  history?: ChatMessage[]
): AbortController {
  const controller = new AbortController()

  const token = localStorage.getItem('token')

  fetch('/api/chat/stream', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${token}`
    },
    body: JSON.stringify({ knowledgeBaseId, question, history }),
    signal: controller.signal
  }).then(response => {
    if (!response.ok || !response.body) {
      onError(new Event('error'))
      return
    }

    const reader = response.body.getReader()
    const decoder = new TextDecoder()
    let buffer = ''

    function read() {
      reader.read().then(({ done, value }) => {
        if (done) { onComplete(); return }

        buffer += decoder.decode(value, { stream: true })
        const lines = buffer.split('\n')
        buffer = lines.pop() || ''

        for (const line of lines) {
          if (line.startsWith('data:')) {
            const data = line.substring(5).trim()
            if (!data) continue

            try {
              const event = JSON.parse(data)
              if (event.type === 'token') {
                onToken(event.content)
              } else if (event.type === 'sources') {
                onSources(event.data)
              } else if (event.type === 'error') {
                onError(new Event(event.message || 'stream error'))
              }
            } catch {
              // ignore malformed JSON lines
            }
          }
        }

        read()
      }).catch(err => {
        if (err.name !== 'AbortError') onError(err)
      })
    }

    read()
  }).catch(err => {
    if (err.name !== 'AbortError') onError(err)
  })

  return controller
}
