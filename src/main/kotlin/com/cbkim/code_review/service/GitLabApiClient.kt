package com.cbkim.code_review.service

import com.cbkim.code_review.dto.GitLabCommentRequest
import com.cbkim.code_review.dto.GitLabCommentResponse
import com.cbkim.code_review.dto.GitLabDiffResponse
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToFlux
import reactor.core.publisher.Mono

@Service
class GitLabApiClient(
    private val webClientBuilder: WebClient.Builder,
    @Value("\${gitlab.baseUrl}") private val gitlabBaseUrl: String,
    @Value("\${gitlab.privateToken}") private val gitlabPrivateToken: String
) {
    private val webClient = webClientBuilder
        .baseUrl("$gitlabBaseUrl/api/v4")
        .defaultHeader("Private-Token", gitlabPrivateToken)
        .build()

    fun getMergeRequestDiff(projectId: Long, mrIid: Long): Mono<String> {
        return webClient.get()
            .uri("/projects/{projectId}/merge_requests/{mrIid}/diffs", projectId, mrIid)
            .retrieve()
            .bodyToFlux<GitLabDiffResponse>() // diff가 여러개일 수 있기 때문에 flux로 우선 처리...
            .map { it.diff }
            .collectList()
            .map { it.joinToString("\n---\n") } // diff 하나로 변환
            .switchIfEmpty(Mono.just("")) // 없는 경우 예외처리
    }

    fun addMergeRequestComment(projectId: Long, mrIid: Long, commentBody: String): Mono<GitLabCommentResponse> {
        val request = GitLabCommentRequest(body = commentBody)
        return webClient.post()
            .uri("/projects/{projectId}/merge_requests/{mrIid}/notes", projectId, mrIid)
            .bodyValue(request)
            .retrieve()
            .bodyToMono(GitLabCommentResponse::class.java)
    }
}