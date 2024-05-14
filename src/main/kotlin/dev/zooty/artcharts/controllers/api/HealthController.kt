package dev.zooty.artcharts.controllers.api

import org.springframework.http.MediaType
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class HealthController {
    @GetMapping("/api/health", produces = [MediaType.TEXT_PLAIN_VALUE])
    fun get(model: Model) : String {
        return "Hello :)"
    }
}