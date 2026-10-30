package com.example.atlas

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import com.example.atlas.data.repository.FirebaseAuthRepository
import com.example.atlas.ui.navigation.MainScreen
import com.example.atlas.ui.screens.auth.GoogleSignInScreen
import com.google.firebase.auth.FirebaseAuth

class MainActivity : ComponentActivity() {
    private val authRepository by lazy { FirebaseAuthRepository() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val firebaseAuth = remember { FirebaseAuth.getInstance() }
            var isSignedIn by remember { mutableStateOf(firebaseAuth.currentUser != null) }

            DisposableEffect(firebaseAuth) {
                val listener = FirebaseAuth.AuthStateListener { auth ->
                    isSignedIn = auth.currentUser != null
                }
                firebaseAuth.addAuthStateListener(listener)
                onDispose { firebaseAuth.removeAuthStateListener(listener) }
            }

            MaterialTheme {
                Surface {
                    if (isSignedIn) {
                        MainScreen(onSignOut = authRepository::signOut)
                    } else {
                        GoogleSignInScreen(
                            onGoogleSignIn = {
                                authRepository.signInWithGoogle(this@MainActivity)
                            }
                        )
                    }
                }
            }
        }
    }
}
