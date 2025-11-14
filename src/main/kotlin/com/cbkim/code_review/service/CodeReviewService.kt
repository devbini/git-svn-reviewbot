package com.cbkim.code_review.service

import org.springframework.stereotype.Service
import reactor.core.publisher.Mono

@Service
class CodeReviewService(
    private val ollamaService: OllamaService
) {
    fun requestReview(title: String, diffContent: String): Mono<String> {
        if (diffContent.isBlank()) {
            return Mono.just("리뷰할 Diff 내용이 없습니다.")
        }

        // GitLab 컨트롤러에 있던 프롬프트를 이곳으로 이동
        val reviewPrompt = """
            Please provide a code review for the following code changes. 
            Focus on potential bugs, code style, security vulnerabilities, and best practices. 
            Give me a concise markdown response.

            ---
            Title: $title
            ---
            Code Diff:
            $diffContent
            ---
        """.trimIndent()

        // Ollama 서비스 호출
        return ollamaService.generateResponse(reviewPrompt)
    }
}