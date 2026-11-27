package uk.co.kilfeder.damask.podcasts.ui

import androidx.compose.ui.window.ComposeUIViewController
import platform.UIKit.UIViewController

@Suppress("FunctionNaming", "FunctionName") // Called from Swift as MainViewControllerKt.MainViewController()
fun MainViewController(): UIViewController = ComposeUIViewController { App() }
