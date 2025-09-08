package com.qlarr.backend.helpers

import net.bramp.ffmpeg.FFmpeg
import net.bramp.ffmpeg.FFmpegExecutor
import net.bramp.ffmpeg.FFprobe
import net.bramp.ffmpeg.builder.FFmpegBuilder
import net.bramp.ffmpeg.probe.FFmpegStream
import org.springframework.stereotype.Component
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.io.File
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.nio.file.StandardOpenOption
import javax.imageio.IIOImage
import javax.imageio.ImageIO
import javax.imageio.ImageWriteParam


@Component
class MediaOptimizer {

    fun optimizeImage(inputStream: InputStream, outputPath: Path, contentType: String): File {
        val originalImage = ImageIO.read(inputStream) ?: throw IllegalArgumentException("Could not read image")

        val (newWidth, newHeight) = calculateDimensions(originalImage.width, originalImage.height)

        val resizedImage = BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_ARGB)
        resizedImage.createGraphics().apply {
            setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR)
            drawImage(originalImage, 0, 0, newWidth, newHeight, null)
            dispose()
        }

        val outFormat = when (contentType.lowercase()) {
            "image/jpeg", "image/jpg" -> "jpeg"
            "image/png" -> "png"
            else -> throw IllegalArgumentException("Unsupported image type: $contentType")
        }

        Files.createDirectories(outputPath.parent)
        Files.newOutputStream(outputPath, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)
            .use { outputStream ->
                val writer = ImageIO.getImageWritersByFormatName(outFormat).asSequence().firstOrNull()
                    ?: throw IllegalArgumentException("No ImageWriter for format: $outFormat")
                val writeParam = writer.defaultWriteParam.apply {
                    when (outFormat) {
                        "jpeg" -> {
                            compressionMode = ImageWriteParam.MODE_EXPLICIT
                            compressionQuality = JPEG_QUALITY
                            try {
                                setProgressiveMode(ImageWriteParam.MODE_DEFAULT)
                            } catch (_: Exception) {
                            }
                        }

                        "png" -> {
                            try {
                                compressionMode = ImageWriteParam.MODE_EXPLICIT
                                compressionQuality = PNG_COMPRESSION
                            } catch (_: Exception) {
                            }
                        }
                    }
                }

                ImageIO.createImageOutputStream(outputStream).use { imageOutputStream ->
                    writer.output = imageOutputStream
                    writer.write(null, IIOImage(resizedImage, null, null), writeParam)
                }
                writer.dispose()
            }
        return outputPath.toFile()
    }

    fun optimizeVideo(inputPath: String, outputPath: Path): File {
        Files.createDirectories(outputPath.parent)

        val tmp = Files.createTempFile(Path.of(inputPath).parent, null, ".mp4")

        try {
            val ffmpeg = FFmpeg()
            val ffprobe = FFprobe()

            val filter = "scale=w='min(1920,iw)':h='min(1920,ih)':force_original_aspect_ratio=decrease,format=yuv420p"
            val hasAudio = ffprobe.probe(inputPath).streams.any { it.codec_type == FFmpegStream.CodecType.AUDIO }

            val builder = FFmpegBuilder()
                .setInput(inputPath)
                .overrideOutputFiles(true)
                .addOutput(tmp.toString())
                .setFormat("mp4")
                .setVideoCodec("libx264")
                .setVideoFilter(filter)
                .addExtraArgs("-crf", "22")
                .addExtraArgs("-preset", "medium")
                .addExtraArgs("-movflags", "+faststart")
                .addExtraArgs("-report")
                .apply {
                    if (hasAudio) {
                        setAudioCodec("aac")
                        setAudioBitRate(128_000)
                        addExtraArgs("-ac", "2")
                        addExtraArgs("-ar", "48000")
                    } else {
                        disableAudio()
                    }
                }
                .done()

            val executor = FFmpegExecutor(ffmpeg, ffprobe)
            executor.createJob(builder).run()

            if (!Files.exists(tmp) || Files.size(tmp) == 0L) {
                throw IllegalStateException("FFmpeg failed to create output")
            }

            Files.move(tmp, outputPath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
            return outputPath.toFile()
        } finally {
            Files.deleteIfExists(tmp)
        }
    }

    private fun calculateDimensions(width: Int, height: Int): Pair<Int, Int> {
        val maxDimension = MAX_DIMENSION
        return if (width > maxDimension || height > maxDimension) {
            if (width > height) {
                val ratio = maxDimension.toDouble() / width
                Pair(maxDimension, (height * ratio).toInt())
            } else {
                val ratio = maxDimension.toDouble() / height
                Pair((width * ratio).toInt(), maxDimension)
            }
        } else {
            Pair(width, height)
        }
    }

    fun isSupportedImage(contentType: String): Boolean {
        return when (contentType.lowercase()) {
            "image/jpeg", "image/jpg",
            "image/png" -> true

            else -> false
        }
    }

    fun isVideoContentType(contentType: String?): Boolean =
        contentType?.lowercase()?.startsWith("video/") == true

    companion object {
        const val MAX_DIMENSION = 1920
        private const val JPEG_QUALITY = 0.80f
        private const val WEBP_QUALITY = 0.75f
        private const val PNG_COMPRESSION = 0.90f
    }
}
