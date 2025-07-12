package com.example.kotlin_test1

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.navigation.compose.rememberNavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.kotlin_test1.LoginScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val navController = rememberNavController()

            Surface(color = MaterialTheme.colorScheme.background) {
                NavHost(navController = navController, startDestination = "login") {
                    composable("login") {
                        LoginScreen(navController)
                    }
                    composable("bag") {
                        BagScreen(navController)
                    }
                }
            }
        }
    }
}
