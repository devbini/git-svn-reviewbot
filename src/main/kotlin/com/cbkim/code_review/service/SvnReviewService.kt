package com.cbkim.code_review.service

import com.cbkim.code_review.dto.SvnWebhookRequest
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths

@Service
class SvnReviewService(
    private val codeReviewService: CodeReviewService,

    @Value("\${review.storage.path:C:/code-review}")
    private val storagePath: String
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    @Async
    fun processSvnReviewAsync(request: SvnWebhookRequest) {
        val title = "SVN r${request.revision} by ${request.author}"
        logger.info("SVN 백그라운드 리뷰 처리 시작: $title")

        val processingFile = getReviewFile(request.revision, ".processing")
        val finalFile = getReviewFile(request.revision, ".md")

        try {
            Files.createDirectories(Paths.get(storagePath))
            processingFile.writeText("AI 리뷰가 진행 중입니다 (r${request.revision})...")

            val reviewComment = codeReviewService.requestReview(title, request.diffContent)
                .block()

            if (reviewComment != null) {
                finalFile.writeText(reviewComment)
                logger.info("SVN 리뷰 파일 저장 완료: ${finalFile.path}")
            }

        } catch (e: Exception) {
            logger.error("SVN 리뷰 파일 생성 실패 (r${request.revision}): ${e.message}", e)
            finalFile.writeText("AI 리뷰 생성 중 오류가 발생했습니다.\n\n${e.message}")
        } finally {
            processingFile.delete()
        }
    }

    fun getReviewFile(revision: Long, extension: String): File {
        val fileName = "r$revision$extension"
        return Paths.get(storagePath, fileName).toFile()
    }
}