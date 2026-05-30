package cs.sbs.web.personalprojectweb2026.controller;

import cs.sbs.web.personalprojectweb2026.config.SecurityUtil;
import cs.sbs.web.personalprojectweb2026.service.RagService;
import cs.sbs.web.personalprojectweb2026.service.RagService.RagResult;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.StreamingChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final RagService ragService;
    private final ChatModel chatModel;
    private final StreamingChatModel streamingChatModel;
    private final SecurityUtil securityUtil;

    /**
     * Non-streaming RAG Q&A.
     */
    @PostMapping("/ask")
    public ResponseEntity<?> ask(@RequestBody Map<String, Object> body) {
        Long kbId = Long.valueOf(body.get("knowledgeBaseId").toString());
        String question = body.get("question").toString();

        RagResult result = ragService.ask(kbId, question);
        return ResponseEntity.ok(Map.of(
                "answer", result.answer(),
                "sources", result.sources()
        ));
    }

    /**
     * SSE streaming RAG Q&A.
     */
    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(@RequestBody Map<String, Object> body) {
        Long kbId = Long.valueOf(body.get("knowledgeBaseId").toString());
        String question = body.get("question").toString();

        SseEmitter emitter = new SseEmitter(300000L); // 5 min timeout

        // Run in a separate thread
        new Thread(() -> {
            try {
                // Build the rendered prompt (with retrieved context)
                var rendered = ragService.buildRenderedPrompt(kbId, question);
                List<Message> messages = new ArrayList<>();
                messages.add(new SystemMessage(rendered.systemPrompt()));
                messages.add(new UserMessage(rendered.userPrompt()));

                // Stream the response
                Flux<ChatResponse> flux = streamingChatModel.stream(new Prompt(messages));
                StringBuilder fullAnswer = new StringBuilder();

                flux.doOnNext(chunk -> {
                    String content = chunk.getResult().getOutput().getText();
                    if (content != null) {
                        fullAnswer.append(content);
                        try {
                            emitter.send(SseEmitter.event()
                                    .name("message")
                                    .data(content));
                        } catch (Exception e) {
                            // client disconnected
                        }
                    }
                }).doOnComplete(() -> {
                    try {
                        // Get sources from the same retrieval
                        RagResult result = ragService.ask(kbId, question);
                        emitter.send(SseEmitter.event()
                                .name("sources")
                                .data(result.sources()));
                        emitter.complete();
                    } catch (Exception e) {
                        emitter.completeWithError(e);
                    }
                }).doOnError(e -> {
                    emitter.completeWithError(e);
                }).subscribe();

            } catch (Exception e) {
                emitter.completeWithError(e);
            }
        }).start();

        return emitter;
    }
}
