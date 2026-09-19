package dev.emreaktas.muteads

import android.app.Notification
import android.content.pm.PackageManager
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import java.util.concurrent.Executors

class NotificationFilterService : NotificationListenerService() {

    private val pool = Executors.newFixedThreadPool(3)

    /** notification key -> hash of the text we last judged for it. */
    private val judged = LinkedHashMap<String, Int>()

    override fun onListenerConnected() {
        Log.i(TAG, "listener connected")
        // Pay for DNS and the TLS handshake now, not on the first notification.
        pool.execute { JevClient.warmUp() }
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (!Store.enabled(this)) return
        if (!worthJudging(sbn)) return

        val snap = snapshot(sbn) ?: return

        // An app that streams text into a live notification fires onNotificationPosted
        // once per update, each with a fresh postTime. Judge the content, not the event,
        // so one notification costs one call.
        if (!isNewContent(sbn.key, snap.state)) return

        val apiKey = Store.apiKey(this)
        if (apiKey.isBlank()) return

        pool.execute {
            val verdict = JevClient.judge(apiKey, snap.state)

            if (verdict == null) {
                // Network down, key rejected, timeout -- leave the notification alone.
                Store.record(this, snap.toEntry(blocked = false, failed = true))
                return@execute
            }

            val threshold = Store.threshold(this)
            val kill = verdict.promo >= threshold && verdict.needed <= NEEDED_CEILING

            if (kill) {
                try {
                    cancelNotification(sbn.key)
                } catch (e: Exception) {
                    Log.w(TAG, "cancel failed: " + e.message)
                }
            }

            Store.record(this, snap.toEntry(blocked = kill, failed = false, verdict = verdict))
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        synchronized(judged) { judged.remove(sbn.key) }
    }

    // ---- what we refuse to touch -------------------------------------------------

    private fun worthJudging(sbn: StatusBarNotification): Boolean {
        if (sbn.packageName == packageName) return false

        val n = sbn.notification ?: return false

        // Anything the user cannot swipe away themselves is not ours to remove.
        if (!sbn.isClearable) return false
        if (sbn.isOngoing) return false
        if (n.flags and Notification.FLAG_ONGOING_EVENT != 0) return false
        if (n.flags and Notification.FLAG_FOREGROUND_SERVICE != 0) return false

        // Categories where a wrong call is expensive: a missed call, a silenced alarm,
        // a dropped navigation prompt.
        when (n.category) {
            Notification.CATEGORY_CALL,
            Notification.CATEGORY_ALARM,
            Notification.CATEGORY_NAVIGATION,
            Notification.CATEGORY_TRANSPORT,
            Notification.CATEGORY_SERVICE,
            Notification.CATEGORY_PROGRESS,
            Notification.CATEGORY_SYSTEM -> return false
        }

        if (PROTECTED.any { sbn.packageName.startsWith(it) }) return false

        return true
    }

    private fun isNewContent(key: String, state: String): Boolean {
        val hash = state.hashCode()
        synchronized(judged) {
            if (judged[key] == hash) return false
            judged[key] = hash
            if (judged.size > SEEN_CAP) {
                val it = judged.entries.iterator()
                while (judged.size > SEEN_CAP / 2 && it.hasNext()) {
                    it.next()
                    it.remove()
                }
            }
            return true
        }
    }

    // ---- turning a notification into something Jev can read ----------------------

    private class Snap(
        val key: String,
        val pkg: String,
        val app: String,
        val title: String,
        val body: String,
        val state: String
    ) {
        fun toEntry(blocked: Boolean, failed: Boolean, verdict: Verdict? = null) = Entry(
            key = key,
            pkg = pkg,
            at = System.currentTimeMillis(),
            app = app,
            title = title,
            body = body,
            blocked = blocked,
            failed = failed,
            promo = verdict?.promo ?: 0.0,
            needed = verdict?.needed ?: 0.0,
            category = verdict?.category ?: "-",
            latencyMs = verdict?.latencyMs ?: 0L,
            tokens = verdict?.inputTokens ?: 0
        )
    }

    private fun snapshot(sbn: StatusBarNotification): Snap? {
        val extras = sbn.notification.extras ?: return null

        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty().trim()
        val body = (extras.getCharSequence(Notification.EXTRA_BIG_TEXT)
            ?: extras.getCharSequence(Notification.EXTRA_TEXT))
            ?.toString().orEmpty().trim()
        val sub = extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString().orEmpty().trim()

        if (title.isEmpty() && body.isEmpty()) return null

        val app = appLabel(sbn.packageName)

        val state = buildString {
            append("App: ").append(app).append(" (").append(sbn.packageName).append(")")
            sbn.notification.category?.let { append("\nAndroid category: ").append(it) }
            if (title.isNotEmpty()) append("\nTitle: ").append(title)
            if (body.isNotEmpty()) append("\nBody: ").append(body)
            if (sub.isNotEmpty()) append("\nSubtext: ").append(sub)
        }

        return Snap(sbn.key, sbn.packageName, app, title, body, state)
    }

    private fun appLabel(pkg: String): String = try {
        val pm: PackageManager = packageManager
        pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString()
    } catch (e: Exception) {
        pkg
    }

    companion object {
        private const val TAG = "PromoFilter/Service"

        /**
         * Jev has to be confident it is promo AND confident it is not needed.
         *
         * Measured against real notifications, is_needed comes back bimodal: promos
         * land at 0.04-0.12, anything genuinely wanted lands at 0.69-0.85. The ceiling
         * sits in the empty band between them rather than hugging either edge.
         */
        private const val NEEDED_CEILING = 0.35

        private const val SEEN_CAP = 400

        private val PROTECTED = listOf(
            "com.android.dialer",
            "com.google.android.dialer",
            "com.samsung.android.dialer",
            "com.android.server.telecom",
            "com.android.incallui",
            "com.android.deskclock",
            "com.google.android.deskclock",
            "com.sec.android.app.clockpackage",
            "com.android.settings",
            "com.android.systemui"
        )
    }
}
