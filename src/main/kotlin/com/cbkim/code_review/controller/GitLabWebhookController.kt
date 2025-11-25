package com.cbkim.code_review.controller

import com.cbkim.code_review.dto.GitLabWebhookMergeRequestEvent
import com.cbkim.code_review.service.GitLabReviewService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RestController

@RestController
class GitLabWebhookController(
    private val gitLabApiClient: GitLabReviewService,
) {
    // 깃랩에서 WebHook을 통해 실행 할 API 엔드포인트
    @PostMapping("/webhook/gitlab")
    fun handleGitLabWebhook(
        @RequestHeader("X-Gitlab-Event") eventHeader: String,
        @RequestBody event: GitLabWebhookMergeRequestEvent
    ): ResponseEntity<String> {
        gitLabApiClient.startCodeReview(eventHeader, event)
        return ResponseEntity.ok("Processing start")
    }
}