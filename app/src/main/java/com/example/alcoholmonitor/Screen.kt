package com.example.alcoholmonitor

sealed class Screen(val route: String) {
    object SignIn : Screen("sign_in")
    object Account : Screen("account")
    object AddAlcohol : Screen("add_alcohol")
    object List : Screen("list")
}