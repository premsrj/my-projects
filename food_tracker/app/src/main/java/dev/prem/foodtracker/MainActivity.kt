package dev.prem.foodtracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import dev.prem.foodtracker.ui.navigation.FoodTrackerRoot
import dev.prem.foodtracker.ui.theme.FoodTrackerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            FoodTrackerTheme {
                FoodTrackerRoot()
            }
        }
    }
}
