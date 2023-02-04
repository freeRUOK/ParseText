package my.freeruok.clipboardtextparser

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ListView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import kotlin.concurrent.thread

// 应用程序的UI业务逻辑
class MainActivity : AppCompatActivity() {
    private lateinit var textView: TextView
    private lateinit var listView: ListView
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        setContentView(R.layout.activity_main)

        textView = findViewById<TextView>(R.id.text_view)
        listView = findViewById<ListView>(R.id.result_list_view)
        listView.emptyView = textView

// ListView的项目被点击事件
        listView.setOnItemClickListener { parent, view, position, id ->
            items.elementAt(position).action(this)
        }

        listView.setOnItemLongClickListener { parent, view, position, id ->
            val item = items.elementAt(position)
            if (item.isSave) {
                item.isSave = false
            } else {
                item.isSave = true
            }
            isItemsChange = true
            (view as TextView).text = item.toString()
            true
        }

//         ListView被滚动的时候
        listView.setOnScrollChangeListener { _, _, _, _, _ ->
            Util.vibrant(longArrayOf(0, 10, 10, 10), intArrayOf(0, 180, 0, 180))
        }

// 清空ListView所有项目
        findViewById<Button>(R.id.clear_button).setOnClickListener {
            val itemCount = items.size
            items.removeIf { !it.isSave }
            if (items.size < itemCount) {
                listView.adapter = ArrayAdapter<ParseResult>(
                    this,
                    android.R.layout.simple_expandable_list_item_1,
                    items.toTypedArray()
                )
            }
        }
        Util.init()

        thread {
            loadStorage()
            val msg = Message()
            msg.what = 0
            handler.sendMessage(msg)
        }


    }

    override fun onResume() {
        super.onResume()
//         解析剪贴板文本
        parse()
    }

    override fun onPause() {
        super.onPause()
        thread {
            saveStorage()
        }

    }

    var items: MutableSet<ParseResult> = mutableSetOf()
    private var isItemsChange = false

    // 在UI上更新处理结果
    val handler = object : Handler(Looper.getMainLooper()) {
        override fun handleMessage(msg: Message) {
//             根据解析反馈更新UI
            when (msg.what) {
                0 -> {
                    val arrayAdapter = ArrayAdapter<ParseResult>(
                        this@MainActivity,
                        android.R.layout.simple_expandable_list_item_1,
                        items.toTypedArray()
                    )
                    this@MainActivity.listView.adapter = arrayAdapter
                }
                else -> {
                    this@MainActivity.textView.text = "剪贴板钟没有可用数据"
                }
            }
            Util.stopVibrant()
        }
    }

    //     开启独立线程， 解析剪贴板文本
    fun parse() {
        Util.vibrant(longArrayOf(0, 10, 10), intArrayOf(0, 255, 0), true)
        thread {
            val msg = Message()

            val results = Util.parseText(Util.getClipboradText())
            msg.what = if (results.isEmpty()) {
                -1
            } else {
                results.addAll(items)
                items = results
                0
            }
//             发送给UI线程进一步处理
            handler.sendMessage(msg)
        }
    }

    fun loadStorage() {
        try {
            val inStream = openFileInput("storage.json")
            val reader = BufferedReader(InputStreamReader(inStream))
            reader.use {
                val jsonStr = reader.readText()
                val gson = Gson()
                val setType = object : TypeToken<MutableSet<ParseResult>>() {}.type
                items = gson.fromJson(jsonStr, setType)
            }
        } catch (e: Exception) {
            try {
                openFileOutput("storage.json", MODE_PRIVATE).use { }
                items = mutableSetOf()
            } catch (e1: Exception) {
                e1.printStackTrace()
            }
        }

    }

    fun saveStorage() {
        if (!isItemsChange) {
            return
        }
        val es = items.filter { it.isSave }
        if (es.isNotEmpty()) {
            thread {
                val jsonStr = Gson().toJson(es)
                try {
                    val outStream = openFileOutput("storage.json", MODE_PRIVATE)
                    val writer = BufferedWriter(OutputStreamWriter(outStream))
                    writer.use {
                        writer.write(jsonStr)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }
}
