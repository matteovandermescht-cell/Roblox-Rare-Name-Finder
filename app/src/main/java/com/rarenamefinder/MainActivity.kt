package com.rarenamefinder

import android.app.Activity
import android.os.Bundle
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets
import android.widget.*
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread
import kotlin.random.Random

class MainActivity : Activity() {
    private val bg = Color.rgb(8, 11, 19)
    private val panel = Color.rgb(18, 24, 38)
    private val panelLight = Color.rgb(27, 35, 52)
    private val accent = Color.rgb(72, 211, 255)
    private val muted = Color.rgb(151, 163, 183)
    private lateinit var results: LinearLayout
    private lateinit var status: TextView
    private lateinit var generateButton: TextView
    private lateinit var lengthChoices: LinearLayout
    private lateinit var countChoices: LinearLayout
    private var selectedLength = 4
    private var selectedCount = 20
    private var busy = false
    private val consonants = "bcdfghjkmnprstvwxz"
    private val vowels = "aeiou"
    private val starts = listOf("zy", "ny", "ka", "va", "xe", "lu", "ra", "ki", "no", "mi", "ae", "ev", "ry", "vi")
    private val endings = listOf("x", "z", "n", "r", "o", "y", "ix", "ex", "on", "en", "is", "ax", "io", "yn")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = bg
        window.navigationBarColor = bg
        window.decorView.systemUiVisibility = 0

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(12), dp(20), dp(12))
            setBackgroundColor(bg)
        }
        root.setOnApplyWindowInsetsListener { view, insets ->
            val top = if (android.os.Build.VERSION.SDK_INT >= 30) insets.getInsets(WindowInsets.Type.statusBars()).top else insets.systemWindowInsetTop
            val bottom = if (android.os.Build.VERSION.SDK_INT >= 30) insets.getInsets(WindowInsets.Type.navigationBars()).bottom else insets.systemWindowInsetBottom
            view.setPadding(dp(20), top + dp(12), dp(20), bottom + dp(12))
            insets
        }

        val scroller = ScrollView(this).apply { clipToPadding = false; isFillViewport = true }
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, 0, 0, dp(18))
        }

        val brandRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, 0, 0, dp(18))
        }
        brandRow.addView(TextView(this).apply {
            text = "R"
            textSize = 18f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setTextColor(bg)
            background = rounded(accent, 12)
        }, LinearLayout.LayoutParams(dp(36), dp(36)))
        val brandText = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(10), 0, 0, 0)
            addView(label("RARENAME", 13f, Color.WHITE, true))
            addView(label("USERNAME STUDIO", 10f, accent, true))
        }
        brandRow.addView(brandText, LinearLayout.LayoutParams(0, -2, 1f))
        brandRow.addView(TextView(this).apply {
            text = "●  READY"
            textSize = 10f
            letterSpacing = .06f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.rgb(115, 235, 177))
            background = rounded(Color.rgb(20, 49, 48), 40)
            setPadding(dp(11), dp(8), dp(11), dp(8))
        })
        content.addView(brandRow)

        content.addView(label("YOUR NEXT", 12f, accent, true).apply { letterSpacing = .18f })
        content.addView(TextView(this).apply {
            text = "rare name.\\nBig aura."
            textSize = 35f
            setTextColor(Color.WHITE)
            typeface = Typeface.create("sans-serif-black", Typeface.BOLD)
            setLineSpacing(0f, .96f)
            setPadding(0, dp(5), 0, dp(8))
        })
        content.addView(label("Find a clean 4–6 character Roblox username.", 14f, muted, false).apply {
            setPadding(0, 0, 0, dp(20))
        })

        val settingsCard = cardContainer().apply { setPadding(dp(16), dp(16), dp(16), dp(16)) }
        settingsCard.addView(label("CUSTOMIZE YOUR SEARCH", 11f, muted, true).apply {
            letterSpacing = .1f
            setPadding(0, 0, 0, dp(15))
        })
        settingsCard.addView(label("NAME LENGTH", 10f, muted, true))
        lengthChoices = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, dp(8), 0, dp(16))
        }
        listOf(4, 5, 6).forEach { length ->
            val choice = choiceButton("$length CHARS")
            choice.setOnClickListener { selectedLength = length; refreshChoices() }
            lengthChoices.addView(choice, LinearLayout.LayoutParams(0, dp(42), 1f).apply {
                if (length > 4) marginStart = dp(8)
            })
        }
        settingsCard.addView(lengthChoices)
        settingsCard.addView(label("HOW MANY IDEAS?", 10f, muted, true))
        countChoices = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, dp(8), 0, 0)
        }
        listOf(10, 20, 30).forEach { count ->
            val choice = choiceButton("$count NAMES")
            choice.setOnClickListener { selectedCount = count; refreshChoices() }
            countChoices.addView(choice, LinearLayout.LayoutParams(0, dp(42), 1f).apply {
                if (count > 10) marginStart = dp(8)
            })
        }
        settingsCard.addView(countChoices)
        content.addView(settingsCard, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(14) })

        generateButton = TextView(this).apply {
            text = "✦   FIND RARE USERNAMES"
            textSize = 15f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setTextColor(Color.rgb(5, 17, 27))
            background = GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT,
                intArrayOf(Color.rgb(72, 211, 255), Color.rgb(100, 240, 210))
            ).apply { cornerRadius = dp(16).toFloat() }
            elevation = dp(3).toFloat()
            setOnClickListener { runScan() }
        }
        content.addView(generateButton, LinearLayout.LayoutParams(-1, dp(56)).apply { bottomMargin = dp(20) })

        val resultsHeader = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(2), 0, dp(2), dp(10))
        }
        resultsHeader.addView(label("YOUR NAME DROPS", 13f, Color.WHITE, true), LinearLayout.LayoutParams(0, -2, 1f))
        resultsHeader.addView(label("4–6 CHARACTERS", 10f, accent, true))
        content.addView(resultsHeader)

        status = TextView(this).apply {
            text = "Your next main username could be here.\\nTap the button to generate names."
            textSize = 13f
            setTextColor(muted)
            gravity = Gravity.CENTER
            setPadding(dp(18), dp(22), dp(18), dp(22))
            background = rounded(panel, 16)
        }
        content.addView(status, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(12) })
        results = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        content.addView(results)

        content.addView(label("Availability is not guaranteed. Roblox may limit requests; confirm any name directly with Roblox before registering.", 11f, Color.rgb(111, 123, 144), false).apply {
            setPadding(dp(2), dp(18), dp(2), dp(8))
        })

        scroller.addView(content)
        root.addView(scroller, LinearLayout.LayoutParams(-1, 0, 1f))
        setContentView(root)
        refreshChoices()
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    private fun rounded(color: Int, radius: Int): GradientDrawable =
        GradientDrawable().apply { setColor(color); cornerRadius = dp(radius).toFloat() }

    private fun outlined(color: Int, stroke: Int, radius: Int): GradientDrawable =
        GradientDrawable().apply {
            setColor(color)
            cornerRadius = dp(radius).toFloat()
            setStroke(dp(1), stroke)
        }

    private fun label(textValue: String, size: Float, color: Int, bold: Boolean): TextView =
        TextView(this).apply {
            text = textValue
            textSize = size
            setTextColor(color)
            if (bold) typeface = Typeface.DEFAULT_BOLD
        }

    private fun cardContainer(): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        background = outlined(panel, Color.rgb(38, 49, 68), 20)
    }

    private fun choiceButton(textValue: String): TextView = TextView(this).apply {
        text = textValue
        textSize = 11f
        gravity = Gravity.CENTER
        typeface = Typeface.DEFAULT_BOLD
        setTextColor(muted)
        background = rounded(panelLight, 12)
    }

    private fun refreshChoices() {
        if (!::lengthChoices.isInitialized || !::countChoices.isInitialized) return
        for (i in 0 until lengthChoices.childCount) {
            val view = lengthChoices.getChildAt(i) as TextView
            val active = i + 4 == selectedLength
            view.setTextColor(if (active) bg else muted)
            view.background = rounded(if (active) accent else panelLight, 12)
        }
        for (i in 0 until countChoices.childCount) {
            val view = countChoices.getChildAt(i) as TextView
            val active = (i + 1) * 10 == selectedCount
            view.setTextColor(if (active) bg else muted)
            view.background = rounded(if (active) accent else panelLight, 12)
        }
    }

    private fun makeName(length: Int): String {
        val name = when (Random.nextInt(4)) {
            0 -> (starts.random() + endings.random()).take(length)
            1 -> buildString {
                append(consonants.random())
                append(vowels.random())
                while (this.length < length) append(if (this.length % 2 == 0) consonants.random() else vowels.random())
            }
            2 -> buildString {
                append(consonants.random().toString().uppercase())
                repeat(length - 1) { index ->
                    append(if (index % 2 == 0) vowels.random() else consonants.random())
                }
            }
            else -> {
                val base = listOf("zy", "vex", "nyx", "kai", "ryn", "lux", "zen", "aki", "neo", "rio", "kyo", "xen").random()
                (base + buildString {
                    while (base.length + this.length < length) append((consonants + vowels).random())
                }).take(length)
            }
        }
        return name.padEnd(length, 'x').take(length)
    }

    private fun runScan() {
        if (busy) return
        busy = true
        generateButton.text = "⏳   GENERATING..."
        generateButton.alpha = .75f
        results.removeAllViews()
        val len = selectedLength
        val count = selectedCount
        status.text = "Building your name drop…\\nChecking candidates with Roblox."
        thread {
            val candidates = linkedSetOf<String>()
            while (candidates.size < count) candidates.add(makeName(len))
            val existing = mutableSetOf<String>()
            var error: String? = null
            try {
                candidates.toList().chunked(10).forEachIndexed { index, batch ->
                    val conn = (URL("https://users.roblox.com/v1/usernames/users").openConnection() as HttpURLConnection)
                    conn.requestMethod = "POST"
                    conn.connectTimeout = 10000
                    conn.readTimeout = 10000
                    conn.setRequestProperty("Content-Type", "application/json")
                    conn.setRequestProperty("Accept", "application/json")
                    conn.doOutput = true
                    val body = JSONObject().put("usernames", JSONArray(batch)).put("excludeBannedUsers", true).toString()
                    conn.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
                    val code = conn.responseCode
                    val stream = if (code in 200..299) conn.inputStream else conn.errorStream
                    val response = stream?.bufferedReader()?.use { it.readText() } ?: ""
                    conn.disconnect()
                    if (code !in 200..299) throw IllegalStateException("Roblox returned HTTP $code")
                    val arr = JSONObject(response).optJSONArray("data") ?: JSONArray()
                    for (i in 0 until arr.length()) {
                        val username = arr.optJSONObject(i)?.optString("requestedUsername", "")?.lowercase()
                        if (!username.isNullOrBlank()) existing.add(username)
                    }
                    runOnUiThread { status.text = "Checking batch \${index + 1}/\${(count + 9) / 10}…" }
                    if (index < (count + 9) / 10 - 1) Thread.sleep(900)
                }
            } catch (e: Exception) {
                error = e.message ?: "Network error"
            }
            val found = candidates.filter { it.lowercase() !in existing }
            runOnUiThread {
                busy = false
                generateButton.text = "✦   FIND RARE USERNAMES"
                generateButton.alpha = 1f
                status.text = if (error == null) {
                    "✦  \${found.size} candidates found\\nNo matching account returned — availability isn't guaranteed."
                } else {
                    "Check incomplete: $error\\nSome results may be missing."
                }
                found.forEachIndexed { index, name -> addResult(name, index + 1) }
                if (found.isEmpty()) status.text = "No candidates this time.\\nTry again in a moment."
            }
        }
    }

    private fun addResult(name: String, index: Int) {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(15), dp(13), dp(10), dp(13))
            background = outlined(panel, Color.rgb(38, 49, 68), 16)
        }
        card.addView(TextView(this).apply {
            text = index.toString().padStart(2, '0')
            textSize = 10f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(accent)
            gravity = Gravity.CENTER
            background = rounded(Color.rgb(25, 44, 59), 9)
        }, LinearLayout.LayoutParams(dp(32), dp(32)).apply { marginEnd = dp(12) })
        card.addView(TextView(this).apply {
            text = name
            textSize = 22f
            letterSpacing = .07f
            setTextColor(Color.WHITE)
            typeface = Typeface.create("sans-serif-black", Typeface.BOLD)
        }, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        card.addView(TextView(this).apply {
            text = "COPY"
            textSize = 10f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setTextColor(accent)
            background = outlined(Color.rgb(20, 35, 49), Color.rgb(42, 82, 104), 10)
            setPadding(dp(12), dp(10), dp(12), dp(10))
            setOnClickListener {
                val clipboard = getSystemService(CLIPBOARD_SERVICE) as android.content.ClipboardManager
                clipboard.setPrimaryClip(android.content.ClipData.newPlainText("Roblox username", name))
                Toast.makeText(this@MainActivity, "Copied: $name", Toast.LENGTH_SHORT).show()
            }
        })
        results.addView(card, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(9) })
    }
}
