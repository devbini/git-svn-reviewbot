package com.cbkim.code_review.controller

import com.cbkim.code_review.dto.GitLabWebhookMergeRequestEvent
import com.cbkim.code_review.service.GitLabApiClient
import com.cbkim.code_review.service.OllamaService
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono

@RestController
class GitLabWebhookController(
    private val gitLabApiClient: GitLabApiClient,
    private val ollamaService: OllamaService
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    @PostMapping("/webhook/gitlab")
    fun handleGitLabWebhook(
        @RequestHeader("X-Gitlab-Event") eventHeader: String,
        @RequestBody event: GitLabWebhookMergeRequestEvent
    ): Mono<ResponseEntity<String>> {
        if (eventHeader != "Merge Request Hook") {
            logger.info("MR이 아님 : $eventHeader")
            return Mono.just(ResponseEntity("Not a MR", HttpStatus.OK))
        }

        // open 또는 update일 때만, close 제외
        if (event.objectAttributes.action == "open" || event.objectAttributes.action == "update") {
            logger.info("리뷰 대상 : ${event.objectAttributes.url}")

            val projectId = event.project.id
            val mrIid = event.objectAttributes.iid
            val mrTitle = event.objectAttributes.title

            return gitLabApiClient.getMergeRequestDiff(projectId, mrIid)
                .flatMap { diffContent ->
                    if (diffContent.isBlank()) {
                        logger.info("Diff 못 찾음 : $mrTitle")
                        return@flatMap Mono.just("No diff.")
                    }

                    logger.info("Fetched diff for MR ($mrTitle):\n$diffContent")

                    // 코드 리뷰 요청 프롬프트 (AI발)
                    val reviewPrompt = """
                        Please provide a code review for the following code changes. 
                        Focus on potential bugs, code style, security vulnerabilities, and best practices. 
                        Give me a concise markdown response.

                        ---
                        Merge Request Title: $mrTitle
                        ---
                        Code Diff:
                        $diffContent
                        ---
                    """.trimIndent()

                    // Ollama 서비스 호출부
                    ollamaService.generateResponse(reviewPrompt)
                        .flatMap { reviewComment ->
                            logger.info("리뷰 생성 : ($mrTitle):\n$reviewComment")
                            gitLabApiClient.addMergeRequestComment(projectId, mrIid, reviewComment)
                                .map { "Review posted success MR: $mrTitle" }
                        }
                        .onErrorResume { e ->
                            logger.error("Error code for MR ($mrTitle): ${e.message}", e)
                            Mono.just("Error review: ${e.message}")
                        }
                }
                .map { ResponseEntity(it, HttpStatus.OK) }
                .onErrorResume { e ->
                    logger.error("Error Webhook: ${e.message}", e)
                    Mono.just(ResponseEntity("Error webhook: ${e.message}", HttpStatus.INTERNAL_SERVER_ERROR))
                }
        } else {
            logger.info("Ignoring action: ${event.objectAttributes.action}")
            return Mono.just(ResponseEntity("Ignoring", HttpStatus.OK))
        }
    }
}