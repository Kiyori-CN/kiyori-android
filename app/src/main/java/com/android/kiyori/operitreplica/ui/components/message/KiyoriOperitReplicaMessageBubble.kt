package com.android.kiyori.operitreplica.ui.components.message

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.kiyori.R
import com.android.kiyori.operitreplica.model.OperitReplicaMessage
import com.android.kiyori.operitreplica.model.OperitReplicaMessageRole

@Composable
internal fun KiyoriOperitReplicaMessageBubble(message: OperitReplicaMessage) {
    val isUser = message.role == OperitReplicaMessageRole.User
    val bubbleShape =
        if (isUser) {
            RoundedCornerShape(20.dp, 20.dp, 8.dp, 20.dp)
        } else {
            RoundedCornerShape(18.dp, 18.dp, 18.dp, 6.dp)
        }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start,
    ) {
        if (message.role == OperitReplicaMessageRole.Assistant) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(start = 2.dp, bottom = 6.dp),
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_kiyori_operit_avatar),
                    contentDescription = null,
                    modifier = Modifier.size(18.dp).clip(CircleShape),
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Operit",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF6B7280),
                )
                if (!message.meta.isNullOrBlank()) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = message.meta.orEmpty(),
                        fontSize = 10.sp,
                        color = Color(0xFF98A2B3),
                    )
                }
            }
        }

        Surface(
            modifier = Modifier.widthIn(max = if (isUser) 304.dp else 316.dp),
            shape = bubbleShape,
            color = if (isUser) Color(0xFF111827) else Color.White,
            border =
                if (isUser) {
                    null
                } else {
                    androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE7EAF1))
                },
        ) {
            SelectionContainer {
                Text(
                    text = message.text,
                    fontSize = 13.sp,
                    lineHeight = 20.sp,
                    color = if (isUser) Color.White else Color(0xFF111827),
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                )
            }
        }

        if (isUser && !message.meta.isNullOrBlank()) {
            Text(
                text = message.meta.orEmpty(),
                fontSize = 10.sp,
                color = Color(0xFF98A2B3),
                modifier = Modifier.padding(top = 6.dp, end = 4.dp),
            )
        }
    }
}
