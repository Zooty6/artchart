package dev.zooty.artcharts.services.api

import com.github.luben.zstd.ZstdInputStream
import net.coobird.thumbnailator.Thumbnails
import org.bytedeco.javacv.FFmpegFrameGrabber
import org.bytedeco.javacv.Java2DFrameConverter
import org.slf4j.LoggerFactory
import org.springframework.cache.annotation.Cacheable
import org.springframework.core.io.ByteArrayResource
import org.springframework.core.io.ClassPathResource
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import java.awt.image.BufferedImage
import java.io.BufferedInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.zip.GZIPInputStream
import javax.imageio.IIOImage
import javax.imageio.ImageIO
import javax.imageio.ImageTypeSpecifier
import javax.imageio.metadata.IIOMetadataNode
import javax.imageio.stream.MemoryCacheImageOutputStream

@Service
class MediaThumbnailService(private val mediaFileService: MediaFileService) {
    companion object {
        private val logger = LoggerFactory.getLogger(MediaThumbnailService::class.java)
        private const val MAX_BLEND_SCAN_BYTES = 64L * 1024 * 1024
        private const val MAX_PREVIEW_DIMENSION = 4096
        private const val MAX_PREVIEW_BLOCK_BYTES = 64L * 1024 * 1024
        private val TEST_BLOCK = "TEST".toByteArray()
        private val END_BLOCK = "ENDB".toByteArray()
        private val IMAGE_EXTENSIONS = setOf(".jpg", ".jpeg", ".jfif", ".png", ".bmp", ".tif", ".tiff", ".wbmp", ".webp")
    }


    @Cacheable(cacheNames = ["media-thumbnails"], key = "#id")
    fun resolveThumbnailForArt(id: Long): MediaFile = createThumbnail(mediaFileService.resolveForArt(id).first())

    private fun createThumbnail(media: MediaFile): MediaFile {
        if (media.fileName == "placeholder.svg") return media
        val format = media.format()
        if (format == ThumbnailFormat.UNKNOWN) {
            logger.warn("Cannot create thumbnail for unsupported media format: {}", media.fileName)
            return placeholderThumbnail()
        }
        if (format == ThumbnailFormat.BLEND) {
            val thumbnail = createBlendThumbnail(media) ?: return placeholderThumbnail()
            return mediaFile(media, thumbnail)
        }

        val thumbnail = when (format) {
            ThumbnailFormat.GIF -> createGifThumbnail(media)
            ThumbnailFormat.MP4 -> createVideoThumbnail(media)
            ThumbnailFormat.BLEND -> error("Blend thumbnails are handled separately")
            ThumbnailFormat.IMAGE -> createImageThumbnail(media)
            ThumbnailFormat.UNKNOWN -> error("Unsupported media format was already handled")
        }
        return mediaFile(media, thumbnail)
    }

    private fun mediaFile(media: MediaFile, thumbnail: Thumbnail): MediaFile =
        MediaFile(
            resource = ByteArrayResource(thumbnail.bytes),
            mediaType = thumbnail.mediaType,
            fileName = "thumbnail-${media.fileName}",
        )

    private fun MediaFile.format(): ThumbnailFormat = when {
        fileName.endsWith(".gif", ignoreCase = true) -> ThumbnailFormat.GIF
        fileName.endsWith(".mp4", ignoreCase = true) -> ThumbnailFormat.MP4
        fileName.endsWith(".blend", ignoreCase = true) -> ThumbnailFormat.BLEND
        IMAGE_EXTENSIONS.any { fileName.endsWith(it, ignoreCase = true) } -> ThumbnailFormat.IMAGE
        else -> ThumbnailFormat.UNKNOWN
    }

    private fun placeholderThumbnail() = MediaFile(
        resource = ClassPathResource("static/site/placeholder.svg"),
        mediaType = MediaType("image", "svg+xml"),
        fileName = "placeholder.svg",
    )

    private fun createBlendThumbnail(media: MediaFile): Thumbnail? {
        val preview = runCatching { extractBlendPreview(media.resource.file) }.getOrNull()
            ?: return null
        val output = ByteArrayOutputStream()
        Thumbnails.of(preview).size(320, 240).outputFormat("jpg").toOutputStream(output)
        if (output.size() == 0) return null
        return Thumbnail(output.toByteArray(), MediaType.IMAGE_JPEG)
    }

    private fun extractBlendPreview(file: File): BufferedImage? {
        val rawInput = BufferedInputStream(file.inputStream())
        return rawInput.use { raw ->
            raw.mark(4)
            val signature = ByteArray(4)
            if (raw.read(signature) != signature.size) return null
            raw.reset()
            val input = when {
                signature[0] == 0x1f.toByte() && signature[1] == 0x8b.toByte() -> GZIPInputStream(raw)
                signature.contentEquals(byteArrayOf(0x28, 0xb5.toByte(), 0x2f, 0xfd.toByte())) -> ZstdInputStream(raw)
                else -> raw
            }
            DataInputStream(input).use { readBlendPreview(it) }
        }
    }

    private fun readBlendPreview(input: DataInputStream): BufferedImage? {
        val format = readBlendFormat(input) ?: return null
        return scanBlendBlocks(input, format)
    }

    private fun readBlendFormat(input: DataInputStream): BlendFormat? {
        val header = ByteArray(17)
        input.readFully(header, 0, 12)
        if (!header.copyOfRange(0, 7).contentEquals("BLENDER".toByteArray())) return null

        val hasModernHeader = header[7].toInt().toChar().isDigit() && header[8].toInt().toChar().isDigit()
        return if (hasModernHeader) readModernBlendFormat(input, header) else readLegacyBlendFormat(header)
    }

    private fun readModernBlendFormat(input: DataInputStream, header: ByteArray): BlendFormat? {
        input.readFully(header, 12, 5)
        val headerSize = header.copyOfRange(7, 9).toString(Charsets.US_ASCII).toIntOrNull() ?: return null
        if (headerSize != 17 || header[10] != '0'.code.toByte() || header[11] != '1'.code.toByte()) return null

        val byteOrder = header.byteOrderAt(12) ?: return null
        return BlendFormat(pointerSize = 8, byteOrder = byteOrder, usesLargeBlockHeaders = true)
    }

    private fun readLegacyBlendFormat(header: ByteArray): BlendFormat? {
        val pointerSize = when (header[7].toInt().toChar()) {
            '-' -> 8
            '_' -> 4
            else -> return null
        }
        val byteOrder = header.byteOrderAt(8) ?: return null
        return BlendFormat(pointerSize, byteOrder, usesLargeBlockHeaders = false)
    }

    private fun ByteArray.byteOrderAt(index: Int): ByteOrder? = when (this[index].toInt().toChar()) {
        'v' -> ByteOrder.LITTLE_ENDIAN
        'V' -> ByteOrder.BIG_ENDIAN
        else -> null
    }

    private fun scanBlendBlocks(input: DataInputStream, format: BlendFormat): BufferedImage? {
        var remaining = MAX_BLEND_SCAN_BYTES
        while (remaining > 0) {
            val code = ByteArray(4)
            if (!input.readFullyOrFalse(code)) return null
            val blockLength = input.readBlendBlockLength(format)
            val headerLength = format.blockHeaderLength
            remaining -= headerLength
            if (blockLength !in 0..remaining) return null
            if (code.contentEquals(TEST_BLOCK)) return readPreviewBlock(input, blockLength, format.byteOrder)
            if (code.contentEquals(END_BLOCK)) return null
            input.skipFully(blockLength)
            remaining -= blockLength
        }
        return null
    }

    private fun DataInputStream.readBlendBlockLength(format: BlendFormat): Long =
        if (format.usesLargeBlockHeaders) {
            skipFully(12)
            readLong(format.byteOrder).also { skipFully(8) }
        } else {
            readInt(format.byteOrder).toLong().and(0xffff_ffffL)
                .also { skipFully((format.pointerSize + 8).toLong()) }
        }

    private fun readPreviewBlock(input: DataInputStream, blockLength: Long, order: ByteOrder): BufferedImage? {
        if (blockLength !in 8..MAX_PREVIEW_BLOCK_BYTES) return null
        val width = input.readInt(order)
        val height = input.readInt(order)
        if (width !in 1..MAX_PREVIEW_DIMENSION || height !in 1..MAX_PREVIEW_DIMENSION) return null
        val expectedPixelBytes = width.toLong() * height * 4
        if (blockLength != expectedPixelBytes + 8) return null

        val rgba = ByteArray(expectedPixelBytes.toInt())
        input.readFully(rgba)
        val image = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
        val pixels = (image.raster.dataBuffer as java.awt.image.DataBufferInt).data
        for (y in 0 until height) {
            val sourceY = height - y - 1
            for (x in 0 until width) {
                val offset = (sourceY * width + x) * 4
                val red = rgba[offset].toInt() and 0xff
                val green = rgba[offset + 1].toInt() and 0xff
                val blue = rgba[offset + 2].toInt() and 0xff
                val alpha = rgba[offset + 3].toInt() and 0xff
                pixels[y * width + x] = (alpha shl 24) or (red shl 16) or (green shl 8) or blue
            }
        }
        return image
    }

    private fun DataInputStream.readLong(order: ByteOrder): Long = ByteBuffer.wrap(readNBytes(8)).order(order).long

    private fun DataInputStream.skipFully(count: Long) {
        var remaining = count
        while (remaining > 0) {
            val skipped = skip(remaining)
            if (skipped == 0L) {
                if (read() == -1) throw java.io.EOFException()
                remaining--
            } else {
                remaining -= skipped
            }
        }
    }

    private fun DataInputStream.readFullyOrFalse(buffer: ByteArray): Boolean =
        runCatching { readFully(buffer) }.isSuccess

    private fun DataInputStream.readInt(order: ByteOrder): Int =
        ByteBuffer.wrap(readNBytes(4)).order(order).int

    private fun createImageThumbnail(media: MediaFile): Thumbnail {
        val output = ByteArrayOutputStream()
        media.resource.inputStream.use { input ->
            Thumbnails.of(input).size(320, 240).outputFormat("jpg").toOutputStream(output)
        }
        return Thumbnail(output.toByteArray(), MediaType.IMAGE_JPEG)
    }

    private fun createGifThumbnail(media: MediaFile): Thumbnail {
        val output = ByteArrayOutputStream()
        val converter = Java2DFrameConverter()
        FFmpegFrameGrabber(media.resource.file).use { grabber ->
            grabber.start()
            val firstFrame = grabber.grabImage()
                ?: throw IllegalArgumentException("GIF ${media.fileName} contains no frames")
            val firstImage = converter.convert(firstFrame)
                ?: throw IllegalArgumentException("Could not decode GIF ${media.fileName}")
            val firstThumbnail = resize(firstImage)
            val writer = ImageIO.getImageWritersByFormatName("gif").asSequence().first()
            MemoryCacheImageOutputStream(output).use { imageOutput ->
                writer.output = imageOutput
                writer.prepareWriteSequence(null)
                val frameRate = grabber.frameRate
                val delayCentiseconds = if (frameRate > 0.0) (100.0 / frameRate).toInt().coerceAtLeast(1) else 10
                writeGifFrame(writer, firstThumbnail, delayCentiseconds)
                while (true) {
                    val frame = grabber.grabImage() ?: break
                    val image = converter.convert(frame) ?: continue
                    writeGifFrame(writer, resize(image), delayCentiseconds)
                }
                writer.endWriteSequence()
            }
            writer.dispose()
            grabber.stop()
        }
        return Thumbnail(output.toByteArray(), MediaType.IMAGE_GIF)
    }

    private fun createVideoThumbnail(media: MediaFile): Thumbnail {
        val converter = Java2DFrameConverter()
        val image = FFmpegFrameGrabber(media.resource.file).use { grabber ->
            grabber.start()
            val frame = grabber.grabImage()
                ?: throw IllegalArgumentException("Could not decode video ${media.fileName}")
            val thumbnail = converter.convert(frame)
                ?: throw IllegalArgumentException("Could not decode video ${media.fileName}")
            grabber.stop()
            resize(thumbnail)
        }
        val output = ByteArrayOutputStream()
        ImageIO.write(image, "jpg", output)
        return Thumbnail(output.toByteArray(), MediaType.IMAGE_JPEG)
    }

    private fun resize(image: BufferedImage): BufferedImage = Thumbnails.of(image).size(320, 240).asBufferedImage()

    private fun writeGifFrame(writer: javax.imageio.ImageWriter, image: BufferedImage, delayCentiseconds: Int) {
        val metadata = writer.getDefaultImageMetadata(ImageTypeSpecifier.createFromRenderedImage(image), writer.defaultWriteParam)
        val root = metadata.getAsTree("javax_imageio_gif_image_1.0") as org.w3c.dom.Node
        val control = (0 until root.childNodes.length)
            .map { root.childNodes.item(it) }
            .filterIsInstance<IIOMetadataNode>()
            .firstOrNull { it.nodeName == "GraphicControlExtension" }
            ?: IIOMetadataNode("GraphicControlExtension").also(root::appendChild)
        control.setAttribute("disposalMethod", "none")
        control.setAttribute("userInputFlag", "FALSE")
        control.setAttribute("transparentColorFlag", "FALSE")
        control.setAttribute("delayTime", delayCentiseconds.toString())
        control.setAttribute("transparentColorIndex", "0")
        metadata.setFromTree("javax_imageio_gif_image_1.0", root)
        writer.writeToSequence(IIOImage(image, null, metadata), writer.defaultWriteParam)
    }

    private data class BlendFormat(
        val pointerSize: Int,
        val byteOrder: ByteOrder,
        val usesLargeBlockHeaders: Boolean,
    ) {
        val blockHeaderLength: Long
            get() = if (usesLargeBlockHeaders) 32 else pointerSize + 16L
    }

    private data class Thumbnail(val bytes: ByteArray, val mediaType: MediaType) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as Thumbnail

            if (!bytes.contentEquals(other.bytes)) return false
            if (mediaType != other.mediaType) return false

            return true
        }

        override fun hashCode(): Int {
            var result = bytes.contentHashCode()
            result = 31 * result + mediaType.hashCode()
            return result
        }
    }

    private enum class ThumbnailFormat { IMAGE, GIF, MP4, BLEND, UNKNOWN }

}
