package com.citruschat.citrusmobile.navigation

import org.junit.Assert.assertEquals
import org.junit.Test

class RoutesTest {
    @Test
    fun `chat route template contains chat id argument`() {
        assertEquals("chat/{chatId}", Routes.Chat)
    }

    @Test
    fun `builds concrete chat route from id`() {
        assertEquals("chat/42", Routes.chat(42))
    }

    @Test
    fun `connected devices route is registered`() {
        assertEquals("connected-devices", Routes.ConnectedDevices)
    }

    @Test
    fun `device qr scanner route is registered`() {
        assertEquals("device-qr-scanner", Routes.DeviceQrScanner)
    }
}
