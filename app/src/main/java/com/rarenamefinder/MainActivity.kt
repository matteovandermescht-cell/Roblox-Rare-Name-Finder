package com.rarenamefinder

import android.app.Activity
import android.os.Bundle
import android.graphics.Color
import android.graphics.Typeface
import android.view.Gravity
import android.view.ViewGroup
import android.widget.*
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread
import kotlin.random.Random

class MainActivity : Activity() {
    private val bg = Color.rgb(10, 14, 22)
    private val panel = Color.rgb(20, 27, 40)
    private val accent = Color.rgb(72, 211, 255)
    private lateinit var results: LinearLayout
    private lateinit var status: TextView
    private lateinit var lengthPicker: Spinner
    private lateinit var countPicker: Spinner
    private var busy = false
    private val alphabet = "abcdefghijklmnopqrstuvwxyz0123456789_"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = bg
        window.navigationBarColor = bg
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(20, 26, 20, 16)
            setBackgroundColor(bg)
        }
        val scroller = ScrollView(this)
        val content = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        content.addView(TextView(this).apply {
            text = "RARENAME / FINDER"
            textSize = 12f; letterSpacing = .18f; setTextColor(accent)
            typeface = Typeface.DEFAULT_BOLD
        })
        content.addView(TextView(this).apply {
            text = "Find your next rare username."
            textSize = 26f; setTextColor(Color.WHITE)
            typeface = Typeface.DEFAULT_BOLD
            setPadding(0, 12, 0, 5)
        })
        content.addView(TextView(this).apply {
            text = "Generiert kurze Namen und prüft sie gegen öffentlich erreichbare Roblox-Kontodaten."
            textSize = 14f; setTextColor(Color.LTGRAY)
        })
        val controls = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, 22, 0, 8)
        }
        lengthPicker = spinner(listOf("4 Zeichen", "5 Zeichen", "6 Zeichen", "7 Zeichen", "8 Zeichen"))
        lengthPicker.setSelection(2)
        countPicker = spinner(listOf("10 Namen", "20 Namen", "30 Namen"))
        controls.addView(lengthPicker, LinearLayout.LayoutParams(0, 52, 1f).apply { marginEnd = 8 })
        controls.addView(countPicker, LinearLayout.LayoutParams(0, 52, 1f))
        content.addView(controls)
        content.addView(Button(this).apply {
            text = "NAMEN GENERIEREN & PRÜFEN"
            setTextColor(bg); setBackgroundTintList(android.content.res.ColorStateList.valueOf(accent))
            setOnClickListener { runScan() }
        }, LinearLayout.LayoutParams(-1, 54))
        status = TextView(this).apply {
            text = "Bereit. Ergebnisse werden nicht als garantiert verfügbar markiert."
            textSize = 13f; setTextColor(Color.LTGRAY); setPadding(0, 14, 0, 14)
        }
        content.addView(status)
        results = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        content.addView(results)
        content.addView(TextView(this).apply {
            text = "Hinweis: Roblox kann Anfragen begrenzen. „Nicht gefunden“ bedeutet nicht garantiert verfügbar. Verfügbarkeit vor der Registrierung direkt bei Roblox bestätigen."
            textSize = 12f; setTextColor(Color.GRAY); setPadding(0, 20, 0, 28)
        })
        scroller.addView(content)
        root.addView(scroller, LinearLayout.LayoutParams(-1, 0, 1f))
        setContentView(root)
    }

    private fun spinner(items: List<String>): Spinner {
        val s = Spinner(this)
        s.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, items)
        return s
    }

    private fun runScan() {
        if (busy) return
        busy = true
        results.removeAllViews()
        val len = lengthPicker.selectedItemPosition + 4
        val count = (countPicker.selectedItemPosition + 1) * 10
        status.text = "Erzeuge $count Namen und prüfe sie …"
        thread {
            val candidates = linkedSetOf<String>()
            while (candidates.size < count) {
                val name = buildString { repeat(len) { append(alphabet[Random.nextInt(alphabet.length)]) } }
                if (name.first().isLetterOrDigit() && name.last().isLetterOrDigit() && "__" !in name) candidates.add(name)
            }
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
                    if (code !in 200..299) throw IllegalStateException("Roblox antwortete mit HTTP $code")
                    val arr = JSONObject(response).optJSONArray("data") ?: JSONArray()
                    for (i in 0 until arr.length()) {
                        val username = arr.optJSONObject(i)?.optString("requestedUsername", "")?.lowercase()
                        if (!username.isNullOrBlank()) existing.add(username)
                    }
                    conn.disconnect()
                    runOnUiThread { status.text = "Prüfe Stapel ${index + 1}/${(count + 9) / 10} …" }
                    if (index < (count + 9) / 10 - 1) Thread.sleep(900)
                }
            } catch (e: Exception) {
                error = e.message ?: "Unbekannter Netzwerkfehler"
            }
            val found = candidates.filter { it.lowercase() !in existing }
            runOnUiThread {
                busy = false
                status.text = if (error == null) "${found.size} Namen ohne gefundenen Account von $count geprüft." else "Prüfung teilweise/gar nicht möglich: $error"
                found.forEach { addResult(it) }
                if (found.isEmpty()) results.addView(TextView(this).apply {
                    text = "Keine Kandidaten in diesem Durchlauf. Bitte später erneut versuchen."
                    setTextColor(Color.LTGRAY); setPadding(0, 12, 0, 12)
                })
            }
        }
    }

    private fun addResult(name: String) {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(16, 12, 12, 12)
            setBackgroundColor(panel)
        }
        card.addView(TextView(this).apply {
            text = name
            textSize = 18f; setTextColor(Color.WHITE); typeface = Typeface.MONOSPACE
        }, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        card.addView(Button(this).apply {
            text = "KOPIEREN"
            setTextColor(accent)
            setOnClickListener {
                val clipboard = getSystemService(CLIPBOARD_SERVICE) as android.content.ClipboardManager
                clipboard.setPrimaryClip(android.content.ClipData.newPlainText("Roblox username", name))
                Toast.makeText(this@MainActivity, "Kopiert: $name", Toast.LENGTH_SHORT).show()
            }
        })
        val wrapper = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, 0, 0, 8)
            addView(card)
            addView(TextView(this@MainActivity).apply {
                text = "Kein bestehender Account gefunden · Verfügbarkeit nicht garantiert"
                textSize = 11f; setTextColor(Color.GRAY); setPadding(12, 5, 12, 0)
            })
        }
        results.addView(wrapper)
    }
}
