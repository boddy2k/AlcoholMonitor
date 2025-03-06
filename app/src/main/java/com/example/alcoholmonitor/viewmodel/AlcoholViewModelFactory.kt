package com.example.alcoholmonitor.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.alcoholmonitor.data.AlcoholRepository

class AlcoholViewModelFactory(
    private val repository: AlcoholRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AlcoholViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AlcoholViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}