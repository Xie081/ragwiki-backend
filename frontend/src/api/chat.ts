import api from './index'

export interface ChatAskResponse {
  answer: string
  sources: Array<{ documentTitle: string; snippet: string }>
}

export function askQuestion(knowledgeBaseId: number, question: string) {
  return api.post<ChatAskResponse>('/chat/ask', { knowledgeBaseId, question })
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
  onComplete: () => void
): AbortController {
  const controller = new AbortController()

  const token = localStorage.getItem('token')

  fetch('/api/chat/stream', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${token}`
    },
    body: JSON.stringify({ knowledgeBaseId, question }),
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
              const parsed = JSON.parse('[' + data.replace(/}{/g, '},{') + ']')
              // Actually, SSE data comes line by line, try parsing as-is
              try {
                const obj = JSON.parse(data)
                if (Array.isArray(obj)) {
                  onSources(obj)
                } else {
                  onToken(data)
                }
              } catch {
                // Raw string token
                onToken(data)
              }
            } catch {
              onToken(data)
            }
          } else if (line.startsWith('event:sources')) {
            // next data line will be sources
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
