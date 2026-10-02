package hu.rayworks.vizit.v10.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.common.BitMatrix
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import hu.rayworks.vizit.R
import hu.rayworks.vizit.v10.ui.theme.V

/** H-szint (középen logóval) ~1200 bájtig; hosszabb névjegynél M-szint, logó nélkül (max. ~2300 bájt). */
fun qrUsesHighLevel(text: String): Boolean = text.toByteArray(Charsets.UTF_8).size <= 1200

fun encodeQr(text: String): BitMatrix? = runCatching {
    QRCodeWriter().encode(
        text,
        BarcodeFormat.QR_CODE,
        0,
        0,
        mapOf(
            EncodeHintType.ERROR_CORRECTION to if (qrUsesHighLevel(text)) ErrorCorrectionLevel.H else ErrorCorrectionLevel.M,
            EncodeHintType.MARGIN to 0,
            EncodeHintType.CHARACTER_SET to "UTF-8",
        ),
    )
}.getOrNull()

/**
 * Valódi, beolvasható QR-kód H-szintű hibajavítással, középen a VIZIT logóval
 * (.qr-logo: a kód szélességének 22%-a, fehér, lekerekített háttér).
 */
@Composable
fun QrCode(content: String, modifier: Modifier = Modifier, color: Color = V.qr, withLogo: Boolean = true) {
    val matrix = remember(content) { encodeQr(content) }
    Box(modifier.aspectRatio(1f), contentAlignment = Alignment.Center) {
        if (matrix != null) Box(
            Modifier
                .fillMaxSize()
                .drawWithCache {
                    val n = matrix.width
                    val cell = size.width / n
                    val path = Path()
                    for (y in 0 until n) {
                        var x = 0
                        while (x < n) {
                            if (matrix.get(x, y)) {
                                val start = x
                                while (x < n && matrix.get(x, y)) x++
                                path.addRect(Rect(start * cell, y * cell, x * cell, (y + 1) * cell + 0.5f))
                            } else {
                                x++
                            }
                        }
                    }
                    onDrawBehind {
                        drawRect(Color.White)
                        drawPath(path, color)
                    }
                }
        )
        if (matrix != null && withLogo && qrUsesHighLevel(content)) {
            Box(
                Modifier
                    .fillMaxSize(0.26f)
                    .clip(RoundedCornerShape(percent = 24))
                    .background(Color.White)
                    .padding(3.dp),
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    painter = painterResource(R.drawable.vizit_logo),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(0.9f),
                )
            }
        }
    }
}
