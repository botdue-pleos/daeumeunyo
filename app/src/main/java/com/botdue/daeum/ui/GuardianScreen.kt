package com.botdue.daeum.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.botdue.daeum.engine.GuardianAlert

/**
 * 4. 보호자 안심 신호. 운전하는 보호자에게만 들리는 한 줄을 화면에도 크게.
 * 버튼은 두 개뿐이고, 말("잠깐 쉬자")로도 된다. 빨강 대신 겨자색: 뒷자리 동승자가 화면을 봐도 놀라지 않게.
 */
@Composable
fun GuardianScreen(alert: GuardianAlert, destination: String, onRest: () -> Unit, onDismiss: () -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .background(C.Felt)
            .padding(start = 64.px, end = 64.px, top = 64.px, bottom = 56.px),
        verticalArrangement = Arrangement.spacedBy(32.px),
    ) {
        T("운전하는 분께만 들려요", 26.tx, color = C.Sub, weight = FontWeight.SemiBold)
        Column(
            Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(C.Now, RoundedCornerShape(40.px))
                .padding(start = 52.px, end = 52.px, top = 56.px, bottom = 52.px),
            verticalArrangement = Arrangement.spacedBy(32.px, Alignment.CenterVertically),
        ) {
            Picto(if (alert.kind == GuardianAlert.Kind.BELT) Pic.BELT else Pic.CAR, 150.px, strokeWidth = 5.5f)
            T(alert.title.replace("가 ", "가\n"), 62.tx, weight = FontWeight.ExtraBold, lineHeight = 1.2f)
            alert.stopHint?.let { T(it, 32.tx, weight = FontWeight.SemiBold) }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(16.px)) {
            PrimaryButton("쉼터 들르기", Modifier.weight(1f), height = 112, onClick = onRest)
            SecondaryButton("괜찮아요", Modifier.weight(1f), height = 112, size = 34, onClick = onDismiss)
        }
        Row(Modifier.padding(horizontal = 8.px), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(20.px)) {
            Picto(Pic.MIC, 44.px, strokeWidth = 7f)
            T("말로 해도 돼요. \"잠깐 쉬자\"", 28.tx)
        }
        Column(
            Modifier
                .fillMaxWidth()
                .background(C.Card, RoundedCornerShape(32.px))
                .padding(horizontal = 36.px, vertical = 32.px),
            verticalArrangement = Arrangement.spacedBy(18.px),
        ) {
            T("쉼터에 들르면 뒷자리에도 미리 말해요", 24.tx, color = C.Sub, weight = FontWeight.ExtraBold)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(24.px)) {
                Picto(Pic.REST, 88.px)
                T("잠깐 쉬었다가\n${destination}에 가", 36.tx, weight = FontWeight.ExtraBold)
            }
            T("오늘의 길에 쉼터 칸이 하나 들어가요", 24.tx, color = C.Sub)
        }
    }
}
