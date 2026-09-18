package hu.rayworks.vizit.ui.screens

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import hu.rayworks.vizit.nfc.NfcRouting

@Composable
internal fun NfcPreferredServiceEffect(
    enabled: Boolean,
    onRoutingFailed: () -> Unit,
) {
    val context = LocalContext.current
    val activity = context.findNfcActivity()
    val lifecycleOwner = LocalLifecycleOwner.current
    val routingFailed = rememberUpdatedState(onRoutingFailed)

    DisposableEffect(enabled, activity, lifecycleOwner) {
        if (!enabled || activity == null) return@DisposableEffect onDispose {}

        var preferred = false

        fun activate() {
            preferred = NfcRouting.activate(activity)
            if (!preferred) {
                NfcRouting.deactivate(activity)
                routingFailed.value()
            }
        }

        fun deactivate() {
            NfcRouting.deactivate(activity)
            preferred = false
        }

        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> activate()
                Lifecycle.Event.ON_PAUSE -> deactivate()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) activate()

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            deactivate()
        }
    }
}

private tailrec fun Context.findNfcActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findNfcActivity()
    else -> null
}
