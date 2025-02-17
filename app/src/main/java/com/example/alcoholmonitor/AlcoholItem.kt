package com.example.alcoholmonitor

data class AlcoholItem(
    val drinkName: String,
    val brandName: String,
    val type: String,
    val abv: Double,
    val calories: Double,
    val carbohydrates: String,
    val sugars: String,
    val proteins: String,
    val fats: String,
    val servingSize: String,
    val alcoholUnits: Double
) {
    fun getCarbohydratesAsDouble(): Double = carbohydrates.replace("g", "").toDoubleOrNull() ?: 0.0
    fun getProteinsAsDouble(): Double = proteins.replace("g", "").toDoubleOrNull() ?: 0.0
    fun getFatsAsDouble(): Double = fats.replace("g", "").toDoubleOrNull() ?: 0.0

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is AlcoholItem) return false
        return drinkName == other.drinkName && brandName == other.brandName
    }

    override fun hashCode(): Int {
        return drinkName.hashCode() * 31 + brandName.hashCode()
    }
}

