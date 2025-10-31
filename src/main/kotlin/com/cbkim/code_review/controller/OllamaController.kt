package com.cbkim.code_review.controller

import com.cbkim.code_review.service.OllamaService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono

@RestController
class OllamaController(private val ollamaService: OllamaService) {

    @GetMapping("/ollama/generate")
    fun generate(@RequestParam prompt: String): Mono<String> {
        return ollamaService.generateResponse(prompt)
    }
}