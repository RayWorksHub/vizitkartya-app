package hu.rayworks.vizit.ui

import androidx.compose.runtime.Composable
import hu.rayworks.vizit.VizitViewModel
import hu.rayworks.vizit.auth.AuthViewModel
import hu.rayworks.vizit.v10.ui.ProductionVizitRoot

/** A 10-es kiadás a csatolt ZIP teljes felületét használja az éles állapotkezelőkkel. */
@Composable
fun VizitRoot(vizitViewModel: VizitViewModel, authViewModel: AuthViewModel) {
    ProductionVizitRoot(vizitViewModel = vizitViewModel, authViewModel = authViewModel)
}
