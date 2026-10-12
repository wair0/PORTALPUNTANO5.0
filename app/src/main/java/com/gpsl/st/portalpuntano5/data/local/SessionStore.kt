package com.gpsl.st.portalpuntano5.data.local

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class SessionStore(context: Context) {
    private val prefs: SharedPreferences = EncryptedSharedPreferences.create(
        context, "pp5_session",
        MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )
    var token: String? get() = prefs.getString("token", null)
        set(v) { prefs.edit().putString("token", v).apply() }
    var docNumber: String? get() = prefs.getString("doc", null)
        set(v) { prefs.edit().putString("doc", v).apply() }
    var fullName: String? get() = prefs.getString("name", null)
        set(v) { prefs.edit().putString("name", v).apply() }
    val isLoggedIn: Boolean get() = !token.isNullOrBlank()
    fun clear() = prefs.edit().clear().apply()
}