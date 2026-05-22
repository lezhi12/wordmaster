package com.example.wordmaster

import android.app.Application
import com.example.wordmaster.data.RoomDataRepository
import com.example.wordmaster.data.WordDatabase

class WordMasterApplication : Application() {
    val database by lazy { WordDatabase.getDatabase(this) }
    val repository by lazy { RoomDataRepository(database.wordDao()) }
}
