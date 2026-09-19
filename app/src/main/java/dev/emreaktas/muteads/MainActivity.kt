package dev.emreaktas.muteads

import android.app.Activity
import android.content.Intent
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class MainActivity : Activity() {

    private lateinit var root: LinearLayout
    private lateinit var scroll: ScrollView
    private lateinit var page: LinearLayout
    private lateinit var navHolder: FrameLayout

    private var tab = 0
    private val clock = SimpleDateFormat("HH:mm", Locale.US)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        T.apply(resolveDark())
        // Avoids a flash of the wrong colour before the first render lands.
        window.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(T.background))

        root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }

        scroll = ScrollView(this).apply {
            isFillViewport = true
            clipToPadding = false
        }
        page = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(26), dp(16), dp(24))
        }
        scroll.addView(page, MATCH_PARENT, WRAP_CONTENT)

        navHolder = FrameLayout(this)

        root.addView(scroll, LinearLayout.LayoutParams(MATCH_PARENT, 0, 1f))
        root.addView(navHolder, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT))
        setContentView(root)
    }

    override fun onResume() {
        super.onResume()
        render()
    }

    private fun render() {
        T.apply(resolveDark())
        paintSystemBars()

        root.setBackgroundColor(T.background)
        scroll.setBackgroundColor(T.background)

        page.removeAllViews()
        page.addView(eyebrow("PROMO FILTER"))
        page.addView(gap(4))

        val bar = row()
        bar.addView(
            h1(listOf("Activity", "Insights", "Settings")[tab]),
            LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f)
        )
        if (tab == 0) bar.addView(statusBadge())
        page.addView(bar)
        page.addView(gap(20))

        when (tab) {
            0 -> activityTab()
            1 -> insightsTab()
            else -> settingsTab()
        }

        navHolder.removeAllViews()
        navHolder.addView(
            bottomNav(
                listOf(
                    NavItem("Activity", R.drawable.ic_activity),
                    NavItem("Insights", R.drawable.ic_insights),
                    NavItem("Settings", R.drawable.ic_settings)
                ),
                tab
            ) {
                tab = it
                render()
                scroll.post { scroll.scrollTo(0, 0) }
            }
        )
    }

    private fun statusBadge() = when {
        Store.apiKey(this).isBlank() -> badge("● No key", BadgeTone.DESTRUCTIVE)
        !hasNotificationAccess() -> badge("● No access", BadgeTone.DESTRUCTIVE)
        Store.enabled(this) -> badge("● Active", BadgeTone.SUCCESS)
        else -> badge("● Paused", BadgeTone.OUTLINE)
    }

    // ================= ACTIVITY =================

    private fun activityTab() {
        val stats = card()
        val r = row()
        r.addView(stat(Store.screened(this).toString(), "Screened"), weight())
        r.addView(vLine())
        r.addView(stat(Store.blocked(this).toString(), "Dismissed"), weight())
        r.addView(vLine())
        r.addView(stat(money(Store.tokens(this) * COST_PER_TOKEN), "Spent"), weight())
        stats.addView(r)
        page.addView(stats)

        val log = Store.log(this)

        page.addView(gap(22))
        val head = row()
        head.addView(sectionLabel("Recent decisions"), LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f))
        head.addView(badge(log.size.toString(), BadgeTone.OUTLINE))
        page.addView(head)
        page.addView(gap(10))

        if (log.isEmpty()) {
            page.addView(emptyCard(
                "Nothing yet",
                if (hasNotificationAccess()) "Waiting for the next notification."
                else "Grant notification access to get started."
            ))
            return
        }

        log.forEach {
            page.addView(decision(it))
            page.addView(gap(8))
        }
        page.addView(gap(4))
        page.addView(button("Clear history", ButtonTone.GHOST) {
            Store.clear(this)
            render()
        })
    }

    private fun decision(e: Entry): View {
        /* edge brighter at the top, the way a real surface catches light; the
           dismissed rows are the interesting ones so they get the coloured edge */
        val edgeTop: Int
        val edgeBottom: Int
        when {
            e.failed -> { edgeTop = 0x1FFFFFFF; edgeBottom = 0x08FFFFFF }
            e.blocked -> { edgeTop = 0x99E5484D.toInt(); edgeBottom = 0x4DE5484D }
            else -> { edgeTop = 0x3DFFFFFF; edgeBottom = 0x0FFFFFFF }
        }
        val c = notifCard(edgeTop, edgeBottom)

        val head = row()
        head.addView(appMark(e))
        head.addView(
            description(e.app.uppercase(Locale.getDefault())).apply {
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
                letterSpacing = 0.09f
                maxLines = 1
                ellipsize = android.text.TextUtils.TruncateAt.END
            },
            LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f).apply { marginStart = dp(10) }
        )
        head.addView(
            when {
                e.failed -> badge("Skipped", BadgeTone.OUTLINE)
                e.blocked -> badge("Dismissed", BadgeTone.DESTRUCTIVE)
                else -> badge("Kept", BadgeTone.OUTLINE)
            }
        )
        c.addView(head)

        if (e.title.isNotEmpty()) {
            c.addView(gap(9))
            c.addView(body(e.title).apply {
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
                maxLines = 2
            })
        }
        if (e.body.isNotEmpty()) {
            c.addView(gap(2))
            c.addView(description(e.body).apply { maxLines = 2 })
        }

        c.addView(gap(11))
        c.addView(separator())
        c.addView(gap(10))

        val foot = row()
        foot.addView(meta(clock.format(Date(e.at))))
        if (e.failed) {
            foot.addView(
                meta("Jev unreachable, notification left alone").apply { maxLines = 1 },
                LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f).apply { marginStart = dp(8) }
            )
        } else {
            foot.addView(
                meter(e.promo, if (e.blocked) T.destructive else T.faintForeground, 4),
                LinearLayout.LayoutParams(0, dp(4), 1f).apply {
                    marginStart = dp(10)
                    marginEnd = dp(10)
                }
            )
            foot.addView(meta("promo " + pct(e.promo) + "  ·  " + e.latencyMs + "ms"))
        }
        c.addView(foot)
        return c
    }

    /** the sender's own icon when it is still installed, the initial otherwise */
    private fun appMark(e: Entry): View {
        val icon = try {
            packageManager.getApplicationIcon(e.pkg)
        } catch (x: Exception) {
            null
        }
        return if (icon != null) {
            android.widget.ImageView(this).apply {
                setImageDrawable(icon)
                layoutParams = LinearLayout.LayoutParams(dp(26), dp(26))
            }
        } else {
            avatar(e.app, e.blocked)
        }
    }

    // ================= INSIGHTS =================

    private fun insightsTab() {
        val log = Store.log(this)
        val judged = log.filter { !it.failed }

        if (judged.isEmpty()) {
            page.addView(emptyCard(
                "Not enough data yet",
                "Numbers appear once notifications start arriving."
            ))
            return
        }

        val dismissed = judged.count { it.blocked }
        val kept = judged.size - dismissed

        // Hero
        val hero = card()
        hero.addView(description("Dismissed"))
        hero.addView(h1(dismissed.toString()).apply {
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 40f)
        })
        hero.addView(gap(12))
        hero.addView(ratioBar(dismissed, kept, 8))
        hero.addView(gap(10))
        val legend = row()
        legend.addView(legendKey(T.destructive, dismissed.toString() + " dismissed"))
        legend.addView(View(this).apply {
            layoutParams = LinearLayout.LayoutParams(dp(14), dp(1))
        })
        legend.addView(legendKey(T.secondary, kept.toString() + " kept"))
        hero.addView(legend)
        page.addView(hero)
        page.addView(gap(10))

        // Last 24 hours
        val buckets = Array(24) { intArrayOf(0, 0) }
        val cal = Calendar.getInstance()
        judged.forEach {
            cal.timeInMillis = it.at
            val h = cal.get(Calendar.HOUR_OF_DAY)
            if (it.blocked) buckets[h][0]++ else buckets[h][1]++
        }
        val chartCard = card()
        chartCard.addView(cardTitle("Last 24 hours"))
        chartCard.addView(gap(3))
        chartCard.addView(description("Red is what never reached you."))
        chartCard.addView(gap(14))
        chartCard.addView(hourChart(buckets.map { Pair(it[0], it[1]) }, 86))
        chartCard.addView(gap(8))
        val axis = row()
        axis.addView(meta("00:00"), LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f))
        axis.addView(meta("12:00").apply { gravity = Gravity.CENTER },
            LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f))
        axis.addView(meta("23:00").apply { gravity = Gravity.END },
            LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f))
        chartCard.addView(axis)
        page.addView(chartCard)
        page.addView(gap(10))

        // By category
        val cats = judged.groupingBy { it.category }.eachCount()
            .entries.sortedByDescending { it.value }
        val catMax = cats.firstOrNull()?.value ?: 1
        val catCard = card()
        catCard.addView(cardTitle("By category"))
        catCard.addView(gap(4))
        cats.forEachIndexed { i, entry ->
            if (i > 0) catCard.addView(separator())
            catCard.addView(
                meterRow(
                    capitalize(entry.key),
                    entry.value.toDouble() / catMax,
                    entry.value.toString(),
                    entry.key != "promo"
                )
            )
        }
        page.addView(catCard)
        page.addView(gap(10))

        // Worst offenders
        val apps = judged.filter { it.blocked }
            .groupingBy { it.app }.eachCount()
            .entries.sortedByDescending { it.value }.take(5)
        if (apps.isNotEmpty()) {
            val appMax = apps.first().value
            val appCard = card()
            appCard.addView(cardTitle("Worst offenders"))
            appCard.addView(gap(3))
            appCard.addView(description("Apps with the most dismissals"))
            appCard.addView(gap(4))
            apps.forEachIndexed { i, entry ->
                if (i > 0) appCard.addView(separator())
                appCard.addView(
                    meterRow(entry.key, entry.value.toDouble() / appMax, entry.value.toString(), false)
                )
            }
            page.addView(appCard)
            page.addView(gap(10))
        }

        // Performance
        val latencies = judged.map { it.latencyMs }.filter { it > 0 }.sorted()
        val median = if (latencies.isEmpty()) 0L else latencies[latencies.size / 2]
        val p95 = if (latencies.isEmpty()) 0L
        else latencies[(latencies.size * 95 / 100).coerceAtMost(latencies.size - 1)]

        val screened = Store.screened(this)
        val perCall = if (screened > 0) Store.tokens(this) * COST_PER_TOKEN / screened else 0.0

        val perf = card()
        perf.addView(cardTitle("Performance"))
        perf.addView(gap(4))
        perf.addView(kvRow("Median latency", median.toString() + "ms"))
        perf.addView(separator())
        perf.addView(kvRow("95th percentile", seconds(p95)))
        perf.addView(separator())
        perf.addView(kvRow("Cost per 1,000", money(perCall * 1000)))
        perf.addView(separator())
        perf.addView(kvRow("Skipped calls", log.count { it.failed }.toString()))
        perf.addView(gap(14))
        perf.addView(separator())
        perf.addView(gap(14))
        perf.addView(
            description(
                "At this rate, screening 100 notifications a day for a year costs about " +
                    money(perCall * 100 * 365) + "."
            )
        )
        page.addView(perf)
    }

    // ================= SETTINGS =================

    private fun settingsTab() {
        val granted = hasNotificationAccess()
        val key = Store.apiKey(this)

        // API key -- first thing anyone running a source build needs.
        val keyCard = card(borderColor = if (key.isBlank()) T.destructiveBorder else T.border)
        val kr = row()
        val kt = column()
        kt.addView(cardTitle("Jev API key"))
        kt.addView(gap(3))
        kt.addView(
            description(
                if (key.isBlank()) "Needed before anything can be judged."
                else "Stored on this device only, never sent anywhere else."
            )
        )
        kr.addView(kt, LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f).apply { marginEnd = dp(10) })
        kr.addView(
            if (key.isBlank()) badge("Not set", BadgeTone.DESTRUCTIVE)
            else badge("● Set", BadgeTone.SUCCESS)
        )
        keyCard.addView(kr)
        keyCard.addView(gap(12))
        val field = textField(key, "apikey_...")
        keyCard.addView(field)
        keyCard.addView(gap(8))
        val keyActions = row()
        keyActions.addView(
            button("Save key", ButtonTone.DEFAULT) {
                Store.setApiKey(this, field.text.toString())
                render()
            },
            LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f).apply { marginEnd = dp(5) }
        )
        keyActions.addView(
            button("Get a key", ButtonTone.OUTLINE) { open("https://console.typesafe.ai") },
            LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f).apply { marginStart = dp(5) }
        )
        keyCard.addView(keyActions)
        page.addView(keyCard)
        page.addView(gap(10))

        val access = card(borderColor = if (granted) T.border else T.destructiveBorder)
        val ar = row()
        val at = column()
        at.addView(cardTitle("Notification access"))
        at.addView(gap(3))
        at.addView(
            description(
                if (granted) "The filter can see notifications."
                else "Nothing works until you grant it."
            )
        )
        ar.addView(at, LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f).apply { marginEnd = dp(10) })
        if (granted) ar.addView(badge("● On", BadgeTone.SUCCESS))
        access.addView(ar)
        if (!granted) {
            access.addView(gap(14))
            access.addView(button("Open settings", ButtonTone.DEFAULT) {
                startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
            })
        }
        page.addView(access)
        page.addView(gap(10))

        // Filter + threshold
        val settings = card()
        val toggle = row()
        val tl = column()
        tl.addView(cardTitle("Filter"))
        tl.addView(gap(3))
        tl.addView(description("When off, no notification is touched."))
        toggle.addView(tl, LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f).apply { marginEnd = dp(12) })
        toggle.addView(shadcnSwitch(Store.enabled(this)) {
            Store.setEnabled(this, it)
            render()
        })
        settings.addView(toggle)

        settings.addView(gap(14))
        settings.addView(separator())
        settings.addView(gap(14))

        val current = Store.threshold(this)
        val th = row()
        th.addView(cardTitle("Confidence threshold"), LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f))
        val chip = badge(pct(current.toDouble()), BadgeTone.SECONDARY)
        th.addView(chip)
        settings.addView(th)
        settings.addView(
            shadcnSlider(39, ((current * 100).toInt() - 60).coerceIn(0, 39)) { p, fromUser ->
                val v = (60 + p) / 100f
                chip.text = pct(v.toDouble())
                if (fromUser) Store.setThreshold(this, v)
            }
        )
        settings.addView(gap(10))
        settings.addView(
            description(
                "Jev's probabilities are calibrated: what it calls 90% is promotional " +
                    "about 90% of the time. Raise this to dismiss less."
            )
        )
        page.addView(settings)
        page.addView(gap(10))

        // Appearance
        val look = card()
        look.addView(cardTitle("Appearance"))
        look.addView(gap(3))
        look.addView(description("Theme used across the app."))
        look.addView(gap(12))
        look.addView(segmented(listOf("Light", "Dark", "System"), Store.themeMode(this)) {
            Store.setThemeMode(this, it)
            render()
        })
        page.addView(look)
        page.addView(gap(10))

        // Never touched
        val never = card()
        never.addView(cardTitle("Never touched"))
        never.addView(gap(3))
        never.addView(description("These never reach Jev at all."))
        never.addView(gap(10))
        chipRows(
            listOf("Calls", "Alarms", "Navigation", "Media", "Progress", "Ongoing", "System UI")
        ).forEach { never.addView(it) }
        page.addView(never)
        page.addView(gap(10))

        // Rubric
        val rubric = card()
        rubric.addView(cardTitle("What Jev is asked"))
        rubric.addView(gap(3))
        rubric.addView(description("Three questions, one call, answered in parallel."))
        rubric.addView(gap(12))
        question(
            rubric, "is_promo",
            "Commercial outreach or a re-engagement nudge: discounts, campaigns, new " +
                "collections, unrequested offers, streak reminders."
        )
        question(
            rubric, "is_needed",
            "A one-time code, a bank transaction, a delivery update, an appointment, a " +
                "security alert, or a message from a real person."
        )
        question(rubric, "category", "promo · transactional · personal · system · content")
        rubric.addView(separator())
        rubric.addView(gap(12))
        rubric.addView(
            description(
                "Dismissed only when is_promo reaches the threshold and is_needed stays " +
                    "at or below 35%. If Jev cannot be reached, nothing is touched."
            )
        )
        page.addView(rubric)
        page.addView(gap(10))

        // About
        val about = card()
        about.addView(cardTitle("MuteAds"))
        about.addView(gap(3))
        about.addView(description("Open source. Built by Emre Aktaş."))
        about.addView(gap(12))
        val links = row()
        links.addView(
            button("GitHub", ButtonTone.OUTLINE) {
                open("https://github.com/emre-aktas/muteads")
            },
            LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f).apply { marginEnd = dp(5) }
        )
        links.addView(
            button("X", ButtonTone.OUTLINE) { open("https://x.com/emredsgn") },
            LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f).apply { marginStart = dp(5) }
        )
        about.addView(links)
        page.addView(about)
    }

    private fun open(url: String) {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, android.net.Uri.parse(url)))
        } catch (e: Exception) {
            // No browser on the device -- nothing useful to do.
        }
    }

    private fun question(into: LinearLayout, name: String, text: String) {
        into.addView(body(name).apply {
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            typeface = android.graphics.Typeface.MONOSPACE
        })
        into.addView(gap(3))
        into.addView(meta(text))
        into.addView(gap(12))
    }

    private fun chipRows(labels: List<String>): List<View> =
        labels.chunked(3).map { chunk ->
            val r = row()
            r.setPadding(0, 0, 0, dp(6))
            chunk.forEach { label ->
                r.addView(
                    badge(label, BadgeTone.OUTLINE),
                    LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT).apply {
                        marginEnd = dp(6)
                    }
                )
            }
            r
        }

    // ================= bits =================

    private fun emptyCard(title: String, desc: String): View {
        val c = card()
        c.addView(body(title))
        c.addView(gap(4))
        c.addView(description(desc))
        return c
    }

    private fun stat(value: String, label: String): View {
        val col = column()
        col.addView(statValue(value))
        col.addView(gap(3))
        col.addView(meta(label))
        return col
    }

    private fun weight() = LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f)

    private fun vLine(): View = View(this).apply {
        setBackgroundColor(T.border)
        layoutParams = LinearLayout.LayoutParams(Math.max(1, dp(1)), dp(34)).apply {
            gravity = Gravity.CENTER_VERTICAL
            marginStart = dp(10)
            marginEnd = dp(10)
        }
    }

    private fun resolveDark(): Boolean = when (Store.themeMode(this)) {
        0 -> false
        1 -> true
        else -> (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
            Configuration.UI_MODE_NIGHT_YES
    }

    private fun paintSystemBars() {
        if (Build.VERSION.SDK_INT >= 21) {
            window.statusBarColor = T.background
            window.navigationBarColor = T.background
        }
        if (Build.VERSION.SDK_INT >= 23) {
            var flags = window.decorView.systemUiVisibility
            flags = if (T.isDark) {
                flags and View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR.inv()
            } else {
                flags or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
            }
            if (Build.VERSION.SDK_INT >= 26) {
                flags = if (T.isDark) {
                    flags and View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR.inv()
                } else {
                    flags or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
                }
            }
            window.decorView.systemUiVisibility = flags
        }
    }

    private fun hasNotificationAccess(): Boolean {
        val flat = Settings.Secure.getString(contentResolver, "enabled_notification_listeners")
        return flat != null && flat.contains(packageName)
    }

    private fun capitalize(s: String): String =
        if (s.isEmpty()) s
        else s.substring(0, 1).uppercase(Locale.US) + s.substring(1)

    private fun pct(v: Double): String = Math.round(v * 100).toString() + "%"

    private fun seconds(ms: Long): String =
        if (ms >= 1000) String.format(Locale.US, "%.2fs", ms / 1000.0) else ms.toString() + "ms"

    private fun money(v: Double): String = when {
        v <= 0.0 -> "$0"
        v < 0.01 -> String.format(Locale.US, "$%.4f", v)
        else -> String.format(Locale.US, "$%.2f", v)
    }

    companion object {
        /** $0.042 per million input tokens, output free. */
        private const val COST_PER_TOKEN = 0.042 / 1_000_000.0
    }
}
