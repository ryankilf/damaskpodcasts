package uk.co.kilfeder.damask.podcasts.ui

import androidx.compose.ui.window.ComposeUIViewController
import platform.UIKit.UIViewController
import uk.co.kilfeder.damask.podcasts.presentation.app.AppGraph
import uk.co.kilfeder.damask.podcasts.presentation.app.create

private val graph by lazy { AppGraph.create() }

@Suppress("FunctionNaming", "FunctionName") // Called from Swift as MainViewControllerKt.MainViewController()
fun MainViewController(): UIViewController = ComposeUIViewController { App(graph) }
