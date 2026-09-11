package dev.zooty.artcharts.controllers

import dev.zooty.artcharts.exceptions.ResourceNotFoundException
import org.hibernate.ObjectNotFoundException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler

@ControllerAdvice(basePackages = ["dev.zooty.artcharts.controllers.site", "dev.zooty.artcharts.controllers.api"])
class ExceptionHandler {
    @ExceptionHandler
    fun notFound(exception: ResourceNotFoundException): ResponseEntity<String> = ResponseEntity.status(HttpStatus.NOT_FOUND).body(exception.message)

    @ExceptionHandler
    fun notFound(exception: ObjectNotFoundException): ResponseEntity<String> = ResponseEntity.status(HttpStatus.NOT_FOUND).body(exception.message)
    
    @ExceptionHandler
    fun illegalArgumentException(exception: IllegalArgumentException): ResponseEntity<String> = ResponseEntity.status(HttpStatus.BAD_REQUEST).body(exception.message)
}
