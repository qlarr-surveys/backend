package com.qlarr.backend.helpers

import net.bramp.ffmpeg.FFmpeg
import net.bramp.ffmpeg.FFmpegExecutor
import net.bramp.ffmpeg.FFprobe
import net.bramp.ffmpeg.builder.FFmpegBuilder
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

    fun optimizeImage(inputStream: InputStream, outputPath: Path): File {
        val originalImage = ImageIO.read(inputStream) ?: throw IllegalArgumentException("Could not read image")

        val (newWidth, newHeight) = calculateDimensions(originalImage.width, originalImage.height)

        val resizedImage = BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_RGB)
        resizedImage.createGraphics().apply {
            setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR)
            drawImage(originalImage, 0, 0, newWidth, newHeight, null)
            dispose()
        }

        Files.createDirectories(outputPath.parent)
        Files.newOutputStream(outputPath, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)
            .use { outputStream ->
                val writer = ImageIO.getImageWritersByFormatName("jpeg").next()
                val writeParam = writer.defaultWriteParam.apply {
                    compressionMode = ImageWriteParam.MODE_EXPLICIT
                    compressionQuality = JPEG_QUALITY
                }

                ImageIO.createImageOutputStream(outputStream).use { imageOutputStream ->
                    writer.output = imageOutputStream
                    writer.write(null, IIOImage(resizedImage, null, null), writeParam)
                }
                writer.dispose()
            }
        return outputPath.toFile()
    }

    fun optimizeMp4(inputPath: String, outputPath: Path): File {
        Files.createDirectories(outputPath.parent)

        val tmp = Files.createTempFile(outputPath.parent, outputPath.fileName.toString(), ".tmp")

        try {
            val ffmpeg = FFmpeg()
            val ffprobe = FFprobe()

            val filter = "scale=w='min(1920,iw)':h='min(1920,ih)':force_original_aspect_ratio=decrease,format=yuv420p"

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
                .setAudioCodec("aac")
                .setAudioBitRate(128_000)
                .addExtraArgs("-ac", "2")
                .addExtraArgs("-ar", "48000")
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

    fun isJpeg(contentType: String): Boolean {
        return contentType.contains("image/jpeg")
    }

    fun isMp4(contentType: String): Boolean {
        return contentType.contains("video/mp4")
    }

    companion object {
        const val MAX_DIMENSION = 1920
        const val JPEG_CONTENT_TYPE = "image/jpeg"
        const val JPEG_QUALITY = 0.8f
    }
}
