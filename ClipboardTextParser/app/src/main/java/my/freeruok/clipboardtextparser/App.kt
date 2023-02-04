package my.freeruok.clipboardtextparser

import android.app.Application
import android.content.Context

// 应用程序的上下文环境
class App : Application() {
    companion object {
        lateinit var context: Context
    }

    override fun onCreate() {
        super.onCreate()
        context = applicationContext
    }
}
