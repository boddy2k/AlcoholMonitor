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

    fun addAlcohol(alcohol: AlcoholItem) {
        val updatedList = _alcoholList.value.toMutableMap()
        updatedList[alcohol.brand] = (updatedList[alcohol.brand] ?: 0) + 1
        _alcoholList.value = updatedList.toMap()

        // Persist macro totals
        _totalCalories.value += alcohol.calories
        _totalFat.value += alcohol.fat
        _totalCarbs.value += alcohol.carbohydrates
        _totalProtein.value += alcohol.protein
        _totalSalt.value += alcohol.salt
        _totalAlcohol.value += alcohol.alcoholContent
    }
}