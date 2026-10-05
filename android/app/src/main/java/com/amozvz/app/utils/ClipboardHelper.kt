package com.amozvz.app.utils

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast

object ClipboardHelper {

    fun copyToClipboard(context: Context, text: String, showToast: Boolean = true) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return
        val clip = ClipData.newPlainText("AmozVz Dictation", text)
        clipboard.setPrimaryClip(clip)

        if (showToast) {
            Toast.makeText(context, "Copied to clipboard!", Toast.LENGTH_SHORT).show()
        }
    }
}
