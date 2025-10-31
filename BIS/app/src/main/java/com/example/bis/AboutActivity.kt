package com.example.bis

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity

class AboutActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_about)

        // Set up email click listener
        findViewById<LinearLayout>(R.id.emailContainer).setOnClickListener {
            val emailIntent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("mailto:gchougale32@gmail.com")
                putExtra(Intent.EXTRA_SUBJECT, "ScopeX App Feedback")
            }
            try {
                startActivity(Intent.createChooser(emailIntent, "Send email via"))
            } catch (e: Exception) {
                // Handle case where no email app is installed
            }
        }

        // Set up website click listener
        findViewById<LinearLayout>(R.id.websiteContainer).setOnClickListener {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://ganesh-chougale.github.io/"))
            try {
                startActivity(intent)
            } catch (e: Exception) {
                // Handle case where no browser is installed
            }
        }
    }
}