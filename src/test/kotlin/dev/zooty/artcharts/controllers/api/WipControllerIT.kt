package dev.zooty.artcharts.controllers.api

import dev.zooty.artcharts.TestFixtures
import dev.zooty.artcharts.dto.DeliverArtRequest
import dev.zooty.artcharts.persistence.ArtRepository
import dev.zooty.artcharts.persistence.ArtistRepository
import dev.zooty.artcharts.persistence.WipRepository
import dev.zooty.artcharts.persistence.entity.Artist
import dev.zooty.artcharts.persistence.entity.Wip
import dev.zooty.artcharts.services.api.MediaFileService
import dev.zooty.artcharts.services.api.WipService
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyBoolean
import org.mockito.ArgumentMatchers.anyList
import org.mockito.ArgumentMatchers.anyString
import org.mockito.ArgumentMatchers.argThat
import org.mockito.ArgumentMatchers.eq
import org.mockito.Mockito.doAnswer
import org.mockito.Mockito.verify
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.core.io.ByteArrayResource
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.client.RestTestClient
import org.springframework.util.LinkedMultiValueMap
import org.springframework.web.multipart.MultipartFile
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = [
    "spring.datasource.url=jdbc:hsqldb:mem:wip-controller-test",
    "spring.datasource.driver-class-name=org.hsqldb.jdbc.JDBCDriver",
    "spring.jpa.database-platform=org.hibernate.dialect.HSQLDialect",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.datasource.dbcp2.default-read-only=false",
    "artcharts.currencyapi.apiKey=test-key",
])
@AutoConfigureRestTestClient
class WipControllerIT {
    @Autowired
    private lateinit var restTestClient: RestTestClient
    @Autowired
    private lateinit var wipRepository: WipRepository
    @Autowired
    private lateinit var artistRepository: ArtistRepository
    @Autowired
    private lateinit var artRepository: ArtRepository
    @Autowired
    private lateinit var wipService: WipService

    @MockitoBean
    private lateinit var mediaFileService: MediaFileService

    private lateinit var wip: Wip

    @BeforeEach
    fun setUp() {
        val artist = artistRepository.save(Artist(
            0L, "Artist", null, null, null, null, null, null, null, null, null, null, null, null, null
        ))
        wip = wipRepository.save(TestFixtures.wip(id = 0L, artist = artist, isNsfw = true))
    }

    @AfterEach
    fun tearDown() {
        wipRepository.deleteAll()
        artRepository.deleteAll()
        artistRepository.deleteAll()
    }

    @Test
    fun `lists persisted wips and returns one by id`() {
        restTestClient.get().uri("/api/wip").exchange()
            .expectStatus().isOk
            .expectBody().jsonPath("$.length()").isEqualTo(1)
            .jsonPath("$[0].id").isEqualTo(wip.id)

        restTestClient.get().uri("/api/wip/${wip.id}").exchange()
            .expectStatus().isOk
            .expectBody().jsonPath("$.artist.name").isEqualTo("Artist")
            .jsonPath("$.species").isEqualTo("cat")
    }

    @Test
    fun `creates wip with normalized fields and persisted price`() {
        restTestClient.post().uri("/api/wip").contentType(MediaType.APPLICATION_JSON)
            .body(mapOf("currency" to "USD", "type" to " sketch ", "species" to " fox ", "artistName" to " artist ", "amount" to 25.0, "isNsfw" to false))
            .exchange().expectStatus().isCreated
            .expectBody().jsonPath("$.type").isEqualTo("sketch")
            .jsonPath("$.artist.name").isEqualTo("Artist")

        val created = wipRepository.findAll().single { it.id != wip.id }
        assertEquals("fox", created.species)
        assertEquals(25.0, created.price.amount)
    }

    @Test
    fun `updates persisted fields and clears optional fields`() {
        restTestClient.post().uri("/api/wip/${wip.id}").contentType(MediaType.APPLICATION_JSON)
            .body(mapOf("currency" to "USD", "type" to " sketch ", "species" to " fox ", "amount" to 42.0, "note" to " ", "isNsfw" to false))
            .exchange().expectStatus().isNoContent

        val updated = wipRepository.findById(wip.id).orElseThrow()
        assertEquals("sketch", updated.type)
        assertEquals("fox", updated.species)
        assertEquals(42.0, updated.price.amount)
        assertEquals(wip.artist.id, updated.artist.id)
        assertNull(updated.note)
        assertNull(updated.quality)
        assertFalse(updated.isNsfw)
    }

    @Test
    fun `delivery creates art from inherited fields and deletes wip`() {
        restTestClient.put().uri("/api/wip/${wip.id}").contentType(MediaType.APPLICATION_JSON)
            .body(DeliverArtRequest(deliveredDate = "2024-02-01", fileName = "delivered"))
            .exchange().expectStatus().isOk
            .expectBody().jsonPath("$.fileName").isEqualTo("delivered")
            .jsonPath("$.species").isEqualTo("cat")

        assertFalse(wipRepository.existsById(wip.id))
        val art = artRepository.findAll().single()
        assertEquals(wip.type, art.type)
        assertEquals(wip.orderedDate, art.orderedDate)
        assertEquals(wip.payedDate, art.payedDate)
        assertEquals(wip.price, art.price)
        assertEquals(wip.note, art.note)
        assertEquals(wip.artist.id, art.artist.id)
        assertTrue(art.isNsfw)
    }

    @Test
    fun `multipart delivery forwards uploaded files`() {
        doAnswer { invocation ->
            val files = invocation.getArgument<List<MultipartFile>>(0)
            assertTrue(files.single().bytes.contentEquals(byteArrayOf(1, 2, 3)))
            null
        }.`when`(mediaFileService).saveFiles(anyList(), anyString(), anyString(), anyBoolean())
        val body = LinkedMultiValueMap<String, Any>()
        body.add("request", HttpEntity(
            """{"deliveredDate":"2024-02-01","fileName":"gallery"}""",
            HttpHeaders().apply { contentType = MediaType.APPLICATION_JSON },
        ))
        body.add("file", object : ByteArrayResource(byteArrayOf(1, 2, 3)) {
            override fun getFilename() = "art.png"
        })

        restTestClient.put().uri("/api/wip/${wip.id}")
            .contentType(MediaType.MULTIPART_FORM_DATA).body(body)
            .exchange().expectStatus().isOk

        verify(mediaFileService).saveFiles(
            argThat<List<MultipartFile>> { files ->
                files.size == 1 && files.single().originalFilename == "art.png"
            } ?: emptyList(),
            eq("gallery") ?: "", eq("2024-02-01") ?: "", eq(true),
        )
        assertFalse(wipRepository.existsById(wip.id))
        assertEquals(1L, artRepository.count())
    }

    @Test
    fun `failed art persistence rolls back wip deletion`() {
        assertFailsWith<Exception> {
            wipService.deliverArt(wip.id, DeliverArtRequest(deliveredDate = "2024-02-01", fileName = "a".repeat(300)), emptyList())
        }

        assertTrue(wipRepository.existsById(wip.id))
        assertEquals(0L, artRepository.count())
    }

    @Test
    fun `unknown ids and artists return not found`() {
        restTestClient.get().uri("/api/wip/999999").exchange().expectStatus().isNotFound
        restTestClient.post().uri("/api/wip/999999").contentType(MediaType.APPLICATION_JSON)
            .body(mapOf("currency" to "USD", "type" to "sketch", "species" to "cat", "amount" to 0.0, "isNsfw" to false))
            .exchange().expectStatus().isNotFound
        restTestClient.put().uri("/api/wip/999999").contentType(MediaType.APPLICATION_JSON)
            .body(DeliverArtRequest(deliveredDate = "2024-02-01", fileName = "art"))
            .exchange().expectStatus().isNotFound
        restTestClient.post().uri("/api/wip").contentType(MediaType.APPLICATION_JSON)
            .body(mapOf("currency" to "USD", "type" to "sketch", "species" to "cat", "artistName" to "Missing", "amount" to 0.0, "isNsfw" to false))
            .exchange().expectStatus().isNotFound
        assertEquals(1L, wipRepository.count())
    }

    @Test
    fun `invalid requests return bad request without changing persistence`() {
        restTestClient.post().uri("/api/wip").contentType(MediaType.APPLICATION_JSON)
            .body(mapOf("type" to " ", "species" to " ", "artistName" to " ", "currency" to "USD", "amount" to 0.0, "isNsfw" to false)).exchange().expectStatus().isBadRequest
        restTestClient.post().uri("/api/wip/${wip.id}").contentType(MediaType.APPLICATION_JSON)
            .body(mapOf("type" to " ", "species" to " ", "artistName" to " ", "currency" to "USD", "amount" to 0.0, "isNsfw" to false)).exchange().expectStatus().isBadRequest
        restTestClient.put().uri("/api/wip/${wip.id}").contentType(MediaType.APPLICATION_JSON)
            .body(DeliverArtRequest()).exchange().expectStatus().isBadRequest

        assertEquals("commission", wipRepository.findById(wip.id).orElseThrow().type)
        assertEquals(1L, wipRepository.count())
        assertEquals(0L, artRepository.count())
    }
}
