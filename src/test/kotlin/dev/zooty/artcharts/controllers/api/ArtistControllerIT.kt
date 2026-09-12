package dev.zooty.artcharts.controllers.api

import dev.zooty.artcharts.dto.CreateArtistRequest
import dev.zooty.artcharts.persistence.ArtistRepository
import dev.zooty.artcharts.persistence.entity.Artist
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.client.RestTestClient

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = [
    "spring.datasource.url=jdbc:hsqldb:mem:artist-controller-test",
    "spring.datasource.driver-class-name=org.hsqldb.jdbc.JDBCDriver",
    "spring.jpa.database-platform=org.hibernate.dialect.HSQLDialect",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.datasource.dbcp2.default-read-only=false",
    "artcharts.currencyapi.apiKey=test-key",
])
@AutoConfigureRestTestClient
class ArtistControllerIT {

    @Autowired
    private lateinit var restTestClient: RestTestClient

    @Autowired
    private lateinit var artistRepository: ArtistRepository

    private lateinit var foxArtist: Artist

    @BeforeEach
    fun setUp() {
        foxArtist = artistRepository.save(artist("Fox"))
        artistRepository.save(artist("Cat"))
    }

    @AfterEach
    fun tearDown() {
        artistRepository.deleteAll()
    }

    @Test
    fun `get artists endpoint returns all persisted artists`() {
        restTestClient.get()
            .uri("/api/artist")
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$.length()").isEqualTo(2)
            .jsonPath("$[0].name").isEqualTo("Fox")
            .jsonPath("$[1].name").isEqualTo("Cat")
    }

    @Test
    fun `artist search endpoint returns matching artists`() {
        restTestClient.get()
            .uri { builder ->
                builder.path("/api/artist/search")
                    .queryParam("query", "fo")
                    .build()
            }
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$.length()").isEqualTo(1)
            .jsonPath("$[0].name").isEqualTo("Fox")
    }

    @Test
    fun `get artist endpoint returns artist by id`() {
        restTestClient.get()
            .uri("/api/artist/${foxArtist.id}")
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$.id").isEqualTo(foxArtist.id)
            .jsonPath("$.name").isEqualTo("Fox")
    }

    @Test
    fun `create artist endpoint creates an artist`() {
        restTestClient.post()
            .uri("/api/artist")
            .contentType(MediaType.APPLICATION_JSON)
            .body(CreateArtistRequest(name = " New Artist ", twitter = "@new-artist"))
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$.id").exists()
            .jsonPath("$.name").isEqualTo("New Artist")
            .jsonPath("$.twitter").isEqualTo("@new-artist")
    }

    private fun artist(name: String) = Artist(
        0L, name, null, null, null, null, null, null, null, null, null, null, null
    )
}
