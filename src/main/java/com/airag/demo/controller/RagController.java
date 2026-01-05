package com.airag.demo.controller;


import com.airag.demo.services.PdfIngestionService;
import com.airag.demo.services.RagService;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@RestController
@RequestMapping("/api/rag")
public class RagController {
    private final RagService ragService;
    private final PdfIngestionService ingestionService;

    public RagController(RagService ragService, PdfIngestionService ingestionService) {
        this.ragService = ragService;
        this.ingestionService = ingestionService;
    }

    @PostMapping("/upload")
    public String upload(@RequestParam MultipartFile file) throws IOException {
        Path temp = Files.createTempFile("pdf", ".pdf");
        file.transferTo(temp.toFile());
        ingestionService.ingestPdf(temp);
        return "PDF ingested successfully!";
    }

    @GetMapping("/ask")
    public String ask(@RequestParam String q) {
        return ragService.ask(q);
    }
}

