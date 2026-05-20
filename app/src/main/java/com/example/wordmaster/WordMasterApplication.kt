package com.example.wordmaster

import android.app.Application
import com.example.wordmaster.data.DefaultDataRepository

class WordMasterApplication : Application() {
    val repository by lazy { DefaultDataRepository() }
}
