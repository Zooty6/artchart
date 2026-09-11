package dev.zooty.artcharts.controllers.site

import dev.zooty.artcharts.exceptions.ResourceNotFoundException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler

@ControllerAdvice(basePackages = ["dev.zooty.artcharts.controllers.site", "dev.zooty.artcharts.controllers.api"])
class SiteExceptionHandler {
    @ExceptionHandler(ResourceNotFoundException::class)
    fun notFound(): ResponseEntity<Void> = ResponseEntity.status(HttpStatus.NOT_FOUND).build()
    
    @ExceptionHandler(IllegalArgumentException::class)
    fun illegalArgumentException(): ResponseEntity<Void> = ResponseEntity.status(HttpStatus.BAD_REQUEST).build()
}
