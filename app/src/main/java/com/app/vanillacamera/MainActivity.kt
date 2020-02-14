package com.app.vanillacamera

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProviders
import com.test.vanillacamera.R

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        
        ViewModelProviders.of(this).get(ScannerSharedViewModel::class.java).scanResult.observe(this,
            Observer { 
                DeepLinkParser.detectUrl(it,this){url: String, isUpi: Boolean ->  

                }
            })
        
    }
}
