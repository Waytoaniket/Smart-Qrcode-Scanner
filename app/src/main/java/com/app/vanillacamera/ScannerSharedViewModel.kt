package com.app.vanillacamera


import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class ScannerSharedViewModel : ViewModel() {
    var scanResult: MutableLiveData<String> = MutableLiveData()
}