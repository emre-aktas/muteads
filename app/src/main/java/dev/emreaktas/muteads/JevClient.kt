package dev.emreaktas.muteads

import android.util.Log
import org.json.JSONObject
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL

data class Verdict(
    val promo: Double,
    val needed: Double,
    val category: String,
    val inputTokens: Int,
    val latencyMs: Long
)

object JevClient {

    private const val HOST = "https://api.typesafe.ai"
    private const val ENDPOINT = HOST + "/v1/systemone"
    private const val MODEL = "jev-latest"
    private const val TAG = "PromoFilter/Jev"

    init {
        // Keep sockets alive between notifications. Without this every call pays a
        // fresh DNS lookup plus TCP and TLS handshake, which costs far more than the
        // model does.
        System.setProperty("http.keepAlive", "true")
        System.setProperty("http.maxConnections", "5")
    }

    /**
     * Opens the connection to the API so the first real notification does not pay for
     * DNS and the TLS handshake. Safe to call from a background thread, ignores errors.
     */
    fun warmUp() {
        try {
            val conn = URL(HOST + "/").openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.connectTimeout = 3000
            conn.readTimeout = 3000
            drain(conn)
            Log.i(TAG, "connection warmed")
        } catch (e: Exception) {
            Log.w(TAG, "warm-up skipped: " + e.message)
        }
    }

    /**
     * Returns null on any failure -- bad key, no network, timeout, unexpected body.
     * Callers must read null as "leave the notification alone", never as "it is promo".
     */
    fun judge(apiKey: String, state: String): Verdict? {
        if (apiKey.isBlank()) {
            Log.w(TAG, "no API key set")
            return null
        }

        val started = System.currentTimeMillis()
        var conn: HttpURLConnection? = null
        try {
            conn = (URL(ENDPOINT).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 3000
                readTimeout = 4000
                doOutput = true
                setRequestProperty("Authorization", "Bearer " + apiKey)
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("Accept", "application/json")
                setRequestProperty("Connection", "keep-alive")
            }

            val payload = JSONObject()
                .put("model", MODEL)
                .put("state", state)
                .put("questions", Rubric.questions())
                .toString()
                .toByteArray(Charsets.UTF_8)

            // Known length lets the stack skip chunked encoding and avoids buffering.
            conn.setFixedLengthStreamingMode(payload.size)
            conn.outputStream.use { it.write(payload) }

            val code = conn.responseCode
            if (code != 200) {
                Log.w(TAG, "HTTP " + code + ": " + readFully(conn.errorStream))
                return null
            }

            val body = readFully(conn.inputStream)
            return parse(body, System.currentTimeMillis() - started)
        } catch (e: Exception) {
            // Only tear the socket down when something actually went wrong with it.
            conn?.disconnect()
            Log.w(TAG, "call failed: " + e.javaClass.simpleName + ": " + e.message)
            return null
        }
        // Deliberately no disconnect() on the success path: that would close the
        // socket and force the next notification into another TLS handshake.
    }

    private fun readFully(stream: InputStream?): String {
        if (stream == null) return ""
        return stream.bufferedReader().use { it.readText() }
    }

    private fun drain(conn: HttpURLConnection) {
        try {
            readFully(conn.inputStream)
        } catch (e: Exception) {
            readFully(conn.errorStream)
        }
    }

    private fun parse(body: String, latencyMs: Long): Verdict? = try {
        val root = JSONObject(body)
        val answers = root.getJSONObject("answers")
        Verdict(
            promo = answers.getJSONObject("is_promo").getDouble("noul"),
            needed = answers.getJSONObject("is_needed").getDouble("noul"),
            category = answers.getJSONObject("category").optString("choice", "?"),
            inputTokens = root.optJSONObject("usage")?.optInt("input_tokens", 0) ?: 0,
            latencyMs = latencyMs
        )
    } catch (e: Exception) {
        Log.w(TAG, "unparseable response: " + e.message)
        null
    }
}
