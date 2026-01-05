package com.airag.demo.services;


import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Service
public class PdfIngestionService {
    private final VectorStore vectorStore;

    public PdfIngestionService(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    public void ingestPdf(Path pdfPath) throws IOException {
        try (PDDocument doc = Loader.loadPDF(pdfPath.toFile())) {
            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(doc);

            // Split into chunks
            List<String> chunks = Arrays.stream(text.split("(?<=\\.)"))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .toList();

            // Convert chunks to Documents
            List<Document> documents = chunks.stream()
                    .map(chunk -> new Document(chunk, Map.of("source", pdfPath.getFileName().toString())))
                    .toList();

            vectorStore.add(documents);
        }
    }
}

