package com.neorevolt.drawimage

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import com.google.android.play.core.splitcompat.SplitCompat
import com.neorevolt.drawimageproject.ShowDetailBaseActivity

var reflectData: String? = null

class ShowDataActivity : AppCompatActivity() {

    private lateinit var btnToBase: Button
    private lateinit var tvResult: TextView


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_show_data)

        btnToBase = findViewById(R.id.btnToBase)
        tvResult = findViewById(R.id.tvResultFromBase)

        btnToBase.setOnClickListener {
            val message = "Ini pesan dari Feature"
            val intent = Intent(this, ShowDetailBaseActivity::class.java)
            intent.putExtra(EXTRA_DETAIL, message)
            startActivity(intent)
            finish()
        }

        Log.d(null, "PESAN YANG DIHARAPKAN : $reflectData")
        tvResult.text = reflectData

    }

    fun setReflectionData(refData: String) {
        reflectData = refData
        Log.d(null, "Pesan dari Base adalah : $reflectData")
    }

    fun loadData(): String {
        return reflectData ?: "null"
    }

    override fun attachBaseContext(newBase: Context?) {
        super.attachBaseContext(newBase)
        SplitCompat.install(this)
    }

    companion object {
        const val EXTRA_DETAIL = "extra_detail"
    }
}