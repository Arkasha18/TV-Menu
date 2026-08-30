package com.h9.tvquickmenu

import org.junit.Assert.assertEquals
import org.junit.Test

class AccessibilityFixTest {
    private val service =
        "com.h9.tvquickmenu/com.h9.tvquickmenu.HotkeyAccessibilityService"

    @Test
    fun emptySettingAddsService() {
        assertEquals(service, AccessibilityFix.mergeEnabledServices("", service))
        assertEquals(service, AccessibilityFix.mergeEnabledServices("null", service))
    }

    @Test
    fun existingServicesArePreserved() {
        val talkBack = "com.google.android.marvin.talkback/.TalkBackService"

        assertEquals(
            "$talkBack:$service",
            AccessibilityFix.mergeEnabledServices(talkBack, service)
        )
    }

    @Test
    fun duplicateServicesAreRemovedCaseInsensitively() {
        val duplicate = service.uppercase()

        assertEquals(
            duplicate,
            AccessibilityFix.mergeEnabledServices("$duplicate::$service", service)
        )
    }

    @Test
    fun shellValuesAreSingleQuoted() {
        assertEquals("'alpha'\\''beta'", AccessibilityFix.shellQuote("alpha'beta"))
    }
}
