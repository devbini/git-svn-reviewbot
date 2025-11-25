package com.cbkim.code_review.dto

/**
 * post-commit 훅에서 Spring Boot로 전송할 JSON
 */
data class SvnWebhookRequest(
    val repo: String,
    val revision: Long,
    val author: String,
    val message: String
)

/**
 * Spring Boot가 post-commit 훅 스크립트에 응답할 JSON
 */
data class SvnWebhookResponse(
    val reviewId: Long,
    val reviewUrl: String,
    val message: String
)