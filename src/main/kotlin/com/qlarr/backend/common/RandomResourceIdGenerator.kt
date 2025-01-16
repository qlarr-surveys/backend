package com.qlarr.backend.common


import org.springframework.web.multipart.MultipartFile
import java.security.SecureRandom

object RandomResourceIdGenerator {
    private const val CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
    private const val ID_LENGTH = 10


    fun generateRandomIdWithExtension(file: MultipartFile, length: Int = ID_LENGTH): String {
        val random = SecureRandom()
        val id = StringBuilder(length)
        repeat(length) {
            val index = random.nextInt(CHARACTERS.length)
            id.append(CHARACTERS[index])
        }

        // Capture the extension from the original filename
        val originalFilename = file.originalFilename ?: ""
        val extension = originalFilename.substringAfterLast('.', "")
        return if (extension.isNotEmpty()) "$id.$extension" else id.toString()
    }


}
