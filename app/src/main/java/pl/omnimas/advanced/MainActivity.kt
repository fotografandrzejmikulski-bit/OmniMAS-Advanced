package pl.omnimas.advanced

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            var mission by remember { mutableStateOf("") }
            var status by remember { mutableStateOf("Gotowy") }
            var confirmation by remember { mutableStateOf<Action?>(null) }

            MaterialTheme {
                Column(
                    modifier = Modifier.fillMaxSize().padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("OmniMAS Advanced", style = MaterialTheme.typography.headlineMedium)
                    Text("LOCAL · Planner / Grounding / Executor / Memory / Supervisor")

                    OutlinedTextField(
                        value = mission,
                        onValueChange = { mission = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Misja") },
                        minLines = 4
                    )

                    Button(
                        onClick = {
                            val service = OmniAccessibilityService.Instance.service
                            if (service == null) {
                                status = "Włącz AccessibilityService"
                            } else {
                                service.launchMission(mission)
                                status = "Misja uruchomiona"
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("START MAS") }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = {
                            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                        }) { Text("Accessibility") }
                        Button(onClick = {
                            startActivity(Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"))
                        }) { Text("Powiadomienia") }
                    }

                    OmniAccessibilityService.Instance.awaitingConfirmation?.let { pending ->
                        confirmation = pending
                    }

                    confirmation?.let { pending ->
                        Text("Potwierdzenie wymagane: ${pending.reason ?: pending.type}")
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = {
                                OmniAccessibilityService.Instance.service?.continueAfterConfirmation(true)
                                confirmation = null
                                status = "Potwierdzono"
                            }) { Text("Wykonaj") }
                            Button(onClick = {
                                OmniAccessibilityService.Instance.service?.continueAfterConfirmation(false)
                                confirmation = null
                                status = "Odrzucono"
                            }) { Text("Odrzuć") }
                        }
                    }

                    Text("Status: ${OmniAccessibilityService.Instance.lastStatus.ifBlank { status }}")
                    Text("Pakiet: ${OmniAccessibilityService.Instance.packageName.ifBlank { "—" }}")
                    Text("Ostatnia akcja: ${OmniAccessibilityService.Instance.lastAction?.type ?: "—"}")
                }
            }
        }
    }
}
