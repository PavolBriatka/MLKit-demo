package com.example.redmlkitdemo

import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import com.example.redmlkitdemo.scanner.ScannerActivity
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        scanner_button.setOnClickListener {
            val scannerIntent = Intent(this, ScannerActivity::class.java)
            startActivity(scannerIntent)
        }
    }
}
