package dev.zooty.artcharts.controllers.api

import dev.zooty.artcharts.persistence.TagRepository
import dev.zooty.artcharts.persistence.entity.Tag
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.web.servlet.client.RestTestClient

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = [
    "spring.datasource.url=jdbc:hsqldb:mem:tag-controller-test",
    "spring.datasource.driver-class-name=org.hsqldb.jdbc.JDBCDriver",
    "spring.jpa.database-platform=org.hibernate.dialect.HSQLDialect",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.datasource.dbcp2.default-read-only=false",
    "artcharts.currencyapi.apiKey=test-key",
])
@AutoConfigureRestTestClient
class TagControllerIT {
    @Autowired
    private lateinit var restTestClient: RestTestClient

    @Autowired
    private lateinit var tagRepository: TagRepository

    @BeforeEach
    fun setUp() {
        tagRepository.saveAll(
            listOf(
                Tag("sketch", "style"),
                Tag("portrait", "general"),
                Tag("landscape", "style"),
            )
        )
    }

    @AfterEach
    fun tearDown() {
        tagRepository.deleteAll()
    }

    @Test
    fun `categories endpoint returns distinct persisted categories in order`() {
        restTestClient.get()
            .uri("/api/tag/categories")
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .json("[\"general\",\"style\"]")
    }
}
