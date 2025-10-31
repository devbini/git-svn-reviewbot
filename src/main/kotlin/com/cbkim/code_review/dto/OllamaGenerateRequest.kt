package com.cbkim.code_review.dto

data class OllamaGenerateRequest (
    val model: String,
    val prompt: String,
    val stream: Boolean = false,
    val options: Map<String, Any>? = null
)