package com.pradarshana.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.pradarshana.core.data.PradarshanaContainer
import com.pradarshana.core.data.PradarshanaContainerOwner

@Composable
inline fun <reified VM : ViewModel> appViewModel(crossinline create: (PradarshanaContainer) -> VM): VM {
    val container = (LocalContext.current.applicationContext as PradarshanaContainerOwner).container
    return viewModel(factory = viewModelFactory { initializer { create(container) } })
}
