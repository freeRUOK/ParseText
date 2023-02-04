package my.freeruok.clipboardtextparser

import android.app.Activity

data class ParseResult(val textType: Util.TextType, val content: String, var isSave: Boolean = false) {
    override fun toString(): String {
        val firstText = when (textType) {
            Util.TextType.EMAIL_ADDRESS -> "邮箱地址： $content - 拷贝"
            Util.TextType.IP_ADDRESS -> "IP地址： $content - 拷贝"
            Util.TextType.PHONE_NUMBER -> "电话号： $content - 拨打"
            Util.TextType.WEB_URL -> "网址： $content - 打开"
        }
        val lastText = if (isSave) {
            " 长按取消收藏"
        } else {
            " 长按收藏"
        }
        return "${firstText} ${lastText}"
    }

    fun action(activity: Activity) {
        when (textType) {
            Util.TextType.WEB_URL -> Util.openURL(content, activity)
            Util.TextType.PHONE_NUMBER -> Util.callPhone(content, activity)
            Util.TextType.EMAIL_ADDRESS -> Util.setClipboardText(content)
            Util.TextType.IP_ADDRESS -> Util.setClipboardText(content)
        }
    }
}
