package hu.rayworks.vizit.v10.ui.home

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import hu.rayworks.vizit.v10.data.AppState
import hu.rayworks.vizit.v10.data.HomeTab
import hu.rayworks.vizit.v10.data.SheetKind
import hu.rayworks.vizit.v10.ui.components.GroupCard
import hu.rayworks.vizit.v10.ui.components.GroupLabel
import hu.rayworks.vizit.v10.ui.components.IconCircleButton
import hu.rayworks.vizit.v10.ui.components.ListRow
import hu.rayworks.vizit.v10.ui.components.OutlineButton
import hu.rayworks.vizit.v10.ui.components.PrimaryButton
import hu.rayworks.vizit.v10.ui.components.ProfileLabel
import hu.rayworks.vizit.v10.ui.components.noRippleClickable
import hu.rayworks.vizit.v10.ui.icons.VIcons
import hu.rayworks.vizit.v10.data.CRM_URL
import hu.rayworks.vizit.v10.data.EDITOR_URL
import hu.rayworks.vizit.v10.data.Page
import hu.rayworks.vizit.v10.ui.LocalActions
import hu.rayworks.vizit.v10.ui.components.DigitalCard
import hu.rayworks.vizit.v10.ui.components.ExtMark
import hu.rayworks.vizit.v10.ui.pages.SettingsContent
import hu.rayworks.vizit.v10.ui.theme.V


/** A elrendezés: három fül (Megosztás · Profil · Továbbiak) alsó navigációval. */
@Composable
fun HomeA(app: AppState) {
    Column(Modifier.fillMaxSize()) {
        Box(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .statusBarsPadding()
        ) {
            when (app.tab) {
                HomeTab.Share -> ShareTab(app)
                HomeTab.Profile -> ProfileTab(app)
                HomeTab.More -> MoreTab(app)
            }
        }
        BottomNav(app)
    }
}

@Composable
private fun TabColumn(content: @Composable () -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) { content() }
}

/** Megosztás fül: VIZIT + beolvasás, állapotsáv, NFC sáv, kártyák, Megosztás gomb. */
@Composable
private fun ShareTab(app: AppState) {
    val side = Modifier.padding(horizontal = 16.dp)
    TabColumn {
        HomeHeader(app, side) {
            IconCircleButton(VIcons.scan, "Névjegy beolvasása", tint = V.ink) { app.push(Page.Scanner) }
        }
        StatusBanners(app, side)
        NfcStrip(app, side)
        ProfileStack(app)
        PrimaryButton("Megosztás", side, icon = VIcons.share, dimmed = app.current == null) { app.sheet = SheetKind.Share }
    }
}

/** Profil fül: digitális kártya, web profil, Profiljaid, Tartalom csoport. */
@Composable
private fun ProfileTab(app: AppState) {
    val actions = LocalActions.current
    val shown = app.focus
    val side = Modifier.padding(horizontal = 16.dp)
    TabColumn {
        HomeHeader(app, side, title = "Profil")
        Box(side.noRippleClickable { app.push(Page.Appearance) }) {
            DigitalCard(shown, app.contactVCard(shown))
        }
        Column(
            side
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(V.surface)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Web profilod", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = V.sub)
                ProfileLabel(shown.label, shown.color)
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(
                    Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(V.blueSoft),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(VIcons.link, contentDescription = null, tint = V.blue, modifier = Modifier.size(20.dp))
                }
                Text(shown.short, fontSize = 16.sp, fontWeight = FontWeight.Medium, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            OutlineButton("Profil megnyitása", VIcons.ext, enabled = app.current != null) { actions.openProfile(shown) }
        }
        GroupCard(side) {
            ListRow("Profiljaid", icon = VIcons.stack, sub = "${app.profiles.size} profil") { app.sheet = SheetKind.Profiles }
        }
        GroupLabel("Tartalom", side)
        GroupCard(side) {
            ListRow("Adatok", icon = VIcons.pencil) { app.openEdit(null) }
            ListRow("Mi látszik a profilon", icon = VIcons.eye, value = "${shown.visibleCount}/6", divider = true) { app.openEdit("lathatosag") }
            ListRow("Kártya megjelenése", icon = VIcons.palette, value = shown.preset.name, divider = true) { app.push(Page.Appearance) }
            ListRow("Online szerkesztő", icon = VIcons.globe, sub = "Színek, logó, közösségi linkek", divider = true, trailing = { ExtMark() }) {
                actions.openUrl(EDITOR_URL)
            }
        }
    }
}

/** Továbbiak fül: eszközök (Statisztikák, Portál, CRM), alatta a teljes Beállítások. */
@Composable
private fun MoreTab(app: AppState) {
    val actions = LocalActions.current
    val side = Modifier.padding(horizontal = 16.dp)
    TabColumn {
        HomeHeader(app, side, title = "Továbbiak")
        GroupCard(side) {
            ListRow("Statisztikák", icon = VIcons.chart, sub = "Megtekintések, mentések és kattintások") { app.push(Page.Analytics) }
            ListRow("Vállalkozói Portál", icon = VIcons.book, sub = "VOSZ, edukáció, digitális segítség", divider = true) { app.push(Page.Hub) }
            ListRow("CRM", icon = VIcons.users, sub = "Partnerek, ügyletek, feladatok", divider = true, trailing = { ExtMark() }) { actions.openUrl(CRM_URL) }
        }
        Column(side, verticalArrangement = Arrangement.spacedBy(14.dp)) {
            SettingsContent(app, includeNfc = true)
        }
    }
}

/** Material 3 alsó navigáció: 80 dp magas, a kijelölt fül ikonja 64×32-es pirulában. */
@Composable
private fun BottomNav(app: AppState) {
    val tabs: List<Triple<HomeTab, ImageVector, String>> = listOf(
        Triple(HomeTab.Share, VIcons.qr, "Megosztás"),
        Triple(HomeTab.Profile, VIcons.person, "Profil"),
        Triple(HomeTab.More, VIcons.more, "Továbbiak"),
    )
    Row(
        Modifier
            .fillMaxWidth()
            .background(V.nav)
            .navigationBarsPadding()
            .height(80.dp)
            .padding(start = 8.dp, end = 8.dp, top = 12.dp, bottom = 14.dp)
    ) {
        tabs.forEach { (tab, icon, label) ->
            val cur = app.tab == tab
            val pill by animateColorAsState(if (cur) V.indicator else Color.Transparent, tween(200), label = "tabPill")
            Column(
                Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .noRippleClickable { app.tab = tab },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Box(
                    Modifier
                        .size(64.dp, 32.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(pill),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(icon, contentDescription = null, tint = if (cur) V.onContainer else V.label, modifier = Modifier.size(22.dp))
                }
                Text(
                    label,
                    fontSize = 12.sp,
                    fontWeight = if (cur) FontWeight.Bold else FontWeight.Medium,
                    color = if (cur) V.ink else V.label,
                    maxLines = 1,
                )
            }
        }
    }
}
