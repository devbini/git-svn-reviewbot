package com.cbkim.code_review.controller

import com.cbkim.code_review.dto.SvnWebhookRequest
import com.cbkim.code_review.dto.SvnWebhookResponse
import com.cbkim.code_review.service.SvnReviewService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.FileSystemResource
import org.springframework.core.io.Resource
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers
import java.io.File // File import 확인

@RestController
class SvnWebhookController(
    private val svnReviewService: SvnReviewService,
    @Value("\${server.base-url:http://localhost:8080}")
    private val baseUrl: String
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    // SVN에서 hook을 통해 실행 할 API 엔드포인트
    @PostMapping("/webhook/svn")
    fun handleSvnWebhook(@RequestBody request: SvnWebhookRequest): Mono<ResponseEntity<SvnWebhookResponse>> {

        svnReviewService.processSvnReviewAsync(request)

        logger.info("SVN 훅 접수 완료: [${request.repo}] r${request.revision}")

        val repoName = File(request.repo).name
        val reviewUrl = "$baseUrl/api/reviews/svn/$repoName/${request.revision}"

        val response = SvnWebhookResponse(
            reviewId = request.revision,
            reviewUrl = reviewUrl,
            message = "Review request accepted."
        )

        return Mono.just(ResponseEntity.ok(response))
    }

    // 결과 조회용
    @GetMapping(
        "/api/reviews/svn/{repoName}/{revision}",
        produces = [MediaType.TEXT_MARKDOWN_VALUE]
    )
    fun getSvnReviewMarkdown(
        @PathVariable repoName: String,
        @PathVariable revision: Long
    ): Mono<ResponseEntity<Resource>> {

        return Mono.fromCallable {
            val finalFile = svnReviewService.getReviewFile(repoName, revision, ".md")
            val processingFile = svnReviewService.getReviewFile(repoName, revision, ".processing")

            if (finalFile.exists()) {
                ResponseEntity.ok(FileSystemResource(finalFile) as Resource)
            } else if (processingFile.exists()) {
                val msg = "# 처리 중...\n\nAI가 열심히 코드를 분석하고 있습니다. 잠시 후 새로고침 해주세요."
                ResponseEntity.ok(FileSystemResource(processingFile) as Resource)
            } else {
                ResponseEntity.notFound().build()
            }
        }.subscribeOn(Schedulers.boundedElastic())
    }
}