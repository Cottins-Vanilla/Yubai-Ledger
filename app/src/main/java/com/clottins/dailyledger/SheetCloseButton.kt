package com.clottins.dailyledger

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

@Composable
internal fun SheetCloseButton(label: String, onClick: () -> Unit) {
    // Reserve the full Material touch target in the header. Draw the smaller disc
    // inside it so neither minimum-touch sizing nor a scrolling parent clips it.
    IconButton(onClick = onClick, modifier = Modifier.size(48.dp).testTag("sheet-dismiss")) {
        Box(
            Modifier.size(40.dp).clip(CircleShape).background(Color(0xFFEAF0E3)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Outlined.Close, label, tint = Muted, modifier = Modifier.size(20.dp))
        }
    }
}
