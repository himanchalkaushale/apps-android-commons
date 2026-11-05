package fr.free.nrw.commons.media.zoomControllers.zoomable

import android.content.Context
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ViewParent
import androidx.test.core.app.ApplicationProvider
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.argThat
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ZoomableTouchHandlerTest {
    private lateinit var context: Context
    private val zoomableController = mock<ZoomableController>()
    private val parent = mock<ViewParent>()
    private val tapDetector = mock<GestureDetector>()
    private var superResult = false

    private lateinit var handler: ZoomableTouchHandler

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        superResult = false
        handler =
            ZoomableTouchHandler(
                context = context,
                zoomableControllerProvider = { zoomableController },
                parentProvider = { parent },
                superOnTouchEvent = { superResult },
                gestureDetector = tapDetector,
            )
    }

    @Test
    fun onTouchEvent_dialToneEnabled_doesNotDispatchToTapOrZoomableAndCallsSuper() {
        handler.isDialToneEnabled = true
        superResult = true
        val event = MotionEvent.obtain(0L, 0L, MotionEvent.ACTION_DOWN, 10f, 10f, 0)

        val handled = handler.onTouchEvent(event)

        assertTrue(handled)
        verify(tapDetector, never()).onTouchEvent(any())
        verify(zoomableController, never()).onTouchEvent(any())
        event.recycle()
    }

    @Test
    fun onTouchEvent_tapDetectorHandles_returnsTrue() {
        val event = MotionEvent.obtain(0L, 0L, MotionEvent.ACTION_DOWN, 10f, 10f, 0)
        whenever(tapDetector.onTouchEvent(event)).thenReturn(true)

        val handled = handler.onTouchEvent(event)

        assertTrue(handled)
        verify(zoomableController, never()).onTouchEvent(any())
        event.recycle()
    }

    @Test
    fun onTouchEvent_zoomableControllerHandles_returnsTrue() {
        val event = MotionEvent.obtain(0L, 0L, MotionEvent.ACTION_DOWN, 10f, 10f, 0)
        whenever(tapDetector.onTouchEvent(event)).thenReturn(false)
        whenever(zoomableController.onTouchEvent(event)).thenReturn(true)

        val handled = handler.onTouchEvent(event)

        assertTrue(handled)
        event.recycle()
    }

    @Test
    fun onTouchEvent_zoomableControllerHandles_disallowsInterceptWhenZoomed() {
        handler.allowTouchInterceptionWhileZoomed = false
        whenever(zoomableController.isIdentity()).thenReturn(false)
        val event = MotionEvent.obtain(0L, 0L, MotionEvent.ACTION_MOVE, 10f, 10f, 0)
        whenever(tapDetector.onTouchEvent(event)).thenReturn(false)
        whenever(zoomableController.onTouchEvent(event)).thenReturn(true)

        val handled = handler.onTouchEvent(event)

        assertTrue(handled)
        verify(parent).requestDisallowInterceptTouchEvent(true)
        event.recycle()
    }

    @Test
    fun onTouchEvent_zoomableControllerHandles_allowsInterceptWhenIdentity() {
        handler.allowTouchInterceptionWhileZoomed = false
        whenever(zoomableController.isIdentity()).thenReturn(true)
        val event = MotionEvent.obtain(0L, 0L, MotionEvent.ACTION_MOVE, 10f, 10f, 0)
        whenever(tapDetector.onTouchEvent(event)).thenReturn(false)
        whenever(zoomableController.onTouchEvent(event)).thenReturn(true)

        val handled = handler.onTouchEvent(event)

        assertTrue(handled)
        verify(parent, never()).requestDisallowInterceptTouchEvent(any())
        event.recycle()
    }

    @Test
    fun onTouchEvent_superHandles_returnsTrue() {
        val event = MotionEvent.obtain(0L, 0L, MotionEvent.ACTION_DOWN, 10f, 10f, 0)
        whenever(tapDetector.onTouchEvent(event)).thenReturn(false)
        whenever(zoomableController.onTouchEvent(event)).thenReturn(false)
        superResult = true

        val handled = handler.onTouchEvent(event)

        assertTrue(handled)
        event.recycle()
    }

    @Test
    fun onTouchEvent_unhandled_dispatchesCancelAndReturnsFalse() {
        val event = MotionEvent.obtain(0L, 0L, MotionEvent.ACTION_DOWN, 10f, 10f, 0)
        whenever(tapDetector.onTouchEvent(any())).thenReturn(false)
        whenever(zoomableController.onTouchEvent(any())).thenReturn(false)
        superResult = false

        val handled = handler.onTouchEvent(event)

        assertFalse(handled)
        verify(tapDetector).onTouchEvent(argThat { action == MotionEvent.ACTION_CANCEL })
        verify(zoomableController).onTouchEvent(argThat { action == MotionEvent.ACTION_CANCEL })
        event.recycle()
    }

    @Test
    fun setIsLongpressEnabled_delegatesToDetector() {
        handler.setIsLongpressEnabled(false)
        verify(tapDetector).setIsLongpressEnabled(false)

        handler.setIsLongpressEnabled(true)
        verify(tapDetector).setIsLongpressEnabled(true)
    }

    @Test
    fun setTapListener_doesNotCrash() {
        val listener = mock<GestureDetector.SimpleOnGestureListener>()
        handler.setTapListener(listener)
    }
}
