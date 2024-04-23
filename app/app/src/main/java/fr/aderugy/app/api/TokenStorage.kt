package fr.aderugy.app.api

import android.content.Context
import android.content.SharedPreferences

class TokenStorage(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("TokenPrefs", Context.MODE_PRIVATE)

    fun saveAuthData(authData: AuthData) {
        prefs.edit().apply {
            putString("ACCESS_TOKEN", authData.access_token)
            putString("REFRESH_TOKEN", authData.refresh_token)
            putInt("EXPIRES", authData.expires)
            apply()
        }
    }

    fun getAccessToken(): String? = prefs.getString("ACCESS_TOKEN", null)
    fun getRefreshToken(): String? = prefs.getString("REFRESH_TOKEN", null)

    fun clearAccessToken() {
        prefs.edit().apply {
            remove("ACCESS_TOKEN")
            apply()
        }
    }

    fun clearTokens() {
        prefs.edit().clear().apply()
    }
}
