package com.cbkim.code_review.dto

import com.fasterxml.jackson.annotation.JsonProperty

// webook으로 전달받는 내용
data class GitLabWebhookMergeRequestEvent(
    val object_kind: String,
    val user: GitLabUser,
    val project: GitLabProject,
    @JsonProperty("object_attributes") // JSON 필드이름이 다른 경우...
    val objectAttributes: GitLabMergeRequestAttributes,
    val changes: GitLabChanges? = null // 코드 변경사항 정보 (리뷰 시 사용)
)

// 유저 정보, gitlab 버전에 따라 다른 값이 올 수 있음.
data class GitLabUser(
    val name: String?,
    val username: String?
)

// 프로젝트 정보
data class GitLabProject(
    val id: Long,
    val name: String,
    val path_with_namespace: String,
    val web_url: String
)

// MR 정보
data class GitLabMergeRequestAttributes(
    val id: Long,
    val iid: Long, // api 호출용 키값
    val title: String,
    val state: String,
    val source_branch: String,
    val target_branch: String,
    val url: String,
    val last_commit: GitLabCommit?,
    val action: String
)

// MR 내 커밋 정보
data class GitLabCommit(
    val id: String,
    val message: String,
    val timestamp: String,
    val url: String,
    val author: GitLabUser
)

// 업데이트 정보
data class GitLabChanges(
    val updated_by_id: GitLabChangeDetail? = null,
)

data class GitLabChangeDetail(
    val previous: Long?,
    val current: Long?
)

// diff 텍스트만 사용
data class GitLabDiffResponse(
    val diff: String,
    val new_path: String,
    val old_path: String,
    val deleted_file: Boolean,
    val new_file: Boolean,
    val renamed_file: Boolean
)

// MR 내 코멘트 남기기용 (리뷰결과)
data class GitLabCommentRequest(
    val body: String
)

data class GitLabCommentResponse(
    val id: Long,
    val body: String,
    val author: GitLabUser,
    val created_at: String
)