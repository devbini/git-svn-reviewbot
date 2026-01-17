package com.cbkim.code_review.service

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono

@Service
class CodeReviewService(
    private val ollamaService: OllamaReviewService,
    @Value("\${review.prompt}") private val reviewInstruction: String
) {
    fun requestReview(title: String, diffContent: String): Mono<String> {
        if (diffContent.isBlank()) {
            return Mono.just("리뷰할 Diff 내용이 없습니다.")
        }

        val reviewPrompt = """
            $reviewInstruction
            
            ---
            제목: $title
            ---
            코드 변경 내용(Diff):
            $diffContent
            ---
        """.trimIndent()

        // Ollama 서비스 호출
        return ollamaService.generateResponse(reviewPrompt)
    }
}