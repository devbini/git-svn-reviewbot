package com.cbkim.code_review.service

import org.springframework.stereotype.Service
import reactor.core.publisher.Mono

@Service
class CodeReviewService(
    private val ollamaService: OllamaReviewService
) {
    fun requestReview(title: String, diffContent: String): Mono<String> {
        if (diffContent.isBlank()) {
            return Mono.just("리뷰할 Diff 내용이 없습니다.")
        }

        val reviewPrompt = """
            다음 코드 변경 사항에 대한 코드 리뷰를 한국어로 작성해 주세요.
            잠재적인 버그, 코드 스타일, 보안 취약점, 모범 사례에 초점을 맞춰주세요.
            간결한 마크다운 형식으로 응답해 주세요.

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