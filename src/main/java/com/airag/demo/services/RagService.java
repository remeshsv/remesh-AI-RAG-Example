package com.airag.demo.services;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

@Service
public class RagService {

    private final ChatClient chatClient;

    public RagService(ChatModel chatModel, VectorStore vectorStore) {
        var advisor = QuestionAnswerAdvisor.builder(vectorStore)
                .searchRequest(SearchRequest.builder().topK(5).build())
                .build();

        this.chatClient = ChatClient.builder(chatModel)
                .defaultAdvisors(advisor)
                .build();
    }

    public String ask(String question) {
        return chatClient.prompt()
                .system("Answer using only the provided documents.")
                .user(question)
                .call()
                .content();
    }

}
