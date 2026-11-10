package com.mobilerun.portal.triggers

import android.content.Context
import com.mobilerun.portal.R
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Test

class TriggerUiSupportTest {
    @Test
    fun localizedDisplayKeepsApiLabelsUnchanged() {
        val context = mockk<Context>()
        every { context.getString(R.string.trigger_source_time_delay) } returns "延迟后"
        every { context.getString(R.string.trigger_description_time_delay) } returns "延迟后运行。"

        assertEquals("延迟后", TriggerUiSupport.sourceLabel(context, TriggerSource.TIME_DELAY))
        assertEquals("延迟后运行。", TriggerUiSupport.sourceDescription(context, TriggerSource.TIME_DELAY))
        assertEquals("After a delay", TriggerUiSupport.sourceLabel(TriggerSource.TIME_DELAY))
        assertEquals("Run once after the selected delay.", TriggerUiSupport.sourceDescription(TriggerSource.TIME_DELAY))
    }
}
