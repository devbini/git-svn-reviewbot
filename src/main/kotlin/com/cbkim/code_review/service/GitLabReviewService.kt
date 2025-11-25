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
class GitLabReviewService(
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

    fun startCodeReview(eventHeader: String, event: GitLabWebhookMergeRequestEvent) {
        if (eventHeader != "Merge Request Hook") {
            logger.info("MR이 아님 : $eventHeader")
            return
        }

        if (event.objectAttributes.action == "open" || event.objectAttributes.action == "update") {
            logger.info("리뷰 요청 수신 : ${event.objectAttributes.url}")

            val projectId = event.project.id
            val mrIid = event.objectAttributes.iid
            val mrTitle = event.objectAttributes.title

            getMergeRequestDiff(projectId, mrIid)
                .flatMap { diffContent ->
                    if (diffContent.isBlank()) {
                        logger.info("Diff 못 찾음 : $mrTitle")
                        Mono.empty()
                    } else {
                        logger.info("Diff 확보 완료. AI 분석 시작... ($mrTitle)")

                        codeReviewService.requestReview(mrTitle, diffContent)
                            .flatMap { reviewComment ->
                                logger.info("AI 분석 완료. GitLab 댓글 등록 중...")
                                addMergeRequestComment(projectId, mrIid, reviewComment)
                            }
                    }
                }
                .subscribe(
                    {
                        logger.info("리뷰 최종 완료 : $mrTitle")
                    },
                    { e ->
                        logger.error("리뷰 에러 발생 : ${e.message}", e)
                    }
                )
        } else {
            logger.info("무시 : ${event.objectAttributes.action}")
        }
    }
}