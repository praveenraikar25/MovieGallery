package com.raikar.moviegallery.ui.bridge

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.viewmodel.CreationExtras
import kotlin.reflect.KClass

/**
 * Owns one ViewModel for a SwiftUI screen, standing in for the navigation entry that owns it on
 * Android. [clear] runs the ViewModel's `onCleared` and cancels its `viewModelScope`; Swift calls
 * it from the owning object's `deinit`.
 *
 * [create] is a plain lambda so this class doesn't depend on Koin. Phase 5 passes a Koin lookup,
 * for example `ViewModelOwner { koin.get<MovieDetailViewModel> { parametersOf(movieId) } }`.
 */
class ViewModelOwner<VM : ViewModel>(
    private val create: () -> VM,
) {
    private val store = ViewModelStore()

    /** Created on first read, then the same instance until [clear]. */
    val viewModel: VM by lazy {
        // A hand-written factory: viewModelFactory { initializer { } } needs a reified type,
        // which a generic class doesn't have.
        val factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(
                    modelClass: KClass<T>,
                    extras: CreationExtras,
                ): T = this@ViewModelOwner.create() as T
            }
        @Suppress("UNCHECKED_CAST")
        ViewModelProvider.create(store, factory).get(KEY, ViewModel::class) as VM
    }

    fun clear() = store.clear()

    private companion object {
        // One ViewModel per owner, so a fixed key; the class is erased in the factory anyway.
        const val KEY = "com.raikar.moviegallery.ui.bridge.ViewModelOwner"
    }
}
