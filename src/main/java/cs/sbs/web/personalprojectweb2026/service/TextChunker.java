package cs.sbs.web.personalprojectweb2026.service;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Simple text chunker using character-based splitting with overlap.
 * Chunk size ~500 chars, overlap ~100 chars.
 */
@Component
public class TextChunker {

    private static final int DEFAULT_CHUNK_SIZE = 500;
    private static final int DEFAULT_OVERLAP = 100;

    public List<String> chunk(String text) {
        return chunk(text, DEFAULT_CHUNK_SIZE, DEFAULT_OVERLAP);
    }

    public List<String> chunk(String text, int chunkSize, int overlap) {
        List<String> chunks = new ArrayList<>();
        if (text == null || text.isBlank()) {
            return chunks;
        }

        // Clean excessive whitespace but preserve paragraph breaks
        text = text.replaceAll("\\r\\n", "\n").replaceAll("\\r", "\n");
        text = text.replaceAll("\\n{4,}", "\n\n\n");

        int start = 0;
        while (start < text.length()) {
            int end = Math.min(start + chunkSize, text.length());

            // Try to break at a natural boundary (paragraph or sentence)
            if (end < text.length()) {
                int breakPoint = findBreakPoint(text, start, end);
                if (breakPoint > start) {
                    end = breakPoint;
                }
            }

            String chunk = text.substring(start, end).trim();
            if (!chunk.isEmpty()) {
                chunks.add(chunk);
            }

            // Advance with overlap; ensure forward progress
            int nextStart = end - overlap;
            if (nextStart <= start || nextStart >= text.length()) break;
            start = nextStart;
        }

        return chunks;
    }

    private int findBreakPoint(String text, int start, int end) {
        // Search backwards from end for natural break points
        int searchStart = Math.max(start + 200, end - 100); // don't break too early
        for (int i = end; i >= searchStart; i--) {
            char c = text.charAt(i);
            // Break at paragraph boundary
            if (c == '\n' && i + 1 < text.length() && text.charAt(i + 1) == '\n') {
                return i;
            }
            // Break at sentence end
            if ((c == '.' || c == '。' || c == '!' || c == '！' || c == '?' || c == '？')
                    && i + 1 < text.length() && Character.isWhitespace(text.charAt(i + 1))) {
                return i + 1;
            }
        }
        // Fallback: break at last space
        int lastSpace = text.lastIndexOf(' ', end);
        return lastSpace > searchStart ? lastSpace : end;
    }
}
