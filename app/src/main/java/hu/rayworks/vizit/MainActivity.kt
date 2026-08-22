package hu.rayworks.vizit

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import hu.rayworks.vizit.ui.VizitApp
import hu.rayworks.vizit.ui.theme.VizitTheme

class MainActivity : ComponentActivity() {
    private val viewModel: VizitViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            VizitTheme {
                VizitApp(viewModel = viewModel)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshNfcStatus()
    }

    override fun onStop() {
        viewModel.stopNfcShare()
        super.onStop()
    }
}
