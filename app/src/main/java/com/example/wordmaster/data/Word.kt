package com.example.wordmaster.data

data class Word(
    val id: Long = 0,
    val word: String,
    val definition: String,
    val example: String = "",
    val level: Int = 0,
    val nextReviewTime: Long = System.currentTimeMillis(),
    val createdAt: Long = System.currentTimeMillis(),
    val lastReviewedAt: Long? = null
)
