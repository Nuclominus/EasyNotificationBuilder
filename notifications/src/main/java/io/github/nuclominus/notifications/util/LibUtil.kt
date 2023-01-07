package io.github.nuclominus.notifications.util

import android.graphics.Typeface
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.StyleSpan
import android.util.Log

fun SpannableStringBuilder.withBoldSpan(boldPart: String): SpannableStringBuilder {
    try {
        val boldPartStart = indexOf(boldPart)
        setSpan(
            StyleSpan(Typeface.BOLD),
            boldPartStart,
            boldPartStart + boldPart.length,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
    } catch (ex: IndexOutOfBoundsException) {
        Log.e(this::class.java.simpleName, "Text does not match")
    } catch (ex: Exception) {
        Log.e(this::class.java.simpleName, ex.message + ex.localizedMessage)
    }
    return this
}