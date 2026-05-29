package com.littletutor.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.littletutor.app.ui.LittleTutorApp
import com.littletutor.app.ui.theme.LittleTutorTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LittleTutorTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    LittleTutorApp()
                }
            }
        }
    }
}

