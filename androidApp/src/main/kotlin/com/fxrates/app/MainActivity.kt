package com.fxrates.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.arkivanov.decompose.defaultComponentContext
import com.fxrates.di.createRootComponent
import com.fxrates.root.RootUiContent

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val root = createRootComponent(defaultComponentContext())
        setContent {
            RootUiContent(component = root)
        }
    }

}
