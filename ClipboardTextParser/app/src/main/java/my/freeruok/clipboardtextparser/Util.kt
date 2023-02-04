package my.freeruok.clipboardtextparser

import android.Manifest
import android.app.Activity
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.text.isDigitsOnly
import java.util.regex.Pattern

object Util {
    private lateinit var vibrator: Vibrator

    public fun init() {
        vibrator = App.context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    }

    public fun vibrant(times: LongArray, strength: IntArray, isRepeat: Boolean = false) {
        if (vibrator.hasVibrator()) {
            val r = if (isRepeat) 0 else -1
            val aa = AudioAttributes.Builder()
                .setContentType(AudioAttributes.USAGE_ALARM)
                .setUsage(AudioAttributes.USAGE_ALARM).build()

            if (Build.VERSION.SDK_INT >= 26) {
                vibrator.vibrate(VibrationEffect.createWaveform(times, strength, r), aa)
            } else {
                vibrator.vibrate(times, r, aa)
            }

        }
    }

    public fun stopVibrant() {
        if (vibrator.hasVibrator()) {
            vibrator.cancel()
        }
    }

    enum class TextType {
        WEB_URL, PHONE_NUMBER, EMAIL_ADDRESS, IP_ADDRESS
    }

    private val patterns = mapOf<TextType, Pattern>(
        TextType.EMAIL_ADDRESS to Pattern.compile("[a-zA-Z0-9_\\.\\+\\-]+@[a-zA-Z0-9_\\.\\+\\-]+"),
        TextType.IP_ADDRESS to Pattern.compile("\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}"),
        TextType.WEB_URL to Pattern.compile("((http|ftp|https):\\/\\/)*[\\w\\-_]+(\\.[\\w\\-_]+)+([\\w\\-\\.,@?^=%&:/~\\+#]*[\\w\\-\\@?^=%&/~\\+#])?"),
        TextType.PHONE_NUMBER to Pattern.compile("\\d{5,14}")
    )

    fun parseText(text: CharSequence): MutableSet<ParseResult> {
        var results = mutableSetOf<ParseResult>()
        var match = patterns[TextType.WEB_URL]!!.matcher(text)
        while (match.regionStart() < match.regionEnd()) {
            var isMatch = false
            for ((key, value) in patterns) {
                match = match.usePattern(value)
                if (match.lookingAt()) {
                    results.add(ParseResult(key, match.group()))
                    match.region(match.end(), match.regionEnd())
                    isMatch = true
                }
            }
            if ((!isMatch) && match.regionStart() < match.regionEnd()) {
                match.region(match.regionStart() + 1, match.regionEnd())
            }
        }
        return results
    }

    fun setClipboardText(content: CharSequence) {
        val s = content.trim()
        if (s.isNotEmpty()) {
            val clip = App.context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clip.text = s
        }
    }

    fun getClipboradText(): CharSequence {
        val clip = App.context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        return clip?.text ?: ""
    }

    public fun callPhone(number: String, activity: Activity) {
        val phoneNumber = number.trim()
        if (!number.isDigitsOnly()) {
            Toast.makeText(App.context, "无效的手机号， 请查证后再试！", Toast.LENGTH_SHORT).show()
            return
        }
        var count = 0
        while (count < 2 && ActivityCompat.checkSelfPermission(
                activity,
                Manifest.permission.CALL_PHONE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requireCallPhonePermission(activity)
            count++
        }
        try {
            val intend = Intent(Intent.ACTION_CALL)
            intend.data = Uri.parse("tel:$phoneNumber")
            activity.startActivity(intend)
        } catch (e: SecurityException) {
            e.printStackTrace()
        }


    }

    public fun openURL(url: String, activity: Activity) {
        val u = url.trim()
        if (u.isNotEmpty()) {
            val intend = Intent(Intent.ACTION_VIEW)
            intend.data = Uri.parse(u)
            activity.startActivity(intend)
        } else {
            toast("打开网址失败， 无效的网址！")
        }
    }

    public fun requireCallPhonePermission(activity: Activity) {
        val intend = Intent(App.context, PermissionActivity::class.java)
        activity.startActivity(intend)
    }

    public fun toast(t: String) {
        Toast.makeText(App.context, t, Toast.LENGTH_SHORT).show()
    }
}
