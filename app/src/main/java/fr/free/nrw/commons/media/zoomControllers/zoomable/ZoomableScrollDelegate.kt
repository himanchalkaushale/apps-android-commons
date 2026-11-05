package fr.free.nrw.commons.media.zoomControllers.zoomable

import androidx.core.view.ScrollingView

/**
 * Delegates [ScrollingView] calls to a [ZoomableController].
 */
class ZoomableScrollDelegate(
    var controllerProvider: (() -> ZoomableController)? = null,
) : ScrollingView {
    override fun computeHorizontalScrollRange(): Int = controllerProvider?.invoke()?.computeHorizontalScrollRange() ?: 0

    override fun computeHorizontalScrollOffset(): Int =
        controllerProvider?.invoke()?.computeHorizontalScrollOffset() ?: 0

    override fun computeHorizontalScrollExtent(): Int =
        controllerProvider?.invoke()?.computeHorizontalScrollExtent() ?: 0

    override fun computeVerticalScrollRange(): Int = controllerProvider?.invoke()?.computeVerticalScrollRange() ?: 0

    override fun computeVerticalScrollOffset(): Int = controllerProvider?.invoke()?.computeVerticalScrollOffset() ?: 0

    override fun computeVerticalScrollExtent(): Int = controllerProvider?.invoke()?.computeVerticalScrollExtent() ?: 0
}
