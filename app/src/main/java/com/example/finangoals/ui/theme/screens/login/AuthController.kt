package com.example.finangoals.ui.theme.screens.login

import android.content.Context
import android.content.Intent
import androidx.activity.result.ActivityResultLauncher
import com.example.finangoals.R
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await
import java.lang.Exception

sealed class AuthRes<out T> {
    data class Exito<T>(val data: T, val isNewUser: Boolean = false) : AuthRes<T>()
    data class Error(val errorMessage: String) : AuthRes<Nothing>()
}
class AuthController(private val context: Context){
    private val firebaseAuth: FirebaseAuth = Firebase.auth
    private val signInClient = Identity.getSignInClient(context)

    // Iniciar sesión con email y contraseña
    suspend fun signInWithEmailPassword(email: String, password: String): AuthRes<FirebaseUser?> {
        return try {
            val authResult = firebaseAuth.signInWithEmailAndPassword(email, password).await()
            AuthRes.Exito(authResult.user)
        } catch (e: Exception) {
            AuthRes.Error(e.message ?: "Error desconocido")
        }
    }

    // Registrar un nuevo usuario con email y contraseña
    suspend fun signUpWithEmailPassword(email: String, password: String): AuthRes<FirebaseUser?> {
        return try {
            val authResult = firebaseAuth.createUserWithEmailAndPassword(email, password).await()
            AuthRes.Exito(authResult.user)
        } catch (e: Exception) {
            AuthRes.Error(e.message ?: "Error desconocido")
        }
    }

    // Comprobar si el usuario está autenticado
    fun isUserAuthenticated(): Boolean {
        return firebaseAuth.currentUser != null
    }

    private val googleSignInClient: GoogleSignInClient by lazy {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(context.getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        GoogleSignIn.getClient(context, gso)
    }

    fun handleSignInResult(task: Task<GoogleSignInAccount>): AuthRes<GoogleSignInAccount>? {
        return try {
            val account = task.getResult(ApiException::class.java)
            AuthRes.Exito(account)
        } catch (e: ApiException) {
            AuthRes.Error(e.message ?: "Inicio de sesión con Google erróneo.")
        }
    }

    suspend fun signInWithGoogleCredential(credential: AuthCredential): AuthRes<FirebaseUser?> {
        return try {
            val authResult = firebaseAuth.signInWithCredential(credential).await()
            val isNewUser = authResult.additionalUserInfo?.isNewUser ?: false
            authResult.user?.let {
                AuthRes.Exito(it, isNewUser)
            } ?: throw Exception("Inicio de sesión con Google erróneo.")
        } catch (e: Exception) {
            AuthRes.Error(e.message ?: "Inicio de sesión con Google erróneo.")
        }
    }

    fun signInWithGoogle(googleSignInLauncher: ActivityResultLauncher<Intent>) {
        val signInIntent = googleSignInClient.signInIntent
        googleSignInLauncher.launch(signInIntent)
    }

    fun signOut() {
        firebaseAuth.signOut()
        signInClient.signOut()
    }
}


