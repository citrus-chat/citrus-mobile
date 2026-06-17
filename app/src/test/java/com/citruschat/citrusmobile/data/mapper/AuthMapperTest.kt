package com.citruschat.citrusmobile.data.mapper

import com.citruschat.citrusmobile.R
import com.citruschat.citrusmobile.domain.auth.AuthError
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AuthMapperTest {
    @Test
    fun `maps unauthorized http error to invalid credentials message`() {
        val messageRes = AuthError.Http(code = 401).toMessageRes()

        assertEquals(R.string.auth_invalid_credentials, messageRes)
    }

    @Test
    fun `maps forbidden http error to access denied message`() {
        val messageRes = AuthError.Http(code = 403).toMessageRes()

        assertEquals(R.string.auth_access_denied, messageRes)
    }

    @Test
    fun `maps unhandled http error to generic login failure message`() {
        val messageRes = AuthError.Http(code = 500).toMessageRes()

        assertEquals(R.string.auth_login_failed, messageRes)
    }

    @Test
    fun `maps network error to network message`() {
        val messageRes = AuthError.Network.toMessageRes()

        assertEquals(R.string.auth_network_error, messageRes)
    }

    @Test
    fun `maps unknown error to generic login failure message`() {
        val messageRes = AuthError.Unknown.toMessageRes()

        assertEquals(R.string.auth_login_failed, messageRes)
    }

    @Test
    fun `resolves known auth http status codes`() {
        assertEquals(AuthHttpStatusCode.UNAUTHORIZED, AuthHttpStatusCode.from(401))
        assertEquals(AuthHttpStatusCode.FORBIDDEN, AuthHttpStatusCode.from(403))
    }

    @Test
    fun `returns null for unsupported auth http status code`() {
        assertNull(AuthHttpStatusCode.from(404))
    }
}
