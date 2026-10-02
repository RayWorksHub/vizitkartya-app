package hu.rayworks.vizit.v10.ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

/**
 * A prototípus saját vonalas ikonjai, ugyanazokkal az SVG-útvonalakkal (24×24 viewBox,
 * 1.8-as vonalvastagság, kerek végek). A színt az Icon(tint = ...) adja.
 */
object VIcons {
    val qr by lazy {
        icon("qr",
            stroke = listOf("M4 4h6v6H4zM14 4h6v6h-6zM4 14h6v6H4z"),
            fill = listOf("M14 14h2.6v2.6H14zM17.4 17.4H20V20h-2.6zM14 18.2h2.6V20H14zM18.2 14H20v2.6h-1.8z"))
    }
    val person by lazy { icon("person", listOf("M12 12a4 4 0 1 0 0-8 4 4 0 0 0 0 8zM4.5 20c1.3-3.6 4.2-5.5 7.5-5.5s6.2 1.9 7.5 5.5")) }
    val more by lazy {
        icon("more",
            stroke = listOf("M12 21a9 9 0 1 0 0-18 9 9 0 0 0 0 18z"),
            fill = listOf(circle(7.8f, 12f, 1.3f), circle(12f, 12f, 1.3f), circle(16.2f, 12f, 1.3f)))
    }
    val scan by lazy { icon("scan", listOf("M4 8.5v-2A2.5 2.5 0 0 1 6.5 4h2M15.5 4h2A2.5 2.5 0 0 1 20 6.5v2M20 15.5v2a2.5 2.5 0 0 1-2.5 2.5h-2M8.5 20h-2A2.5 2.5 0 0 1 4 17.5v-2M7.5 12h9")) }
    val share by lazy {
        icon("share", listOf("M8.8 10.7l6.4-3.7M8.8 13.3l6.4 3.7", circle(17.5f, 5.8f, 2.6f), circle(6.5f, 12f, 2.6f), circle(17.5f, 18.2f, 2.6f)))
    }
    val chev by lazy { icon("chev", listOf("M9.5 6l6 6-6 6")) }
    val chevDown by lazy { icon("chevDown", listOf("M6 9.5l6 6 6-6")) }
    val chevDownBold by lazy { icon("chevDownBold", listOf("M6 9.5l6 6 6-6"), strokeWidth = 2.2f) }
    val pencil by lazy { icon("pencil", listOf("M4 20h4.2L19.3 8.9a2 2 0 0 0 0-2.8l-1.4-1.4a2 2 0 0 0-2.8 0L4 15.8zM13.8 6.2l4 4")) }
    val ext by lazy { icon("ext", listOf("M14 4h6v6M20 4l-8.5 8.5M18 14v4.5a1.5 1.5 0 0 1-1.5 1.5h-11A1.5 1.5 0 0 1 4 18.5v-11A1.5 1.5 0 0 1 5.5 6H10")) }
    val mail by lazy { icon("mail", listOf("M4.5 5.5h15A1.5 1.5 0 0 1 21 7v10a1.5 1.5 0 0 1-1.5 1.5h-15A1.5 1.5 0 0 1 3 17V7a1.5 1.5 0 0 1 1.5-1.5zM3.5 7l8.5 6 8.5-6")) }
    val nfc by lazy {
        icon("nfc",
            stroke = listOf("M9 8.5a5 5 0 0 1 0 7M12.5 6a8.5 8.5 0 0 1 0 12M16 3.5a12 12 0 0 1 0 17"),
            fill = listOf(circle(5.5f, 12f, 1.6f)))
    }
    val close by lazy { icon("close", listOf("M6 6l12 12M18 6L6 18")) }
    val eye by lazy { icon("eye", listOf("M2.5 12S6 5.5 12 5.5 21.5 12 21.5 12 18 18.5 12 18.5 2.5 12 2.5 12z", circle(12f, 12f, 3f))) }
    val chart by lazy { icon("chart", listOf("M5 20v-7M10 20V6M15 20v-9M20 20v-4")) }
    val book by lazy { icon("book", listOf("M6 4h12v16H6a2 2 0 0 1-2-2V6a2 2 0 0 1 2-2zM4 16a2 2 0 0 1 2-2h12M9 8h6")) }
    val bell by lazy { icon("bell", listOf("M6.5 16.5V11a5.5 5.5 0 0 1 11 0v5.5l1.5 2h-14zM10 20.5a2 2 0 0 0 4 0")) }
    val globe by lazy { icon("globe", listOf("M12 21a9 9 0 1 0 0-18 9 9 0 0 0 0 18zM3 12h18M12 3c2.5 2.6 3.7 5.6 3.7 9s-1.2 6.4-3.7 9c-2.5-2.6-3.7-5.6-3.7-9S9.5 5.6 12 3z")) }
    val shield by lazy { icon("shield", listOf("M12 3l7 2.8v5.4c0 4.6-3 8.3-7 9.8-4-1.5-7-5.2-7-9.8V5.8z")) }
    val help by lazy {
        icon("help",
            stroke = listOf("M12 21a9 9 0 1 0 0-18 9 9 0 0 0 0 18zM9.6 9.4a2.5 2.5 0 1 1 3.6 2.3c-.8.4-1.2.9-1.2 1.8v.5"),
            fill = listOf(circle(12f, 17f, 1.1f)))
    }
    val sync by lazy { icon("sync", listOf("M19.5 10.5A7.8 7.8 0 0 0 5.6 7M4.5 13.5A7.8 7.8 0 0 0 18.4 17M5 3.5V7h3.5M19 20.5V17h-3.5")) }
    val sliders by lazy { icon("sliders", listOf("M4 7h9M17 7h3M4 17h3M11 17h9", circle(15f, 7f, 2f), circle(9f, 17f, 2f))) }
    val palette by lazy {
        icon("palette",
            stroke = listOf("M12 3a9 9 0 1 0 0 18c1.2 0 1.8-.8 1.8-1.7 0-.5-.2-.9-.5-1.2-.3-.3-.5-.7-.5-1.2 0-1 .8-1.7 1.8-1.7H17a4 4 0 0 0 4-4C21 6.6 17 3 12 3z"),
            fill = listOf(circle(7.5f, 11.5f, 1.2f), circle(10f, 7.5f, 1.2f), circle(15f, 7.5f, 1.2f)))
    }
    val logout by lazy { icon("logout", listOf("M15 4h3a2 2 0 0 1 2 2v12a2 2 0 0 1-2 2h-3M10 8l-4 4 4 4M6 12h10")) }
    val link by lazy { icon("link", listOf("M10 13.5a4 4 0 0 0 5.7.3l2.9-2.9a4 4 0 0 0-5.7-5.7l-1.2 1.2M14 10.5a4 4 0 0 0-5.7-.3l-2.9 2.9a4 4 0 0 0 5.7 5.7l1.2-1.2")) }
    val plus by lazy { icon("plus", listOf("M12 5v14M5 12h14")) }
    val stack by lazy { icon("stack", listOf("M4 8l8-4 8 4-8 4zM4 12.5l8 4 8-4M4 16.5l8 4 8-4")) }
    val grip by lazy { icon("grip", listOf("M8 8h8M8 12h8M8 16h8")) }
    val check by lazy { icon("check", listOf("M5 12.5l4.5 4.5L19 7.5")) }
    val checkBold by lazy { icon("checkBold", listOf("M5 12.5l4.5 4.5L19 7.5"), strokeWidth = 2.6f) }
    val briefcase by lazy { icon("briefcase", listOf("M5 7h14a2 2 0 0 1 2 2v9a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V9a2 2 0 0 1 2-2zM8.5 7V5.5A1.5 1.5 0 0 1 10 4h4a1.5 1.5 0 0 1 1.5 1.5V7M3 12.5h18")) }
    val camera by lazy { icon("camera", listOf("M3 8.5h3.2L8 6h8l1.8 2.5H21V19a1.5 1.5 0 0 1-1.5 1.5h-15A1.5 1.5 0 0 1 3 19z", circle(12f, 13.5f, 3.4f))) }
    val phone by lazy { icon("phone", listOf("M6.5 3.5h3l1.8 4.6-2.2 1.4a11 11 0 0 0 5.4 5.4l1.4-2.2 4.6 1.8v3a2 2 0 0 1-2.2 2A16.5 16.5 0 0 1 4.5 5.7a2 2 0 0 1 2-2.2z")) }

    // A varázsló blokkjainak nagy, vékonyabb vonalú ikonjai (.blk-big-ic: stroke-width 1.5)
    val personThin by lazy { icon("personThin", listOf("M12 12a4 4 0 1 0 0-8 4 4 0 0 0 0 8zM4.5 20c1.3-3.6 4.2-5.5 7.5-5.5s6.2 1.9 7.5 5.5"), strokeWidth = 1.5f) }
    val briefcaseThin by lazy { icon("briefcaseThin", listOf("M5 7h14a2 2 0 0 1 2 2v9a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V9a2 2 0 0 1 2-2zM8.5 7V5.5A1.5 1.5 0 0 1 10 4h4a1.5 1.5 0 0 1 1.5 1.5V7M3 12.5h18"), strokeWidth = 1.5f) }
    val globeThin by lazy { icon("globeThin", listOf("M12 21a9 9 0 1 0 0-18 9 9 0 0 0 0 18zM3 12h18M12 3c2.5 2.6 3.7 5.6 3.7 9s-1.2 6.4-3.7 9c-2.5-2.6-3.7-5.6-3.7-9S9.5 5.6 12 3z"), strokeWidth = 1.5f) }
    val checkThin by lazy { icon("checkThin", listOf("M5 12.5l4.5 4.5L19 7.5"), strokeWidth = 1.5f) }

    // Az éles app további funkcióinak ikonjai (ugyanabban a vonalas stílusban)
    val back by lazy { icon("back", listOf("M14.5 5l-7 7 7 7")) }
    val arrowRight by lazy { icon("arrowRight", listOf("M5 12h14M13 6l6 6-6 6")) }
    val copy by lazy { icon("copy", listOf("M9.5 9h9A1.5 1.5 0 0 1 20 10.5v9a1.5 1.5 0 0 1-1.5 1.5h-9A1.5 1.5 0 0 1 8 19.5v-9A1.5 1.5 0 0 1 9.5 9zM16 9V5.5A1.5 1.5 0 0 0 14.5 4h-9A1.5 1.5 0 0 0 4 5.5v9A1.5 1.5 0 0 0 5.5 16H8")) }
    val doc by lazy { icon("doc", listOf("M7.5 3.5H14l4 4V19a1.5 1.5 0 0 1-1.5 1.5h-9A1.5 1.5 0 0 1 6 19V5a1.5 1.5 0 0 1 1.5-1.5zM14 3.5V8h4M9 12h6M9 15.5h6")) }
    val contact by lazy {
        icon("contact", listOf(
            "M4.5 5h15A1.5 1.5 0 0 1 21 6.5v11a1.5 1.5 0 0 1-1.5 1.5h-15A1.5 1.5 0 0 1 3 17.5v-11A1.5 1.5 0 0 1 4.5 5zM13.5 10h4M13.5 13.5h4M5.8 16c.5-1.4 1.6-2.2 2.7-2.2s2.2.8 2.7 2.2",
            circle(8.5f, 10.5f, 2f),
        ))
    }
    val download by lazy { icon("download", listOf("M12 4v11M7.5 10.5L12 15l4.5-4.5M5 19.5h14")) }
    val image by lazy { icon("image", listOf("M5 4.5h14A1.5 1.5 0 0 1 20.5 6v12a1.5 1.5 0 0 1-1.5 1.5H5A1.5 1.5 0 0 1 3.5 18V6A1.5 1.5 0 0 1 5 4.5zM3.5 16l5-5 4.5 4.5 2.5-2.5 5 5", circle(15.5f, 9f, 1.5f))) }
    val flash by lazy { icon("flash", listOf("M13 3L5.5 13.5H11L10 21l8-11h-5.5z")) }
    val play by lazy { icon("play", stroke = emptyList(), fill = listOf("M8 5.5v13l10.5-6.5z")) }
    val playCircle by lazy { icon("playCircle", stroke = listOf(circle(12f, 12f, 9f)), fill = listOf("M10 8.5v7l5.5-3.5z")) }
    val checkCircle by lazy { icon("checkCircle", listOf(circle(12f, 12f, 9f), "M8 12.2l2.8 2.8L16.2 9.5")) }
    val mic by lazy { icon("mic", listOf("M12 4a2.5 2.5 0 0 1 2.5 2.5V12a2.5 2.5 0 0 1-5 0V6.5A2.5 2.5 0 0 1 12 4zM6.5 11.5a5.5 5.5 0 0 0 11 0M12 17v3.5")) }
    val micOff by lazy { icon("micOff", listOf("M12 4a2.5 2.5 0 0 1 2.5 2.5V12a2.5 2.5 0 0 1-5 0V6.5A2.5 2.5 0 0 1 12 4zM6.5 11.5a5.5 5.5 0 0 0 11 0M12 17v3.5M4 4l16 16")) }
    val video by lazy { icon("video", listOf("M4.5 7h9A1.5 1.5 0 0 1 15 8.5v7a1.5 1.5 0 0 1-1.5 1.5h-9A1.5 1.5 0 0 1 3 15.5v-7A1.5 1.5 0 0 1 4.5 7zM15 10.5l5.5-3v9l-5.5-3")) }
    val videoOff by lazy { icon("videoOff", listOf("M4.5 7h9A1.5 1.5 0 0 1 15 8.5v7a1.5 1.5 0 0 1-1.5 1.5h-9A1.5 1.5 0 0 1 3 15.5v-7A1.5 1.5 0 0 1 4.5 7zM15 10.5l5.5-3v9l-5.5-3M3 4l18 16")) }
    val callEnd by lazy { icon("callEnd", listOf("M3.5 13.5c4.8-4.4 12.2-4.4 17 0l-2.2 2.6-3.3-1.3v-2.3a9.5 9.5 0 0 0-6 0v2.3l-3.3 1.3z")) }
    val bulb by lazy { icon("bulb", listOf("M9 18h6M10 21h4M12 3a6 6 0 0 0-3.5 10.9c.6.5 1 1.2 1 2.1h5c0-.9.4-1.6 1-2.1A6 6 0 0 0 12 3z")) }
    val timer by lazy { icon("timer", listOf(circle(12f, 13f, 8f), "M12 9v4l2.5 2.5M9.5 2.5h5")) }
    val cloud by lazy { icon("cloud", listOf("M7 18.5a4.5 4.5 0 0 1-.6-9A6 6 0 0 1 18 8.6a4 4 0 0 1-.5 9.9z")) }
    val cloudOff by lazy { icon("cloudOff", listOf("M7 18.5a4.5 4.5 0 0 1-.6-9A6 6 0 0 1 18 8.6a4 4 0 0 1-.5 9.9zM4 4l16 16")) }
    val moon by lazy { icon("moon", listOf("M20 14.5A8 8 0 0 1 9.5 4a8 8 0 1 0 10.5 10.5z")) }
    val lock by lazy { icon("lock", listOf("M6.5 10.5h11a1.5 1.5 0 0 1 1.5 1.5v7a1.5 1.5 0 0 1-1.5 1.5h-11A1.5 1.5 0 0 1 5 19v-7a1.5 1.5 0 0 1 1.5-1.5zM8.5 10.5V7.5a3.5 3.5 0 0 1 7 0v3")) }
    val trash by lazy { icon("trash", listOf("M5 7h14M10 7V5h4v2M7 7l1 12.5a1.5 1.5 0 0 0 1.5 1.5h5a1.5 1.5 0 0 0 1.5-1.5L17 7M10.5 11v6M13.5 11v6")) }
    val warning by lazy { icon("warning", stroke = listOf("M12 4l9 16H3zM12 10v4"), fill = listOf(circle(12f, 17f, 1f))) }
    val info by lazy { icon("info", stroke = listOf(circle(12f, 12f, 9f), "M12 11v5"), fill = listOf(circle(12f, 8f, 1.1f))) }
    val building by lazy { icon("building", listOf("M5 20.5V5.5A1.5 1.5 0 0 1 6.5 4h7A1.5 1.5 0 0 1 15 5.5v15M15 10h3.5A1.5 1.5 0 0 1 20 11.5v9M3.5 20.5h17M8 8h3M8 11.5h3M8 15h3")) }
    val pin by lazy { icon("pin", listOf("M12 21s-6.5-5.6-6.5-11a6.5 6.5 0 0 1 13 0c0 5.4-6.5 11-6.5 11z", circle(12f, 10f, 2.4f))) }
    val school by lazy { icon("school", listOf("M2.5 9L12 4.5 21.5 9 12 13.5zM6.5 11v4.5c0 1.5 2.5 3 5.5 3s5.5-1.5 5.5-3V11M21.5 9v5")) }
    val headset by lazy { icon("headset", listOf("M4.5 13.5V12a7.5 7.5 0 0 1 15 0v1.5M4.5 13.5H7v5H5.5a1 1 0 0 1-1-1zM19.5 13.5H17v5h1.5a1 1 0 0 0 1-1zM17 18.5c0 1.4-1.5 2-3.5 2")) }
    val wrench by lazy { icon("wrench", listOf("M15 4.5a4.5 4.5 0 0 0-4.2 6.1L4.5 16.9a1.9 1.9 0 0 0 2.6 2.6l6.3-6.3A4.5 4.5 0 0 0 19.5 9l-2.8 2.8-2.5-.9-.9-2.5z")) }
    val users by lazy { icon("users", listOf(circle(9f, 8.5f, 3f), "M3.5 19c.8-3 3-4.5 5.5-4.5s4.7 1.5 5.5 4.5M16 5.8a3 3 0 0 1 0 5.4M17.5 14.8c1.5.6 2.6 1.9 3 4.2")) }
    val fullscreen by lazy { icon("fullscreen", listOf("M4 9V5.5A1.5 1.5 0 0 1 5.5 4H9M15 4h3.5A1.5 1.5 0 0 1 20 5.5V9M20 15v3.5a1.5 1.5 0 0 1-1.5 1.5H15M9 20H5.5A1.5 1.5 0 0 1 4 18.5V15")) }
    val eyeOff by lazy { icon("eyeOff", listOf("M2.5 12S6 5.5 12 5.5 21.5 12 21.5 12 18 18.5 12 18.5 2.5 12 2.5 12zM4 4l16 16", circle(12f, 12f, 3f))) }
    val textLines by lazy { icon("textLines", listOf("M5 6.5h14M5 10.5h14M5 14.5h9M5 18.5h6")) }
    val flask by lazy { icon("flask", listOf("M9.5 3.5h5M10.5 3.5v5.5L5 18.5A1.5 1.5 0 0 0 6.3 20.5h11.4a1.5 1.5 0 0 0 1.3-2L13.5 9V3.5M7.5 14.5h9")) }
    val person3 by lazy { icon("person3", listOf("M12 12a4 4 0 1 0 0-8 4 4 0 0 0 0 8zM4.5 20c1.3-3.6 4.2-5.5 7.5-5.5s6.2 1.9 7.5 5.5"), strokeWidth = 1.4f) }

    /** A varázsló kezdőképernyőjének NFC-hullámai (.intro-waves, 220×240-es viewBox, 260 dp széles). */
    val introWaves by lazy {
        val white15 = SolidColor(Color.White.copy(alpha = 0.15f))
        ImageVector.Builder(
            name = "introWaves",
            defaultWidth = 260.dp,
            defaultHeight = 283.6.dp,
            viewportWidth = 220f,
            viewportHeight = 240f,
        ).apply {
            addPath(pathData = addPathNodes(circle(40f, 120f, 13f)), fill = white15)
            listOf(
                "M78 62a82 82 0 0 1 0 116",
                "M112 30a124 124 0 0 1 0 180",
                "M146 -2a166 166 0 0 1 0 244",
            ).forEach { d ->
                addPath(
                    pathData = addPathNodes(d),
                    fill = null,
                    stroke = white15,
                    strokeLineWidth = 12f,
                    strokeLineCap = StrokeCap.Round,
                    strokeLineJoin = StrokeJoin.Round,
                )
            }
        }.build()
    }
}

private fun num(x: Float): String = if (x == x.toInt().toFloat()) x.toInt().toString() else x.toString()

/** SVG <circle> átírva útvonallá (két félkörív). */
private fun circle(cx: Float, cy: Float, r: Float): String =
    "M${num(cx - r)} ${num(cy)}a${num(r)} ${num(r)} 0 1 0 ${num(2 * r)} 0a${num(r)} ${num(r)} 0 1 0 ${num(-2 * r)} 0z"

private fun icon(
    name: String,
    stroke: List<String>,
    fill: List<String> = emptyList(),
    strokeWidth: Float = 1.8f,
): ImageVector {
    val builder = ImageVector.Builder(
        name = name,
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    )
    stroke.forEach { d ->
        builder.addPath(
            pathData = addPathNodes(d),
            fill = null,
            stroke = SolidColor(Color.Black),
            strokeLineWidth = strokeWidth,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
        )
    }
    fill.forEach { d ->
        builder.addPath(pathData = addPathNodes(d), fill = SolidColor(Color.Black))
    }
    return builder.build()
}
