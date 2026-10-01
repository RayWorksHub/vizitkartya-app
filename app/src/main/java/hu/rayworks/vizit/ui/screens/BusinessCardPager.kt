package hu.rayworks.vizit.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import hu.rayworks.vizit.data.cards.OwnedBusinessCard
import hu.rayworks.vizit.ui.design.Vizit
import kotlinx.coroutines.flow.distinctUntilChanged

/** Native horizontal swipe between the signed-in account's own cards. */
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
                } else if (page == cards.size) {
                    onCreateCard()
                    pagerState.scrollToPage(cards.lastIndex)
                }
            }
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Vizit.space.sm),
    ) {
        HorizontalPager(
            state = pagerState,
            key = { page -> cards.getOrNull(page)?.profileId ?: "create-business-card" },
            pageSpacing = Vizit.space.sm,
            modifier = Modifier.fillMaxWidth(),
        ) { page ->
            val card = cards.getOrNull(page)
            if (card != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(role = Role.Button, onClick = onOpenCard),
                ) {
                    ProfileCard(
                        profile = card.profile,
                        presentation = card.presentation,
                    )
                }
            } else {
                CreateBusinessCardTile(onClick = onCreateCard)
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            cards.indices.forEach { index ->
                val selected = pagerState.currentPage == index
                Box(
                    Modifier
                        .padding(horizontal = 3.dp)
                        .size(if (selected) 8.dp else 6.dp)
                        .background(
                            if (selected) Vizit.colors.primary else Vizit.colors.borderStrong,
                            CircleShape,
                        ),
                )
            }
            Text(
                text = "  ${cards.size} névjegy · húzd oldalra",
                style = Vizit.type.caption,
                color = Vizit.colors.textMuted,
            )
            TextButton(onClick = onCreateCard) {
                Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Text("Új")
            }
        }
    }
}

@Composable
private fun CreateBusinessCardTile(onClick: () -> Unit) {
    val shape = RoundedCornerShape(Vizit.radius.xl)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(343f / 216f)
            .background(Vizit.colors.surface, shape)
            .border(1.dp, Vizit.colors.borderStrong, shape)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(Vizit.space.lg),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = Icons.Outlined.Add,
            contentDescription = null,
            tint = Vizit.colors.primary,
            modifier = Modifier.size(32.dp),
        )
        Text(
            text = "Új névjegy létrehozása",
            style = Vizit.type.h3,
            color = Vizit.colors.textPrimary,
        )
        Text(
            text = "A meglévő névjegyeid megmaradnak.",
            style = Vizit.type.bodySmall,
            color = Vizit.colors.textMuted,
        )
    }
}
