package hu.rayworks.vizit.v10.ui.pages

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import hu.rayworks.vizit.v10.data.AppState
import hu.rayworks.vizit.v10.data.CardLayout
import hu.rayworks.vizit.v10.data.PRESETS
import hu.rayworks.vizit.v10.ui.components.DigitalCard
import hu.rayworks.vizit.v10.ui.components.GroupCard
import hu.rayworks.vizit.v10.ui.components.GroupLabel
import hu.rayworks.vizit.v10.ui.components.ListRow
import hu.rayworks.vizit.v10.ui.components.PageScaffold
import hu.rayworks.vizit.v10.ui.components.Segmented
import hu.rayworks.vizit.v10.ui.components.VSwitch
import hu.rayworks.vizit.v10.ui.components.cssLinearGradient
import hu.rayworks.vizit.v10.ui.icons.VIcons
import hu.rayworks.vizit.v10.ui.theme.V

/**
 * Kártya megjelenése (éles CardAppearanceScreen): élő előnézet, 6 színvilág, Álló / Fekvő
 * elrendezés, megjelenő elemek. Minden változás azonnal érvényes, mentés gomb nincs.
 */
@Composable
fun AppearancePage(app: AppState) {
    val p = app.focus
    PageScaffold("Kártya megjelenése", onBack = { app.pop() }) {
        Box(Modifier.padding(horizontal = 4.dp, vertical = 6.dp)) {
            DigitalCard(p, app.contactVCard(p))
        }

        GroupLabel("Színvilág", trailing = p.preset.name)
        GroupCard {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                PRESETS.forEach { pr ->
                    val on = p.presetId == pr.id
                    Box(
                        Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .clickable { app.updateFocus { it.copy(presetId = pr.id) } },
                        contentAlignment = Alignment.Center,
                    ) {
                        Box(
                            Modifier
                                .size(34.dp)
                                .drawBehind {
                                    val r = size.minDimension / 2f
                                    if (on) drawCircle(V.blue, radius = r + 4.dp.toPx(), style = Stroke(2.dp.toPx()))
                                    drawCircle(cssLinearGradient(135f, size, 0f to pr.c1, 0.5f to pr.c2, 1f to pr.c3), radius = r)
                                },
                            contentAlignment = Alignment.Center,
                        ) {
                            if (on) Icon(VIcons.checkBold, contentDescription = pr.name, tint = pr.acc, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }

        GroupLabel("Elrendezés")
        Segmented(CardLayout.entries.map { it.label }, p.layout.ordinal) { i -> app.updateFocus { it.copy(layout = CardLayout.entries[i]) } }

        GroupLabel("Megjelenő elemek")
        GroupCard {
            ListRow(
                "Profilkép",
                icon = VIcons.person,
                sub = if (p.photo == null) "Még nincs feltöltött profilképed" else null,
                trailing = { VSwitch(p.showPhoto) { v -> app.updateFocus { it.copy(showPhoto = v) } } },
            ) { app.updateFocus { it.copy(showPhoto = !it.showPhoto) } }
            ListRow(
                "QR-kód a kártyán",
                icon = VIcons.qr,
                divider = true,
                trailing = { VSwitch(p.showQr) { v -> app.updateFocus { it.copy(showQr = v) } } },
            ) { app.updateFocus { it.copy(showQr = !it.showQr) } }
            val socialAllowed = p.vis["social"] != false
            ListRow(
                "Közösségi profilok",
                icon = VIcons.link,
                sub = if (!socialAllowed) "Az Adatláthatóságban ki van kapcsolva" else null,
                divider = true,
                enabled = socialAllowed,
                trailing = { VSwitch(p.showSocial && socialAllowed, if (socialAllowed) { v -> app.updateFocus { it.copy(showSocial = v) } } else null) },
            ) { app.updateFocus { it.copy(showSocial = !it.showSocial) } }
        }
    }
}
