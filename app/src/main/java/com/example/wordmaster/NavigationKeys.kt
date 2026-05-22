package com.example.wordmaster

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable data object Home : NavKey
@Serializable data object AddWord : NavKey
@Serializable data object ImportWord : NavKey
@Serializable data object ReviewWords : NavKey

