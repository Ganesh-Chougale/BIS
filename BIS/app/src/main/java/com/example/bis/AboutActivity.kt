package com.example.bis

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.LinearLayout
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.graphics.Typeface
import android.graphics.Color
import androidx.cardview.widget.CardView

class AboutActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_about)

        // Set up email click listener
        findViewById<LinearLayout>(R.id.emailContainer).setOnClickListener {
            val emailIntent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("mailto:gchougale32@gmail.com")
                putExtra(Intent.EXTRA_SUBJECT, "ScopeR App Feedback")
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

        // Set up Use Cases click listener
        findViewById<CardView>(R.id.useCasesCard).setOnClickListener {
            showUseCasesDialog()
        }
    }

    private fun showUseCasesDialog() {
        val useCasesText = buildUseCasesText()

        AlertDialog.Builder(this, android.R.style.Theme_Material_Dialog)
            .setTitle("Use Cases")
            .setMessage(useCasesText)
            .setPositiveButton("Got it!") { dialog, _ -> dialog.dismiss() }
            .create()
            .apply {
                show()
                // Style the dialog
                window?.setBackgroundDrawableResource(android.R.color.background_dark)
            }
    }

    private fun buildUseCasesText(): SpannableString {
        val text = StringBuilder()

        // Use case 1
        text.append("\uD83C\uDFA8 Designers / UI Developers\n")
        text.append("Zoom into layouts and inspect pixel precision instantly.\n")
        text.append("→ Square or circle frames help check alignment and spacing.\n\n")

        // Use case 2
        text.append("\uD83D\uDDBC️ Digital Artists & Photographers\n")
        text.append("Focus on fine image details for editing or retouching.\n")
        text.append("→ Real-time magnification plus color filters improve clarity.\n\n")

        // Use case 3
        text.append("\uD83C\uDF93 Educators & Students\n")
        text.append("Highlight diagrams, slides, or notes during classes.\n")
        text.append("→ Floating zoom window enhances visibility without breaking flow.\n\n")

        // Use case 4
        text.append("\uD83C\uDFAE Gamers\n")
        text.append("Use fairly & avoid it in competitive esports.\n")
        text.append("Zoom into small in-game areas for better awareness.\n")
        text.append("→ Movable windows let you focus on details without changing resolution.\n\n")

        // Use case 5
        text.append("♿ Accessibility Users\n")
        text.append("Read small text or interface elements easily.\n")
        text.append("→ Adjustable zoom and filters boost readability.\n\n")

        // Use case 6
        text.append("\uD83C\uDFA5 Streamers & Creators\n")
        text.append("Guide viewer focus during screen recordings or live sessions.\n")
        text.append("→ Movable zoom window acts like a live focus lens.\n\n")

        // Use case 7
        text.append("\uD83D\uDCBC Productivity & Office Users\n")
        text.append("Compare or cross-check document sections efficiently.\n")
        text.append("→ Custom input/output areas simplify multitasking.")

        val spannable = SpannableString(text.toString())
        
        // Apply bold styling to titles (lines with emojis)
        val lines = text.toString().split("\n")
        var currentPos = 0
        for (line in lines) {
            if (line.contains("\uD83C") || line.contains("\uD83D") || line.contains("♿")) {
                spannable.setSpan(
                    StyleSpan(Typeface.BOLD),
                    currentPos,
                    currentPos + line.length,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
                spannable.setSpan(
                    ForegroundColorSpan(Color.parseColor("#4CAF50")),
                    currentPos,
                    currentPos + line.length,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }
            currentPos += line.length + 1 // +1 for newline
        }

        return spannable
    }
}