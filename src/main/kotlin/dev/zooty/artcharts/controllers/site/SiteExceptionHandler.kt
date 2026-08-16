package dev.zooty.artcharts.controllers.site

import dev.zooty.artcharts.exceptions.ResourceNotFoundException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler

@ControllerAdvice(basePackages = ["dev.zooty.artcharts.controllers.site"])
class SiteExceptionHandler {
    @ExceptionHandler(ResourceNotFoundException::class)
    fun notFound(): ResponseEntity<Void> = ResponseEntity.status(HttpStatus.NOT_FOUND).build()
}
