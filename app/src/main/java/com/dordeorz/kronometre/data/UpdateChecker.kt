package com.dordeorz.kronometre.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

sealed interface UpdateStatus {
    data object Unknown : UpdateStatus
    data object Checking : UpdateStatus
    data object UpToDate : UpdateStatus
    data class Available(val version: String, val url: String) : UpdateStatus
    data object Failed : UpdateStatus
}

object UpdateChecker {
    const val REPO_URL = "https://github.com/DorDeorz/Kronometre"
    const val RELEASES_URL = "$REPO_URL/releases"
    private const val LATEST_API = "https://api.github.com/repos/DorDeorz/Kronometre/releases/latest"

    suspend fun check(currentVersion: String): UpdateStatus = withContext(Dispatchers.IO) {
        try {
            val connection = URL(LATEST_API).openConnection() as HttpURLConnection
            connection.connectTimeout = TIMEOUT_MS
            connection.readTimeout = TIMEOUT_MS
            connection.setRequestProperty("Accept", "application/vnd.github+json")
            try {
                if (connection.responseCode != HttpURLConnection.HTTP_OK) return@withContext UpdateStatus.Failed
                val json = JSONObject(connection.inputStream.bufferedReader().use { it.readText() })
                val tag = json.optString("tag_name").removePrefix("v")
                val url = json.optString("html_url").ifEmpty { RELEASES_URL }
                if (isNewer(tag, currentVersion)) UpdateStatus.Available(tag, url) else UpdateStatus.UpToDate
            } finally {
                connection.disconnect()
            }
        } catch (e: IOException) {
            UpdateStatus.Failed
        } catch (e: org.json.JSONException) {
            UpdateStatus.Failed
        }
    }

    fun isNewer(candidate: String, current: String): Boolean {
        val a = parts(candidate)
        val b = parts(current)
        if (a.isEmpty()) return false
        for (i in 0 until maxOf(a.size, b.size)) {
            val x = a.getOrElse(i) { 0 }
            val y = b.getOrElse(i) { 0 }
            if (x != y) return x > y
        }
        return false
    }

    private fun parts(version: String): List<Int> =
        version.trim().removePrefix("v").substringBefore('-').split('.').mapNotNull { it.toIntOrNull() }

    private const val TIMEOUT_MS = 10_000
}
