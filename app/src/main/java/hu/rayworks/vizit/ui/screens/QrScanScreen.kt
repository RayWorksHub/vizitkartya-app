package hu.rayworks.vizit.ui.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.ContactsContract
import android.util.Size
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FlashlightOff
import androidx.compose.material.icons.outlined.FlashlightOn
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size as ComposeSize
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.foundation.Canvas
import androidx.compose.ui.viewinterop.AndroidView
import hu.rayworks.vizit.nfc.VCardFields
import hu.rayworks.vizit.qr.QrFrameDecoder
import hu.rayworks.vizit.ui.design.Vizit
import hu.rayworks.vizit.ui.design.components.VizitEmptyState
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

private val ScannerAccent = Color(0xFF0FBEE6)
private val ScannerChrome = Color(0xFF0A0F1C)

/**
 * Névjegy beolvasása — the designed viewfinder: a dimmed surround with a
 * cyan-bracketed aperture and a sweeping line, so it is obvious where the code
 * has to go and that the camera is live.
 *
 * Nothing is captured or stored: frames are analysed in memory and dropped.
 * A scanned web address is never opened on its own; the owner confirms first.
 */
@Composable
fun QrScanScreen(
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var granted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED,
        )
    }
    var permissionAsked by remember { mutableStateOf(false) }
    var scanned by remember { mutableStateOf<String?>(null) }
    var message by remember { mutableStateOf<String?>(null) }
    var torchOn by remember { mutableStateOf(false) }
    var torchAvailable by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { allowed ->
        granted = allowed
        permissionAsked = true
    }

    val pickLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent(),
    ) { uri: Uri? ->
        if (uri != null) {
            val value = decodePickedImage(context, uri)
            if (value == null) {
                message = "Ezen a képen nem találtunk beolvasható QR-kódot."
            } else {
                scanned = value
            }
        }
    }

    LaunchedEffect(Unit) {
        if (!granted) permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    Box(modifier = modifier.fillMaxSize().background(ScannerChrome)) {
        if (granted) {
            CameraViewfinder(
                torchOn = torchOn,
                onTorchAvailability = { torchAvailable = it },
                onScanned = { value -> if (scanned == null) scanned = value },
            )
            ViewfinderOverlay()
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.statusBars)
                .background(Color.Black.copy(alpha = 0.55f))
                .padding(horizontal = Vizit.space.md),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 48.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Bezárás",
                    style = Vizit.type.button,
                    color = ScannerAccent,
                    modifier = Modifier
                        .defaultMinSize(minHeight = 44.dp)
                        .clickable(role = Role.Button) { torchOn = false; onClose() }
                        .padding(vertical = Vizit.space.sm),
                )
                Text(
                    text = "Beolvasás",
                    style = Vizit.type.title,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f),
                )
                if (torchAvailable) {
                    Icon(
                        imageVector = if (torchOn) {
                            Icons.Outlined.FlashlightOn
                        } else {
                            Icons.Outlined.FlashlightOff
                        },
                        contentDescription = if (torchOn) "Vaku kikapcsolása" else "Vaku bekapcsolása",
                        tint = if (torchOn) ScannerAccent else Color.White,
                        modifier = Modifier
                            .size(44.dp)
                            .clickable(role = Role.Button) { torchOn = !torchOn }
                            .padding(10.dp),
                    )
                }
            }
        }

        if (granted) {
            ScannerInstructions(
                modifier = Modifier.align(Alignment.BottomCenter),
                onPickImage = { pickLauncher.launch("image/*") },
            )
        } else {
            VizitEmptyState(
                icon = Icons.Outlined.Image,
                title = "A kamera nincs engedélyezve",
                message = if (permissionAsked) {
                    "A beolvasáshoz a rendszerbeállításokban engedélyezd a kamerát a VIZIT számára. " +
                        "Addig is beolvashatsz egy mentett képet a képtáradból."
                } else {
                    "A beolvasáshoz kameraengedély szükséges. Képet nem készítünk és nem tárolunk."
                },
                actionLabel = if (permissionAsked) "Kód kiválasztása a képtárból" else "Engedélyezés",
                onAction = {
                    if (permissionAsked) {
                        pickLauncher.launch("image/*")
                    } else {
                        permissionLauncher.launch(Manifest.permission.CAMERA)
                    }
                },
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(horizontal = Vizit.space.md),
            )
        }
    }

    scanned?.let { value ->
        ScanResultSheet(
            value = value,
            onDismiss = { scanned = null },
            onFinished = { torchOn = false; onClose() },
        )
    }

    message?.let { text ->
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { message = null },
            title = { Text("Beolvasás") },
            text = { Text(text) },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = { message = null }) { Text("Rendben") }
            },
        )
    }
}

/**
 * The live preview, bound to this composable's lifecycle and nothing else.
 * Only ever composed once the CAMERA permission is held.
 */
@Composable
private fun CameraViewfinder(
    torchOn: Boolean,
    onTorchAvailability: (Boolean) -> Unit,
    onScanned: (String) -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentOnScanned by rememberUpdatedState(onScanned)
    val currentOnTorch by rememberUpdatedState(onTorchAvailability)

    val analysisExecutor = remember { Executors.newSingleThreadExecutor() }
    val decoder = remember { QrFrameDecoder() }
    // One result per scanner session: the analyzer keeps running for a frame or
    // two after a hit, and a second callback would reopen a closed sheet.
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
            val preview = Preview.Builder().build()
                .also { it.setSurfaceProvider(previewView.surfaceProvider) }

            val analysis = ImageAnalysis.Builder()
                .setResolutionSelector(
                    ResolutionSelector.Builder()
                        .setResolutionStrategy(
                            ResolutionStrategy(
                                Size(1280, 720),
                                ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER,
                            ),
                        )
                        .build(),
                )
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()

            analysis.setAnalyzer(analysisExecutor) { image ->
                try {
                    if (!delivered.get()) {
                        val plane = image.planes.firstOrNull()
                        if (plane != null) {
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
                    // A single unreadable frame is not a failure; the next one follows.
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
                currentOnTorch(camera?.cameraInfo?.hasFlashUnit() == true)
            }
        }, ContextCompat.getMainExecutor(context))

        onDispose {
            runCatching { future.get()?.unbindAll() }
            analysisExecutor.shutdown()
        }
    }

    LaunchedEffect(torchOn, camera) {
        val device = camera ?: return@LaunchedEffect
        if (device.cameraInfo.hasFlashUnit()) device.cameraControl.enableTorch(torchOn)
    }

    AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())
}

/** The dimmed surround, the bracketed aperture and the sweeping line. */
@Composable
private fun ViewfinderOverlay() {
    val transition = rememberInfiniteTransition(label = "scan-sweep")
    val sweep by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1900), RepeatMode.Reverse),
        label = "scan-sweep-offset",
    )

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val side = minOf(232.dp, minOf(maxWidth, maxHeight) * 0.62f)

        Canvas(modifier = Modifier.fillMaxSize()) {
            val sidePx = side.toPx()
            val left = (size.width - sidePx) / 2f
            val top = (size.height - sidePx) / 2f
            val right = left + sidePx
            val bottom = top + sidePx
            val dim = Color.Black.copy(alpha = 0.55f)

            // The surround is dimmed with four plain rectangles rather than a
            // blend-mode cutout, so the aperture never punches through the
            // camera preview beneath this layer.
            drawRect(dim, Offset.Zero, ComposeSize(size.width, top))
            drawRect(dim, Offset(0f, bottom), ComposeSize(size.width, size.height - bottom))
            drawRect(dim, Offset(0f, top), ComposeSize(left, sidePx))
            drawRect(dim, Offset(right, top), ComposeSize(size.width - right, sidePx))

            val bracket = 34.dp.toPx()
            val strokeWidth = 4.dp.toPx()
            // Four corner brackets, each two strokes meeting at the corner.
            listOf(
                Triple(Offset(left, top + bracket), Offset(left, top), Offset(left + bracket, top)),
                Triple(Offset(right - bracket, top), Offset(right, top), Offset(right, top + bracket)),
                Triple(Offset(right, bottom - bracket), Offset(right, bottom), Offset(right - bracket, bottom)),
                Triple(Offset(left + bracket, bottom), Offset(left, bottom), Offset(left, bottom - bracket)),
            ).forEach { (from, corner, to) ->
                drawLine(ScannerAccent, from, corner, strokeWidth, StrokeCap.Round)
                drawLine(ScannerAccent, corner, to, strokeWidth, StrokeCap.Round)
            }

            val y = top + 6.dp.toPx() + sweep * (sidePx - 12.dp.toPx())
            drawLine(
                color = ScannerAccent,
                start = Offset(left + 4.dp.toPx(), y),
                end = Offset(right - 4.dp.toPx(), y),
                strokeWidth = 2.dp.toPx(),
            )
        }
    }
}

@Composable
private fun ScannerInstructions(modifier: Modifier = Modifier, onPickImage: () -> Unit) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.Black.copy(alpha = 0.82f))
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(horizontal = Vizit.space.lg, vertical = Vizit.space.lg),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Vizit.space.sm),
    ) {
        Text(
            text = "Irányítsd a kamerát a másik kódra",
            style = Vizit.type.h3,
            color = Color.White,
            textAlign = TextAlign.Center,
        )
        Text(
            text = "A beolvasott névjegyet elmentheted a kapcsolataid közé, és offline is " +
                "elérhető marad. Képet nem készítünk és nem tárolunk.",
            style = Vizit.type.bodySmall,
            color = Color.White.copy(alpha = 0.74f),
            textAlign = TextAlign.Center,
        )
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(Vizit.radius.full))
                .background(Color.White.copy(alpha = 0.14f))
                .clickable(role = Role.Button, onClick = onPickImage)
                .defaultMinSize(minHeight = 44.dp)
                .padding(horizontal = Vizit.space.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Vizit.space.xs),
        ) {
            Icon(
                imageVector = Icons.Outlined.Image,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(18.dp),
            )
            Text(
                text = "Kód kiválasztása a képtárból",
                style = Vizit.type.label,
                color = Color.White,
            )
        }
    }
}

/**
 * What a scanned code turns into. A vCard becomes a pre-filled contact the
 * owner confirms; a web address is shown in full before anything is opened.
 * Anything else is reported and nothing happens.
 */
@Composable
private fun ScanResultSheet(value: String, onDismiss: () -> Unit, onFinished: () -> Unit) {
    val context = LocalContext.current
    val trimmed = value.trim()
    val isVCard = trimmed.uppercase().startsWith("BEGIN:VCARD")
    val webUrl = trimmed.takeIf { it.startsWith("https://", ignoreCase = true) && it.length <= 2048 }

    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                when {
                    isVCard -> "Beolvasott névjegy"
                    webUrl != null -> "Webcím a QR-kódban"
                    else -> "Ismeretlen tartalom"
                },
            )
        },
        text = {
            Text(
                when {
                    isVCard -> "Mentés előtt a rendszer névjegyszerkesztőjében minden adatot ellenőrizhetsz."
                    webUrl != null -> webUrl
                    else -> "Ez nem támogatott névjegy-QR vagy HTTPS-webcím. Az alkalmazás nem végzett műveletet."
                },
            )
        },
        confirmButton = {
            when {
                isVCard -> androidx.compose.material3.TextButton(
                    onClick = {
                        openContactInsert(context, trimmed)
                        onDismiss()
                        onFinished()
                    },
                ) { Text("Mentés a kapcsolatokhoz") }

                webUrl != null -> androidx.compose.material3.TextButton(
                    onClick = {
                        runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(webUrl))) }
                        onDismiss()
                    },
                ) { Text("Megnyitás") }

                else -> androidx.compose.material3.TextButton(onClick = onDismiss) { Text("Rendben") }
            }
        },
        dismissButton = {
            if (isVCard || webUrl != null) {
                androidx.compose.material3.TextButton(onClick = onDismiss) { Text("Mégse") }
            }
        },
    )
}

/**
 * Hands the scanned card to the system contact editor, pre-filled. The app
 * never writes to the contact store itself, so no contacts permission is
 * needed and the owner always sees what is about to be saved.
 */
private fun openContactInsert(context: Context, vcard: String) {
    val fields = VCardFields.parse(vcard)
    val intent = Intent(ContactsContract.Intents.Insert.ACTION).apply {
        type = ContactsContract.RawContacts.CONTENT_TYPE
        fields["FN"]?.let { putExtra(ContactsContract.Intents.Insert.NAME, it) }
        fields["ORG"]?.let { putExtra(ContactsContract.Intents.Insert.COMPANY, it) }
        fields["TITLE"]?.let { putExtra(ContactsContract.Intents.Insert.JOB_TITLE, it) }
        fields["TEL"]?.let { putExtra(ContactsContract.Intents.Insert.PHONE, it) }
        fields["EMAIL"]?.let { putExtra(ContactsContract.Intents.Insert.EMAIL, it) }
        fields["ADR"]?.let { putExtra(ContactsContract.Intents.Insert.POSTAL, it) }
    }
    runCatching { context.startActivity(intent) }
}

private fun decodePickedImage(context: Context, uri: Uri): String? = runCatching {
    val options = BitmapFactory.Options().apply { inPreferredConfig = android.graphics.Bitmap.Config.ARGB_8888 }
    val bitmap = context.contentResolver.openInputStream(uri).use { stream ->
        BitmapFactory.decodeStream(stream, null, options)
    } ?: return@runCatching null
    QrFrameDecoder().decodeBitmap(bitmap).also { bitmap.recycle() }
}.getOrNull()
