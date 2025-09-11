package com.qlarr.backend.helpers

import org.bytedeco.ffmpeg.global.avcodec.AV_CODEC_ID_AAC
import org.bytedeco.ffmpeg.global.avcodec.AV_CODEC_ID_H264
import org.bytedeco.ffmpeg.global.avutil.AV_PIX_FMT_YUV420P
import org.bytedeco.javacv.FFmpegFrameGrabber
import org.bytedeco.javacv.FFmpegFrameRecorder
import org.bytedeco.javacv.Frame
import org.bytedeco.javacv.OpenCVFrameConverter
import org.bytedeco.opencv.global.opencv_imgproc.resize
import org.bytedeco.opencv.opencv_core.Mat
import org.bytedeco.opencv.opencv_core.Size
import org.springframework.stereotype.Component
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.io.File
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import javax.imageio.IIOImage
import javax.imageio.ImageIO
import javax.imageio.ImageWriteParam


@Component
class MediaOptimizer {

    fun optimizeImage(inputStream: InputStream, outputPath: Path, contentType: String): File {
        val originalImage = ImageIO.read(inputStream) ?: throw IllegalArgumentException("Could not read image")

        val (newWidth, newHeight) = calculateDimensions(originalImage.width, originalImage.height)

        val outFormat = when (contentType.lowercase()) {
            "image/jpeg", "image/jpg" -> "jpeg"
            "image/png" -> "png"
            else -> throw IllegalArgumentException("Unsupported image type: $contentType")
        }

        // Use RGB for JPEG, ARGB for PNG
        val imageType = if (outFormat == "jpeg") BufferedImage.TYPE_INT_RGB else BufferedImage.TYPE_INT_ARGB
        val resizedImage = BufferedImage(newWidth, newHeight, imageType)
        resizedImage.createGraphics().apply {
            setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR)
            drawImage(originalImage, 0, 0, newWidth, newHeight, null)
            dispose()
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
        val outputPathMp4 = Path.of("${outputPath.toString().substringBeforeLast('.')}.mp4")
        Files.createDirectories(outputPathMp4.parent)

        val grabber = FFmpegFrameGrabber(inputPath)
        val converter = OpenCVFrameConverter.ToMat()

        try {
            grabber.start()

            val iw = grabber.imageWidth
            val ih = grabber.imageHeight
            if (iw <= 0 || ih <= 0) throw IllegalStateException("Could not read video stream")

            val scale = minOf(1920.0 / iw, 1080.0 / ih, 1.0)
            fun makeEven(x: Int) = if (x % 2 == 0) x else x - 1
            val tw = makeEven(Math.round(iw * scale).toInt().coerceAtLeast(2))
            val th = makeEven(Math.round(ih * scale).toInt().coerceAtLeast(2))

            val hasAudio = grabber.audioChannels > 0
            val recorder = FFmpegFrameRecorder(outputPathMp4.toFile(), tw, th, if (hasAudio) 2 else 0)

            recorder.format = "mp4"
            recorder.videoCodec = AV_CODEC_ID_H264
            recorder.setVideoOption("crf", "22")
            recorder.setVideoOption("preset", "medium")
            recorder.setOption("movflags", "+faststart")
            recorder.pixelFormat = AV_PIX_FMT_YUV420P
            recorder.frameRate = if (grabber.frameRate < 30) grabber.frameRate else 30.0
            recorder.gopSize = (recorder.frameRate * 2).toInt().coerceAtLeast(24)

            recorder.setDisplayRotation(grabber.displayRotation)

            if (hasAudio) {
                recorder.audioCodec = AV_CODEC_ID_AAC
                recorder.audioBitrate = 128_000
                recorder.sampleRate = if (grabber.sampleRate > 0) grabber.sampleRate else 48_000
                recorder.audioChannels = 2
            }

            recorder.start()

            var frame: Frame?
            while (true) {
                frame = grabber.grab() ?: break
                when {
                    frame.image != null -> {
                        val mat = converter.convertToMat(frame)
                        val resized = Mat()
                        // Resize to the precomputed exact target size (keeps AR by construction)
                        resize(mat, resized, Size(tw, th))
                        val out = converter.convert(resized)
                        recorder.record(out)
                        resized.release()
                        mat.release()
                    }

                    frame.samples != null && hasAudio -> {
                        // Pass audio through
                        recorder.record(frame)
                    }

                    else -> {
                        // ignore other frame types
                    }
                }
            }

            recorder.stop()
            recorder.release()

            if (!Files.exists(outputPathMp4) || Files.size(outputPathMp4) == 0L) {
                throw IllegalStateException("Encoding failed to produce output")
            }

            return outputPathMp4.toFile()
        } finally {
            try {
                grabber.stop()
                grabber.release()
            } catch (_: Throwable) {
            }
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
        private const val PNG_COMPRESSION = 0.90f
        const val MP4_MIME_TYPE = "video/mp4"
    }
}
