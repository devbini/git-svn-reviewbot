package com.cbkim.code_review.controller

import com.cbkim.code_review.dto.GitLabWebhookMergeRequestEvent
import com.cbkim.code_review.service.GitLabApiClientService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono

@RestController
class GitLabWebhookController(
    private val gitLabApiClient: GitLabApiClientService,
) {
    @PostMapping("/webhook/gitlab")
    fun handleGitLabWebhook(
        @RequestHeader("X-Gitlab-Event") eventHeader: String,
        @RequestBody event: GitLabWebhookMergeRequestEvent
    ): Mono<ResponseEntity<String>> {
        return gitLabApiClient.startCodeReview(eventHeader, event);
    }
}