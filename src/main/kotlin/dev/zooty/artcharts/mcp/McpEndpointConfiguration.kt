package dev.zooty.artcharts.mcp

import jakarta.servlet.Filter
import jakarta.servlet.ReadListener
import jakarta.servlet.ServletInputStream
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletRequestWrapper
import jakarta.servlet.http.HttpServletResponse
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.web.servlet.FilterRegistrationBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.util.UriComponentsBuilder
import java.io.ByteArrayInputStream
import java.net.URI

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty("spring.ai.mcp.server.enabled", havingValue = "true")
class McpEndpointConfiguration {
    @Bean
    fun mcpPublicLinks(@Value($$"${artcharts.mcp.public-base-url}") baseUrl: String): McpPublicLinks =
        McpPublicLinks(baseUrl)

    @Bean
    fun mcpRequestFilter(
        links: McpPublicLinks,
        @Value($$"${spring.ai.mcp.server.stateless.mcp-endpoint:/mcp}") endpoint: String,
    ): FilterRegistrationBean<Filter> {
        val filter = Filter { request, response, chain ->
            request as HttpServletRequest
            response as HttpServletResponse
            val origins = request.getHeaders("Origin").toList()
            when {
                origins.isNotEmpty() && origins != listOf(links.origin) -> {
                    response.sendError(HttpServletResponse.SC_FORBIDDEN)
                }
                request.contentLengthLong > MAX_REQUEST_BYTES -> {
                    rejectOversize(response)
                }
                request.method == "POST" && request.contentLengthLong < 0 -> {
                    val bytes = request.inputStream.readNBytes(MAX_REQUEST_BYTES + 1)
                    if (bytes.size > MAX_REQUEST_BYTES) {
                        rejectOversize(response)
                    } else {
                        chain.doFilter(BufferedMcpRequest(request, bytes), response)
                    }
                }
                else -> {
                    chain.doFilter(request, response)
                }
            }
        }
        return FilterRegistrationBean(filter).apply { addUrlPatterns(endpoint) }
    }

    private fun rejectOversize(response: HttpServletResponse) {
        response.status = HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE
        response.setContentLength(0)
        response.flushBuffer()
    }

    private companion object {
        const val MAX_REQUEST_BYTES = 32 * 1024 * 1024
    }
}

class McpPublicLinks(baseUrl: String) {
    private val base = URI(baseUrl.trim().trimEnd('/'))

    init {
        val localHttp = base.scheme == "http" && base.host in setOf("localhost", "127.0.0.1", "[::1]")
        require((base.scheme == "https" || localHttp) && base.host != null && base.userInfo == null &&
            base.query == null && base.fragment == null) {
            "Configure an HTTPS MCP public base URL or a loopback HTTP URL"
        }
    }

    val origin: String = "${base.scheme}://${base.rawAuthority}"

    fun url(path: String, query: Map<String, Any?> = emptyMap()): String {
        val values = mutableMapOf<String, Any>()
        val builder = UriComponentsBuilder.fromUriString(base.toString()).path(path)
        query.forEach { (name, value) ->
            val items = value as? Iterable<*> ?: listOf(value)
            items.filterNotNull().forEach { item ->
                val variable = "value${values.size}"
                builder.queryParam(name, "{$variable}")
                values[variable] = item
            }
        }
        return builder.encode().buildAndExpand(values).toUriString()
    }
}

private class BufferedMcpRequest(request: HttpServletRequest, bytes: ByteArray) : HttpServletRequestWrapper(request) {
    private val body = ByteArrayInputStream(bytes)
    private val stream = object : ServletInputStream() {
        override fun read() = body.read()
        override fun read(buffer: ByteArray, offset: Int, length: Int) = body.read(buffer, offset, length)
        override fun isFinished() = body.available() == 0
        override fun isReady() = true
        override fun setReadListener(listener: ReadListener) = throw UnsupportedOperationException()
    }

    override fun getInputStream() = stream
    override fun getReader() = stream.bufferedReader()
}
