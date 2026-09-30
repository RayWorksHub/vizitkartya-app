package hu.rayworks.vizit.v10.ui.pages

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Size as AndroidSize
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeReader
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import hu.rayworks.vizit.qr.QrFrameDecoder
import hu.rayworks.vizit.v10.data.AppState
import hu.rayworks.vizit.v10.ui.LocalActions
import hu.rayworks.vizit.v10.ui.components.IconCircleButton
import hu.rayworks.vizit.v10.ui.components.PrimaryButton
import hu.rayworks.vizit.v10.ui.components.TextLink
import hu.rayworks.vizit.v10.ui.components.drawEllipticalGradient
import hu.rayworks.vizit.v10.ui.components.noRippleClickable
import hu.rayworks.vizit.v10.ui.icons.VIcons
import hu.rayworks.vizit.v10.ui.theme.V
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

private val EaseInOut = CubicBezierEasing(0.42f, 0f, 0.58f, 1f)

/** Egy beolvasás eredménye. */
private sealed interface ScanResult {
    data class Contact(val name: String, val org: String, val title: String, val phone: String, val email: String) : ScanResult
    data class Web(val url: String) : ScanResult
    data object Unknown : ScanResult
    data object NotFound : ScanResult
}

private fun classify(text: String): ScanResult {
    val t = text.trim()
    if (t.startsWith("BEGIN:VCARD", ignoreCase = true)) {
        fun field(key: String) = t.lines().firstOrNull { it.uppercase().startsWith(key) }?.substringAfter(':')?.replace("\\,", ",")?.replace("\\;", ";")?.trim() ?: ""
        return ScanResult.Contact(field("FN"), field("ORG"), field("TITLE"), field("TEL"), field("EMAIL"))
    }
    if (t.startsWith("https://") && t.length <= 2048) return ScanResult.Web(t)
    return ScanResult.Unknown
}

/** Valódi QR-olvasás egy képtárból választott képről (ZXing). */
private suspend fun decodeFromImage(context: Context, uri: Uri): String? = withContext(Dispatchers.IO) {
    try {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        var sample = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= 1600) sample *= 2
        val bmp = context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
        } ?: return@withContext null
        val px = IntArray(bmp.width * bmp.height)
        bmp.getPixels(px, 0, bmp.width, 0, 0, bmp.width, bmp.height)
        val bitmap = BinaryBitmap(HybridBinarizer(RGBLuminanceSource(bmp.width, bmp.height, px)))
        QRCodeReader().decode(bitmap, mapOf(DecodeHintType.TRY_HARDER to true)).text
    } catch (e: Exception) {
        null
    }
}

/**
 * Névjegy beolvasása (éles QrScanScreen): sötét kameranézet, keresőkeret mozgó sugárral, vaku,
 * valamint valódi kamera- és képtári QR-felismerés. Képkockát nem tárolunk.
 */
@Composable
fun ScannerPage(app: AppState) {
    val actions = LocalActions.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var granted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED,
        )
    }
    var permissionAsked by remember { mutableStateOf(false) }
    var torch by remember { mutableStateOf(false) }
    var torchAvailable by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf<ScanResult?>(null) }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { allowed ->
        granted = allowed
        permissionAsked = true
    }
    val gallery = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) scope.launch {
            val text = decodeFromImage(context, uri)
            result = if (text == null) ScanResult.NotFound else classify(text)
        }
    }
    val pickImage = { gallery.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }
    BackHandler(enabled = result != null) { result = null }
    androidx.compose.runtime.LaunchedEffect(Unit) {
        if (!granted) permission.launch(Manifest.permission.CAMERA)
    }

    Box(
        Modifier
            .fillMaxSize()
            .clipToBounds()
            .drawBehind {
                drawRect(V.cam2)
                drawEllipticalGradient(size.width * 0.3f, size.height * 0.2f, size.width * 1.2f, size.height * 0.8f, 0f to V.cam1, 0.62f to V.cam2)
            }
            .noRippleClickable { }
    ) {
        if (granted && result == null) {
            LiveCameraPreview(
                torchOn = torch,
                onTorchAvailability = { torchAvailable = it },
                onScanned = { value -> if (result == null) result = classify(value) },
            )
        }
        Column(Modifier.fillMaxSize()) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.55f))
                    .statusBarsPadding()
                    .heightIn(min = 56.dp)
                    .padding(horizontal = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextLink("Bezárás", color = V.cyan) { app.pop() }
                Text("Beolvasás", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Medium, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
                IconCircleButton(
                    VIcons.flash,
                    if (torch) "Vaku kikapcsolása" else "Vaku bekapcsolása",
                    tint = if (torch) V.cyan else Color.White.copy(alpha = if (torchAvailable) 1f else 0.45f),
                ) { if (torchAvailable) torch = !torch }
            }
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                if (granted) {
                    Finder()
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(24.dp)) {
                        Box(Modifier.size(64.dp).clip(RoundedCornerShape(20.dp)).background(V.white10), contentAlignment = Alignment.Center) {
                            Icon(VIcons.camera, contentDescription = null, tint = Color.White, modifier = Modifier.size(30.dp))
                        }
                        Text("A kamera nincs engedélyezve", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Medium)
                        Text(
                            if (permissionAsked) "Engedélyezd a kamerát a rendszerben, vagy válassz képet a képtárból."
                            else "Képet nem készítünk és nem tárolunk.",
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center,
                        )
                        Box(
                            Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(V.cyan)
                                .clickable {
                                    if (permissionAsked) pickImage() else permission.launch(Manifest.permission.CAMERA)
                                }
                                .padding(horizontal = 18.dp, vertical = 10.dp)
                        ) {
                            Text(if (permissionAsked) "Kód a képtárból" else "Engedélyezés", color = V.h1, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                    .background(Color.Black.copy(alpha = 0.82f))
                    .navigationBarsPadding()
                    .padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 22.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Text("Irányítsd a kamerát a kódra", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Medium)
                Row(
                    Modifier
                        .clip(RoundedCornerShape(24.dp))
                        .background(V.white14)
                        .clickable(onClick = pickImage)
                        .padding(horizontal = 18.dp, vertical = 11.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(VIcons.image, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                    Text("Kód a képtárból", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                }
            }
        }

        // eredmény lap
        AnimatedVisibility(result != null, enter = fadeIn(tween(200)), exit = fadeOut(tween(200))) {
            Box(Modifier.fillMaxSize().background(V.scrim).noRippleClickable { result = null })
        }
        var last by remember { mutableStateOf<ScanResult?>(null) }
        if (result != null) last = result
        AnimatedVisibility(
            result != null,
            modifier = Modifier.align(Alignment.BottomCenter),
            enter = slideInVertically(tween(320)) { it },
            exit = slideOutVertically(tween(320)) { it },
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                    .background(V.bg)
                    .noRippleClickable { }
                    .navigationBarsPadding()
                    .padding(start = 20.dp, end = 20.dp, top = 22.dp, bottom = 20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                when (val r = last) {
                    is ScanResult.Contact -> {
                        Text("Beolvasott névjegy", fontSize = 20.sp, fontWeight = FontWeight.Medium)
                        Column(
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(20.dp))
                                .background(V.surface)
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Text(r.name, fontSize = 17.sp, fontWeight = FontWeight.Medium)
                            val sub = listOf(r.title, r.org).filter { it.isNotBlank() }.joinToString(" · ")
                            if (sub.isNotEmpty()) Text(sub, fontSize = 13.sp, color = V.sub)
                            Spacer(Modifier.height(6.dp))
                            if (r.phone.isNotBlank()) Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(VIcons.phone, contentDescription = null, tint = V.sub, modifier = Modifier.size(16.dp))
                                Text(r.phone, fontSize = 14.sp)
                            }
                            if (r.email.isNotBlank()) Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(VIcons.mail, contentDescription = null, tint = V.sub, modifier = Modifier.size(16.dp))
                                Text(r.email, fontSize = 14.sp)
                            }
                        }
                        PrimaryButton("Mentés a kapcsolatokhoz", icon = VIcons.contact) {
                            result = null
                            actions.insertContact(r.name, r.org, r.title, r.phone, r.email)
                        }
                        TextLink("Mégse", Modifier.align(Alignment.CenterHorizontally)) { result = null }
                    }
                    is ScanResult.Web -> {
                        Text("Webcím a QR-kódban", fontSize = 20.sp, fontWeight = FontWeight.Medium)
                        Text(r.url, fontSize = 14.sp, color = V.sub, maxLines = 3, overflow = TextOverflow.Ellipsis)
                        PrimaryButton("Megnyitás", icon = VIcons.ext) {
                            result = null
                            actions.openUrl(r.url)
                        }
                        TextLink("Mégse", Modifier.align(Alignment.CenterHorizontally)) { result = null }
                    }
                    ScanResult.Unknown, ScanResult.NotFound, null -> {
                        Text(if (r == ScanResult.NotFound) "Nincs QR-kód" else "Ismeretlen tartalom", fontSize = 20.sp, fontWeight = FontWeight.Medium)
                        Text(
                            if (r == ScanResult.NotFound) "Ezen a képen nem találtunk beolvasható QR-kódot." else "Nem támogatott névjegy-QR vagy HTTPS-webcím.",
                            fontSize = 14.sp, color = V.sub,
                        )
                        PrimaryButton("Rendben") { result = null }
                    }
                }
            }
        }
    }
}

/** A ZIP keresőanimációja alatt futó CameraX előnézet és memóriabeli QR-elemzés. */
@Composable
private fun LiveCameraPreview(
    torchOn: Boolean,
    onTorchAvailability: (Boolean) -> Unit,
    onScanned: (String) -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentOnScanned by rememberUpdatedState(onScanned)
    val currentOnTorchAvailability by rememberUpdatedState(onTorchAvailability)
    val executor = remember { Executors.newSingleThreadExecutor() }
    val decoder = remember { QrFrameDecoder() }
    val delivered = remember { AtomicBoolean(false) }
    var camera by remember { mutableStateOf<androidx.camera.core.Camera?>(null) }
    val previewView = remember {
        PreviewView(context).apply {
            scaleType = PreviewView.ScaleType.FILL_CENTER
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
        }
    }

    DisposableEffect(lifecycleOwner) {
        val future = ProcessCameraProvider.getInstance(context)
        future.addListener({
            val provider = runCatching { future.get() }.getOrNull() ?: return@addListener
            val preview = Preview.Builder().build().also { it.setSurfaceProvider(previewView.surfaceProvider) }
            val analysis = ImageAnalysis.Builder()
                .setResolutionSelector(
                    ResolutionSelector.Builder()
                        .setResolutionStrategy(
                            ResolutionStrategy(
                                AndroidSize(1280, 720),
                                ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER,
                            ),
                        )
                        .build(),
                )
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
            analysis.setAnalyzer(executor) { image ->
                try {
                    if (!delivered.get()) {
                        image.planes.firstOrNull()?.let { plane ->
                            val buffer = plane.buffer
                            val bytes = ByteArray(buffer.remaining())
                            buffer.get(bytes)
                            val value = decoder.decodeLuminance(
                                data = bytes,
                                width = image.width,
                                height = image.height,
                                rowStride = plane.rowStride,
                            )
                            if (value != null && delivered.compareAndSet(false, true)) {
                                previewView.post { currentOnScanned(value) }
                            }
                        }
                    }
                } catch (_: Throwable) {
                    // A következő képkocka automatikusan érkezik.
                } finally {
                    image.close()
                }
            }
            runCatching {
                provider.unbindAll()
                camera = provider.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    analysis,
                )
                currentOnTorchAvailability(camera?.cameraInfo?.hasFlashUnit() == true)
            }
        }, ContextCompat.getMainExecutor(context))

        onDispose {
            runCatching { future.get()?.unbindAll() }
            executor.shutdown()
        }
    }

    androidx.compose.runtime.LaunchedEffect(torchOn, camera) {
        camera?.takeIf { it.cameraInfo.hasFlashUnit() }?.cameraControl?.enableTorch(torchOn)
    }
    AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())
}

/** 232 dp-es keresőkeret: négy 44 dp-es, 4 dp vastag, 18 dp-es sarokív + sugár (2,4 s, oda-vissza). */
@Composable
private fun Finder(modifier: Modifier = Modifier) {
    val t = rememberInfiniteTransition(label = "beam")
    val p by t.animateFloat(0.18f, 0.82f, infiniteRepeatable(tween(2400, easing = EaseInOut), RepeatMode.Reverse), label = "beamY")
    Canvas(modifier.size(232.dp)) {
        val sw = 4.dp.toPx()
        val arm = 44.dp.toPx()
        val r = 18.dp.toPx() - sw / 2f
        val h = sw / 2f
        val corner = Path().apply {
            moveTo(h, arm)
            lineTo(h, h + r)
            arcTo(Rect(h, h, h + 2 * r, h + 2 * r), 180f, 90f, false)
            lineTo(arm, h)
        }
        repeat(4) { k -> rotate(90f * k) { drawPath(corner, Color.White, style = Stroke(width = sw, cap = StrokeCap.Butt)) } }
        val inset = 16.dp.toPx()
        val y = size.height * p
        val glow = 14.dp.toPx()
        val line = 2.dp.toPx()
        drawRect(
            Brush.verticalGradient(0f to V.cyan.copy(alpha = 0f), 0.5f to V.cyan.copy(alpha = 0.55f), 1f to V.cyan.copy(alpha = 0f), startY = y - glow, endY = y + line + glow),
            topLeft = Offset(inset - glow / 2f, y - glow),
            size = Size(size.width - 2 * inset + glow, line + 2 * glow),
        )
        drawRect(V.cyan, topLeft = Offset(inset, y), size = Size(size.width - 2 * inset, line))
    }
}
