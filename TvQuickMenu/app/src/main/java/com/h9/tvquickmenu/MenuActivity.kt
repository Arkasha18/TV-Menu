package com.h9.tvquickmenu

import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

/**
 * Меню на Activity, а не на «окне поверх» — так на ТВ работает пульт.
 */
class MenuActivity : AppCompatActivity() {
    private lateinit var panelMain: LinearLayout
    private lateinit var panelSleep: LinearLayout
    private lateinit var panelDim: LinearLayout
    private lateinit var sleepChoices: LinearLayout
    private lateinit var dimChoices: LinearLayout
    private lateinit var subtitle: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Сразу убрать то, что успела открыть прошивка (Иви и т.п.).
        // Не вызываем BACK здесь — это делала служба и иногда закрывала нас.
        setContentView(R.layout.overlay_menu)
        bindUi()
        window.decorView.post {
            findViewById<Button>(R.id.btnSleepTimer).requestFocus()
        }
    }

    override fun onResume() {
        super.onResume()
        findViewById<Button>(R.id.btnSleepTimer)?.requestFocus()
    }

    private fun bindUi() {
        panelMain = findViewById(R.id.panelMain)
        panelSleep = findViewById(R.id.panelSleep)
        panelDim = findViewById(R.id.panelDim)
        sleepChoices = findViewById(R.id.sleepChoices)
        dimChoices = findViewById(R.id.dimChoices)
        subtitle = findViewById(R.id.menuSubtitle)

        findViewById<View>(R.id.overlayRoot).setBackgroundColor(0xCC000000.toInt())

        findViewById<Button>(R.id.btnClose).setOnClickListener { finish() }
        findViewById<Button>(R.id.btnSleepBack).setOnClickListener { showMain() }
        findViewById<Button>(R.id.btnDimBack).setOnClickListener { showMain() }

        findViewById<Button>(R.id.btnSleepTimer).setOnClickListener {
            panelMain.visibility = View.GONE
            panelSleep.visibility = View.VISIBLE
            panelDim.visibility = View.GONE
            subtitle.text = "Таймер до сна"
            sleepChoices.removeAllViews()
            AppPrefs.SLEEP_MINUTES.forEach { minutes ->
                val btn = Button(this, null, 0, R.style.TvButton)
                btn.text = formatMinutes(minutes)
                btn.isFocusable = true
                btn.setOnClickListener {
                    SleepScheduler.schedule(this, minutes)
                    Toast.makeText(this, "Таймер: ${formatMinutes(minutes)}", Toast.LENGTH_SHORT).show()
                    finish()
                }
                sleepChoices.addView(btn)
            }
            findViewById<Button>(R.id.btnSleepBack).requestFocus()
        }

        findViewById<Button>(R.id.btnDim).setOnClickListener {
            panelMain.visibility = View.GONE
            panelSleep.visibility = View.GONE
            panelDim.visibility = View.VISIBLE
            subtitle.text = "Ночное затемнение"
            dimChoices.removeAllViews()
            for (level in 0..5) {
                val btn = Button(this, null, 0, R.style.TvButton)
                btn.text = if (level == 0) "Выключить затемнение" else "Уровень $level из 5"
                btn.isFocusable = true
                btn.setOnClickListener {
                    DimOverlayService.setLevel(this, level)
                    // Меню остаётся открытым — закрытие только «Назад».
                }
                dimChoices.addView(btn)
            }
            findViewById<Button>(R.id.btnDimBack).requestFocus()
        }

        findViewById<Button>(R.id.btnSleepNow).setOnClickListener {
            DevicePower.sleepNow(this)
            finish()
        }

        showMain()
    }

    private fun showMain() {
        panelMain.visibility = View.VISIBLE
        panelSleep.visibility = View.GONE
        panelDim.visibility = View.GONE
        subtitle.text = "Выберите действие"
        findViewById<Button>(R.id.btnSleepTimer).requestFocus()
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (panelMain.visibility != View.VISIBLE) {
                showMain()
                return true
            }
            finish()
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    private fun formatMinutes(minutes: Int): String {
        return when {
            minutes < 60 -> "$minutes мин"
            minutes % 60 == 0 -> "${minutes / 60} ч"
            else -> "${minutes / 60} ч ${minutes % 60} мин"
        }
    }

    companion object {
        @Volatile
        var isOpen: Boolean = false
            private set
    }

    override fun onStart() {
        super.onStart()
        isOpen = true
    }

    override fun onStop() {
        isOpen = false
        super.onStop()
    }
}
