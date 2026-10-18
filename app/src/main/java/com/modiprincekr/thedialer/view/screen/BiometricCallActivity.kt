package com.modiprincekr.thedialer.view.screen

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.telecom.VideoProfile
import androidx.activity.compose.setContent
import androidx.fragment.app.FragmentActivity
import com.modiprincekr.thedialer.controller.CallService
import com.modiprincekr.thedialer.controller.util.PreferenceManager
import com.modiprincekr.thedialer.view.theme.Rivo4Theme
import org.koin.android.ext.android.inject

class BiometricCallActivity : FragmentActivity() {

    private val prefs: PreferenceManager by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                android.view.WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                android.view.WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }

        val action = intent?.getStringExtra("NOTIFICATION_PENDING_ACTION") ?: run { finish(); return }
        val biometricType = prefs.getString(PreferenceManager.KEY_BIOMETRICS_TYPE, "") ?: ""

        // Verify we should actually gate this specific call
        val callPhoneNumber = CallService.incomingCallSession.value?.call?.details?.handle?.schemeSpecificPart
            ?: CallService.currentCallSession.value?.call?.details?.handle?.schemeSpecificPart
        if (!prefs.shouldGateCallWithBiometric(callPhoneNumber) || biometricType.isEmpty()) {
            // Lock scope excludes this number — perform action directly
            when (action) {
                "ANSWER" -> CallService.answerCall()
                "DECLINE" -> CallService.declineCall()
            }
            finish()
            return
        }

        setContent {
            Rivo4Theme {
                com.modiprincekr.thedialer.view.screen.auth.AuthLockScreen(
                    title = "The Dialer",
                    subtitle = "Verify your identity to access this call",
                    onSuccess = {
                        when (action) {
                            "ANSWER" -> CallService.answerCall()
                            "DECLINE" -> CallService.declineCall()
                        }
                        finish()
                    },
                    onDismiss = { finish() }
                )
            }
        }
    }
}
