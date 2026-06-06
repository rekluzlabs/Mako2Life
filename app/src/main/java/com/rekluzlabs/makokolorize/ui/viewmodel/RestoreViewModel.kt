package com.rekluzlabs.makokolorize.ui.viewmodel

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.rekluzlabs.makokolorize.data.image.ImageRepository
import com.rekluzlabs.makokolorize.domain.ColorizeUseCase
import com.rekluzlabs.makokolorize.domain.RunConfig
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class RestoreUiState(
    val imageUri: Uri,
    val processedImageUri: Uri? = null,
    val isProcessing: Boolean = false,
    val progress: Float = 0f,
    val config: RunConfig = RunConfig().copy(colorizeEnabled = false),
    val error: String? = null,
    val showAdvancedSheet: Boolean = false
)

sealed interface RestoreUiEvent {
    data object OneButtonFullRestore : RestoreUiEvent
    data object ToggleDenoise : RestoreUiEvent
    data object ToggleColorize : RestoreUiEvent
    data object ToggleFaceRestore : RestoreUiEvent
    data object ToggleUpscale : RestoreUiEvent
    data object ClickRestore : RestoreUiEvent
    data object CancelProcessing : RestoreUiEvent
    data object ShowAdvancedSheet : RestoreUiEvent
    data object DismissAdvancedSheet : RestoreUiEvent
    data class ApplyAdvancedConfig(val config: RunConfig) : RestoreUiEvent
}

sealed interface RestoreEffect {
    data class NavigateToResult(val resultUri: Uri) : RestoreEffect
}

class RestoreViewModel(
    private val imageRepository: ImageRepository,
    private val colorizeUseCase: ColorizeUseCase,
    imageUri: Uri
) : ViewModel() {

    private val _state = MutableStateFlow(RestoreUiState(imageUri = imageUri))
    val state: StateFlow<RestoreUiState> = _state.asStateFlow()

    private val _effects = Channel<RestoreEffect>(Channel.BUFFERED)
    val effects: Flow<RestoreEffect> = _effects.receiveAsFlow()

    private var processingJob: Job? = null

    fun onEvent(event: RestoreUiEvent) {
        when (event) {
            RestoreUiEvent.OneButtonFullRestore -> {
                _state.update { it.copy(config = RunConfig().withFullRestore()) }
            }
            RestoreUiEvent.ToggleDenoise -> {
                _state.update {
                    it.copy(config = it.config.copy(denoisingEnabled = !it.config.denoisingEnabled))
                }
            }
            RestoreUiEvent.ToggleColorize -> {
                _state.update {
                    it.copy(config = it.config.copy(colorizeEnabled = !it.config.colorizeEnabled))
                }
            }
            RestoreUiEvent.ToggleUpscale -> {
                _state.update {
                    it.copy(config = it.config.copy(upscalingEnabled = !it.config.upscalingEnabled))
                }
            }
            RestoreUiEvent.ToggleFaceRestore -> {
                _state.update {
                    it.copy(config = it.config.copy(faceRestoreEnabled = !it.config.faceRestoreEnabled))
                }
            }
            RestoreUiEvent.ClickRestore -> startProcessing()
            RestoreUiEvent.CancelProcessing -> cancelProcessing()
            RestoreUiEvent.ShowAdvancedSheet -> {
                _state.update { it.copy(showAdvancedSheet = true) }
            }
            RestoreUiEvent.DismissAdvancedSheet -> {
                _state.update { it.copy(showAdvancedSheet = false) }
            }
            is RestoreUiEvent.ApplyAdvancedConfig -> {
                _state.update {
                    it.copy(config = event.config, showAdvancedSheet = false)
                }
            }
        }
    }

    private fun startProcessing() {
        val imageUri = _state.value.imageUri
        val config = _state.value.config

        processingJob = viewModelScope.launch {
            _state.update { it.copy(isProcessing = true, progress = 0f, error = null) }

            imageRepository.loadBitmap(imageUri)
                .onSuccess { bitmap ->
                    try {
                        colorizeUseCase.execute(
                            bitmap,
                            { progress -> _state.update { it.copy(progress = progress) } },
                            config
                        ).onSuccess { colorizedBitmap ->
                            imageRepository.saveProcessedBitmap(colorizedBitmap)
                                .onSuccess { uri ->
                                    _state.update {
                                        it.copy(
                                            processedImageUri = uri,
                                            isProcessing = false,
                                            progress = 1f
                                        )
                                    }
                                    _effects.send(RestoreEffect.NavigateToResult(uri))
                                }
                                .onFailure { e ->
                                    _state.update {
                                        it.copy(
                                            isProcessing = false,
                                            progress = 0f,
                                            error = "Failed to save result: ${e.localizedMessage}"
                                        )
                                    }
                                }
                        }.onFailure { e ->
                            _state.update {
                                it.copy(isProcessing = false, progress = 0f, error = e.message)
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("RestoreVM", "Processing failed", e)
                        _state.update {
                            it.copy(
                                isProcessing = false,
                                progress = 0f,
                                error = e.localizedMessage ?: "Unknown error"
                            )
                        }
                    }
                }
                .onFailure { e ->
                    _state.update {
                        it.copy(
                            isProcessing = false,
                            progress = 0f,
                            error = "Failed to load image: ${e.localizedMessage}"
                        )
                    }
                }
        }
    }

    private fun cancelProcessing() {
        processingJob?.cancel()
        processingJob = null
        _state.update { it.copy(isProcessing = false, progress = 0f) }
    }

    override fun onCleared() {
        super.onCleared()
        colorizeUseCase.cleanup()
        imageRepository.cleanupCache()
    }
}

class RestoreViewModelFactory(
    private val imageRepository: ImageRepository,
    private val colorizeUseCase: ColorizeUseCase,
    private val imageUri: Uri
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return RestoreViewModel(
            imageRepository = imageRepository,
            colorizeUseCase = colorizeUseCase,
            imageUri = imageUri
        ) as T
    }
}
