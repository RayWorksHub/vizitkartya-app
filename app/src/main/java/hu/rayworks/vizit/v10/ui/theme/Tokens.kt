package hu.rayworks.vizit.v10.ui.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color

/** Világos / sötét / rendszer – a Beállítások › Téma választója. */
enum class ThemeMode { Light, Dark, System }

/** A futó téma. Minden szín ebből dönt, így a váltás azonnal újrarajzol mindent. */
object ThemeState {
    var mode by mutableStateOf(ThemeMode.Light)
    var dark by mutableStateOf(false)
}

private fun c(light: Long, dark: Long): Color = Color(if (ThemeState.dark) dark else light)

/**
 * A prototípus androidos színei (a CSS `--a-*` és `--m-*` tokenjei) világos és sötét változatban.
 * Minden képernyő innen veszi a színeit.
 */
object V {
    // Alap felületek
    val bg get() = c(0xFFF7F8FE, 0xFF111318)          // app háttér (--m-bg)
    val surface get() = c(0xFFFFFFFF, 0xFF1B1E26)     // kártyák, csoportok
    val ink get() = c(0xFF0E1733, 0xFFE4E7EF)         // fő szöveg
    val sub get() = c(0xFF677087, 0xFF9AA2B3)         // másodlagos szöveg
    val line get() = c(0xFFE3E7EF, 0xFF2C313C)        // elválasztók, keretek
    val chip get() = c(0xFFEEF1F6, 0xFF262B35)        // semleges kapszula
    val emptyBg get() = c(0xFFF3F5F9, 0xFF20242C)     // üres kép-helyőrző
    val placeholder get() = c(0xFF8A90A0, 0xFF6E7587) // mező helykitöltő

    // Márka és állapot
    val blue get() = c(0xFF2A5BD7, 0xFF9DB8FF)        // kék szöveg, ikon, keret
    val blueFill get() = c(0xFF2A5BD7, 0xFF3F6FE8)    // kék kitöltés fehér tartalommal
    val onBlueFill get() = Color.White
    val blueSoft get() = c(0xFFE8EEFC, 0xFF243150)
    val green get() = c(0xFF1F9D57, 0xFF5CCB8C)
    val greenSoft get() = c(0xFFE4F6EE, 0xFF1D3A2B)
    val red get() = c(0xFFD13B3B, 0xFFFF8A80)
    val redFill get() = c(0xFFD13B3B, 0xFFC8453F)
    val redSoft get() = c(0xFFFDECEC, 0xFF45201F)
    val warn get() = c(0xFFB86F0E, 0xFFF0B35C)
    val warnSoft get() = c(0xFFFBF0DF, 0xFF3D2E17)
    val cyan = Color(0xFF4FB3D9)
    val qr = Color(0xFF0B1330)                        // a QR mindig sötét a fehér lapon
    val qrPlate = Color.White
    val chev get() = c(0xFFB3BAC8, 0xFF5B6272)
    val switchOff get() = c(0xFFE3E5EA, 0xFF2E333D)
    val dot get() = c(0xFFC7CCD8, 0xFF4A5060)
    val onAccent get() = c(0xFFFFFFFF, 0xFF111318)    // szöveg színes kitöltésen (blokk jelvény)

    // Material 3 (androidos) elemek
    val container get() = c(0xFFDCE4FC, 0xFF2A3A63)
    val onContainer get() = c(0xFF0E2A6E, 0xFFD6E0FF)
    val nav get() = c(0xFFEDF0F9, 0xFF171A21)
    val indicator get() = c(0xFFD2DCFA, 0xFF34467A)
    val outline get() = c(0xFF79808F, 0xFF8C92A0)
    val label get() = c(0xFF44474F, 0xFFC4C7D0)
    val toast get() = c(0xFF2F3036, 0xFFE4E7EF)
    val onToast get() = c(0xFFFFFFFF, 0xFF1B1E26)

    // Grafikon (validált pár: világos #2A5BD7 / #EB6834, sötét #5A86F0 / #E0703E)
    val series1 get() = c(0xFF2A5BD7, 0xFF5A86F0)
    val series2 get() = c(0xFFEB6834, 0xFFE0703E)
    val grid get() = c(0xFFE3E7EF, 0xFF2C313C)

    // Átfedések
    val scrim = Color(0x6B080C18)
    val cam1 = Color(0xFF2C3448)
    val cam2 = Color(0xFF0A0E18)

    // Varázsló kezdőképernyő, NFC-küldés (mindkét témában sötét)
    val h1 = Color(0xFF0C2C63)
    val h2 = Color(0xFF2A5BD7)
    val h3 = Color(0xFF05163A)
    val accent = Color(0xFF0FBEE6)
    val glow1 = Color(0x614FB3D9)
    val glow2 = Color(0x992A5BD7)
    val white10 = Color(0x1AFFFFFF)
    val white14 = Color(0x24FFFFFF)
    val white15 = Color(0x26FFFFFF)
    val white22 = Color(0x38FFFFFF)

    // Kurzuslejátszó videó felülete
    val player = Color(0xFF0B0B0F)
}

/** A varázsló blokkjainak színpárja: erős szín + lágy háttér, témánként. */
class BlockColor(private val l: Long, private val ls: Long, private val d: Long, private val ds: Long) {
    val c: Color get() = Color(if (ThemeState.dark) d else l)
    val soft: Color get() = Color(if (ThemeState.dark) ds else ls)
}

val B1 = BlockColor(0xFF2A5BD7, 0xFFE8EEFC, 0xFF7FA2FF, 0xFF22304F) // Személyes adatok
val B2 = BlockColor(0xFFB86F0E, 0xFFFBF0DF, 0xFFE3A04A, 0xFF3A2B16) // Céges adatok
val B3 = BlockColor(0xFF0E8494, 0xFFDCF2F4, 0xFF3DC1D1, 0xFF163539) // Online elérés
val B4 = BlockColor(0xFF1F9D57, 0xFFE4F6EE, 0xFF5CCB8C, 0xFF1D3A2B) // Befejezés

/** cubic-bezier(.2,.8,.2,1) – a lapok, oldalak és a varázsló mozgása. */
val SheetEasing = CubicBezierEasing(0.2f, 0.8f, 0.2f, 1f)
