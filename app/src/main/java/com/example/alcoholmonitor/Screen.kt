package com.example.alcoholmonitor

sealed class Screen(val route: String) {
    object Account : Screen("account")
    object AddAlcohol : Screen("add_alcohol")
    object List : Screen("list")
}