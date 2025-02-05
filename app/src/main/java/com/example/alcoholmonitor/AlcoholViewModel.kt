package com.example.alcoholmonitor

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class AlcoholViewModel : ViewModel() {

    private val _alcoholList = MutableStateFlow<Map<String, Int>>(emptyMap())
    val alcoholList: StateFlow<Map<String, Int>> = _alcoholList

    private val _totalCalories = MutableStateFlow(0.0)
    val totalCalories: StateFlow<Double> = _totalCalories

    private val _totalFat = MutableStateFlow(0.0)
    val totalFat: StateFlow<Double> = _totalFat

    private val _totalCarbs = MutableStateFlow(0.0)
    val totalCarbs: StateFlow<Double> = _totalCarbs

    private val _totalProtein = MutableStateFlow(0.0)
    val totalProtein: StateFlow<Double> = _totalProtein

    private val _totalSalt = MutableStateFlow(0.0)
    val totalSalt: StateFlow<Double> = _totalSalt

    private val _totalAlcohol = MutableStateFlow(0.0)
    val totalAlcohol: StateFlow<Double> = _totalAlcohol

    // Function to safely convert any invalid number to 0.0
    private fun safeNumber(value: Double?): Double {
        return value?.takeIf { it.isFinite() } ?: 0.0
    }

    fun addAlcohol(alcohol: AlcoholItem) {
        _alcoholList.value = _alcoholList.value.toMutableMap().apply {
            this[alcohol.brand] = (this[alcohol.brand] ?: 0) + 1
        }

        // Ensure that only valid numeric values are added
        _totalCalories.value += safeNumber(alcohol.calories)
        _totalFat.value += safeNumber(alcohol.fat)
        _totalCarbs.value += safeNumber(alcohol.carbohydrates)
        _totalProtein.value += safeNumber(alcohol.protein)
        _totalSalt.value += safeNumber(alcohol.salt)
        _totalAlcohol.value += safeNumber(alcohol.alcoholContent)
    }
}