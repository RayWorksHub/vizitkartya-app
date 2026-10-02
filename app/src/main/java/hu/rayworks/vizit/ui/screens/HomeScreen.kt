package hu.rayworks.vizit.ui.screens

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.StartOffsetType
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Nfc
import androidx.compose.material.icons.outlined.QrCode2
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.BusinessCenter
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import hu.rayworks.vizit.NfcStatus
import hu.rayworks.vizit.data.ContactProfile
import hu.rayworks.vizit.data.AppFeatureFlags
import hu.rayworks.vizit.data.card.CardPresentation
import hu.rayworks.vizit.data.cards.OwnedBusinessCard
import hu.rayworks.vizit.data.sync.ProfileSyncState
import hu.rayworks.vizit.data.sync.ProfileSyncStatus
import hu.rayworks.vizit.ui.design.Vizit
import hu.rayworks.vizit.ui.design.components.VizitIdentityChip
import hu.rayworks.vizit.ui.design.components.VizitLargeTitle
import hu.rayworks.vizit.ui.design.components.VizitButton
import hu.rayworks.vizit.ui.design.components.VizitButtonStyle
import hu.rayworks.vizit.ui.design.components.VizitBanner
import hu.rayworks.vizit.ui.design.components.VizitIconChip
import hu.rayworks.vizit.ui.design.components.VizitRow
import hu.rayworks.vizit.ui.design.components.VizitStatusPill
import hu.rayworks.vizit.ui.design.components.VizitTone
import hu.rayworks.vizit.ui.design.components.VizitGroup
import hu.rayworks.vizit.ui.util.rememberProfilePhoto
import kotlinx.coroutines.launch

/** V10 default B layout, connected to the production ViewModel callbacks. */
@Composable
fun HomeScreen(
    profile: ContactProfile,
    nfcStatus: NfcStatus,
    syncState: ProfileSyncState,
    onStartNfcShare: () -> String?,
    onOpenCard: () -> Unit,
    onOpenMenu: () -> Unit = onOpenCard,
    onOpenProfile: () -> Unit = onOpenCard,
    onOpenShare: () -> Unit,
    onOpenKnowledgeHub: () -> Unit,
    onOpenAnalytics: () -> Unit,
    onOpenCRM: () -> Unit,
    onOpenOnlineEditor: () -> Unit,
    onShareAsText: (Context) -> Unit,
    featureFlags: AppFeatureFlags = AppFeatureFlags(),
    modifier: Modifier = Modifier,
    onOpenScanner: () -> Unit = {},
    presentation: CardPresentation = CardPresentation(),
    businessCards: List<OwnedBusinessCard> = emptyList(),
    activeBusinessCardId: String? = null,
    onSelectBusinessCard: (String) -> Unit = {},
    onCreateBusinessCard: () -> Unit = {},
) {
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val hasBanner = syncState.status == ProfileSyncStatus.CONFLICT ||
        syncState.status == ProfileSyncStatus.RETRY_SCHEDULED
    val hasNfc = nfcStatus.isAvailable && nfcStatus.hasHostCardEmulation
    val centerIndex = 1 + (if (hasBanner) 1 else 0) + (if (hasNfc) 1 else 0)

    BoxWithConstraints(
        modifier
            .fillMaxSize()
            .background(Vizit.colors.canvas)
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .heightIn(min = maxHeight)
                .padding(bottom = 18.dp),
            verticalArrangement = V10CenterItemArrangement(16.dp, centerIndex),
        ) {
            V10HomeHeader(profile = profile, onClick = onOpenMenu, modifier = Modifier.padding(horizontal = 16.dp))

            if (hasBanner) {
                VizitBanner(
                    text = if (syncState.status == ProfileSyncStatus.CONFLICT) {
                        "A szinkron ütközött"
                    } else {
                        "A szinkron újrapróbálásra vár"
                    },
                    tone = if (syncState.status == ProfileSyncStatus.CONFLICT) VizitTone.Error else VizitTone.Warning,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }

            if (hasNfc) {
                V10NfcStrip(
                    profileLabel = businessCards.firstOrNull { it.profileId == activeBusinessCardId }
                        ?.profile?.company?.ifBlank { "Személyes" }
                        ?: profile.company.ifBlank { "Személyes" },
                    ready = nfcStatus.isReady,
                    onEnable = {
                        runCatching {
                            context.startActivity(Intent(Settings.ACTION_NFC_SETTINGS))
                        }.onFailure {
                            scope.launch { snackbar.showSnackbar("Az NFC-beállítások nem nyithatók meg ezen a készüléken.") }
                        }
                    },
                    onClick = {
                        onStartNfcShare()?.let { message ->
                            scope.launch { snackbar.showSnackbar(message) }
                        }
                    },
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }

            if (businessCards.isNotEmpty()) {
                BusinessCardPager(
                    cards = businessCards,
                    activeProfileId = activeBusinessCardId,
                    onSelect = onSelectBusinessCard,
                    onOpenCard = onOpenCard,
                    onCreateCard = onCreateBusinessCard,
                )
            } else {
                Box(Modifier.padding(horizontal = 28.dp)) {
                    ProfileCard(profile = profile, presentation = presentation)
                }
            }

            Column(
                Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                VizitButton(
                    text = "Megosztás",
                    onClick = onOpenShare,
                    icon = Icons.Outlined.Share,
                    enabled = profile.resolvedDisplayName.isNotBlank(),
                    modifier = Modifier.fillMaxWidth(),
                )
                VizitButton(
                    text = "Profil megnyitása",
                    onClick = onOpenProfile,
                    style = VizitButtonStyle.Secondary,
                    enabled = profile.resolvedDisplayName.isNotBlank(),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        SnackbarHost(
            hostState = snackbar,
            modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp),
        )
    }
}

@Composable
private fun V10HomeHeader(profile: ContactProfile, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val photo = rememberProfilePhoto(profile.photoBase64)
    Row(
        modifier.fillMaxWidth().heightIn(min = 58.dp).padding(top = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = "VIZIT",
            fontSize = 22.sp,
            lineHeight = 22.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.18.em,
            color = Vizit.colors.textPrimary,
        )
        Spacer(Modifier.weight(1f))
        Box(
            Modifier
                .size(51.dp)
                .background(Vizit.colors.border, CircleShape)
                .padding(1.5.dp)
                .background(Vizit.colors.surface, CircleShape)
                .padding(2.dp)
                .clip(CircleShape)
                .clickable(role = Role.Button, onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            if (photo != null) {
                Image(
                    bitmap = photo,
                    contentDescription = "Menü",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                Text(
                    text = profile.initials.ifBlank { "V" },
                    style = Vizit.type.label,
                    color = Vizit.colors.primary,
                )
            }
        }
    }
}

private val V10NfcEaseOut = CubicBezierEasing(0f, 0f, 0.58f, 1f)

@Composable
private fun V10NfcStrip(
    profileLabel: String,
    ready: Boolean,
    onClick: () -> Unit,
    onEnable: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = Vizit.colors
    val shape = RoundedCornerShape(24.dp)
    Row(
        modifier
            .fillMaxWidth()
            .clip(shape)
            .background(if (ready) colors.nfcContainer else colors.surface)
            .then(if (ready) Modifier else Modifier.border(1.dp, colors.border, shape))
            .clickable(enabled = ready, role = Role.Button, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        V10NfcIcon(ready)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = if (ready) "NFC kész" else "NFC kikapcsolva",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = if (ready) colors.onNfcContainer else colors.textPrimary,
            )
            Text(
                text = if (ready) "$profileLabel profil" else "Kapcsold be a telefon beállításaiban",
                fontSize = 13.sp,
                color = if (ready) colors.onNfcContainer.copy(alpha = 0.85f) else colors.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (ready) {
            Icon(
                imageVector = Icons.Outlined.ChevronRight,
                contentDescription = "NFC-küldés indítása",
                tint = colors.onNfcContainer,
                modifier = Modifier.size(20.dp),
            )
        } else {
            Text(
                text = "Bekapcsolás",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = colors.onNfcContainer,
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(colors.nfcContainer)
                    .clickable(role = Role.Button, onClick = onEnable)
                    .padding(horizontal = 14.dp, vertical = 9.dp),
            )
        }
    }
}

@Composable
private fun V10NfcIcon(ready: Boolean) {
    Box(Modifier.size(46.dp), contentAlignment = Alignment.Center) {
        if (ready) {
            val transition = rememberInfiniteTransition(label = "nfcPing")
            val first by transition.animateFloat(
                initialValue = 0f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(tween(2200, easing = V10NfcEaseOut), RepeatMode.Restart),
                label = "nfcRingOne",
            )
            val second by transition.animateFloat(
                initialValue = 0f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    tween(2200, easing = V10NfcEaseOut),
                    RepeatMode.Restart,
                    initialStartOffset = StartOffset(1100, StartOffsetType.FastForward),
                ),
                label = "nfcRingTwo",
            )
            listOf(first, second).forEach { progress ->
                Box(
                    Modifier
                        .size(46.dp)
                        .graphicsLayer {
                            val scale = 1f + 0.75f * progress
                            scaleX = scale
                            scaleY = scale
                            alpha = 0.7f * (1f - progress)
                        }
                        .border(2.dp, Vizit.colors.primary, CircleShape),
                )
            }
        }
        Box(
            Modifier
                .size(46.dp)
                .background(if (ready) Vizit.colors.primaryFill else Vizit.colors.controlDisabled, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Outlined.Nfc,
                contentDescription = null,
                tint = if (ready) Vizit.colors.textOnBrand else Vizit.colors.textMuted,
                modifier = Modifier.size(22.dp),
            )
        }
    }
}

private class V10CenterItemArrangement(
    private val gap: Dp,
    private val centerIndex: Int,
) : Arrangement.Vertical {
    override val spacing: Dp get() = gap

    override fun Density.arrange(totalSize: Int, sizes: IntArray, outPositions: IntArray) {
        val gapPx = gap.roundToPx()
        val used = sizes.sum() + gapPx * (sizes.size - 1).coerceAtLeast(0)
        val free = (totalSize - used).coerceAtLeast(0)
        var y = 0
        sizes.forEachIndexed { index, size ->
            if (index == centerIndex) y += free / 2
            outPositions[index] = y
            y += size + gapPx
            if (index == centerIndex) y += free - free / 2
        }
    }
}

/**
 * Home answers four questions immediately: who is signed in, what their card
 * looks like, how to hand it over, and whether the phone is actually ready to
 * do it. One primary action, three shortcuts, then status.
 */
@Composable
private fun LegacyHomeScreen(
    profile: ContactProfile,
    nfcStatus: NfcStatus,
    syncState: ProfileSyncState,
    onStartNfcShare: () -> String?,
    onOpenCard: () -> Unit,
    onOpenShare: () -> Unit,
    onOpenKnowledgeHub: () -> Unit,
    onOpenAnalytics: () -> Unit,
    onOpenCRM: () -> Unit,
    onOpenOnlineEditor: () -> Unit,
    onShareAsText: (Context) -> Unit,
    featureFlags: AppFeatureFlags = AppFeatureFlags(),
    modifier: Modifier = Modifier,
    onOpenScanner: () -> Unit = {},
    presentation: CardPresentation = CardPresentation(),
    businessCards: List<OwnedBusinessCard> = emptyList(),
    activeBusinessCardId: String? = null,
    onSelectBusinessCard: (String) -> Unit = {},
    onCreateBusinessCard: () -> Unit = {},
) {
    val colors = Vizit.colors
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Box(modifier = modifier.fillMaxSize().background(colors.canvas)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(horizontal = Vizit.space.md),
            verticalArrangement = Arrangement.spacedBy(Vizit.space.xl),
        ) {
            Spacer(Modifier.height(Vizit.space.xs))

            VizitLargeTitle(title = "VIZIT", letterSpacing = 4.0) {
                VizitIdentityChip(
                    initials = profile.initials,
                    photoBase64 = profile.photoBase64,
                    displayName = profile.resolvedDisplayName,
                    onClick = onOpenCard,
                )
            }

            if (businessCards.isNotEmpty()) {
                BusinessCardPager(
                    cards = businessCards,
                    activeProfileId = activeBusinessCardId,
                    onSelect = onSelectBusinessCard,
                    onOpenCard = onOpenCard,
                    onCreateCard = onCreateBusinessCard,
                )
            } else {
                Box(modifier = Modifier.clickable(role = Role.Button, onClick = onOpenCard)) {
                    ProfileCard(profile = profile, presentation = presentation)
                }
            }

            VizitButton(
                text = "Névjegy megosztása",
                onClick = onOpenShare,
                icon = Icons.Outlined.Share,
                modifier = Modifier.fillMaxWidth(),
            )

            if (featureFlags.qrScanner) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(Vizit.space.sm),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(Vizit.space.sm),
                    ) {
                        QuickTile(
                            icon = Icons.Outlined.Nfc,
                            title = "NFC",
                            subtitle = "Érintéssel",
                            enabled = nfcStatus.isReady,
                            modifier = Modifier.weight(1f),
                        ) {
                            onStartNfcShare()?.let { message ->
                                scope.launch { snackbarHostState.showSnackbar(message) }
                            }
                        }
                        QuickTile(
                            icon = Icons.Outlined.QrCode2,
                            title = "QR-kód",
                            subtitle = "Mutatás",
                            modifier = Modifier.weight(1f),
                            onClick = onOpenShare,
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(Vizit.space.sm),
                    ) {
                        QuickTile(
                            icon = Icons.Outlined.QrCodeScanner,
                            title = "Beolvasás",
                            subtitle = "Új kapcsolat",
                            modifier = Modifier.weight(1f),
                            onClick = onOpenScanner,
                        )
                        QuickTile(
                            icon = Icons.Outlined.ContentCopy,
                            title = "Egyéb",
                            subtitle = "Megosztás",
                            modifier = Modifier.weight(1f),
                        ) { onShareAsText(context) }
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Vizit.space.sm),
                ) {
                    QuickTile(
                        icon = Icons.Outlined.Nfc,
                        title = "NFC",
                        subtitle = "Érintéssel",
                        enabled = nfcStatus.isReady,
                        modifier = Modifier.weight(1f),
                    ) {
                        onStartNfcShare()?.let { message ->
                            scope.launch { snackbarHostState.showSnackbar(message) }
                        }
                    }
                    QuickTile(
                        icon = Icons.Outlined.QrCode2,
                        title = "QR-kód",
                        subtitle = "Mutatás",
                        modifier = Modifier.weight(1f),
                        onClick = onOpenShare,
                    )
                    QuickTile(
                        icon = Icons.Outlined.ContentCopy,
                        title = "Egyéb",
                        subtitle = "Megosztás",
                        modifier = Modifier.weight(1f),
                    ) { onShareAsText(context) }
                }
            }

            VizitStatusPill(
                text = if (nfcStatus.isReady) "NFC használatra kész" else "NFC beállítás szükséges",
                tone = if (nfcStatus.isReady) VizitTone.Success else VizitTone.Warning,
            )

            if (syncState.status == ProfileSyncStatus.RETRY_SCHEDULED ||
                syncState.status == ProfileSyncStatus.CONFLICT
            ) {
                VizitStatusPill(
                    text = if (syncState.status == ProfileSyncStatus.CONFLICT) {
                        "A szinkron ütközött"
                    } else {
                        "A szinkron újrapróbálásra vár"
                    },
                    tone = if (syncState.status == ProfileSyncStatus.CONFLICT) {
                        VizitTone.Error
                    } else {
                        VizitTone.Warning
                    },
                )
            }

            VizitGroup {
                if (featureFlags.businessPortal) {
                    VizitRow(
                        label = "Vállalkozói Portál",
                        supporting = "VOSZ, edukáció, digitális segítség és eszköztár",
                        icon = Icons.AutoMirrored.Outlined.MenuBook,
                        onClick = onOpenKnowledgeHub,
                    )
                }
                if (featureFlags.analytics) {
                    VizitRow(
                        label = "Statisztikák",
                        supporting = "Profilmegtekintés, mentések és kattintások · 30 nap",
                        icon = Icons.Outlined.BarChart,
                        onClick = onOpenAnalytics,
                    )
                }
                if (featureFlags.crm) {
                    VizitRow(
                        label = "CRM",
                        supporting = "Partnerek, ügyletek, feladatok és ajánlatok a webes munkatérben",
                        icon = Icons.Outlined.BusinessCenter,
                        onClick = onOpenCRM,
                    )
                }
                if (featureFlags.onlineEditor) {
                    VizitRow(
                        label = "Online névjegy szerkesztése",
                        supporting = "A nyilvános profil színei, logója és közösségi hivatkozásai",
                        icon = Icons.Outlined.Edit,
                        onClick = onOpenOnlineEditor,
                    )
                }
            }

            Spacer(Modifier.height(Vizit.space.md))
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(Vizit.space.md),
        )
    }
}

@Composable
private fun QuickTile(
    icon: ImageVector,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    val colors = Vizit.colors
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(Vizit.radius.lg))
            .background(colors.surface, RoundedCornerShape(Vizit.radius.lg))
            .border(1.dp, colors.border, RoundedCornerShape(Vizit.radius.lg))
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = Vizit.space.sm, vertical = Vizit.space.md),
        verticalArrangement = Arrangement.spacedBy(Vizit.space.xs + 2.dp),
    ) {
        VizitIconChip(
            icon = icon,
            tint = if (enabled) colors.primary else colors.textDisabled,
            background = if (enabled) colors.primarySubtle else colors.controlDisabled,
        )
        Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
            // Four tiles share one row on a phone, so a long label shrinks
            // rather than wrapping the tile out of alignment with its siblings.
            Text(
                text = title,
                style = Vizit.type.label,
                color = if (enabled) colors.textPrimary else colors.textDisabled,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = subtitle,
                style = Vizit.type.caption,
                color = colors.textMuted,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
