package com.citruschat.citrusmobile

import org.junit.Assert.assertEquals
import org.junit.Test

class AppPackageTest {
    @Test
    fun `keeps expected application package name`() {
        assertEquals("com.citruschat.citrusmobile", BuildConfig.APPLICATION_ID)
    }
}
