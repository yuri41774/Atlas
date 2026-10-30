package com.example.atlas.data.repository

import android.app.Activity
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.tasks.await

class FirebaseAuthRepository(
    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance()
) {
    suspend fun signInWithGoogle(activity: Activity) {
        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(
                activity.getString(com.example.atlas.R.string.default_web_client_id)
            )
            .setAutoSelectEnabled(false)
            .build()
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()
        val credentialManager = CredentialManager.create(activity)
        val credentialResponse = credentialManager.getCredential(activity, request)
        val googleCredential = GoogleIdTokenCredential.createFrom(
            credentialResponse.credential.data
        )
        val firebaseCredential = GoogleAuthProvider.getCredential(
            googleCredential.idToken,
            null
        )

        firebaseAuth.signInWithCredential(firebaseCredential).await()
    }

    fun signOut() {
        firebaseAuth.signOut()
    }
}
