package com.orchestrator.app.data.repository

import com.orchestrator.app.data.api.ApiService
import com.orchestrator.app.data.model.LoginRequest
import com.orchestrator.app.data.store.TokenStore
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val api: ApiService,
    private val tokenStore: TokenStore
) {

    suspend fun login(username: String, password: String): Result<Unit> {
        return try {
            val response = api.login(LoginRequest(username, password))
            tokenStore.saveToken(response.access_token)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun logout() {
        tokenStore.clearToken()
    }

    fun isLoggedIn(): Boolean {
        return tokenStore.getToken() != null
    }
}
