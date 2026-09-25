package dev.zooty.artcharts.configuration

import jakarta.servlet.http.HttpServletRequest
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes

@Aspect
@Component
class ControllerLoggingAspect {
    private val log = LoggerFactory.getLogger(javaClass)

    @Around(
        "(@within(org.springframework.stereotype.Controller) || " +
            "@within(org.springframework.web.bind.annotation.RestController)) && " +
            "execution(public * *(..))"
    )
    fun logControllerCall(joinPoint: ProceedingJoinPoint): Any? {
        val operation = "${joinPoint.signature.declaringType.simpleName}.${joinPoint.signature.name}"
        val request = currentRequest()
        val requestDescription = request?.let { "${it.method} ${it.requestURI}" } ?: "unknown request"
        val startedAt = System.nanoTime()

        log.info("Controller call started: {} ({})", operation, requestDescription)

        var outcome = "successful"
        try {
            return joinPoint.proceed()
        } catch (exception: Throwable) {
            outcome = "failed (${exception.javaClass.simpleName})"
            throw exception
        } finally {
            val durationMs = (System.nanoTime() - startedAt) / 1_000_000
            log.info("Controller call finished: {} ({}, {} ms)", operation, outcome, durationMs)
        }
    }

    private fun currentRequest(): HttpServletRequest? =
        (RequestContextHolder.getRequestAttributes() as? ServletRequestAttributes)?.request
}
