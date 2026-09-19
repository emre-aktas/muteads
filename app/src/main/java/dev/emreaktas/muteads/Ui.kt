package dev.emreaktas.muteads

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.ClipDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import android.graphics.drawable.RippleDrawable
import android.graphics.drawable.StateListDrawable
import android.os.Build
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.Switch
import android.widget.TextView

/**
 * shadcn/ui is React-only, so its design language is reimplemented here natively:
 * the zinc palette in both modes, an 8/12px radius scale, hairline borders on flat
 * surfaces, and the same badge / button / switch / slider / segmented shapes.
 *
 * Tokens are mutable because the theme is a runtime choice. Views read them while
 * they are built, and the whole tree is rebuilt on a theme change.
 */
object T {
    var background = 0
    var card = 0
    var elevated = 0
    var border = 0
    var foreground = 0
    var mutedForeground = 0
    var faintForeground = 0
    var secondary = 0
    var primary = 0
    var primaryForeground = 0
    var destructive = 0
    var destructiveBorder = 0
    var destructiveSoft = 0
    var success = 0
    var successBorder = 0
    var tabsList = 0
    var ripple = 0
    var rowTop = 0
    var rowBottom = 0

    var isDark = true
        private set

    const val RADIUS_MD = 8
    const val RADIUS_LG = 12

    fun apply(dark: Boolean) {
        isDark = dark
        if (dark) {
            background = 0xFF060607.toInt()
            card = 0xFF101014.toInt()
            elevated = 0xFF131316.toInt()
            border = 0xFF1C1C21.toInt()
            foreground = 0xFFF4F4F5.toInt()
            mutedForeground = 0xFF8B8B93.toInt()
            faintForeground = 0xFF5A5A62.toInt()
            secondary = 0xFF1C1C21.toInt()
            primary = 0xFFF4F4F5.toInt()
            primaryForeground = 0xFF08080A.toInt()
            destructive = 0xFFE5484D.toInt()
            destructiveBorder = 0x66E5484D
            destructiveSoft = 0x1FE5484D
            success = 0xFF4ADE80.toInt()
            successBorder = 0xFF14532D.toInt()
            tabsList = 0xFF131316.toInt()
            ripple = 0x33F4F4F5
            rowTop = 0xFF27272E.toInt()
            rowBottom = 0xFF17171C.toInt()
        } else {
            background = 0xFFFAFAFA.toInt()
            card = 0xFFFFFFFF.toInt()
            elevated = 0xFFFFFFFF.toInt()
            border = 0xFFE4E4E7.toInt()
            foreground = 0xFF09090B.toInt()
            mutedForeground = 0xFF71717A.toInt()
            faintForeground = 0xFFA1A1AA.toInt()
            secondary = 0xFFF4F4F5.toInt()
            primary = 0xFF18181B.toInt()
            primaryForeground = 0xFFFAFAFA.toInt()
            destructive = 0xFFDC2626.toInt()
            destructiveBorder = 0xFFFECACA.toInt()
            destructiveSoft = 0x14DC2626
            success = 0xFF16A34A.toInt()
            successBorder = 0xFFBBF7D0.toInt()
            tabsList = 0xFFF4F4F5.toInt()
            ripple = 0x1A000000
            rowTop = 0xFFFFFFFF.toInt()
            rowBottom = 0xFFF7F7F8.toInt()
        }
    }
}

fun Context.dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()
fun Context.dpf(v: Float): Float = v * resources.displayMetrics.density

// ---- surfaces --------------------------------------------------------------------

fun Context.card(borderColor: Int = T.border, fill: Int = T.card): LinearLayout =
    LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(16), dp(16), dp(16), dp(16))
        background = GradientDrawable().apply {
            cornerRadius = dpf(T.RADIUS_LG.toFloat())
            setColor(fill)
            setStroke(Math.max(1, dp(1)), borderColor)
        }
        layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
    }

fun Context.separator(): View = View(this).apply {
    setBackgroundColor(T.border)
    layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, Math.max(1, dp(1)))
}

fun Context.gap(h: Int): View = View(this).apply {
    layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, dp(h))
}

fun Context.row(): LinearLayout = LinearLayout(this).apply {
    orientation = LinearLayout.HORIZONTAL
    gravity = Gravity.CENTER_VERTICAL
    layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
}

fun Context.column(): LinearLayout = LinearLayout(this).apply {
    orientation = LinearLayout.VERTICAL
    layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
}

// ---- type ------------------------------------------------------------------------

private fun Context.text(s: CharSequence, size: Float, color: Int): TextView =
    TextView(this).apply {
        text = s
        setTextColor(color)
        setTextSize(TypedValue.COMPLEX_UNIT_SP, size)
        setLineSpacing(dpf(2f), 1f)
    }

fun Context.eyebrow(s: CharSequence): TextView = text(s, 10f, T.faintForeground).apply {
    letterSpacing = 0.14f
    setTypeface(typeface, android.graphics.Typeface.BOLD)
}

fun Context.h1(s: CharSequence): TextView = text(s, 28f, T.foreground)
fun Context.cardTitle(s: CharSequence): TextView = text(s, 15f, T.foreground)
fun Context.body(s: CharSequence): TextView = text(s, 14f, T.foreground)
fun Context.description(s: CharSequence): TextView = text(s, 13f, T.mutedForeground)
fun Context.meta(s: CharSequence): TextView = text(s, 11f, T.faintForeground)
fun Context.statValue(s: CharSequence): TextView = text(s, 21f, T.foreground)
fun Context.sectionLabel(s: CharSequence): TextView = text(s, 12f, T.mutedForeground)

// ---- badge -----------------------------------------------------------------------

enum class BadgeTone { DEFAULT, SECONDARY, DESTRUCTIVE, SUCCESS, OUTLINE }

fun Context.badge(label: String, tone: BadgeTone): TextView {
    val fill: Int
    val stroke: Int
    val ink: Int
    when (tone) {
        BadgeTone.DEFAULT -> { fill = T.primary; stroke = T.primary; ink = T.primaryForeground }
        BadgeTone.SECONDARY -> { fill = T.secondary; stroke = T.secondary; ink = T.foreground }
        BadgeTone.DESTRUCTIVE -> { fill = Color.TRANSPARENT; stroke = T.destructiveBorder; ink = T.destructive }
        BadgeTone.SUCCESS -> { fill = Color.TRANSPARENT; stroke = T.successBorder; ink = T.success }
        BadgeTone.OUTLINE -> { fill = Color.TRANSPARENT; stroke = T.border; ink = T.mutedForeground }
    }
    return TextView(this).apply {
        text = label
        setTextColor(ink)
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
        setPadding(dp(8), dp(3), dp(8), dp(3))
        background = GradientDrawable().apply {
            cornerRadius = dpf(999f)
            setColor(fill)
            setStroke(Math.max(1, dp(1)), stroke)
        }
    }
}

// ---- buttons ---------------------------------------------------------------------

enum class ButtonTone { DEFAULT, OUTLINE, GHOST }

fun Context.button(label: String, tone: ButtonTone, onClick: () -> Unit): TextView {
    val fill: Int
    val stroke: Int
    val ink: Int
    when (tone) {
        ButtonTone.DEFAULT -> { fill = T.primary; stroke = T.primary; ink = T.primaryForeground }
        ButtonTone.OUTLINE -> { fill = Color.TRANSPARENT; stroke = T.border; ink = T.foreground }
        ButtonTone.GHOST -> { fill = Color.TRANSPARENT; stroke = Color.TRANSPARENT; ink = T.mutedForeground }
    }
    val shape = GradientDrawable().apply {
        cornerRadius = dpf(T.RADIUS_MD.toFloat())
        setColor(fill)
        if (tone != ButtonTone.GHOST) setStroke(Math.max(1, dp(1)), stroke)
    }
    val mask = GradientDrawable().apply {
        cornerRadius = dpf(T.RADIUS_MD.toFloat())
        setColor(Color.WHITE)
    }
    return TextView(this).apply {
        text = label
        setTextColor(ink)
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
        gravity = Gravity.CENTER
        minimumHeight = dp(42)
        setPadding(dp(16), dp(11), dp(16), dp(11))
        background = RippleDrawable(ColorStateList.valueOf(T.ripple), shape, mask)
        isClickable = true
        isFocusable = true
        setOnClickListener { onClick() }
        layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
    }
}

// ---- bottom navigation -----------------------------------------------------------

class NavItem(val label: String, val icon: Int)

fun Context.bottomNav(items: List<NavItem>, selected: Int, onSelect: (Int) -> Unit): LinearLayout {
    val bar = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setBackgroundColor(T.background)
        layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
    }
    bar.addView(separator())

    val strip = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        setPadding(dp(8), dp(8), dp(8), dp(12))
    }

    items.forEachIndexed { i, item ->
        val active = i == selected
        val ink = if (active) T.foreground else T.faintForeground

        val cell = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(0, dp(6), 0, dp(6))
            background = RippleDrawable(
                ColorStateList.valueOf(T.ripple),
                null,
                GradientDrawable().apply {
                    cornerRadius = dpf(10f)
                    setColor(Color.WHITE)
                }
            )
            isClickable = true
            setOnClickListener { if (!active) onSelect(i) }
        }
        cell.addView(ImageView(this).apply {
            setImageResource(item.icon)
            setColorFilter(ink)
            layoutParams = LinearLayout.LayoutParams(dp(21), dp(21))
        })
        cell.addView(View(this).apply {
            layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, dp(4))
        })
        cell.addView(TextView(this).apply {
            text = item.label
            setTextColor(ink)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 10f)
            gravity = Gravity.CENTER
        })

        strip.addView(cell, LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f))
    }

    bar.addView(strip)
    return bar
}

// ---- segmented control -----------------------------------------------------------

fun Context.segmented(options: List<String>, selected: Int, onSelect: (Int) -> Unit): LinearLayout {
    val bar = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        setPadding(dp(4), dp(4), dp(4), dp(4))
        background = GradientDrawable().apply {
            cornerRadius = dpf(10f)
            setColor(T.tabsList)
        }
        layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
    }
    options.forEachIndexed { i, label ->
        val active = i == selected
        val shape = GradientDrawable().apply {
            cornerRadius = dpf(7f)
            setColor(if (active) T.elevated else Color.TRANSPARENT)
            if (active) setStroke(Math.max(1, dp(1)), T.border)
        }
        val mask = GradientDrawable().apply {
            cornerRadius = dpf(7f)
            setColor(Color.WHITE)
        }
        bar.addView(
            TextView(this).apply {
                text = label
                setTextColor(if (active) T.foreground else T.mutedForeground)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
                gravity = Gravity.CENTER
                setPadding(dp(4), dp(8), dp(4), dp(8))
                background = RippleDrawable(ColorStateList.valueOf(T.ripple), shape, mask)
                isClickable = true
                setOnClickListener { if (!active) onSelect(i) }
            },
            LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f)
        )
    }
    return bar
}

/**
 * The notification card from the site: a vertical gradient body inside a hairline
 * that is brighter at the top, the way a real surface catches light. A single
 * setStroke() cannot do that, so the edge is its own gradient layer underneath.
 */
fun Context.notifCard(edgeTop: Int, edgeBottom: Int): LinearLayout {
    val r = dpf(14f)
    val edge = GradientDrawable(
        GradientDrawable.Orientation.TOP_BOTTOM, intArrayOf(edgeTop, edgeBottom)
    ).apply { cornerRadius = r }
    val fill = GradientDrawable(
        GradientDrawable.Orientation.TOP_BOTTOM, intArrayOf(T.rowTop, T.rowBottom)
    ).apply { cornerRadius = r - dpf(1f) }

    val stack = LayerDrawable(arrayOf<Drawable>(edge, fill))
    val hair = Math.max(1, dp(1))
    stack.setLayerInset(1, hair, hair, hair, hair)

    return LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(14), dp(13), dp(14), dp(13))
        background = stack
        layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
    }
}

// ---- text field ------------------------------------------------------------------

fun Context.textField(value: String, hint: String): android.widget.EditText =
    android.widget.EditText(this).apply {
        setText(value)
        this.hint = hint
        setHintTextColor(T.faintForeground)
        setTextColor(T.foreground)
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
        typeface = android.graphics.Typeface.MONOSPACE
        setSingleLine(true)
        setPadding(dp(12), dp(11), dp(12), dp(11))
        background = GradientDrawable().apply {
            cornerRadius = dpf(T.RADIUS_MD.toFloat())
            setColor(T.background)
            setStroke(Math.max(1, dp(1)), T.border)
        }
        layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
    }

// ---- avatar ----------------------------------------------------------------------

fun Context.avatar(name: String, hot: Boolean): TextView = TextView(this).apply {
    text = name.trim().take(1).uppercase(java.util.Locale.getDefault())
    setTextColor(if (hot) T.destructive else T.mutedForeground)
    setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
    gravity = Gravity.CENTER
    background = GradientDrawable().apply {
        cornerRadius = dpf(8f)
        setColor(if (hot) T.destructiveSoft else T.secondary)
        setStroke(Math.max(1, dp(1)), if (hot) T.destructiveBorder else T.border)
    }
    layoutParams = LinearLayout.LayoutParams(dp(28), dp(28))
}

// ---- meters ----------------------------------------------------------------------

/** A thin pill meter. [fraction] is clamped, and a floor keeps tiny values visible. */
fun Context.meter(fraction: Double, fill: Int, height: Int): LinearLayout {
    val f = fraction.coerceIn(0.0, 1.0).toFloat().coerceAtLeast(0.02f)
    return LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        background = GradientDrawable().apply {
            cornerRadius = dpf(999f)
            setColor(T.secondary)
        }
        addView(View(context).apply {
            background = GradientDrawable().apply {
                cornerRadius = dpf(999f)
                setColor(fill)
            }
            layoutParams = LinearLayout.LayoutParams(0, MATCH_PARENT, f)
        })
        addView(View(context).apply {
            layoutParams = LinearLayout.LayoutParams(0, MATCH_PARENT, 1f - f)
        })
        layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, dp(height))
    }
}

/** label · meter · value, the row used by the Insights breakdowns. */
fun Context.meterRow(label: String, fraction: Double, value: String, dim: Boolean): LinearLayout {
    val r = row()
    r.setPadding(0, dp(9), 0, dp(9))
    r.addView(description(label).apply {
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
        maxLines = 1
        ellipsize = android.text.TextUtils.TruncateAt.END
    }, LinearLayout.LayoutParams(dp(86), WRAP_CONTENT))
    r.addView(
        meter(fraction, if (dim) T.faintForeground else T.primary, 6),
        LinearLayout.LayoutParams(0, dp(6), 1f).apply {
            marginStart = dp(10)
            marginEnd = dp(10)
        }
    )
    r.addView(body(value).apply {
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
        gravity = Gravity.END
    }, LinearLayout.LayoutParams(dp(30), WRAP_CONTENT))
    return r
}

/** Two-part proportion bar, dismissed against kept. */
fun Context.ratioBar(a: Int, b: Int, height: Int): LinearLayout {
    val total = (a + b).coerceAtLeast(1)
    return LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        addView(View(context).apply {
            background = GradientDrawable().apply {
                cornerRadius = dpf(999f); setColor(T.destructive)
            }
            layoutParams = LinearLayout.LayoutParams(0, MATCH_PARENT, a.toFloat() / total)
                .apply { marginEnd = dp(2) }
        })
        addView(View(context).apply {
            background = GradientDrawable().apply {
                cornerRadius = dpf(999f); setColor(T.secondary)
            }
            layoutParams = LinearLayout.LayoutParams(0, MATCH_PARENT, b.toFloat() / total)
        })
        layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, dp(height))
    }
}

fun Context.legendKey(color: Int, label: String): LinearLayout {
    val r = row()
    r.addView(View(this).apply {
        background = GradientDrawable().apply { cornerRadius = dpf(2f); setColor(color) }
        layoutParams = LinearLayout.LayoutParams(dp(8), dp(8)).apply { marginEnd = dp(6) }
    })
    r.addView(meta(label))
    r.layoutParams = LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT)
    return r
}

/** 24 stacked columns: dismissed on top of kept, scaled to the busiest hour. */
fun Context.hourChart(buckets: List<Pair<Int, Int>>, heightDp: Int): LinearLayout {
    val max = buckets.maxOfOrNull { it.first + it.second }?.coerceAtLeast(1) ?: 1
    val full = dp(heightDp)

    return LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.BOTTOM
        layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, full)

        buckets.forEach { (hot, cold) ->
            val col = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.BOTTOM
            }
            val hotPx = (full * hot.toFloat() / max).toInt()
            val coldPx = (full * cold.toFloat() / max).toInt()

            if (hotPx > 0) {
                col.addView(View(context).apply {
                    background = GradientDrawable().apply {
                        cornerRadius = dpf(2f); setColor(T.destructive)
                    }
                    layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, hotPx)
                        .apply { bottomMargin = dp(2) }
                })
            }
            if (coldPx > 0) {
                col.addView(View(context).apply {
                    background = GradientDrawable().apply {
                        cornerRadius = dpf(2f); setColor(T.secondary)
                    }
                    layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, coldPx)
                })
            }
            addView(col, LinearLayout.LayoutParams(0, MATCH_PARENT, 1f).apply {
                marginStart = dp(1)
                marginEnd = dp(1)
            })
        }
    }
}

/** key on the left, value on the right -- the Performance list. */
fun Context.kvRow(key: String, value: String): LinearLayout {
    val r = row()
    r.setPadding(0, dp(9), 0, dp(9))
    r.addView(description(key), LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f))
    r.addView(body(value))
    return r
}

// ---- switch ----------------------------------------------------------------------

fun Context.shadcnSwitch(checked: Boolean, onChange: (Boolean) -> Unit): Switch {
    val trackW = dp(44)
    val trackH = dp(24)
    val thumbSize = dp(18)

    fun pill(color: Int) = GradientDrawable().apply {
        cornerRadius = dpf(999f)
        setColor(color)
        setSize(trackW, trackH)
    }

    fun dot(color: Int) = GradientDrawable().apply {
        shape = GradientDrawable.OVAL
        setColor(color)
        setSize(thumbSize, thumbSize)
    }

    val track = StateListDrawable().apply {
        addState(intArrayOf(android.R.attr.state_checked), pill(T.primary))
        addState(intArrayOf(), pill(T.secondary))
    }
    val thumb = StateListDrawable().apply {
        addState(intArrayOf(android.R.attr.state_checked), dot(T.primaryForeground))
        addState(intArrayOf(), dot(T.mutedForeground))
    }

    return Switch(this).apply {
        isChecked = checked
        trackDrawable = track
        thumbDrawable = thumb
        if (Build.VERSION.SDK_INT >= 21) splitTrack = false
        text = ""
        background = null
        setPadding(0, 0, 0, 0)
        switchMinWidth = trackW
        setOnCheckedChangeListener { _, v -> onChange(v) }
    }
}

// ---- slider ----------------------------------------------------------------------

fun Context.shadcnSlider(steps: Int, progress: Int, onChange: (Int, Boolean) -> Unit): SeekBar {
    val trackH = dp(6)

    val rail = GradientDrawable().apply {
        cornerRadius = dpf(999f); setColor(T.secondary); setSize(0, trackH)
    }
    val range = ClipDrawable(
        GradientDrawable().apply {
            cornerRadius = dpf(999f); setColor(T.primary); setSize(0, trackH)
        },
        Gravity.START,
        ClipDrawable.HORIZONTAL
    )

    val layers = LayerDrawable(arrayOf<Drawable>(rail, range)).apply {
        setId(0, android.R.id.background)
        setId(1, android.R.id.progress)
        if (Build.VERSION.SDK_INT >= 23) {
            setLayerHeight(0, trackH)
            setLayerHeight(1, trackH)
            setLayerGravity(0, Gravity.CENTER_VERTICAL)
            setLayerGravity(1, Gravity.CENTER_VERTICAL)
        }
    }

    val knob = GradientDrawable().apply {
        shape = GradientDrawable.OVAL
        setColor(T.elevated)
        setStroke(dp(2), T.primary)
        setSize(dp(18), dp(18))
    }

    return SeekBar(this).apply {
        max = steps
        this.progress = progress
        progressDrawable = layers
        thumb = knob
        if (Build.VERSION.SDK_INT >= 21) splitTrack = false
        thumbOffset = 0
        setPadding(dp(9), dp(8), dp(9), dp(8))
        setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, p: Int, fromUser: Boolean) =
                onChange(p, fromUser)

            override fun onStartTrackingTouch(sb: SeekBar?) {}
            override fun onStopTrackingTouch(sb: SeekBar?) {}
        })
        layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
    }
}
