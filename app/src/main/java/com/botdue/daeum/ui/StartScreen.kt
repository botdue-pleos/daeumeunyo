package com.botdue.daeum.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.botdue.daeum.engine.Card

/** 1. 출발 전: 오늘의 길 카드. 조수석·주차 중에 동승자와 같이 본다 */
@Composable
fun StartScreen(
    destination: String,
    cards: List<Card>,
    songs: Int,
    tunnelSongPicked: Boolean,
    onDepart: () -> Unit,
    onPickSong: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxSize()
            .background(C.Felt)
            .padding(start = 64.px, end = 64.px, top = 64.px, bottom = 56.px),
        verticalArrangement = Arrangement.spacedBy(36.px),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.px)) {
            T("$destination 가는 길", 26.tx, color = C.Sub, weight = FontWeight.SemiBold)
            T("오늘은 이렇게 가요", 56.tx, weight = FontWeight.ExtraBold)
        }
        Row(
            Modifier
                .fillMaxWidth()
                .background(C.Card, RoundedCornerShape(24.px))
                .padding(horizontal = 28.px, vertical = 22.px),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            T("노래 ${songs}곡만큼 걸려요", 28.tx, weight = FontWeight.SemiBold)
            SongDots(songs)
        }
        VerticalStrip(cards, cardStates(cards.size, 0, emptySet()), compact = cards.size >= 5)
        Spacer(Modifier.weight(1f))
        Column(verticalArrangement = Arrangement.spacedBy(16.px)) {
            PrimaryButton("출발해요", Modifier.fillMaxWidth(), onClick = onDepart)
            SecondaryButton(
                if (tunnelSongPicked) "터널에서 들을 노래: 골라 둔 3곡" else "터널에서 들을 노래 고르기",
                Modifier.fillMaxWidth(),
                onClick = onPickSong,
            )
        }
    }
}

/** 도착: 띠가 다 접히고, 보호자에게 오늘 힘들었던 순간을 묻는다 (말로 답해도 된다) */
@Composable
fun ArrivedScreen(destination: String, onHard: () -> Unit, onFine: () -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .background(C.Felt)
            .padding(start = 64.px, end = 64.px, top = 64.px, bottom = 56.px),
        verticalArrangement = Arrangement.spacedBy(32.px),
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(C.Card, RoundedCornerShape(40.px))
                .nowRing(8.px, 40.px)
                .padding(56.px),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(28.px, Alignment.CenterVertically),
        ) {
            Picto(Pic.HOSPITAL, 220.px, strokeWidth = 4.5f)
            T("${destination}에 도착했어", 60.tx, weight = FontWeight.ExtraBold)
            T("잘 왔어", 34.tx, color = C.Sub)
        }
        Column(
            Modifier
                .fillMaxWidth()
                .background(C.Card, RoundedCornerShape(32.px))
                .padding(36.px),
            verticalArrangement = Arrangement.spacedBy(20.px),
        ) {
            T("오늘 힘들어했던 순간이 있었나요?", 34.tx, weight = FontWeight.ExtraBold)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.px)) {
                Picto(Pic.MIC, 40.px, strokeWidth = 7f)
                T("말로 해도 돼요. \"아까 힘들어했어\"", 26.tx)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(16.px)) {
                PrimaryButton("있었어요", Modifier.weight(1f), height = 96, onClick = onHard)
                SecondaryButton("없었어요", Modifier.weight(1f), height = 96, size = 32, onClick = onFine)
            }
            T("가장 가까웠던 상황(길 바뀜, 터널 등)과 묶어서 이 차에만 남겨요", 24.tx, color = C.Sub)
        }
        Box(Modifier.fillMaxWidth())
    }
}
