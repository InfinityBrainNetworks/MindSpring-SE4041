package com.mindspring.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.mindspring.app.AppContainer
import com.mindspring.app.MindSpringApp

/**
 * Creates a ViewModel scoped to the current navigation entry, handing it the app's repositories.
 * Keeps screens free of any knowledge of which repository implementation is running.
 */
@Composable
inline fun <reified VM : ViewModel> appViewModel(
    key: String? = null,
    crossinline create: (AppContainer) -> VM,
): VM {
    val container = (LocalContext.current.applicationContext as MindSpringApp).container
    return viewModel(key = key, factory = viewModelFactory { initializer { create(container) } })
}
