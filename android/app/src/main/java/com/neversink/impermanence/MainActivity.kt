package com.neversink.impermanence

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.neversink.impermanence.ui.ImpermanenceApp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val appContainer = (application as ImpermanenceApplication).container
        setContent {
            ImpermanenceApp(appContainer = appContainer)
        }
    }
}
