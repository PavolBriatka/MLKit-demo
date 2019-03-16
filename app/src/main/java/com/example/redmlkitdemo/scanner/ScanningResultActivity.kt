package com.example.redmlkitdemo.scanner

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import com.example.redmlkitdemo.R
import kotlinx.android.synthetic.main.activity_scanning_result.*

class ScanningResultActivity : AppCompatActivity() {

    companion object {
        private const val RESULT_TAG = "scanning_result"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_scanning_result)

        scanning_result.text = intent.getStringExtra(RESULT_TAG)
    }
}
