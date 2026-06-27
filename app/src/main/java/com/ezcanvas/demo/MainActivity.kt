package com.ezcanvas.demo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.ezcanvas.demo.ui.EzColors
import com.ezcanvas.demo.ui.EzCanvasTheme

/** Single Activity hosting the whole Compose demo. */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            EzCanvasTheme {
                AppRoot()
            }
        }
    }
}

private enum class Screen { Canvas, Examples }

/**
 * The canvas is the home screen; Examples is reached from the canvas top bar (the hi-fi has no
 * bottom nav — the bottom of the canvas is the color pill).
 */
@Composable
private fun AppRoot() {
    var screen by rememberSaveable { mutableStateOf(Screen.Canvas) }
    var openExampleId by rememberSaveable { mutableStateOf("") }

    Surface(Modifier.fillMaxSize(), color = EzColors.Surface) {
        when (screen) {
            Screen.Canvas -> PlaygroundScreen(onOpenExamples = { screen = Screen.Examples })
            Screen.Examples ->
                if (openExampleId.isEmpty()) {
                    GalleryScreen(
                        onOpenExample = { openExampleId = it },
                        onBack = { screen = Screen.Canvas },
                    )
                } else {
                    ExampleScreen(exampleId = openExampleId, onBack = { openExampleId = "" })
                }
        }
    }
}
