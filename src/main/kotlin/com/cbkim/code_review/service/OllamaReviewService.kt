package com.cbkim.code_review.service

import com.cbkim.code_review.dto.OllamaGenerateRequestDTO
import com.cbkim.code_review.dto.OllamaGenerateResponseDTO
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono

@Service
class OllamaReviewService(
    private val webClientBuilder: WebClient.Builder,
    @Value("\${ollama.baseUrl}") private val ollamaBaseUrl: String,
    @Value("\${ollama.model}") private val ollamaModel: String
) {
    private val webClient = webClientBuilder.baseUrl(ollamaBaseUrl).build()

    fun generateResponse(prompt: String): Mono<String> {
        val request = OllamaGenerateRequestDTO(
            model = ollamaModel,
            prompt = prompt,
            stream = false
        )

        return webClient.post()
            .uri("/api/generate")
            .bodyValue(request)
            .retrieve()
            .bodyToMono(OllamaGenerateResponseDTO::class.java)
            .map { it.response }
    }
}