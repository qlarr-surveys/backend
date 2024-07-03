package com.frankie.backend.services

import com.frankie.backend.exceptions.GoogleAuthError
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.util.*

@Component
class GoogleAuthService(
    @Value("\${auth.googleClientId}")
    val clientId: String
) {

    fun getPayload(credential: String): GoogleIdToken.Payload {
        GoogleIdToken.Payload()
        val transport = NetHttpTransport()
        val jsonFactory = GsonFactory()
        val verifier = GoogleIdTokenVerifier.Builder(
            transport,
            jsonFactory
        )
            .setAudience(Collections.singletonList(clientId))
            .build()

        val idToken: GoogleIdToken? = verifier.verify(credential)
        return if (idToken != null && idToken.payload.emailVerified == true) {
            idToken.payload
        } else {
            throw GoogleAuthError()
        }
    }
}