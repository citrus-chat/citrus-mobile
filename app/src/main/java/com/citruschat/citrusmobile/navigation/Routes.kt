package com.citruschat.citrusmobile.navigation

object Routes {
    const val Splash = "splash"
    const val Login = "login"
    const val Home = "home"

    const val Main = "main"
    const val Chat = "chat/{chatId}"
    const val ChatProfile = "chat/{chatId}/profile"

    const val Profile = "profile"
    const val ConnectedDevices = "connected-devices"
    const val DeviceQrScanner = "device-qr-scanner"

    fun chat(chatId: Long) = "chat/$chatId"

    fun chatProfile(chatId: Long) = "chat/$chatId/profile"
}
