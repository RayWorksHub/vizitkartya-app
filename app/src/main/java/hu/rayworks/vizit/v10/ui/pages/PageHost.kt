package hu.rayworks.vizit.v10.ui.pages

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import hu.rayworks.vizit.v10.data.AppState
import hu.rayworks.vizit.v10.data.Page
import hu.rayworks.vizit.v10.data.fades
import hu.rayworks.vizit.v10.ui.components.noRippleClickable
import hu.rayworks.vizit.v10.ui.theme.SheetEasing

/**
 * A teljes képernyős oldalak verme. Előre: jobbról úszik be (320 ms), vissza: jobbra csúszik ki.
 * A QR, az NFC-küldés és a beolvasás áttűnéssel jelenik meg, mint a prototípus átfedései.
 */
@Composable
fun PageHost(app: AppState) {
    AnimatedContent(
        targetState = app.pages.lastOrNull(),
        transitionSpec = {
            val fade = targetState?.fades == true || initialState?.fades == true
            when {
                fade -> fadeIn(tween(220)) togetherWith fadeOut(tween(200))
                app.navForward ->
                    (slideInHorizontally(tween(320, easing = SheetEasing)) { it } + fadeIn(tween(160))) togetherWith
                        (slideOutHorizontally(tween(320, easing = SheetEasing)) { -it / 5 } + fadeOut(tween(220)))
                else ->
                    (slideInHorizontally(tween(320, easing = SheetEasing)) { -it / 5 } + fadeIn(tween(220))) togetherWith
                        (slideOutHorizontally(tween(320, easing = SheetEasing)) { it } + fadeOut(tween(260)))
            }
        },
        label = "pages",
    ) { page ->
        if (page != null) {
            Box(Modifier.fillMaxSize().noRippleClickable { }) {
                when (page) {
                    Page.Settings -> SettingsPage(app)
                    Page.Appearance -> AppearancePage(app)
                    Page.Analytics -> AnalyticsPage(app)
                    is Page.QrFull -> QrPage(app, page.index)
                    Page.NfcSend -> NfcSendPage(app)
                    Page.Scanner -> ScannerPage(app)
                    Page.Hub -> HubPage(app)
                    Page.HubVosz -> HubVoszPage(app)
                    Page.HubEdu -> HubEduPage(app)
                    is Page.HubCourse -> HubCoursePage(app, page.id)
                    Page.HubHelp -> HubHelpPage(app)
                    Page.HubToolkit -> HubToolkitPage(app)
                }
            }
        }
    }
}
