package com.cbkim.code_review.service

import com.cbkim.code_review.dto.GitLabCommentRequest
import com.cbkim.code_review.dto.GitLabCommentResponse
import com.cbkim.code_review.dto.GitLabDiffResponse
import com.cbkim.code_review.dto.GitLabWebhookMergeRequestEvent
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToFlux
import reactor.core.publisher.Mono

@Service
class GitLabApiClientService(
    private val webClientBuilder: WebClient.Builder,
    private val codeReviewService: CodeReviewService,
    @Value("\${gitlab.baseUrl}") private val gitlabBaseUrl: String,
    @Value("\${gitlab.privateToken}") private val gitlabPrivateToken: String
) {
    private val logger = LoggerFactory.getLogger(javaClass)

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

    fun startCodeReview(eventHeader: String, event: GitLabWebhookMergeRequestEvent): Mono<ResponseEntity<String>> {
        if (eventHeader != "Merge Request Hook") {
            logger.info("MR이 아님 : $eventHeader")
            return Mono.just(ResponseEntity("Not a MR", HttpStatus.OK))
        }

        if (event.objectAttributes.action == "open" || event.objectAttributes.action == "update") {
            logger.info("리뷰 대상 : ${event.objectAttributes.url}")

            val projectId = event.project.id
            val mrIid = event.objectAttributes.iid
            val mrTitle = event.objectAttributes.title

            return getMergeRequestDiff(projectId, mrIid)
                .flatMap { diffContent ->
                    if (diffContent.isBlank()) {
                        logger.info("Diff 못 찾음 : $mrTitle")
                        return@flatMap Mono.just("No diff.")
                    }

                    logger.info("Fetched diff for MR ($mrTitle):\n$diffContent")

                    codeReviewService.requestReview(mrTitle, diffContent)
                        .flatMap { reviewComment ->
                            logger.info("리뷰 생성 : ($mrTitle):\n$reviewComment")
                            addMergeRequestComment(projectId, mrIid, reviewComment)
                                .map { "리뷰 생성 성공 MR: $mrTitle" }
                        }
                        .onErrorResume { e ->
                            logger.error("리뷰 생성 실패 MR : ($mrTitle): ${e.message}", e)
                            Mono.just("Error review: ${e.message}")
                        }
                }
                .map { responseBody ->
                    ResponseEntity(responseBody, HttpStatus.OK)
                }
                .onErrorResume { e ->
                    logger.error("Error Webhook : ${e.message}", e)
                    Mono.just(ResponseEntity("Error webhook: ${e.message}", HttpStatus.INTERNAL_SERVER_ERROR))
                }
        } else {
            logger.info("무시 : ${event.objectAttributes.action}")
            return Mono.just(ResponseEntity("Ignoring", HttpStatus.OK))
        }
    }
}