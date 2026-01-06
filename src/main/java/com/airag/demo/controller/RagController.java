package com.airag.demo.controller;


import com.airag.demo.services.PdfIngestionService;
import com.airag.demo.services.RagService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Mono;

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


    @PostMapping(value = "/upload")
    public Mono<String> upload(@RequestPart("file") FilePart file) throws IOException {
        Path temp = Files.createTempFile("pdf", ".pdf"); // use reactive-friendly APIs
        return file.transferTo(temp.toFile())
                .then(Mono.fromCallable(() -> {
                    ingestionService.ingestPdf(temp);
                    return "PDF ingested successfully!";
                }));
    }


    @GetMapping("/ask")
    public String ask(@RequestParam String q) {
        return ragService.ask(q);
    }

    @GetMapping("/ping")
    public String ping() { return "pong"; }

}

