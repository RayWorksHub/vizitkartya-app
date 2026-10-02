package hu.rayworks.vizit.v10.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import hu.rayworks.vizit.v10.data.AppState
import hu.rayworks.vizit.v10.data.NfcDevice
import hu.rayworks.vizit.v10.data.SheetKind
import hu.rayworks.vizit.v10.data.SyncStatus
import hu.rayworks.vizit.v10.ui.LocalActions
import hu.rayworks.vizit.v10.ui.components.OutlineButton
import hu.rayworks.vizit.v10.ui.components.PrimaryButton
import hu.rayworks.vizit.v10.ui.icons.VIcons

/**
 * B elrendezés (alapértelmezett): egyetlen képernyő.
 * Fejléc (VIZIT + profilkép → menü), állapotsáv ha kell, NFC sáv, a profil kártyák a maradék hely
 * közepén, alul Megosztás (→ megosztási lap) és Profil megnyitása.
 */
@Composable
fun HomeB(app: AppState) {
    val actions = LocalActions.current
    BoxWithConstraints(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        val side = Modifier.padding(horizontal = 16.dp)
        val hasBanner = app.offline || app.sync == SyncStatus.Conflict || app.sync == SyncStatus.Retry
        val hasNfc = app.nfcDevice != NfcDevice.Unsupported
        val center = 1 + (if (hasBanner) 1 else 0) + (if (hasNfc) 1 else 0)
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .heightIn(min = maxHeight)
                .padding(bottom = 18.dp),
            verticalArrangement = CenterItemArrangement(16.dp, centerIndex = center),
        ) {
            HomeHeader(app, side) { MeButton(app) { app.sheet = SheetKind.Menu } }
            if (hasBanner) StatusBanners(app, side)
            if (hasNfc) NfcStrip(app, side)
            ProfileStack(app)
            val cur = app.current
            Column(side, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                PrimaryButton("Megosztás", icon = VIcons.share, dimmed = cur == null) { app.sheet = SheetKind.Share }
                OutlineButton("Profil megnyitása", VIcons.ext, enabled = cur != null) { cur?.let { actions.openProfile(it) } }
            }
        }
    }
}
