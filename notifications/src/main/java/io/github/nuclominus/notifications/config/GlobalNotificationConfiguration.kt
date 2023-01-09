package io.github.nuclominus.notifications.config

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.core.app.NotificationManagerCompat

// Default vibrate pattern
private val VIBRATE_PATTERN = longArrayOf(0, 300)

/**
 *  Configuration model for each type of notification
 *  @property context [Context]
 *  @property vibrationPattern [LongArray] notification vibration pattern (optional)
 *  @property smallIcon [Int] notification small icon (optional)
 *  @property color [Int] notification led color (optional)
 *  @property defaultIcon [Int] notification default icon (optional)
 */
class GlobalNotificationConfiguration private constructor(
    private val context: Context,
    private val vibrationPattern: LongArray? = null,
    @DrawableRes private val smallIcon: Int? = null,
    @ColorInt private val color: Int? = null,
    @DrawableRes private val defaultIcon: Int? = null,
) {

    fun getContext(): Context = context

    fun getSmallIcon(): Int? = smallIcon

    fun getNotificationColor(): Int? = color

    fun getNotificationDefaultIcon(): Bitmap? {
        return defaultIcon?.let {
            BitmapFactory.decodeResource(context.resources, it)
        }
    }

    fun withNotificationManager(action: NotificationManagerCompat.() -> Unit) {
        with(NotificationManagerCompat.from(context)) {
            action.invoke(this)
        }
    }

    fun getVibratePattern() = vibrationPattern ?: VIBRATE_PATTERN

    /**
     * GlobalNotificationConfiguration public builder class
     */
    data class Builder(
        @DrawableRes
        private var smallIcon: Int? = null,

        @ColorInt
        private var color: Int? = null,

        @DrawableRes
        private var defaultIcon: Int? = null,

        @StringRes
        private var replayLabel: Int? = null,

        @DrawableRes
        private var replayActionIcon: Int? = null,

        private var vibrationPattern: LongArray? = null,
    ) {

        fun smallIcon(@DrawableRes smallIcon: Int) = apply { this.smallIcon = smallIcon }
        fun color(@ColorInt color: Int) = apply { this.color = color }
        fun defaultIcon(@DrawableRes defaultIcon: Int) = apply { this.defaultIcon = defaultIcon }
        fun vibrationPattern(vibrationPattern: LongArray) = apply { this.vibrationPattern = vibrationPattern }

        fun build(context: Context) = GlobalNotificationConfiguration(
            context = context,
            smallIcon = smallIcon,
            color = color,
            defaultIcon = defaultIcon,
        )

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as Builder

            if (smallIcon != other.smallIcon) return false
            if (color != other.color) return false
            if (defaultIcon != other.defaultIcon) return false
            if (replayLabel != other.replayLabel) return false
            if (replayActionIcon != other.replayActionIcon) return false
            if (vibrationPattern != null) {
                if (other.vibrationPattern == null) return false
                if (!vibrationPattern.contentEquals(other.vibrationPattern)) return false
            } else if (other.vibrationPattern != null) return false

            return true
        }

        override fun hashCode(): Int {
            var result = smallIcon ?: 0
            result = 31 * result + (color ?: 0)
            result = 31 * result + (defaultIcon ?: 0)
            result = 31 * result + (replayLabel ?: 0)
            result = 31 * result + (replayActionIcon ?: 0)
            result = 31 * result + (vibrationPattern?.contentHashCode() ?: 0)
            return result
        }
    }
}