package com.example.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class BankSmsBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        if (messages.isNullOrEmpty()) return

        // Group multi-part SMS messages from the same sender
        val groupedBySender = linkedMapOf<String, Pair<StringBuilder, Long>>()
        for (sms in messages) {
            if (sms == null) continue
            val sender = sms.displayOriginatingAddress ?: sms.originatingAddress ?: "BANK"
            val text = sms.displayMessageBody ?: sms.messageBody ?: continue
            val ts = sms.timestampMillis.takeIf { it > 0L } ?: System.currentTimeMillis()
            val entry = groupedBySender.getOrPut(sender) { StringBuilder() to ts }
            entry.first.append(text)
        }

        if (groupedBySender.isEmpty()) return

        val pendingResult = goAsync()
        val appContext = context.applicationContext
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                for ((sender, pair) in groupedBySender) {
                    BankAlertCoordinator.processIncomingMessage(
                        context = appContext,
                        senderAddress = sender,
                        messageBody = pair.first.toString(),
                        timestampMillis = pair.second
                    )
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
