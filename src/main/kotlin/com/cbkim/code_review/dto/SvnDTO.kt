package com.cbkim.code_review.dto

/**
 * post-commit 훅에서 Spring Boot로 전송할 JSON
 */
data class SvnWebhookRequest(
    val revision: Long,
    val author: String,
    val logMessage: String,
    val diffContent: String
)

/**
 * Spring Boot가 post-commit 훅 스크립트에 응답할 JSON
 */
data class SvnWebhookResponse(
    val reviewId: Long, // 여기서는 revision과 동일한 번호
    val reviewUrl: String, // "http://서버주소/api/reviews/svn/123"
    val message: String
)