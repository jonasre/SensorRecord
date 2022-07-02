package no.uio.ifi.jonaspr.sensorrecord.ui

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.Html
import android.webkit.WebView
import android.widget.TextView
import no.uio.ifi.jonaspr.sensorrecord.R

class HelpActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_help)
        // Load documentation html
        findViewById<WebView>(R.id.webview).loadDataWithBaseURL(
            null,
            getString(R.string.help_text),
            "text/html", "utf-8",
            null)
    }
}