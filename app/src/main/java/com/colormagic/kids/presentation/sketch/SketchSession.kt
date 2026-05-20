package com.colormagic.kids.presentation.sketch

import com.colormagic.kids.domain.model.Sketch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

// Process-scoped holder for the sketch currently being worked on.
//
// The create flow spans three screens — Loading generates the sketch,
// SketchPreview shows it, Coloring colours it. Rather than threading a long
// image URL through navigation routes, the Loading screen writes the result
// here and the later screens read it. Single-active-sketch by design.
@Singleton
class SketchSession @Inject constructor() {

    private val _currentSketch = MutableStateFlow<Sketch?>(null)
    val currentSketch: StateFlow<Sketch?> = _currentSketch.asStateFlow()

    fun setCurrentSketch(sketch: Sketch) {
        _currentSketch.value = sketch
    }

    fun clear() {
        _currentSketch.value = null
    }
}
