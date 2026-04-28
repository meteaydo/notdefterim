package com.notdefterim.app.data.remote

import android.content.Context
import android.content.Intent
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.api.services.drive.DriveScopes
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await

/**
 * Google Sign-In akışı ve Drive API erişim token yönetimi.
 *
 * Sadece drive.appdata scope'u talep ediyoruz; bu kullanıcının
 * Drive içeriğine erişmeden yalnızca uygulama verisini yönetmemizi sağlar.
 */
class GoogleAuthManager(private val context: Context) {

  private val _authState = MutableStateFlow<GoogleAuthState>(GoogleAuthState.SignedOut)
  val authState: StateFlow<GoogleAuthState> = _authState.asStateFlow()

  private val signInClient: GoogleSignInClient by lazy {
    val options = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
      .requestEmail()
      // Drive appdata scope — kullanıcı Drive'ını görmez, yalnızca uygulama verisi
      .requestScopes(com.google.android.gms.common.api.Scope(DriveScopes.DRIVE_APPDATA))
      .build()
    GoogleSignIn.getClient(context, options)
  }

  /** Mevcut oturumu kontrol eder (uygulama açılışında çağrılır). */
  fun checkSignInStatus() {
    val account = GoogleSignIn.getLastSignedInAccount(context)
    _authState.value = if (account != null && !account.isExpired) {
      GoogleAuthState.SignedIn(account)
    } else {
      GoogleAuthState.SignedOut
    }
  }

  /** Sessiz yeniden giriş dener; başarısız olursa SignedOut döner. */
  suspend fun trySilentSignIn() {
    try {
      val account = signInClient.silentSignIn().await()
      _authState.value = GoogleAuthState.SignedIn(account)
    } catch (e: Exception) {
      _authState.value = GoogleAuthState.SignedOut
    }
  }

  /** Açık giriş için Intent döndürür; Activity'den başlatılmalı. */
  fun getSignInIntent(): Intent = signInClient.signInIntent

  /** Sign-In intent sonucunu işler. */
  suspend fun handleSignInResult(data: Intent?) {
    try {
      val task = GoogleSignIn.getSignedInAccountFromIntent(data)
      val account = task.await()
      _authState.value = GoogleAuthState.SignedIn(account)
    } catch (e: Exception) {
      _authState.value = GoogleAuthState.Error("Giriş başarısız: ${e.localizedMessage}")
    }
  }

  /** Oturumu kapatır. */
  suspend fun signOut() {
    try {
      signInClient.signOut().await()
      _authState.value = GoogleAuthState.SignedOut
    } catch (e: Exception) {
      _authState.value = GoogleAuthState.Error("Çıkış yapılamadı: ${e.localizedMessage}")
    }
  }

  /** Geçerli oturumdaki hesabı döndürür. */
  fun getCurrentAccount(): GoogleSignInAccount? =
    GoogleSignIn.getLastSignedInAccount(context)
}

sealed class GoogleAuthState {
  object SignedOut : GoogleAuthState()
  data class SignedIn(val account: GoogleSignInAccount) : GoogleAuthState()
  data class Error(val message: String) : GoogleAuthState()
}
