package hu.rayworks.vizit.data.remote

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BackendConfigurationTest {
    @Test
    fun `missing public client values keep backend disabled`() {
        val configuration = configuration(supabaseUrl = "", publishableKey = "")

        assertEquals(BackendConfigurationStatus.MISSING, configuration.status)
        assertFalse(configuration.isSupabaseReady)
    }

    @Test
    fun `valid HTTPS configuration is ready`() {
        val configuration = configuration()

        assertEquals(BackendConfigurationStatus.READY, configuration.status)
        assertTrue(configuration.validationErrors().isEmpty())
    }

    @Test
    fun `HTTP Supabase endpoint is rejected`() {
        val configuration = configuration(supabaseUrl = "http://example.supabase.co")

        assertEquals(BackendConfigurationStatus.INVALID, configuration.status)
        assertTrue(configuration.validationErrors().any { it.contains("Supabase URL") })
    }

    @Test
    fun `local emulator HTTP endpoint is accepted in DEV only`() {
        val devConfiguration = configuration(
            environment = "DEV",
            supabaseUrl = "http://10.0.2.2:54321",
        )
        val betaConfiguration = configuration(
            environment = "BETA",
            supabaseUrl = "http://10.0.2.2:54321",
        )

        assertEquals(BackendConfigurationStatus.READY, devConfiguration.status)
        assertEquals(BackendConfigurationStatus.INVALID, betaConfiguration.status)
    }

    @Test
    fun `secret key is rejected from mobile configuration`() {
        val configuration = configuration(publishableKey = "sb_secret_do-not-ship")

        assertEquals(BackendConfigurationStatus.INVALID, configuration.status)
        assertTrue(configuration.validationErrors().any { it.contains("Titkos") })
    }

    @Test
    fun `public profile URL cannot contain fragment`() {
        val configuration = configuration(publicProfileBaseUrl = "https://vizit.hu/p#unsafe")

        assertEquals(BackendConfigurationStatus.INVALID, configuration.status)
    }

    private fun configuration(
        environment: String = "DEV",
        supabaseUrl: String = "https://example.supabase.co",
        publishableKey: String = "sb_publishable_example",
        publicProfileBaseUrl: String = "https://vizit.hu/p",
    ) = BackendConfiguration(
        environment = environment,
        supabaseUrl = supabaseUrl,
        supabasePublishableKey = publishableKey,
        publicProfileBaseUrl = publicProfileBaseUrl,
        googleAuthEnabled = false,
    )
}
