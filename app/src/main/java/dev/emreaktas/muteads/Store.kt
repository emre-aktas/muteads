package dev.emreaktas.muteads

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class Entry(
    val key: String,
    val pkg: String,
    val at: Long,
    val app: String,
    val title: String,
    val body: String,
    val blocked: Boolean,
    val failed: Boolean,
    val promo: Double,
    val needed: Double,
    val category: String,
    val latencyMs: Long,
    val tokens: Int
)

/**
 * Every decision is written down, including the ones that dismissed something.
 * A filter you cannot audit is a filter you will not trust.
 */
object Store {

    private const val PREFS = "muteads"
    private const val KEY_ENABLED = "enabled"
    private const val KEY_THRESHOLD = "threshold"
    private const val KEY_THEME = "theme"
    private const val KEY_API = "apiKey"
    private const val KEY_LOG = "log"
    private const val KEY_SCREENED = "screened"
    private const val KEY_BLOCKED = "blocked"
    private const val KEY_TOKENS = "tokens"

    private const val MAX_LOG = 150

    /** Re-posts of the same notification within this window update the existing row. */
    private const val COLLAPSE_WINDOW_MS = 5 * 60_000L

    private fun prefs(c: Context) = c.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun enabled(c: Context): Boolean = prefs(c).getBoolean(KEY_ENABLED, true)
    fun setEnabled(c: Context, v: Boolean) = prefs(c).edit().putBoolean(KEY_ENABLED, v).apply()

    fun threshold(c: Context): Float = prefs(c).getFloat(KEY_THRESHOLD, 0.90f)
    fun setThreshold(c: Context, v: Float) = prefs(c).edit().putFloat(KEY_THRESHOLD, v).apply()

    /** 0 = light, 1 = dark, 2 = follow the system. */
    fun themeMode(c: Context): Int = prefs(c).getInt(KEY_THEME, 2)
    fun setThemeMode(c: Context, v: Int) = prefs(c).edit().putInt(KEY_THEME, v).apply()

    /**
     * The key the user pasted in, falling back to one compiled into the build.
     * Source builds ship without a key, so the app asks for one on first run.
     */
    fun apiKey(c: Context): String {
        val saved = prefs(c).getString(KEY_API, "").orEmpty().trim()
        return if (saved.isNotEmpty()) saved else BuildConfig.JEV_API_KEY.trim()
    }

    fun setApiKey(c: Context, v: String) =
        prefs(c).edit().putString(KEY_API, v.trim()).apply()

    fun screened(c: Context): Int = prefs(c).getInt(KEY_SCREENED, 0)
    fun blocked(c: Context): Int = prefs(c).getInt(KEY_BLOCKED, 0)
    fun tokens(c: Context): Long = prefs(c).getLong(KEY_TOKENS, 0L)

    @Synchronized
    fun record(c: Context, e: Entry) {
        val p = prefs(c)
        val existing = JSONArray(p.getString(KEY_LOG, "[]"))

        // Apps that stream text into a live notification post it several times. Those
        // are updates to one notification, not several notifications -- collapse them
        // onto a single row instead of stacking near-identical entries.
        var replaceAt = -1
        var wasBlocked = false
        for (i in 0 until existing.length()) {
            val o = existing.optJSONObject(i) ?: continue
            if (o.optString("key") == e.key && e.at - o.optLong("at") <= COLLAPSE_WINDOW_MS) {
                replaceAt = i
                wasBlocked = o.optBoolean("blocked")
                break
            }
        }

        val row = JSONObject()
            .put("key", e.key)
            .put("pkg", e.pkg)
            .put("at", e.at)
            .put("app", e.app)
            .put("title", e.title)
            .put("body", e.body)
            .put("blocked", e.blocked)
            .put("failed", e.failed)
            .put("promo", e.promo)
            .put("needed", e.needed)
            .put("category", e.category)
            .put("latencyMs", e.latencyMs)
            .put("tokens", e.tokens)

        val next = JSONArray().put(row)
        for (i in 0 until existing.length()) {
            if (i == replaceAt) continue
            if (next.length() >= MAX_LOG) break
            next.put(existing.get(i))
        }

        val edit = p.edit().putString(KEY_LOG, next.toString())

        if (replaceAt < 0) {
            edit.putInt(KEY_SCREENED, p.getInt(KEY_SCREENED, 0) + 1)
            if (e.blocked) edit.putInt(KEY_BLOCKED, p.getInt(KEY_BLOCKED, 0) + 1)
        } else {
            // Same notification, revised verdict -- move the blocked counter by the
            // difference rather than counting it twice.
            val delta = (if (e.blocked) 1 else 0) - (if (wasBlocked) 1 else 0)
            if (delta != 0) {
                edit.putInt(KEY_BLOCKED, (p.getInt(KEY_BLOCKED, 0) + delta).coerceAtLeast(0))
            }
        }

        edit.putLong(KEY_TOKENS, p.getLong(KEY_TOKENS, 0L) + e.tokens)
        edit.apply()
    }

    @Synchronized
    fun log(c: Context): List<Entry> {
        val arr = JSONArray(prefs(c).getString(KEY_LOG, "[]"))
        val out = ArrayList<Entry>(arr.length())
        for (i in 0 until arr.length()) {
            val o = arr.optJSONObject(i) ?: continue
            out.add(
                Entry(
                    key = o.optString("key"),
                    pkg = o.optString("pkg"),
                    at = o.optLong("at"),
                    app = o.optString("app"),
                    title = o.optString("title"),
                    body = o.optString("body"),
                    blocked = o.optBoolean("blocked"),
                    failed = o.optBoolean("failed"),
                    promo = o.optDouble("promo", 0.0),
                    needed = o.optDouble("needed", 0.0),
                    category = o.optString("category"),
                    latencyMs = o.optLong("latencyMs"),
                    tokens = o.optInt("tokens")
                )
            )
        }
        return out
    }

    @Synchronized
    fun clear(c: Context) {
        prefs(c).edit()
            .remove(KEY_LOG)
            .remove(KEY_SCREENED)
            .remove(KEY_BLOCKED)
            .remove(KEY_TOKENS)
            .apply()
    }
}
