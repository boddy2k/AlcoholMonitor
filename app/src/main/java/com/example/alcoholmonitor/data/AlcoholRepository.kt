package com.example.alcoholmonitor.data

import android.content.Context
import android.os.Environment
import android.util.Log
import com.example.alcoholmonitor.AlcoholItem
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class AlcoholRepository {

    private val db = Firebase.firestore

    suspend fun fetchAlcoholIntake(userId: String): Map<AlcoholItem, Int> {
        val weekId = SimpleDateFormat("yyyy-'W'ww", Locale.getDefault()).format(Calendar.getInstance().time)
        val docRef = db.collection("users").document(userId)
            .collection("alcohol_intake").document(weekId)

        return try {
            val document = docRef.get().await()
            if (document.exists()) {
                document.data?.mapNotNull { (drinkName, drinkData) ->
                    val dataMap = drinkData as? Map<String, Any> ?: return@mapNotNull null
                    val count = (dataMap["count"] as? Long)?.toInt() ?: 0
                    val alcoholItem = AlcoholItem(
                        drinkName = drinkName,
                        brandName = "Unknown",
                        type = "Unknown",
                        abv = 0.0,
                        calories = 0.0,
                        carbohydrates = "0g",
                        sugars = "0g",
                        proteins = "0g",
                        fats = "0g",
                        servingSize = "N/A",
                        alcoholUnits = (dataMap["unitsPerDrink"] as? Double) ?: 0.0
                    )
                    alcoholItem to count
                }?.toMap() ?: emptyMap()
            } else {
                emptyMap()
            }
        } catch (e: Exception) {
            Log.e("Firestore", "❌ Error fetching weekly alcohol intake", e)
            emptyMap()
        }
    }

    suspend fun logAlcoholIntake(userId: String, alcohol: AlcoholItem, count: Int) {
        val weekId = SimpleDateFormat("yyyy-'W'ww", Locale.getDefault()).format(Calendar.getInstance().time)
        val docRef = db.collection("users").document(userId)
            .collection("alcohol_intake").document(weekId)

        db.runTransaction { transaction ->
            val snapshot = transaction.get(docRef)
            val currentData = snapshot.data?.toMutableMap() ?: mutableMapOf()

            val drinkData = currentData[alcohol.drinkName] as? Map<*, *> ?: mapOf("count" to 0L, "units" to 0.0)
            val currentCount = (drinkData["count"] as? Long) ?: 0L
            val newCount = (currentCount + count).coerceAtLeast(0)

            if (newCount > 0) {
                currentData[alcohol.drinkName] = mapOf(
                    "count" to newCount,
                    "unitsPerDrink" to alcohol.alcoholUnits
                )
            } else {
                currentData.remove(alcohol.drinkName)
                transaction.update(docRef, mapOf(alcohol.drinkName to FieldValue.delete()))
            }

            transaction.set(docRef, currentData, SetOptions.merge())
        }.await()
    }

    fun searchAlcoholBrands(query: String, onResult: (List<AlcoholItem>) -> Unit) {
        if (query.isEmpty()) {
            onResult(emptyList())
            return
        }

        db.collection("alcohol_data")
            .get()
            .addOnSuccessListener { documents ->
                val filteredResults = documents.mapNotNull { doc ->
                    val name = doc.getString("Drink Name") ?: ""
                    if (name.startsWith(query, ignoreCase = true)) {
                        AlcoholItem(
                            drinkName = name,
                            brandName = doc.getString("Brand Name") ?: "",
                            type = doc.getString("Type") ?: "",
                            abv = doc.getDouble("ABV") ?: 0.0,
                            calories = doc.getDouble("Calories") ?: 0.0,
                            carbohydrates = doc.getString("Carbohydrates") ?: "0g",
                            sugars = doc.getString("Sugars") ?: "0g",
                            proteins = doc.getString("Proteins") ?: "0g",
                            fats = doc.getString("Fats") ?: "0g",
                            servingSize = doc.getString("Serving Size") ?: "Unknown",
                            alcoholUnits = doc.getDouble("UK Alcohol Units") ?: 0.0
                        )
                    } else null
                }
                onResult(filteredResults)
            }
            .addOnFailureListener {
                Log.e("Firestore", "❌ Error fetching alcohol data", it)
                onResult(emptyList())
            }
    }


    fun exportWeeklyDataToCSV(context: Context, dataList: Map<AlcoholItem, Int>): File? {
        val weekId = SimpleDateFormat("yyyy-'W'ww", Locale.getDefault()).format(Calendar.getInstance().time)
        val fileName = "alcohol_weekly_${weekId}.csv"
        val directory = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS) ?: return null
        val file = File(directory, fileName)

        return try {
            FileWriter(file).use { writer ->
                writer.append("Drink Name,Total Count,Alcohol Units,Week ID\n")
                dataList.forEach { (alcoholItem, count) ->
                    writer.append("${alcoholItem.drinkName},$count,${alcoholItem.alcoholUnits * count},$weekId\n")
                }
            }
            file
        } catch (e: IOException) {
            Log.e("Kaggle", "❌ Error creating CSV file", e)
            null
        }
    }

    fun fetchWeeklyAlcoholData(userId: String, onComplete: (Map<AlcoholItem, Int>) -> Unit) {
        val weekId = SimpleDateFormat("yyyy-'W'ww", Locale.getDefault()).format(Calendar.getInstance().time)

        val docRef = db.collection("users").document(userId)
            .collection("alcohol_intake").document(weekId)

        docRef.get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val data = document.data?.mapValues { entry ->
                        entry.value as? Map<String, Any> ?: emptyMap()
                    } ?: emptyMap()

                    val alcoholData = mutableMapOf<AlcoholItem, Int>()
                    data.forEach { (drinkName, drinkData) ->
                        val count = (drinkData["count"] as? Long)?.toInt() ?: 0
                        val units = (drinkData["unitsPerDrink"] as? Double) ?: 0.0

                        if (count > 0) {
                            alcoholData[AlcoholItem(
                                drinkName = drinkName,
                                brandName = "Unknown",
                                type = "Unknown",
                                abv = 0.0,
                                calories = 0.0,
                                carbohydrates = "0g",
                                sugars = "0g",
                                proteins = "0g",
                                fats = "0g",
                                servingSize = "N/A",
                                alcoholUnits = units
                            )] = count
                        }
                    }

                    onComplete(alcoholData)
                } else {
                    Log.d("Firestore", "No alcohol intake data found for this week.")
                    onComplete(emptyMap())
                }
            }
            .addOnFailureListener { exception ->
                Log.e("Firestore", "Error fetching weekly alcohol intake", exception)
            }
    }

    fun sendCsvToFlaskServer(context: Context, userId: String) {
        fetchWeeklyAlcoholData(userId) { weeklyData ->
            if (weeklyData.isNotEmpty()) {
                val csvFile = exportWeeklyDataToCSV(context, weeklyData)
                if (csvFile != null) {
                    val flaskUrl = "http://10.0.2.2:5000/upload"

                    val requestBody = MultipartBody.Builder()
                        .setType(MultipartBody.FORM)
                        .addFormDataPart("file", csvFile.name, csvFile.asRequestBody("text/csv".toMediaTypeOrNull()))
                        .build()

                    val request = Request.Builder()
                        .url(flaskUrl)
                        .post(requestBody)
                        .build()

                    val client = OkHttpClient()

                    client.newCall(request).enqueue(object : Callback {
                        override fun onFailure(call: okhttp3.Call, e: IOException) {
                            Log.e("FlaskUpload", "❌ Upload failed", e)
                        }

                        override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                            if (response.isSuccessful) {
                                Log.d("FlaskUpload", "✅ Upload successful!")
                            } else {
                                Log.e("FlaskUpload", "❌ Upload failed: ${response.code} - ${response.message}")
                            }
                        }
                    })
                } else {
                    Log.e("FlaskUpload", "❌ Failed to create CSV file.")
                }
            } else {
                Log.d("FlaskUpload", "⚠ No weekly data available to upload.")
            }
        }
    }

}