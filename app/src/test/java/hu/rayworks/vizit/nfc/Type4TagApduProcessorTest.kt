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
        assertTrue(processor.isNdefFullyRead)
    }

    @Test
    fun `partial NDEF reads are not reported as complete until all bytes were read`() {
        processor.process(hex("00A4040007D276000085010100"))
        processor.process(hex("00A4000C02E104"))

        processor.process(hex("00B0000002"))
        assertFalse(processor.isNdefFullyRead)

        processor.process(hex("00B0000203"))
        assertTrue(processor.isNdefFullyRead)
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
        assertFalse(processor.isNdefFullyRead)
    }

    @Test
    fun `unknown file is rejected`() {
        processor.process(hex("00A4040007D276000085010100"))
        val response = processor.process(hex("00A4000C02E105"))
        assertArrayEquals(byteArrayOf(0x6A, 0x82.toByte()), response)
    }

    private fun hex(value: String): ByteArray = value
        .chunked(2)
        .map { it.toInt(16).toByte() }
        .toByteArray()
}
