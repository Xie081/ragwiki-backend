import api from './index'
import type { ChatMessage } from '@/types'

export interface ChatAskResponse {
  answer: string
  sources: Array<{ documentTitle: string; snippet: string }>
}

export function askQuestion(knowledgeBaseId: number, question: string, history?: ChatMessage[]) {
  return api.post<ChatAskResponse>('/chat/ask', { knowledgeBaseId, question, history })
}

/**
 * SSE streaming chat. Returns an AbortController to cancel.
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
