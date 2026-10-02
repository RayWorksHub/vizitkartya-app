package hu.rayworks.vizit.v10.ui.pages

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import hu.rayworks.vizit.v10.data.APP_VERSION
import hu.rayworks.vizit.v10.data.AppState
import hu.rayworks.vizit.v10.data.DialogSpec
import hu.rayworks.vizit.v10.data.PRIVACY_URL
import hu.rayworks.vizit.v10.data.SyncStatus
import hu.rayworks.vizit.v10.data.TERMS_URL
import hu.rayworks.vizit.v10.ui.LocalActions
import hu.rayworks.vizit.v10.ui.components.Banner
import hu.rayworks.vizit.v10.ui.components.GroupCard
import hu.rayworks.vizit.v10.ui.components.GroupLabel
import hu.rayworks.vizit.v10.ui.components.ListRow
import hu.rayworks.vizit.v10.ui.components.OutlineButton
import hu.rayworks.vizit.v10.ui.components.PageScaffold
import hu.rayworks.vizit.v10.ui.components.Segmented
import hu.rayworks.vizit.v10.ui.components.Tone
import hu.rayworks.vizit.v10.ui.components.ExtMark
import hu.rayworks.vizit.v10.ui.components.VSwitch
import hu.rayworks.vizit.v10.ui.icons.VIcons
import hu.rayworks.vizit.v10.ui.sheets.NfcSwitchRow
import hu.rayworks.vizit.v10.ui.theme.ThemeMode
import hu.rayworks.vizit.v10.ui.theme.ThemeState
import hu.rayworks.vizit.v10.ui.theme.V

private fun syncText(s: SyncStatus, offline: Boolean): String = when {
    offline -> "Offline · a szinkron később folytatódik"
    s == SyncStatus.Synced -> "A helyi és a felhőprofil szinkronban van"
    s == SyncStatus.Pending -> "Felhőszinkronra vár"
    s == SyncStatus.Syncing -> "Felhőszinkron folyamatban"
    s == SyncStatus.Retry -> "A felhőszinkron átmenetileg nem érhető el"
    s == SyncStatus.Conflict -> "A helyi és a felhőprofil egyszerre módosult"
    else -> "Csak ezen az eszközön tárolva"
}

/**
 * A Beállítások tartalma (B: külön oldal a menüből, A: a Továbbiak fül alja).
 * Megjelenés (téma), Megosztás (NFC, csak A-ban – B-ben a menüben van), Szinkronizálás
 * (automatikus szinkron, újrapróbálás, ütközés feloldása), Saját adatok (export), Jogi, Fiók, verzió.
 */
@Composable
fun SettingsContent(app: AppState, includeNfc: Boolean) {
    val actions = LocalActions.current
    val exporter = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) actions.writeExport(uri)
    }

    GroupLabel("Megjelenés")
    GroupCard {
        ListRow("Téma", icon = VIcons.moon)
        Column(Modifier.padding(start = 14.dp, end = 14.dp, bottom = 14.dp)) {
            Segmented(listOf("Világos", "Sötét", "Rendszer"), ThemeState.mode.ordinal) { app.changeTheme(ThemeMode.entries[it]) }
        }
    }

    if (includeNfc) {
        GroupLabel("Megosztás")
        GroupCard { NfcSwitchRow(app, withStatus = true) }
    }

    GroupLabel("Szinkronizálás")
    GroupCard {
        ListRow(
            "Automatikus szinkron",
            icon = VIcons.cloud,
            sub = syncText(app.sync, app.offline),
            trailing = { VSwitch(app.autoSync) { app.changeAutoSync(it) } },
        ) {
            app.changeAutoSync(!app.autoSync)
        }
        if (app.sync == SyncStatus.Retry || app.sync == SyncStatus.Conflict) {
            Column(Modifier.padding(start = 14.dp, end = 14.dp, bottom = 14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (app.sync == SyncStatus.Retry) {
                    Banner("A szinkron megszakadt. Az adatok helyben megmaradtak.", Tone.Error)
                    OutlineButton("Szinkron újrapróbálása", VIcons.sync) { app.retrySync() }
                } else {
                    Banner("Feloldás szükséges. A helyi adat megmaradt.", Tone.Warn)
                    OutlineButton("A helyi változat feltöltése", VIcons.cloud) {
                        app.resolveConflict(keepLocal = true)
                    }
                    OutlineButton("A felhőváltozat használata", VIcons.download) {
                        app.dialog = DialogSpec(
                            title = "A felhőváltozat használata?",
                            body = "Lecseréled az ezen a készüléken még fel nem töltött adatokat és profilképet a felhőben tárolt változatra.",
                            confirm = "Helyi módosítások lecserélése",
                            danger = true,
                            onConfirm = {
                                app.resolveConflict(keepLocal = false)
                            },
                        )
                    }
                }
            }
        }
    }

    GroupLabel("Saját adatok")
    GroupCard {
        ListRow("Adataim exportálása", icon = VIcons.download, sub = "Profil, névjegy és hozzájárulások JSON-fájlban") {
            exporter.launch("vizit-adataim.json")
        }
    }

    GroupLabel("Jogi tudnivalók")
    GroupCard {
        ListRow("Adatkezelési tájékoztató", icon = VIcons.shield, trailing = { ExtMark() }) { actions.openUrl(PRIVACY_URL) }
        ListRow("Felhasználási feltételek", icon = VIcons.doc, divider = true, trailing = { ExtMark() }) { actions.openUrl(TERMS_URL) }
    }

    Text("Fiók", color = V.red, fontSize = 14.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(start = 6.dp, top = 6.dp))
    GroupCard {
        ListRow("Kijelentkezés", icon = VIcons.logout, danger = true) { app.logout() }
        ListRow("Fiók végleges törlése", icon = VIcons.trash, danger = true, divider = true, sub = "A fiók, a felhőprofil és a helyi adatok is törlődnek") {
            app.askDeleteAccount()
        }
    }

    Text(APP_VERSION, fontSize = 12.sp, color = V.sub, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(top = 4.dp))
}

/** B elrendezés: a Beállítások külön oldal. */
@Composable
fun SettingsPage(app: AppState) {
    PageScaffold("Beállítások", onBack = { app.pop() }) {
        SettingsContent(app, includeNfc = false)
    }
}
