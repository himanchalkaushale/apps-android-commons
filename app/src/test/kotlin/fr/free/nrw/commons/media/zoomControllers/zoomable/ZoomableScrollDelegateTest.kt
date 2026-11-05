package fr.free.nrw.commons.media.zoomControllers.zoomable

import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import org.junit.Assert.assertEquals
import org.junit.Test

class ZoomableScrollDelegateTest {
    @Test
    fun whenProviderNull_returnsZeroForAllScrollMetrics() {
        val delegate = ZoomableScrollDelegate()

        assertEquals(0, delegate.computeHorizontalScrollRange())
        assertEquals(0, delegate.computeHorizontalScrollOffset())
        assertEquals(0, delegate.computeHorizontalScrollExtent())
        assertEquals(0, delegate.computeVerticalScrollRange())
        assertEquals(0, delegate.computeVerticalScrollOffset())
        assertEquals(0, delegate.computeVerticalScrollExtent())
    }

    @Test
    fun whenProviderReturnsController_delegatesToController() {
        val controller = mock<ZoomableController>()
        whenever(controller.computeHorizontalScrollRange()).thenReturn(100)
        whenever(controller.computeHorizontalScrollOffset()).thenReturn(20)
        whenever(controller.computeHorizontalScrollExtent()).thenReturn(50)
        whenever(controller.computeVerticalScrollRange()).thenReturn(200)
        whenever(controller.computeVerticalScrollOffset()).thenReturn(40)
        whenever(controller.computeVerticalScrollExtent()).thenReturn(80)

        val delegate = ZoomableScrollDelegate { controller }

        assertEquals(100, delegate.computeHorizontalScrollRange())
        assertEquals(20, delegate.computeHorizontalScrollOffset())
        assertEquals(50, delegate.computeHorizontalScrollExtent())
        assertEquals(200, delegate.computeVerticalScrollRange())
        assertEquals(40, delegate.computeVerticalScrollOffset())
        assertEquals(80, delegate.computeVerticalScrollExtent())
    }
}
