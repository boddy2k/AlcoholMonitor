package com.example.alcoholmonitor.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.alcoholmonitor.data.AlcoholItem
import com.example.alcoholmonitor.data.AlcoholRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class AlcoholViewModel(
    private val repository: AlcoholRepository
) : ViewModel() {

    private val _alcoholList = MutableStateFlow<Map<AlcoholItem, Int>>(emptyMap())
    val alcoholList: StateFlow<Map<AlcoholItem, Int>> = _alcoholList

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

    init {
        val user = FirebaseAuth.getInstance().currentUser
        user?.let {
            fetchAlcoholIntake(it.uid)
        }
    }

    fun addAlcohol(alcohol: AlcoholItem) {
        val user = FirebaseAuth.getInstance().currentUser
        user?.let { currentUser ->
            _alcoholList.value = _alcoholList.value.toMutableMap().apply {
                this[alcohol] = (this[alcohol] ?: 0) + 1
            }

            updateTotals()

            viewModelScope.launch {
                repository.logAlcoholIntake(currentUser.uid, alcohol, 1)
            }
        }
    }

    fun removeAlcohol(alcohol: AlcoholItem) {
        val user = FirebaseAuth.getInstance().currentUser
        user?.let { currentUser ->
            _alcoholList.value = _alcoholList.value.toMutableMap().apply {
                val newCount = (this[alcohol] ?: 1) - 1
                if (newCount > 0) {
                    this[alcohol] = newCount
                } else {
                    this.remove(alcohol)
                }
            }

            updateTotals()

            viewModelScope.launch {
                repository.logAlcoholIntake(currentUser.uid, alcohol, -1)
            }
        }
    }

    fun fetchAlcoholIntake(userId: String) {
        viewModelScope.launch {
            val data = repository.fetchAlcoholIntake(userId)
            _alcoholList.value = data
            updateTotals()
        }
    }

    private fun updateTotals() {
        var calories = 0.0
        var fat = 0.0
        var carbs = 0.0
        var protein = 0.0
        var alcoholUnits = 0.0

        _alcoholList.value.forEach { (alcohol, count) ->
            calories += alcohol.calories * count
            fat += alcohol.getFatsAsDouble() * count
            carbs += alcohol.getCarbohydratesAsDouble() * count
            protein += alcohol.getProteinsAsDouble() * count
            alcoholUnits += alcohol.alcoholUnits * count
        }

        _totalCalories.value = calories
        _totalFat.value = fat
        _totalCarbs.value = carbs
        _totalProtein.value = protein
        _totalAlcohol.value = alcoholUnits
    }

    fun calculateWarnings(): List<String> {
        val warnings = mutableListOf<String>()

        if (_totalAlcohol.value >= 2.5) {
            warnings.add("🚗 UK motor regulation prohibits you from driving a vehicle once you have consumed 2.5 units of alcohol.")
        }
        if (_totalAlcohol.value >= 7) {
            warnings.add("⚠️ You are halfway to the weekly recommended limit of alcohol consumption. Consider slowing down.")
        }
        if (_totalAlcohol.value >= 14) {
            warnings.add("🚨 NHS guidelines recommend no more than 14 units of alcohol per week. You have reached this limit.")
        }

        return warnings
    }
}
