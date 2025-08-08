package com.example.exoplayer.ui.main

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.exoplayer.R

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Load ChannelsFragment
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, ChannelsFragment())
                .commit()
        }
    }
}
