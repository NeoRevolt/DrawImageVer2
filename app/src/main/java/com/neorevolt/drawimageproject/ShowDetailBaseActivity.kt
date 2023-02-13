package com.neorevolt.drawimageproject

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.neorevolt.drawimageproject.databinding.ActivityMainBinding
import com.neorevolt.drawimageproject.databinding.ActivityShowDetailBaseBinding

class ShowDetailBaseActivity : AppCompatActivity() {
    private lateinit var binding: ActivityShowDetailBaseBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityShowDetailBaseBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val detail = intent.getStringExtra(EXTRA_DETAIL)

        binding.apply {
            tvDetail.text = detail
        }
    }
    companion object{
        const val EXTRA_DETAIL = "extra_detail"
    }

}