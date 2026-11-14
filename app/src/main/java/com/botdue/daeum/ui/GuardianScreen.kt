package com.botdue.daeum.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
 * 4. 보호자 안심 신호. 소리는 차 안 모두에게 들리므로 뒷자리가 들어도 괜찮은 말([GuardianAlert.spoken])로 하고,
 * 무슨 일인지는 화면에만 쓴다. 버튼은 두 개뿐이고, 말("잠깐 쉬자")로도 된다.
 * 겨자색은 "지금 칸"에만 쓰므로 여기는 흰 바탕 + 진한 잉크 테두리. 빨강은 쓰지 않는다.
 */
@Composable
fun GuardianScreen(alert: GuardianAlert, destination: String, onRest: () -> Unit, onDismiss: () -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .background(C.Felt)
            .padding(start = 64.px, end = 64.px, top = 64.px, bottom = 56.px),
        verticalArrangement = Arrangement.spacedBy(28.px),
    ) {
        T("보호자께 알려요", 26.tx, color = C.Sub, weight = FontWeight.SemiBold)
        Column(
            Modifier
                .fillMaxWidth()
                .weight(1f)
                .border(8.px, C.GuardianEdge, RoundedCornerShape(40.px))
                .background(C.Card, RoundedCornerShape(40.px))
                .padding(start = 52.px, end = 52.px, top = 44.px, bottom = 40.px),
            verticalArrangement = Arrangement.spacedBy(28.px, Alignment.CenterVertically),
        ) {
            Picto(if (alert.kind == GuardianAlert.Kind.BELT) Pic.BELT else Pic.CAR, 120.px, strokeWidth = 5.5f)
            T(alert.title.replace("를 ", "를\n"), 58.tx, weight = FontWeight.ExtraBold, lineHeight = 1.2f)
            alert.stopHint?.let { T(it, 32.tx, weight = FontWeight.SemiBold) }
            Column(
                Modifier.fillMaxWidth().background(C.Felt, RoundedCornerShape(24.px)).padding(horizontal = 28.px, vertical = 22.px),
                verticalArrangement = Arrangement.spacedBy(8.px),
            ) {
                T("차 안에 들리는 말", 22.tx, color = C.Sub, weight = FontWeight.ExtraBold)
                T("\"${alert.spoken}\"", 28.tx, weight = FontWeight.SemiBold)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(16.px)) {
            PrimaryButton("쉼터 들르기", Modifier.weight(1f), height = 112, onClick = onRest)
            SecondaryButton("괜찮아요", Modifier.weight(1f), height = 112, size = 34, onClick = onDismiss)
        }
        Row(Modifier.padding(horizontal = 8.px), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(20.px)) {
            Picto(Pic.MIC, 44.px, strokeWidth = 7f)
            T("말로 해도 돼요. \"잠깐 쉬자\"", 28.tx)
        }
        GuardianLine("쉼터에 들르면 뒷자리에도 이렇게 미리 말하고, 오늘의 길에 쉼터 칸을 넣어요", size = 24)
        // 동승자에게 하는 말은 그림 카드 안에
        Row(
            Modifier
                .fillMaxWidth()
                .background(C.Card, RoundedCornerShape(32.px))
                .padding(horizontal = 36.px, vertical = 28.px),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(24.px),
        ) {
            Picto(Pic.REST, 88.px)
            T("잠깐 쉬었다가\n${destination}에 가", 36.tx, weight = FontWeight.ExtraBold)
        }
    }
}
