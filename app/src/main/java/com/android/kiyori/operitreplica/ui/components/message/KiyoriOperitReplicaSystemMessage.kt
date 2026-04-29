package com.android.kiyori.operitreplica.ui.components.message

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.kiyori.operitreplica.model.OperitReplicaMessage

@Composable
internal fun KiyoriOperitReplicaSystemMessage(message: OperitReplicaMessage) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
    ) {
        Surface(shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp), color = Color(0xFFF4F6FA)) {
            Column(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = message.text,
                    fontSize = 11.sp,
                    lineHeight = 17.sp,
                    color = Color(0xFF667085),
                    textAlign = TextAlign.Center,
                )
                if (!message.meta.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = message.meta.orEmpty(),
                        fontSize = 10.sp,
                        color = Color(0xFF98A2B3),
                    )
                }
            }
        }
    }
}
