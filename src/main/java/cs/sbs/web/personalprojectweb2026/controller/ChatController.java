package cs.sbs.web.personalprojectweb2026.controller;

import cs.sbs.web.personalprojectweb2026.config.SecurityUtil;
import cs.sbs.web.personalprojectweb2026.model.entity.ChatMessage;
import cs.sbs.web.personalprojectweb2026.repository.ChatMessageRepository;
import cs.sbs.web.personalprojectweb2026.service.RagService;
import cs.sbs.web.personalprojectweb2026.service.RagService.ConversationMessage;
import cs.sbs.web.personalprojectweb2026.service.RagService.RagResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.StreamingChatModel;
import org.springframework.ai.chat.prompt.Prompt;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
@Slf4j
public class ChatController {

    private final RagService ragService;
    private final StreamingChatModel streamingChatModel;
    private final Executor taskExecutor;
    private final ObjectMapper objectMapper;
    private final SecurityUtil securityUtil;
    private final ChatMessageRepository chatMessageRepository;

    /**
     * Non-streaming RAG Q&A.
     */
    @PostMapping("/ask")
    public ResponseEntity<?> ask(@RequestBody Map<String, Object> body) {
        Long kbId = Long.valueOf(body.get("knowledgeBaseId").toString());
        String question = body.get("question").toString();

        // Input validation
        validateQuestion(question);

        List<ConversationMessage> history = parseHistory(body);

        RagResult result = ragService.ask(kbId, question, history);
        return ResponseEntity.ok(Map.of(
                "answer", result.answer(),
                "sources", result.sources()
        ));
    }

    /**
     * SSE streaming RAG Q&A.
     * Sends JSON-enveloped events: {"type":"token","content":"..."} and {"type":"sources","data":[...]}
     */
    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(@RequestBody Map<String, Object> body) {
        Long kbId = Long.valueOf(body.get("knowledgeBaseId").toString());
        String question = body.get("question").toString();

        // Input validation
        validateQuestion(question);

        List<ConversationMessage> history = parseHistory(body);

        SseEmitter emitter = new SseEmitter(300000L); // 5 min timeout

        taskExecutor.execute(() -> {
            try {
                // Single retrieval: get both messages and sources in one call
                var renderedWithSources = ragService.buildRenderedPrompt(kbId, question, history);

                // Stream the response
                Flux<ChatResponse> flux = streamingChatModel.stream(
                        new Prompt(renderedWithSources.messages()));

                flux.doOnNext(chunk -> {
                    String content = chunk.getResult().getOutput().getText();
                    if (content != null) {
                        try {
                            String json = objectMapper.writeValueAsString(
                                    Map.of("type", "token", "content", content));
                            emitter.send(SseEmitter.event()
                                    .name("message")
                                    .data(json));
                        } catch (Exception e) {
                            // client disconnected, stop the flux
                        }
                    }
                }).doOnComplete(() -> {
                    try {
                        // Send sources (from the single retrieval, no double LLM call)
                        String sourcesJson = objectMapper.writeValueAsString(
                                Map.of("type", "sources", "data", renderedWithSources.sources()));
                        emitter.send(SseEmitter.event()
                                .name("sources")
                                .data(sourcesJson));
                        emitter.complete();
                    } catch (Exception e) {
                        emitter.completeWithError(e);
                    }
                }).doOnError(e -> {
                    log.error("Streaming error for question: {}", question, e);
                    try {
                        String errorJson = objectMapper.writeValueAsString(
                                Map.of("type", "error", "message", e.getMessage()));
                        emitter.send(SseEmitter.event()
                                .name("error")
                                .data(errorJson));
                    } catch (Exception ignored) {}
                    emitter.completeWithError(e);
                }).subscribe();

            } catch (Exception e) {
                log.error("Failed to build prompt for question: {}", question, e);
                emitter.completeWithError(e);
            }
        });

        // Clean up on timeout or client disconnect
        emitter.onTimeout(() -> {
            log.debug("SSE emitter timed out");
            emitter.complete();
        });
        emitter.onError(emitter::completeWithError);

        return emitter;
    }

    @SuppressWarnings("unchecked")
    private List<ConversationMessage> parseHistory(Map<String, Object> body) {
        List<ConversationMessage> history = new ArrayList<>();
        Object historyObj = body.get("history");
        if (historyObj instanceof List<?> list) {
            for (Object item : list) {
                if (item instanceof Map<?, ?> map) {
                    Object roleObj = map.get("role");
                    Object contentObj = map.get("content");
                    String role = roleObj != null ? roleObj.toString() : "";
                    String content = contentObj != null ? contentObj.toString() : "";
                    if (!role.isEmpty() && !content.isEmpty()) {
                        history.add(new ConversationMessage(role, content));
                    }
                }
            }
        }
        // Keep only last 3 rounds (6 messages) to avoid exceeding context window
        if (history.size() > 6) {
            history = history.subList(history.size() - 6, history.size());
        }
        return history;
    }

    private void validateQuestion(String question) {
        if (question == null || question.isBlank()) {
            throw new IllegalArgumentException("问题不能为空");
        }
        if (question.length() > 2000) {
            throw new IllegalArgumentException("问题长度不能超过2000个字符");
        }
    }

    @GetMapping("/history/{kbId}")
    public ResponseEntity<?> loadHistory(@PathVariable Long kbId) {
        Long userId = securityUtil.getCurrentUserId();
        List<ChatMessage> messages = chatMessageRepository.findByUserIdAndKbIdOrderByCreatedAtAsc(userId, kbId);
        var list = messages.stream()
                .map(m -> {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("role", m.getRole());
                    item.put("content", m.getContent());
                    if (m.getSources() != null) item.put("sources", m.getSources());
                    item.put("timestamp", m.getCreatedAt().toString());
                    return item;
                })
                .collect(Collectors.toList());
        return ResponseEntity.ok(list);
    }

    @PostMapping("/history/{kbId}")
    public ResponseEntity<?> syncHistory(@PathVariable Long kbId, @RequestBody List<Map<String, Object>> body) {
        Long userId = securityUtil.getCurrentUserId();
        // Replace: delete old messages for this user+kb, then insert new ones
        chatMessageRepository.deleteByKbId(kbId);
        for (Map<String, Object> item : body) {
            ChatMessage msg = ChatMessage.builder()
                    .userId(userId)
                    .kbId(kbId)
                    .role(item.get("role").toString())
                    .content(item.get("content").toString())
                    .sources(item.get("sources") != null ? item.get("sources").toString() : null)
                    .build();
            chatMessageRepository.save(msg);
        }
        return ResponseEntity.ok(Map.of("synced", body.size()));
    }
}
