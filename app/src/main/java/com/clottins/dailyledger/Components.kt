package com.clottins.dailyledger

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val Bg = Color(0xFFF7F8F4)
val Ink = Color(0xFF233E35)
val Green = Color(0xFF214E3E)
val Muted = Color(0xFF8F9B83)
val Line = Color(0xFFE4E9DD)
val Sage = Color(0xFFD8E6B6)
val ChartColors =
    listOf(
        Color(0xFF456C4D),
        Color(0xFF91AA70),
        Color(0xFFC2CF9D),
        Color(0xFFDDCEAA),
        Color(0xFFB6C8AC),
        Color(0xFFA5AE8B),
        Color(0xFFDAC4AB),
        Color(0xFFE4E9DA),
    )

fun categoryIcon(icon: String): ImageVector =
    when (icon) {
        "food" -> Icons.Outlined.Restaurant
        "train" -> Icons.Outlined.Train
        "bag" -> Icons.Outlined.ShoppingBag
        "daily" -> Icons.Outlined.Soap
        "home" -> Icons.Outlined.Cottage
        "game" -> Icons.Outlined.SportsEsports
        "medical" -> Icons.Outlined.MedicalServices
        "wallet" -> Icons.Outlined.AccountBalanceWallet
        "gift" -> Icons.Outlined.CardGiftcard
        "work" -> Icons.Outlined.Work
        "redpacket" -> Icons.Outlined.MailOutline
        "refund" -> Icons.AutoMirrored.Outlined.Undo
        else -> Icons.Outlined.GridView
    }

@Composable
fun LedgerTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme =
            lightColorScheme(
                primary = Green,
                onPrimary = Color.White,
                background = Bg,
                surface = Bg,
                onSurface = Ink,
                surfaceVariant = Color(0xFFEDF1E5),
                outline = Line,
                secondary = Color(0xFF769359),
            ),
        typography = Typography(
            bodyLarge = TextStyle(fontSize = 14.sp, lineHeight = TextUnit.Unspecified),
            bodyMedium = TextStyle(fontSize = 12.sp, lineHeight = TextUnit.Unspecified),
            bodySmall = TextStyle(fontSize = 11.sp, lineHeight = TextUnit.Unspecified),
            labelLarge = TextStyle(fontSize = 13.sp, lineHeight = TextUnit.Unspecified),
        ),
        content = content,
    )
}

@Composable
fun SmallLabel(text: String, modifier: Modifier = Modifier) {
    Text(text, modifier, color = Muted, fontSize = 11.sp, lineHeight = 15.sp)
}

@Composable
fun Segment(
    options: List<String>,
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier.clip(RoundedCornerShape(13.dp)).background(Color(0xFFECEFE3)).padding(3.dp)) {
        options.forEachIndexed { i, text ->
            Box(
                Modifier.weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (i == selected) Color(0xFFFDFEFA) else Color.Transparent)
                    .clickable { onSelect(i) }
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text,
                    fontSize = 11.sp,
                    color = if (i == selected) Green else Muted,
                    fontWeight = if (i == selected) FontWeight.SemiBold else FontWeight.Normal,
                )
            }
        }
    }
}

@Composable
fun BalanceCard(
    totals: Totals,
    monthly: Totals,
    hidden: Boolean,
    statistics: Boolean,
    onHide: () -> Unit,
) {
    if (statistics) {
        Row(
            Modifier.fillMaxWidth()
                .clip(RoundedCornerShape(25.dp))
                .background(Green)
                .padding(21.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text("当前余额", fontSize = 11.sp, color = Color(0xFFD2DFD5))
                Spacer(Modifier.height(11.dp))
                Text(
                    if (hidden) "••••••"
                    else "${if(totals.balance<0)"− "else ""}¥${Ledger.money(totals.balance)}",
                    fontSize = 27.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White,
                    maxLines = 1,
                )
            }
            Column(
                Modifier.width(IntrinsicSize.Max)
                    .padding(start = 14.dp)
                    .border(0.dp, Color.Transparent)
            ) {
                Text("本月收入", fontSize = 10.sp, color = Color(0xFFD2DFD5))
                Text("+ ${Ledger.money(monthly.income)}", fontSize = 13.sp, color = Color.White)
                Spacer(Modifier.height(17.dp))
                Text("本月支出", fontSize = 10.sp, color = Color(0xFFD2DFD5))
                Text("− ${Ledger.money(monthly.expense)}", fontSize = 13.sp, color = Color.White)
            }
        }
    } else {
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(27.dp)).background(Green).padding(23.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("当前余额", fontSize = 12.sp, color = Color(0xFFD2DFD5))
                IconButton(onClick = onHide, modifier = Modifier.size(35.dp)) {
                    Icon(
                        if (hidden) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                        contentDescription = "隐藏或显示余额",
                        tint = Color(0xFFB7CEBB),
                        modifier = Modifier.size(17.dp),
                    )
                }
            }
            Spacer(Modifier.height(6.dp))
            Text(
                if (hidden) "••••••"
                else "${if(totals.balance<0)"− "else ""}¥${Ledger.money(totals.balance)}",
                fontSize = 37.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White,
                maxLines = 1,
            )
            Spacer(Modifier.height(20.dp))
            HorizontalDivider(color = Color.White.copy(alpha = .13f))
            Spacer(Modifier.height(17.dp))
            Row {
                Column(Modifier.weight(1f)) {
                    Text("↓  本月收入", fontSize = 10.sp, color = Color(0xFFC6D8CB))
                    Spacer(Modifier.height(7.dp))
                    Text("+ ${Ledger.money(monthly.income)}", fontSize = 17.sp, color = Color.White)
                }
                Column(Modifier.weight(1f).padding(start = 16.dp)) {
                    Text("↑  本月支出", fontSize = 10.sp, color = Color(0xFFC6D8CB))
                    Spacer(Modifier.height(7.dp))
                    Text(
                        "− ${Ledger.money(monthly.expense)}",
                        fontSize = 17.sp,
                        color = Color.White,
                    )
                }
            }
        }
    }
}

@Composable
fun IconTile(icon: String, modifier: Modifier = Modifier, selected: Boolean = false) {
    Box(
        modifier
            .size(45.dp)
            .clip(RoundedCornerShape(15.dp))
            .background(if (selected) Color(0xFFE0EACB) else Color(0xFFEDF0E5)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(categoryIcon(icon), null, tint = Color(0xFF81966F), modifier = Modifier.size(22.dp))
    }
}

@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Button(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().height(52.dp),
        enabled = enabled,
        shape = RoundedCornerShape(16.dp),
        contentPadding = PaddingValues(10.dp),
    ) {
        Text(text, fontSize = 13.sp)
    }
}

@Composable
fun EmptyState(text: String, modifier: Modifier = Modifier) {
    Column(
        modifier.fillMaxWidth().padding(vertical = 30.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            Icons.AutoMirrored.Outlined.ReceiptLong,
            null,
            tint = Color(0xFFB2BEA4),
            modifier = Modifier.size(34.dp),
        )
        Spacer(Modifier.height(12.dp))
        Text(text, color = Muted, fontSize = 12.sp)
    }
}
