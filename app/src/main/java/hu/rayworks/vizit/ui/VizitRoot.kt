package hu.rayworks.vizit.ui

import androidx.compose.runtime.Composable
import hu.rayworks.vizit.VizitViewModel
import hu.rayworks.vizit.auth.AuthViewModel
import hu.rayworks.vizit.v10.ui.ProductionVizitRoot

/**
 * Production entry point.
 *
 * Presentation and navigation come from the exact Android source delivered in
 * VizitTeljes(3).zip. Production data, authentication, sync and NFC remain
 * owned by the current ViewModels through ProductionVizitRoot.
 */
@Composable
fun VizitRoot(vizitViewModel: VizitViewModel, authViewModel: AuthViewModel) {
    ProductionVizitRoot(
        vizitViewModel = vizitViewModel,
        authViewModel = authViewModel,
    )
}
