
package com.airag.demo.services;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Path;
import java.text.BreakIterator;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class PdfIngestionService {
    private final VectorStore vectorStore;
    private static final int MAX_CHARS_PER_CHUNK = 1400; // keep well under embedding token limits

    public PdfIngestionService(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    public void ingestPdf(Path pdfPath) throws IOException {
        // Load PDF (fail fast with clear messages for encrypted docs)
        try (PDDocument doc = Loader.loadPDF(pdfPath.toFile())) {
            // If encrypted, verify we can actually extract text
            if (doc.isEncrypted() && !doc.getCurrentAccessPermission().canExtractContent()) {
                throw new IllegalArgumentException("PDF is encrypted or restricts text extraction. " +
                        "Please provide a decrypted copy or a PDF with extraction permissions.");
            }

            PDFTextStripper stripper = new PDFTextStripper();
            int pageCount = doc.getNumberOfPages();
            List<Document> documents = new ArrayList<>();

            for (int page = 1; page <= pageCount; page++) {
                stripper.setStartPage(page);
                stripper.setEndPage(page);

                String pageText = safeNormalize(stripper.getText(doc));
                if (pageText.isBlank()) {
                    // Skip image-only or empty pages
                    continue;
                }

                // Sentence-aware chunking, then fold into fixed-size chunks
                List<String> sentenceChunks = sentenceChunks(pageText, MAX_CHARS_PER_CHUNK);
                int idx = 0;
                for (String chunk : sentenceChunks) {
                    if (chunk.isBlank()) continue;

                    Map<String, Object> metadata = Map.of(
                            "source", pdfPath.getFileName().toString(),
                            "page", page,
                            "chunkIndex", idx++
                    );

                    documents.add(new Document(chunk, metadata));
                }
            }

            if (documents.isEmpty()) {
                throw new IllegalArgumentException("No extractable text found in PDF (possibly scanned images).");
            }

            // Write to vector store (batch if you expect large volumes)
            vectorStore.add(documents);
        }
        // Loader / PDDocument close automatically via try-with-resources
    }

    private static String safeNormalize(String s) {
        // Optional: collapse excessive whitespace, normalize non-breaking spaces, etc.
        return s.replace('\u00A0', ' ').trim();
    }

    /**
     * Use BreakIterator to split into sentences and then pack them
     * into chunks of ~MAX_CHARS_PER_CHUNK.
     */
    private static List<String> sentenceChunks(String text, int maxChars) {
        List<String> out = new ArrayList<>();
        BreakIterator it = BreakIterator.getSentenceInstance(Locale.ENGLISH);
        it.setText(text);

        StringBuilder current = new StringBuilder();
        int start = it.first();
        for (int end = it.next(); end != BreakIterator.DONE; start = end, end = it.next()) {
            String sentence = text.substring(start, end).trim();
            if (sentence.isEmpty()) continue;

            if (current.length() + sentence.length() + 1 <= maxChars) {
                current.append(sentence).append(' ');
            } else {
                out.add(current.toString().trim());
                current.setLength(0);
                current.append(sentence).append(' ');
            }
        }
        if (current.length() > 0) {
            out.add(current.toString().trim());
        }
        return out;
    }
}
