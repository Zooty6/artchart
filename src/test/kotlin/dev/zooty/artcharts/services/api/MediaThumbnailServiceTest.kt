package dev.zooty.artcharts.services.api

import com.github.luben.zstd.ZstdOutputStream
import dev.zooty.artcharts.TestFixtures
import dev.zooty.artcharts.persistence.ArtRepository
import org.bytedeco.ffmpeg.global.avcodec
import org.bytedeco.javacv.FFmpegFrameRecorder
import org.bytedeco.javacv.Java2DFrameConverter
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.mockito.Mockito.`when`
import java.awt.Color
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.file.Files
import java.nio.file.Path
import java.util.zip.GZIPOutputStream
import javax.imageio.IIOImage
import javax.imageio.ImageIO
import javax.imageio.ImageTypeSpecifier
import javax.imageio.metadata.IIOMetadataNode
import javax.imageio.stream.MemoryCacheImageOutputStream

class MediaThumbnailServiceTest {
    @TempDir
    lateinit var mediaRoot: Path

    @Test
    fun `creates a resized image thumbnail`() {
        val source = createMedia("art.png")
        ImageIO.write(BufferedImage(640, 480, BufferedImage.TYPE_INT_RGB), "png", source.toFile())
        val service = thumbnailService(source)

        val thumbnail = service.resolveThumbnailForArt(1L)
        val image = ImageIO.read(thumbnail.resource.inputStream)

        assertEquals("image/png", thumbnail.mediaType.toString())
        assertTrue(image.width <= 320 && image.height <= 240)
    }

    @Test
    fun `creates a thumbnail from a JFIF image`() {
        val source = createMedia("art.jfif")
        ImageIO.write(BufferedImage(640, 480, BufferedImage.TYPE_INT_RGB), "jpeg", source.toFile())

        val thumbnail = thumbnailService(source, "art.jfif").resolveThumbnailForArt(1L)
        val image = ImageIO.read(thumbnail.resource.inputStream)

        assertEquals("image/jpeg", thumbnail.mediaType.toString())
        assertTrue(image.width <= 320 && image.height <= 240)
    }

    @Test
    fun `creates a thumbnail from a WebP image`() {
        val source = createMedia("art.webp")
        val webp = "UklGRiIAAABXRUJQVlA4IBYAAAAwAQCdASoBAAEAAUAmJaQAA3AA/v89WAAAAA=="
        Files.write(source, java.util.Base64.getDecoder().decode(webp))

        val thumbnail = thumbnailService(source, "art.webp").resolveThumbnailForArt(1L)

        assertEquals("image/jpeg", thumbnail.mediaType.toString())
        assertTrue(thumbnail.resource.contentLength() > 0)
        assertTrue(ImageIO.read(thumbnail.resource.inputStream) != null)
    }

    @Test
    fun `keeps GIF thumbnails animated`() {
        val source = createMedia("art.gif")
        writeAnimatedGif(source)
        val service = thumbnailService(source, "art.gif")

        val thumbnail = service.resolveThumbnailForArt(1L)
        val reader = ImageIO.getImageReadersByFormatName("gif").next()
        ImageIO.createImageInputStream(thumbnail.resource.inputStream).use { imageInput ->
            reader.input = imageInput
            assertEquals("image/gif", thumbnail.mediaType.toString())
            assertEquals(2, reader.getNumImages(true))
            reader.dispose()
        }
    }

    @Test
    fun `creates an image thumbnail from the first MP4 frame`() {
        val source = createMedia("art.mp4")
        writeMp4(source)
        val thumbnail = thumbnailService(source, "art.mp4").resolveThumbnailForArt(1L)

        assertEquals("image/jpeg", thumbnail.mediaType.toString())
        assertTrue(ImageIO.read(thumbnail.resource.inputStream).width <= 320)
    }

    @Test
    fun `extracts an embedded preview from a modern blend file`() {
        val source = createMedia("art.blend")
        writeBlend(source, modernHeader = true)

        val thumbnail = thumbnailService(source, "art.blend").resolveThumbnailForArt(1L)

        assertEquals("image/jpeg", thumbnail.mediaType.toString())
        assertTrue(thumbnail.resource.exists())
        assertTrue(thumbnail.resource.contentLength() > 0)
    }

    @Test
    fun `extracts an embedded preview from a zstd compressed blend file`() {
        val source = createMedia("art.blend")
        writeBlend(source, modernHeader = false, compression = BlendCompression.ZSTD)

        val thumbnail = thumbnailService(source, "art.blend").resolveThumbnailForArt(1L)

        assertEquals("image/jpeg", thumbnail.mediaType.toString())
        assertTrue(thumbnail.resource.exists())
        assertTrue(thumbnail.resource.contentLength() > 0)
    }

    @Test
    fun `extracts an embedded preview from a gzip compressed blend file`() {
        val source = createMedia("art.blend")
        writeBlend(source, modernHeader = false, compression = BlendCompression.GZIP)

        val thumbnail = thumbnailService(source, "art.blend").resolveThumbnailForArt(1L)

        assertEquals("image/jpeg", thumbnail.mediaType.toString())
        assertTrue(thumbnail.resource.exists())
        assertTrue(thumbnail.resource.contentLength() > 0)
    }

    @Test
    fun `uses placeholder when blend file has no embedded preview`() {
        val source = createMedia("art.blend")
        writeBlend(source, modernHeader = false, includePreview = false)

        val thumbnail = thumbnailService(source, "art.blend").resolveThumbnailForArt(1L)

        assertEquals("image/svg+xml", thumbnail.mediaType.toString())
        assertEquals("placeholder.svg", thumbnail.fileName)
    }

    @Test
    fun `uses placeholder for unknown media format`() {
        val source = createMedia("art.unknown")
        Files.write(source, byteArrayOf(1, 2, 3))

        val thumbnail = thumbnailService(source, "art.unknown").resolveThumbnailForArt(1L)

        assertEquals("image/svg+xml", thumbnail.mediaType.toString())
        assertEquals("placeholder.svg", thumbnail.fileName)
    }

    private fun createMedia(fileName: String): Path = mediaRoot.resolve("SFW/2024/$fileName").also {
        Files.createDirectories(it.parent)
    }

    private fun thumbnailService(file: Path, fileName: String = file.fileName.toString()): MediaThumbnailService {
        val artRepository = org.mockito.Mockito.mock(ArtRepository::class.java)
        val mediaFileService = MediaFileService(artRepository, mediaRoot.toString())
        `when`(artRepository.findById(1L)).thenReturn(java.util.Optional.of(TestFixtures.art(fileName = fileName)))
        return MediaThumbnailService(mediaFileService)
    }

    private fun writeAnimatedGif(path: Path) {
        val output = ByteArrayOutputStream()
        val writer = ImageIO.getImageWritersByFormatName("gif").next()
        MemoryCacheImageOutputStream(output).use { imageOutput ->
            writer.output = imageOutput
            writer.prepareWriteSequence(null)
            repeat(2) { index ->
                val image = BufferedImage(640, 480, BufferedImage.TYPE_INT_RGB)
                val graphics = image.createGraphics()
                graphics.color = if (index == 0) Color.RED else Color.GREEN
                graphics.fillRect(0, 0, image.width, image.height)
                graphics.dispose()
                val metadata = writer.getDefaultImageMetadata(
                    ImageTypeSpecifier.createFromRenderedImage(image), writer.defaultWriteParam
                )
                val tree = metadata.getAsTree("javax_imageio_gif_image_1.0") as org.w3c.dom.Node
                val control = (0 until tree.childNodes.length)
                    .map { tree.childNodes.item(it) }
                    .filterIsInstance<IIOMetadataNode>()
                    .first { it.nodeName == "GraphicControlExtension" }
                control.setAttribute("delayTime", "10")
                metadata.setFromTree("javax_imageio_gif_image_1.0", tree)
                writer.writeToSequence(IIOImage(image, null, metadata), writer.defaultWriteParam)
            }
            writer.endWriteSequence()
        }
        writer.dispose()
        Files.write(path, output.toByteArray())
    }

    private fun writeMp4(path: Path) {
        val converter = Java2DFrameConverter()
        FFmpegFrameRecorder(path.toFile(), 64, 48).use { recorder ->
            recorder.format = "mp4"
            recorder.videoCodec = avcodec.AV_CODEC_ID_MPEG4
            recorder.frameRate = 2.0
            recorder.start()
            repeat(2) { index ->
                val image = BufferedImage(64, 48, BufferedImage.TYPE_3BYTE_BGR)
                val graphics = image.createGraphics()
                graphics.color = if (index == 0) Color.RED else Color.BLUE
                graphics.fillRect(0, 0, image.width, image.height)
                graphics.dispose()
                recorder.record(converter.convert(image))
            }
            recorder.stop()
        }
    }

    private fun writeBlend(
        path: Path,
        modernHeader: Boolean,
        compression: BlendCompression? = null,
        includePreview: Boolean = true,
    ) {
        val blend = ByteArrayOutputStream()
        val header = if (modernHeader) "BLENDER17-01v0502" else "BLENDER-v300"
        blend.write(header.toByteArray())
        val image = ByteArrayOutputStream()
        writeInt(image, 2)
        writeInt(image, 2)
        image.write(byteArrayOf(
            0xff.toByte(), 0, 0, 0xff.toByte(), 0, 0xff.toByte(), 0, 0xff.toByte(),
            0, 0, 0xff.toByte(), 0xff.toByte(), 0xff.toByte(), 0xff.toByte(), 0, 0xff.toByte(),
        ))
        val payload = image.toByteArray()
        if (includePreview) {
            if (modernHeader) {
                blend.write("TEST".toByteArray())
                writeInt(blend, 0)
                writeLong(blend, 0)
                writeLong(blend, payload.size.toLong())
                writeLong(blend, 1)
            } else {
                blend.write("TEST".toByteArray())
                writeInt(blend, payload.size)
                writeLong(blend, 0)
                writeInt(blend, 0)
                writeInt(blend, 1)
            }
            blend.write(payload)
        }
        if (modernHeader) {
            blend.write("ENDB".toByteArray())
            writeInt(blend, 0)
            writeLong(blend, 0)
            writeLong(blend, 0)
            writeLong(blend, 0)
        } else {
            blend.write("ENDB".toByteArray())
            writeInt(blend, 0)
            writeLong(blend, 0)
            writeInt(blend, 0)
            writeInt(blend, 0)
        }
        when (compression) {
            BlendCompression.GZIP -> GZIPOutputStream(Files.newOutputStream(path)).use { it.write(blend.toByteArray()) }
            BlendCompression.ZSTD -> ZstdOutputStream(Files.newOutputStream(path)).use { it.write(blend.toByteArray()) }
            null -> Files.write(path, blend.toByteArray())
        }
    }

    private fun writeInt(output: ByteArrayOutputStream, value: Int) {
        output.write(ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(value).array())
    }

    private fun writeLong(output: ByteArrayOutputStream, value: Long) {
        output.write(ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN).putLong(value).array())
    }

    private enum class BlendCompression { GZIP, ZSTD }
}
