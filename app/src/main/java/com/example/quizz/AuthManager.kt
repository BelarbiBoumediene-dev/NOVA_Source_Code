package com.example.quizz

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.tasks.await

class AuthManager(
    private val context: Context
) {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    private val credentialManager: CredentialManager =
        CredentialManager.create(context)

    suspend fun signInWithGoogle(): Result<Boolean> {

        return try {

            val googleIdOption = GetGoogleIdOption.Builder()
                .setServerClientId(
                    "228197820031-72ajrvvuoeumj71apje4odchjdgq8jlb.apps.googleusercontent.com"
                )
                .setFilterByAuthorizedAccounts(false)
                .setAutoSelectEnabled(false)
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            val result: GetCredentialResponse =
                withContext(Dispatchers.Main) {
                    credentialManager.getCredential(
                        context = context,
                        request = request
                    )
                }

            val credential = result.credential

            val googleCredential =
                GoogleIdTokenCredential.createFrom(credential.data)

            val firebaseCredential =
                GoogleAuthProvider.getCredential(
                    googleCredential.idToken,
                    null
                )

            auth.signInWithCredential(firebaseCredential).await()

            Result.success(true)

        } catch (e: Exception) {

            Result.failure(e)
        }
    }

    fun getCurrentUser() = auth.currentUser

    fun signOut() {
        auth.signOut()
    }
}