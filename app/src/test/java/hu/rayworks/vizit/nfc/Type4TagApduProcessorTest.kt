package hu.rayworks.vizit.nfc

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class Type4TagApduProcessorTest {
    private val processor = Type4TagApduProcessor(byteArrayOf(0x11, 0x22, 0x33))

    @Test
    fun `reader can select application and read NDEF file`() {
        assertArrayEquals(
            Type4TagApduProcessor.STATUS_OK,
            processor.process(hex("00A4040007D276000085010100")),
        )
        assertArrayEquals(
            Type4TagApduProcessor.STATUS_OK,
            processor.process(hex("00A4000C02E104")),
        )

        val response = processor.process(hex("00B0000005"))

        assertArrayEquals(
            byteArrayOf(0x00, 0x03, 0x11, 0x22, 0x33, 0x90.toByte(), 0x00),
            response,
        )
    }

    @Test
    fun `capability container exposes read only NDEF file`() {
        processor.process(hex("00A4040007D276000085010100"))
        processor.process(hex("00A4000C02E103"))

        val response = processor.process(hex("00B000000F"))

        assertEquals(17, response.size)
        assertEquals(0x00, response[13].toInt())
        assertEquals(0xFF, response[14].toInt() and 0xFF)
        assertEquals(0x90, response[15].toInt() and 0xFF)
        assertEquals(0x00, response[16].toInt())
    }

    @Test
    fun `file selection is rejected before NDEF application selection`() {
        assertArrayEquals(
            Type4TagApduProcessor.STATUS_COMMAND_NOT_ALLOWED,
            processor.process(hex("00A4000C02E104")),
        )
    }

    @Test
    fun `payload read is complete only after every NDEF file byte was covered`() {
        val progress = mutableListOf<NdefReadProgress>()
        val trackedProcessor = Type4TagApduProcessor(
            ndefMessage = byteArrayOf(0x11, 0x22, 0x33),
            onNdefReadProgress = progress::add,
        )
        trackedProcessor.process(hex("00A4040007D276000085010100"))
        trackedProcessor.process(hex("00A4000C02E104"))

        trackedProcessor.process(hex("00B0000002"))
        assertFalse(progress.last().isComplete)

        trackedProcessor.process(hex("00B0000203"))
        assertTrue(progress.last().isComplete)
        assertEquals(5, progress.last().coveredBytes)
        assertEquals(5, progress.last().totalBytes)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `oversized NDEF message is rejected`() {
        Type4TagApduProcessor(ByteArray(Type4TagApduProcessor.MAX_NDEF_SIZE + 1))
    }

    @Test
    fun `malformed read binary returns wrong length`() {
        processor.process(hex("00A4040007D276000085010100"))
        processor.process(hex("00A4000C02E104"))

        assertArrayEquals(hex("6700"), processor.process(hex("00B000")))
    }

    private fun hex(value: String): ByteArray = value
        .chunked(2)
        .map { it.toInt(16).toByte() }
        .toByteArray()
}
