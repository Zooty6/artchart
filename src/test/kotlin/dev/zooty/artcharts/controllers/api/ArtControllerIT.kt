package dev.zooty.artcharts.controllers.api

import dev.zooty.artcharts.dto.TagDto
import dev.zooty.artcharts.persistence.ArtRepository
import dev.zooty.artcharts.persistence.ArtistRepository
import dev.zooty.artcharts.persistence.TagRepository
import dev.zooty.artcharts.persistence.entity.Art
import dev.zooty.artcharts.persistence.entity.Artist
import dev.zooty.artcharts.persistence.entity.Currency
import dev.zooty.artcharts.persistence.entity.Price
import dev.zooty.artcharts.persistence.entity.Tag
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.client.RestTestClient

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = [
    "spring.datasource.url=jdbc:hsqldb:mem:art-search-test",
    "spring.datasource.driver-class-name=org.hsqldb.jdbc.JDBCDriver",
    "spring.jpa.database-platform=org.hibernate.dialect.HSQLDialect",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.datasource.dbcp2.default-read-only=false",
    "artcharts.currencyapi.apiKey=test-key",
])
@AutoConfigureRestTestClient
class ArtControllerIT {

    @Autowired
    private lateinit var restTestClient: RestTestClient
    @Autowired
    private lateinit var artRepository: ArtRepository
    @Autowired
    private lateinit var artistRepository: ArtistRepository
    @Autowired
    private lateinit var tagRepository: TagRepository

    private lateinit var foxArt: Art

    @BeforeEach
    fun setUp() {
        val fox = artistRepository.save(artist("Fox"))
        val cat = artistRepository.save(artist("Cat"))
        val cute = tagRepository.save(Tag("cute", "general"))
        foxArt = artRepository.save(art(fox, "Fox commission", "2024-01-01", mutableSetOf(cute)))
        artRepository.save(art(cat, "Cat commission", "2023-01-01"))
        artRepository.flush()
    }
    
    @AfterEach
    fun tearDown() {
        artRepository.deleteAll()
        tagRepository.deleteAll()
        artistRepository.deleteAll()
    }

    @Test
    fun `search endpoint filters persisted arts by artist and tag`() {
        restTestClient.get()
            .uri { builder ->
                builder.path("/api/art/search")
                    .queryParam("searchParams", "artist:fox tag:cute")
                    .build()
            }
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$[0].id").isEqualTo(foxArt.id)
            .jsonPath("$.length()").isEqualTo(1)
    }

    @Test
    fun `search endpoint evaluates persisted date and nsfw filters`() {
        restTestClient.get()
            .uri { builder ->
                builder.path("/api/art/search")
                    .queryParam("searchParams", "deliveredDate:2024 nsfw:false")
                    .build()
            }
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$[0].id").isEqualTo(foxArt.id)
            .jsonPath("$.length()").isEqualTo(1)
    }

    @Test
    fun `get arts endpoint returns all persisted arts`() {
        restTestClient.get()
            .uri("/api/art")
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$.length()").isEqualTo(2)
            .jsonPath("$[0].id").isEqualTo(foxArt.id)
    }

    @Test
    fun `get art endpoint returns art by id`() {
        restTestClient.get()
            .uri("/api/art/${foxArt.id}")
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$.id").isEqualTo(foxArt.id)
            .jsonPath("$.fileName").isEqualTo("Fox commission")
    }

    @Test
    fun `create art endpoint creates an art`() {
        restTestClient.post()
            .uri("/api/art")
            .contentType(MediaType.APPLICATION_JSON)
            .body("""
                {
                  "type": " Commission ",
                  "species": " cat ",
                  "deliveredDate": "2025-01-01",
                  "fileName": "new-art.png",
                  "artistName": " fox ",
                  "currency": "UNKNOWN",
                  "amount": 0.0,
                  "isNsfw": false
                }
            """.trimIndent())
            .exchange()
            .expectStatus().isCreated
            .expectBody()
            .jsonPath("$.id").exists()
            .jsonPath("$.type").isEqualTo("Commission")
            .jsonPath("$.artist.name").isEqualTo("Fox")
    }

    @Test
    fun `add tag endpoint adds a tag to an art`() {
        restTestClient.post()
            .uri("/api/art/${foxArt.id}/tag")
            .contentType(MediaType.APPLICATION_JSON)
            .body(TagDto("new-tag", "style"))
            .exchange()
            .expectStatus().isNoContent

        restTestClient.get()
            .uri("/api/art/${foxArt.id}")
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$.tags[?(@.name == 'new-tag')].name").isEqualTo(listOf("new-tag"))
    }

    @Test
    fun `remove tag endpoint removes a tag from an art`() {
        restTestClient.post()
            .uri("/api/art/${foxArt.id}/tag")
            .contentType(MediaType.APPLICATION_JSON)
            .body(TagDto("removable-tag", "style"))
            .exchange()
            .expectStatus().isNoContent

        restTestClient.delete()
            .uri("/api/art/${foxArt.id}/tag/removable-tag")
            .exchange()
            .expectStatus().isNoContent

        restTestClient.get()
            .uri("/api/art/${foxArt.id}")
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$.tags[?(@.name == 'removable-tag')]").doesNotExist()
    }

    private fun artist(name: String) = Artist(
        0L, name, null, null, null, null, null, null, null, null, null, null, null
    )

    private fun art(
        artist: Artist,
        fileName: String,
        deliveredDate: String,
        tags: MutableSet<Tag> = mutableSetOf(),
    ) = Art(
        0L, null, "commission", "High", "cat", "2023-01-01", "2023-02-01",
        deliveredDate, fileName, Price(Currency.USD, 10.0), null, artist, false,
        "https://example.com", tags
    )
}
