package com.cbkim.code_review.service

import com.cbkim.code_review.dto.SvnWebhookRequest
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import org.tmatesoft.svn.core.SVNURL
import org.tmatesoft.svn.core.wc.SVNClientManager
import org.tmatesoft.svn.core.wc.SVNRevision
import org.tmatesoft.svn.core.wc.SVNWCUtil
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths

@Service
class SvnReviewService(
    private val codeReviewService: CodeReviewService,

    @Value("\${review.storage.path:C:/code-review}")
    private val storagePath: String,

    @Value("\${svn.server.url:svn://localhost:3690}")
    private val svnServerBaseUrl: String
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    @Async
    fun processSvnReviewAsync(request: SvnWebhookRequest) {
        // 1. 저장소 이름 읽기
        val repoName = File(request.repo).name
        val title = "SVN [$repoName] r${request.revision} by ${request.author}"

        logger.info("SVN 리뷰 시작: $title")

        val processingFile = getReviewFile(repoName, request.revision, ".processing")
        val finalFile = getReviewFile(repoName, request.revision, ".md")

        try {
            Files.createDirectories(Paths.get(storagePath))
            processingFile.writeText("# AI 리뷰 생성 중...\n\n잠시만 기다려주세요. (Repo: $repoName, Rev: ${request.revision})")

            // 2. 동적 URL 생성하여 전달
            val fullSvnUrl = "$svnServerBaseUrl/$repoName"
            val diffContent = fetchSvnDiff(fullSvnUrl, request.revision)

            if (diffContent.isBlank()) {
                finalFile.writeText("# 리뷰 불가\n\n변경된 텍스트 파일이 없습니다.")
                return
            }

            val reviewComment = codeReviewService.requestReview(title, diffContent).block()

            if (reviewComment != null) {
                finalFile.writeText(reviewComment)
                logger.info("SVN 리뷰 저장 완료: ${finalFile.path}")
            } else {
                finalFile.writeText("# 오류\n\nAI 응답이 비어있습니다.")
            }

        } catch (e: Exception) {
            logger.error("SVN 리뷰 실패 (r${request.revision}): ${e.message}", e)
            finalFile.writeText("# 오류 발생\n\n리뷰 생성 중 문제가 발생했습니다.\n\n`${e.message}`")
        } finally {
            processingFile.delete()
        }
    }

    fun getReviewFile(repoName: String, revision: Long, extension: String): File {
        val fileName = "${repoName}_r${revision}${extension}"
        return Paths.get(storagePath, fileName).toFile()
    }

    private fun fetchSvnDiff(fullUrl: String, revision: Long): String {
        val options = SVNWCUtil.createDefaultOptions(true)
        val authManager = SVNWCUtil.createDefaultAuthenticationManager("", "")
        val clientManager = SVNClientManager.newInstance(options, authManager)
        val outputStream = ByteArrayOutputStream()

        return try {
            val svnUrl = SVNURL.parseURIEncoded(fullUrl)
            val diffClient = clientManager.diffClient

            logger.info("Diff 추출 : URL=$fullUrl, Rev=$revision")

            diffClient.doDiff(
                svnUrl,
                SVNRevision.create(revision - 1),
                svnUrl,
                SVNRevision.create(revision),
                org.tmatesoft.svn.core.SVNDepth.INFINITY,
                true,
                outputStream
            )
            outputStream.toString("UTF-8")
        } catch (e: Exception) {
            logger.error("SVNKit Diff Error", e)
            throw e
        } finally {
            clientManager.dispose()
        }
    }
}