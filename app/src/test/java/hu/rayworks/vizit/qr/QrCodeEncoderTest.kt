package hu.rayworks.vizit.qr

import com.google.zxing.BinaryBitmap
import com.google.zxing.MultiFormatReader
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.HybridBinarizer
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class QrCodeEncoderTest {
    @Test
    fun `encoding is deterministic and round trips Hungarian text`() {
        val content = "BEGIN:VCARD\r\nVERSION:3.0\r\nFN:Őrsi Ágnes\r\nEND:VCARD\r\n"

        val first = QrCodeEncoder.encode(content, size = 512)
        val second = QrCodeEncoder.encode(content, size = 512)

        assertArrayEquals(first.pixels, second.pixels)
        assertEquals(content, decode(first))
    }

    @Test
    fun `render keeps white quiet zone on every edge`() {
        val qr = QrCodeEncoder.encode("https://vizit.hu/p/orsi-agnes", size = 512)

        assertTrue((0 until qr.size).all { x -> qr.pixels[x] == WHITE })
        assertTrue((0 until qr.size).all { x -> qr.pixels[(qr.size - 1) * qr.size + x] == WHITE })
        assertTrue((0 until qr.size).all { y -> qr.pixels[y * qr.size] == WHITE })
        assertTrue((0 until qr.size).all { y -> qr.pixels[y * qr.size + qr.size - 1] == WHITE })
    }

    @Test
    fun `medium correction survives the centered logo plate`() {
        val content = "BEGIN:VCARD\r\nVERSION:3.0\r\nFN:Teszt Elek\r\nTEL:+36201234567\r\nEND:VCARD\r\n"
        val original = QrCodeEncoder.encode(content, size = 512)
        val pixels = original.pixels.copyOf()
        val plateSize = (original.size * 0.14f).toInt()
        val start = (original.size - plateSize) / 2
        for (y in start until start + plateSize) {
            for (x in start until start + plateSize) {
                pixels[y * original.size + x] = WHITE
            }
        }

        assertEquals(content, decode(RenderedQrCode(original.size, pixels)))
    }

    private fun decode(qr: RenderedQrCode): String {
        val source = RGBLuminanceSource(qr.size, qr.size, qr.pixels)
        return MultiFormatReader()
            .decode(BinaryBitmap(HybridBinarizer(source)))
            .text
    }

    private companion object {
        const val WHITE: Int = -0x1
    }
}
