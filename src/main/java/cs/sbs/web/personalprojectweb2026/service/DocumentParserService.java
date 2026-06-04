package cs.sbs.web.personalprojectweb2026.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.text.TextContentRenderer;
import org.jsoup.Jsoup;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

@Service
@Slf4j
public class DocumentParserService {

    /**
     * Parse a document file and extract plain text.
     */
    public String parse(Path filePath, String fileType) throws IOException {
        return switch (fileType) {
            case "PDF"      -> parsePdf(filePath);
            case "MARKDOWN" -> parseMarkdown(filePath);
            case "TXT"      -> parseTxt(filePath);
            case "DOCX"     -> parseDocx(filePath);
            case "HTML"     -> parseHtml(filePath);
            default -> throw new IllegalArgumentException("不支持的文档格式: " + fileType);
        };
    }

    private String parsePdf(Path filePath) throws IOException {
        try (PDDocument document = Loader.loadPDF(filePath.toFile())) {
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            String text = stripper.getText(document);
            log.info("Parsed PDF: {} pages, {} chars", document.getNumberOfPages(), text.length());
            return text;
        }
    }

    private String parseMarkdown(Path filePath) throws IOException {
        String markdown = Files.readString(filePath);
        Parser parser = Parser.builder().build();
        TextContentRenderer renderer = TextContentRenderer.builder().build();
        String text = renderer.render(parser.parse(markdown));
        log.info("Parsed Markdown: {} chars (original: {} chars)", text.length(), markdown.length());
        return text;
    }

    private String parseTxt(Path filePath) throws IOException {
        String text = Files.readString(filePath);
        log.info("Parsed TXT: {} chars", text.length());
        return text;
    }

    private String parseDocx(Path filePath) throws IOException {
        try (InputStream is = Files.newInputStream(filePath);
             XWPFDocument document = new XWPFDocument(is)) {
            StringBuilder sb = new StringBuilder();
            document.getParagraphs().forEach(p -> {
                sb.append(p.getText()).append("\n");
            });
            String text = sb.toString();
            log.info("Parsed DOCX: {} paragraphs, {} chars",
                    document.getParagraphs().size(), text.length());
            return text;
        }
    }

    private String parseHtml(Path filePath) throws IOException {
        String html = Files.readString(filePath);
        String text = Jsoup.parse(html).wholeText();
        log.info("Parsed HTML: {} chars (original: {} chars)", text.length(), html.length());
        return text;
    }
}
