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
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers

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

        logger.info("SVN 훅 접수 완료: r${request.revision} (백그라운드 처리 시작)")

        val reviewUrl = "$baseUrl/api/reviews/svn/${request.revision}"

        val response = SvnWebhookResponse(
            reviewId = request.revision,
            reviewUrl = reviewUrl,
            message = "Review request accepted. Processing in background."
        )

        return Mono.just(ResponseEntity.ok(response))
    }

    // 해당 API는 현재 사용하지 않음...
    // 이후 마크다운 확인을 할 수 있는 FE 페이지 개발 시 사용 될 것
    // 혹시 모르니 읽기가 가능한 형태로 API를 미리 만들어 놓음
    @GetMapping(
        "/api/reviews/svn/{revision}",
        produces = [MediaType.TEXT_MARKDOWN_VALUE]
    )
    fun getSvnReviewMarkdown(@PathVariable revision: Long): Mono<ResponseEntity<Resource>> {

        return Mono.fromCallable {
            val finalFile = svnReviewService.getReviewFile(revision, ".md")
            val processingFile = svnReviewService.getReviewFile(revision, ".processing")

            if (finalFile.exists()) {
                ResponseEntity.ok(FileSystemResource(finalFile) as Resource)
            } else if (processingFile.exists()) {
                ResponseEntity.accepted().body(FileSystemResource(processingFile) as Resource)
            } else {
                ResponseEntity.notFound().build()
            }
        }.subscribeOn(Schedulers.boundedElastic())
    }
}