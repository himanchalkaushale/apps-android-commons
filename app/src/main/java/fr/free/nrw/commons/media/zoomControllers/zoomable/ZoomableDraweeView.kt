package fr.free.nrw.commons.media.zoomControllers.zoomable

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Resources
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.RectF
import android.graphics.drawable.Animatable
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import androidx.core.view.ScrollingView
import com.facebook.common.internal.Preconditions
import com.facebook.common.logging.FLog
import com.facebook.drawee.controller.AbstractDraweeController
import com.facebook.drawee.controller.BaseControllerListener
import com.facebook.drawee.drawable.ScalingUtils
import com.facebook.drawee.generic.GenericDraweeHierarchy
import com.facebook.drawee.generic.GenericDraweeHierarchyBuilder
import com.facebook.drawee.generic.GenericDraweeHierarchyInflater
import com.facebook.drawee.interfaces.DraweeController
import com.facebook.drawee.view.DraweeView

private const val HUGE_IMAGE_SCALE_FACTOR_THRESHOLD = 1.1f
private val TAG = ZoomableDraweeView::class.java

/**
 * DraweeView that has zoomable capabilities.
 *
 * <p>Once the image loads, pinch-to-zoom and translation gestures are enabled.
 */
open class ZoomableDraweeView
    @JvmOverloads
    constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0,
        private val scrollDelegate: ZoomableScrollDelegate = ZoomableScrollDelegate(),
    ) : DraweeView<GenericDraweeHierarchy>(context, attrs, defStyleAttr),
        ScrollingView by scrollDelegate {
        private val imageBounds = RectF()
        private val viewBounds = RectF()

        internal var hugeImageController: DraweeController? = null
        private var zoomableController: ZoomableController = AnimatedZoomableController.newInstance()

        val touchHandler =
            ZoomableTouchHandler(
                context = context,
                zoomableControllerProvider = { zoomableController },
                parentProvider = { parent },
                superOnTouchEvent = { super.onTouchEvent(it) },
            )
        var isZoomingEnabled = true
        private var transformationListener: TransformationListener? = null

        internal val controllerListener =
            object : BaseControllerListener<Any>() {
                override fun onFinalImageSet(
                    id: String,
                    imageInfo: Any?,
                    animatable: Animatable?,
                ) {
                    FLog.v(TAG, "onFinalImageSet: view ${this@ZoomableDraweeView.hashCode()}")
                    if (!zoomableController.isEnabled() && isZoomingEnabled) {
                        zoomableController.setEnabled(true)
                        updateZoomableControllerBounds()
                    }
                }

                override fun onRelease(id: String) {
                    FLog.v(TAG, "onRelease: view ${this@ZoomableDraweeView.hashCode()}")
                    zoomableController.setEnabled(false)
                }
            }

        private val zoomableListener =
            object : ZoomableController.Listener {
                override fun onTransformBegin(transform: Matrix) {}

                override fun onTransformChanged(transform: Matrix) {
                    this@ZoomableDraweeView.onTransformChanged(transform)
                }

                override fun onTransformEnd(transform: Matrix) {
                    transformationListener?.onTransformationEnd()
                }
            }

        init {
            scrollDelegate.controllerProvider = { zoomableController }
            inflateHierarchy(context, attrs)
            zoomableController.setListener(zoomableListener)
        }

        constructor(context: Context, hierarchy: GenericDraweeHierarchy) : this(context, null, 0) {
            setHierarchy(hierarchy)
        }

        fun getZoomableController(): ZoomableController = zoomableController

        fun setTransformationListener(transformationListener: TransformationListener) {
            this.transformationListener = transformationListener
        }

        fun setZoomableController(zoomableController: ZoomableController) {
            Preconditions.checkNotNull(zoomableController)
            this.zoomableController.setListener(null)
            this.zoomableController = zoomableController
            this.zoomableController.setListener(zoomableListener)
        }

        override fun setController(controller: DraweeController?) {
            setControllers(controller, null)
        }

        fun setControllers(
            controller: DraweeController?,
            hugeImageController: DraweeController?,
        ) {
            applyControllersInternal(null, null) { super.setController(it) }
            zoomableController.setEnabled(false)
            applyControllersInternal(controller, hugeImageController) { super.setController(it) }
        }

        override fun onDraw(canvas: Canvas) {
            val saveCount = canvas.save()
            canvas.concat(zoomableController.getTransform())
            try {
                super.onDraw(canvas)
            } catch (e: Exception) {
                val controller = controller
                if (controller is AbstractDraweeController<*, *>) {
                    val callerContext = controller.callerContext
                    if (callerContext != null) {
                        throw IllegalStateException("Exception in onDraw, callerContext=$callerContext", e)
                    }
                }
                throw e
            }
            canvas.restoreToCount(saveCount)
        }

        @SuppressLint("ClickableViewAccessibility")
        override fun onTouchEvent(event: MotionEvent): Boolean = touchHandler.onTouchEvent(event)

        override fun onLayout(
            changed: Boolean,
            left: Int,
            top: Int,
            right: Int,
            bottom: Int,
        ) {
            FLog.v(TAG, "onLayout: view ${hashCode()}")
            super.onLayout(changed, left, top, right, bottom)
            updateZoomableControllerBounds()
        }

        protected fun onTransformChanged(transform: Matrix) {
            FLog.v(TAG, "onTransformChanged: view ${hashCode()}, transform: $transform")
            if (
                hugeImageController != null &&
                zoomableController.getScaleFactor() > HUGE_IMAGE_SCALE_FACTOR_THRESHOLD
            ) {
                applyControllersInternal(hugeImageController, null) { super.setController(it) }
            }
            invalidate()
        }

        protected fun updateZoomableControllerBounds() {
            hierarchy.getActualImageBounds(imageBounds)
            viewBounds.set(0f, 0f, width.toFloat(), height.toFloat())
            zoomableController.setImageBounds(imageBounds)
            zoomableController.setViewBounds(viewBounds)

            FLog.v(
                TAG,
                "updateZoomableControllerBounds: view ${hashCode()}, " +
                    "view bounds: $viewBounds, image bounds: $imageBounds",
            )
        }

        /**
         * Interface to listen for scale change events.
         */
        interface TransformationListener {
            fun onTransformationEnd()
        }
    }

/**
 * Enables or disables zooming on this view.
 *
 * @param zoomingEnabled true to enable zooming, false to disable
 */
fun ZoomableDraweeView.setZoomingEnabled(zoomingEnabled: Boolean) {
    this.isZoomingEnabled = zoomingEnabled
    this.getZoomableController().setEnabled(false)
}

/**
 * Sets whether dial tone feedback/interception is enabled.
 *
 * @param isDialToneEnabled true to enable dial tone mode, false otherwise
 */
fun ZoomableDraweeView.setIsDialToneEnabled(isDialToneEnabled: Boolean) {
    touchHandler.isDialToneEnabled = isDialToneEnabled
}

/**
 * Sets whether touch interception is allowed while zoomed in.
 *
 * @param allowTouchInterceptionWhileZoomed true to allow interception, false otherwise
 */
fun ZoomableDraweeView.setAllowTouchInterceptionWhileZoomed(allowTouchInterceptionWhileZoomed: Boolean) {
    touchHandler.allowTouchInterceptionWhileZoomed = allowTouchInterceptionWhileZoomed
}

/**
 * Enables or disables long press detection on the tap gesture detector.
 *
 * @param isLongpressEnabled true to enable long press, false otherwise
 */
fun ZoomableDraweeView.setIsLongpressEnabled(isLongpressEnabled: Boolean) {
    touchHandler.setIsLongpressEnabled(isLongpressEnabled)
}

/**
 * Sets the tap listener for the view.
 *
 * @param tapListener the listener to receive tap events
 */
fun ZoomableDraweeView.setTapListener(tapListener: GestureDetector.SimpleOnGestureListener) {
    touchHandler.setTapListener(tapListener)
}

internal fun ZoomableDraweeView.applyControllersInternal(
    controller: DraweeController?,
    hugeImageController: DraweeController?,
    setSuperController: (DraweeController?) -> Unit,
) {
    detachListener(getController(), controllerListener)
    attachListener(controller, controllerListener)
    this.hugeImageController = hugeImageController
    setSuperController(controller)
}

private fun ZoomableDraweeView.inflateHierarchy(
    context: Context,
    attrs: AttributeSet?,
) {
    val resources: Resources = context.resources
    val builder =
        GenericDraweeHierarchyBuilder(resources)
            .setActualImageScaleType(ScalingUtils.ScaleType.FIT_CENTER)
    GenericDraweeHierarchyInflater.updateBuilder(builder, context, attrs)
    aspectRatio = builder.desiredAspectRatio
    setHierarchy(builder.build())
}

private fun detachListener(
    controller: DraweeController?,
    listener: BaseControllerListener<Any>,
) {
    if (controller is AbstractDraweeController<*, *>) {
        controller.removeControllerListener(listener)
    }
}

private fun attachListener(
    controller: DraweeController?,
    listener: BaseControllerListener<Any>,
) {
    if (controller is AbstractDraweeController<*, *>) {
        controller.addControllerListener(listener)
    }
}
