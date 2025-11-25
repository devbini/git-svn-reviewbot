package com.cbkim.code_review

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableAsync

@EnableAsync
@SpringBootApplication
class CodeReviewApplication

fun main(args: Array<String>) {
	runApplication<CodeReviewApplication>(*args)
}
