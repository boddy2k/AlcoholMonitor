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

    private val _totalAlcohol = MutableStateFlow(0.0)
    val totalAlcohol: StateFlow<Double> = _totalAlcohol

    fun addAlcohol(alcohol: AlcoholItem) {
        _alcoholList.value = _alcoholList.value.toMutableMap().apply {
            this[alcohol.brandName] = (this[alcohol.brandName] ?: 0) + 1
        }

        _totalCalories.value += alcohol.calories
        _totalFat.value += alcohol.fats.replace("g", "").toDoubleOrNull() ?: 0.0
        _totalCarbs.value += alcohol.getCarbohydratesAsDouble()  // FIXED
        _totalProtein.value += alcohol.proteins.replace("g", "").toDoubleOrNull() ?: 0.0
        _totalAlcohol.value += alcohol.alcoholUnits
    }

    fun removeAlcohol(alcohol: AlcoholItem) {
        _alcoholList.value = _alcoholList.value.toMutableMap().apply {
            val currentCount = this[alcohol.brandName] ?: 0
            if (currentCount > 1) {
                this[alcohol.brandName] = currentCount - 1
            } else {
                this.remove(alcohol.brandName)
            }
        }

        // Subtract the macros from totals
        _totalCalories.value -= alcohol.calories
        _totalFat.value -= alcohol.fats.toDoubleOrNull() ?: 0.0
        _totalCarbs.value -= alcohol.carbohydrates.toDoubleOrNull() ?: 0.0
        _totalProtein.value -= alcohol.proteins.toDoubleOrNull() ?: 0.0
        _totalAlcohol.value -= alcohol.alcoholUnits

        // Prevent negative values
        if (_totalCalories.value < 0) _totalCalories.value = 0.0
        if (_totalFat.value < 0) _totalFat.value = 0.0
        if (_totalCarbs.value < 0) _totalCarbs.value = 0.0
        if (_totalProtein.value < 0) _totalProtein.value = 0.0
        if (_totalAlcohol.value < 0) _totalAlcohol.value = 0.0
    }

    fun getAlcoholItem(drinkName: String): AlcoholItem {
        return AlcoholItem(
            drinkName = drinkName,
            brandName = "", // We don’t need brand here
            type = "",
            abv = 0.0,
            calories = 0.0,
            carbohydrates = "0",
            sugars = "0",
            proteins = "0",
            fats = "0",
            servingSize = "",
            alcoholUnits = 0.0
        )
    }
}