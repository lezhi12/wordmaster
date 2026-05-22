package com.example.wordmaster.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "words",
    indices = [Index(value = ["nextReviewTime"])]
)
data class Word(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val word: String,
    val definition: String,
    val example: String = "",
    val level: Int = 0,
    val nextReviewTime: Long = System.currentTimeMillis(),
    val createdAt: Long = System.currentTimeMillis(),
    val lastReviewedAt: Long? = null
)
