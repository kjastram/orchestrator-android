package com.orchestrator.app.data.store

import android.app.Application
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TokenStore @Inject constructor(private val context: Application) {

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs = EncryptedSharedPreferences.create(
        context,
        "orchestrator_secure_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun saveToken(token: String) {
        prefs.edit().putString(KEY_AUTH_TOKEN, token).apply()
    }

    fun getToken(): String? {
        return prefs.getString(KEY_AUTH_TOKEN, null)
    }

    fun clearToken() {
        prefs.edit().remove(KEY_AUTH_TOKEN).apply()
    }

    companion object {
        private const val KEY_AUTH_TOKEN = "auth_token"
    }
}
