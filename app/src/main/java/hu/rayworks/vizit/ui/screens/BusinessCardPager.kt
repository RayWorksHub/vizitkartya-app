package hu.rayworks.vizit.ui.screens

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import hu.rayworks.vizit.R
import hu.rayworks.vizit.data.card.visibleThrough
import hu.rayworks.vizit.data.cards.OwnedBusinessCard
import hu.rayworks.vizit.qr.QrCodeGenerator
import hu.rayworks.vizit.qr.QrPayloadFactory
import hu.rayworks.vizit.ui.design.Vizit
import hu.rayworks.vizit.ui.design.components.accentColor
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

/**
 * V10 B-layout stack from the supplied prototype. It renders only cards that
 * already passed the repository's active-owner checks.
 */
@Composable
internal fun BusinessCardPager(
    cards: List<OwnedBusinessCard>,
    activeProfileId: String?,
    onSelect: (String) -> Unit,
    onOpenCard: () -> Unit,
    onCreateCard: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (cards.isEmpty()) return
    val initialPage = cards.indexOfFirst { it.profileId == activeProfileId }.coerceAtLeast(0)
    val pagerState = rememberPagerState(initialPage = initialPage) { cards.size + 1 }
    val scope = rememberCoroutineScope()
    var fullScreenQr by remember { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(cards.map { it.profileId }, activeProfileId) {
        val selectedIndex = cards.indexOfFirst { it.profileId == activeProfileId }
        if (selectedIndex >= 0 && pagerState.currentPage != selectedIndex) {
            pagerState.scrollToPage(selectedIndex)
        }
    }
    LaunchedEffect(pagerState, cards.map { it.profileId }) {
        snapshotFlow { pagerState.settledPage }
            .distinctUntilChanged()
            .collect { page ->
                if (page in cards.indices) {
                    onSelect(cards[page].profileId)
                }
            }
    }

    Column(
        modifier = modifier.fillMaxWidth(),
    ) {
        HorizontalPager(
            state = pagerState,
            key = { page -> cards.getOrNull(page)?.profileId ?: "create-business-card" },
            contentPadding = PaddingValues(start = 28.dp, end = 28.dp, top = 6.dp, bottom = 20.dp),
            pageSpacing = 12.dp,
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top,
        ) { page ->
            val card = cards.getOrNull(page)
            if (card != null) {
                V10QrProfileCard(
                    card = card,
                    onOpenCard = onOpenCard,
                    onQr = { bitmap -> fullScreenQr = bitmap },
                )
            } else {
                V10CreateBusinessCardTile(
                    template = cards.first(),
                    onClick = onCreateCard,
                )
            }
        }

        V10PagerDots(count = cards.size, selected = pagerState.currentPage) { page ->
            scope.launch { pagerState.animateScrollToPage(page) }
        }
    }

    fullScreenQr?.let { bitmap ->
        FullScreenQrDialog(bitmap = bitmap, onDismiss = { fullScreenQr = null })
    }
}

@Composable
private fun V10QrProfileCard(
    card: OwnedBusinessCard,
    onOpenCard: () -> Unit,
    onQr: (Bitmap) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = Vizit.colors
    val context = androidx.compose.ui.platform.LocalContext.current
    val logo = remember(context) { BitmapFactory.decodeResource(context.resources, R.drawable.vizit_logo_mark) }
    val shared = remember(card.profile, card.presentation) { card.profile.visibleThrough(card.presentation) }
    val payload = remember(shared, card.updatedAt) {
        QrPayloadFactory.profileUrl(shared, card.updatedAt.isNotBlank())
            ?: QrPayloadFactory.contact(shared).getOrNull()
    }
    val qr = remember(payload, logo) {
        payload?.let { runCatching { QrCodeGenerator.create(it, sizePx = 720, logo = logo) }.getOrNull() }
    }
    val shape = RoundedCornerShape(28.dp)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.surface, shape)
            .border(1.dp, colors.border, shape)
            .clickable(role = Role.Button, onClick = onOpenCard)
            .padding(start = 18.dp, end = 18.dp, top = 14.dp, bottom = 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                Modifier
                    .weight(1f, fill = false)
                    .background(colors.skeletonBase, RoundedCornerShape(999.dp))
                    .padding(start = 9.dp, end = 11.dp, top = 5.dp, bottom = 5.dp),
                horizontalArrangement = Arrangement.spacedBy(7.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.size(9.dp).background(card.presentation.colorway.accentColor, CircleShape))
                Text(
                    text = card.profile.company.ifBlank { "Személyes" },
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (card.isPrimary) {
                Text(
                    text = "ELSŐDLEGES",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.primary,
                    modifier = Modifier
                        .padding(start = 8.dp)
                        .background(colors.primarySubtle, RoundedCornerShape(6.dp))
                        .padding(horizontal = 7.dp, vertical = 3.dp),
                )
            }
        }
        Box(
            Modifier
                .width(184.dp)
                .height(184.dp)
                .background(Color.White, RoundedCornerShape(12.dp))
                .clickable(enabled = qr != null) { qr?.let(onQr) }
                .padding(4.dp),
            contentAlignment = Alignment.Center,
        ) {
            if (qr != null) {
                Image(
                    bitmap = qr.asImageBitmap(),
                    contentDescription = "QR-kód teljes képernyőn",
                    modifier = Modifier.fillMaxWidth(),
                )
            } else {
                Icon(
                    imageVector = Icons.Outlined.Add,
                    contentDescription = null,
                    tint = Color(0xFF0B1330),
                    modifier = Modifier.size(44.dp),
                )
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = card.profile.resolvedDisplayName,
                fontSize = 17.sp,
                fontWeight = FontWeight.Medium,
                color = colors.textPrimary,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = if (card.profile.isPublic && card.profile.publicSlug.isNotBlank()) {
                    "vizitkartyam.hu/${card.profile.publicSlug}"
                } else card.profile.email.ifBlank { card.profile.phone },
                fontSize = 13.sp,
                color = colors.textMuted,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun V10CreateBusinessCardTile(template: OwnedBusinessCard, onClick: () -> Unit) {
    Box {
        V10QrProfileCard(
            card = template,
            onOpenCard = {},
            onQr = {},
            modifier = Modifier.alpha(0f).clearAndSetSemantics { },
        )
        Column(
            modifier = Modifier
                .matchParentSize()
                .v10DashedBorder(color = Vizit.colors.borderStrong, radius = 28.dp)
                .clickable(role = Role.Button, onClick = onClick)
                .padding(horizontal = 18.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterVertically),
        ) {
            Box(
                Modifier
                    .size(64.dp)
                    .background(Vizit.colors.primarySubtle, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Outlined.Add,
                    contentDescription = null,
                    tint = Vizit.colors.primary,
                    modifier = Modifier.size(30.dp),
                )
            }
            Text(
                text = "Új profil",
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = Vizit.colors.textPrimary,
            )
            Text(
                text = "Létrehozás",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = Vizit.colors.primary,
                modifier = Modifier
                    .background(Vizit.colors.primarySubtle, RoundedCornerShape(999.dp))
                    .padding(horizontal = 14.dp, vertical = 9.dp),
            )
        }
    }
}

@Composable
private fun V10PagerDots(count: Int, selected: Int, onPick: (Int) -> Unit) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(count + 1) { index ->
            val current = index == selected
            val add = index == count
            val width by animateDpAsState(if (current) 22.dp else 8.dp, tween(200), label = "dotWidth")
            val color by animateColorAsState(
                targetValue = when {
                    current -> Vizit.colors.textPrimary
                    add -> Color.Transparent
                    else -> Vizit.colors.borderStrong
                },
                animationSpec = tween(200),
                label = "dotColor",
            )
            Box(
                Modifier
                    .height(20.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        role = Role.Button,
                    ) { onPick(index) }
                    .padding(horizontal = 3.dp),
                contentAlignment = Alignment.Center,
            ) {
                val shape = RoundedCornerShape(4.dp)
                Box(
                    Modifier
                        .width(width)
                        .height(8.dp)
                        .background(color, shape)
                        .then(
                            if (add && !current) Modifier.border(1.5.dp, Vizit.colors.borderStrong, shape)
                            else Modifier,
                        ),
                )
            }
        }
    }
}

private fun Modifier.v10DashedBorder(color: Color, radius: androidx.compose.ui.unit.Dp): Modifier =
    drawWithCache {
        val width = 2.dp.toPx()
        val stroke = Stroke(
            width = width,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 5.dp.toPx())),
        )
        val corner = CornerRadius(radius.toPx(), radius.toPx())
        onDrawBehind {
            drawRoundRect(
                color = color,
                topLeft = Offset(width / 2, width / 2),
                size = Size(size.width - width, size.height - width),
                cornerRadius = corner,
                style = stroke,
            )
        }
    }
