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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import com.botdue.daeum.engine.Card
import com.botdue.daeum.engine.LegKind
import com.botdue.daeum.engine.RouteChange
import com.botdue.daeum.engine.TunnelStage

/**
 * 2. 주행 중: 지금 칸 하나를 크게. 운전자는 보지 않는다 (동승자·조수석용, 말은 차가 한다).
 * 터널 예고 중이면 터널 문장과 노래 띠를 보여 준다.
 */
@Composable
fun DrivingScreen(
    cards: List<Card>,
    current: Int,
    changed: Set<Int>,
    songs: Int,
    tunnel: TunnelStage,
    musicOn: Boolean,
    recirculationOn: Boolean,
) {
    val card = cards[current]
    val next = cards.getOrNull(current + 1)
    val (title, sub) = when {
        card.kind == LegKind.TUNNEL && tunnel == TunnelStage.FIRST ->
            "곧 터널이야\n금방 밝아져" to "1분 뒤에 들어가요. 30초 전에 한 번 더 말해 줄게요"
        card.kind == LegKind.TUNNEL && tunnel >= TunnelStage.SECOND && tunnel < TunnelStage.PASSED ->
            "이제 터널에 들어가\n노래 들으면서 가자" to "어두워져도 금방 밝아져요"
        else -> card.text to (next?.let { "다음은 ${it.text}" } ?: "")
    }
    Column(
        Modifier
            .fillMaxSize()
            .background(C.Felt)
            .padding(start = 64.px, end = 64.px, top = 64.px, bottom = 56.px),
        verticalArrangement = Arrangement.spacedBy(32.px),
    ) {
        MiniStrip(cards, cardStates(cards.size, current, changed))
        Column(
            Modifier
                .fillMaxWidth()
                .weight(1f)
                .nowRing(8.px, 40.px)
                .background(C.Card, RoundedCornerShape(40.px))
                .padding(start = 56.px, end = 56.px, top = 56.px, bottom = 48.px),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(28.px),
        ) {
            Picto(card.kind.pic(), 260.px, strokeWidth = 4.5f)
            T(title, 64.tx, weight = FontWeight.ExtraBold, align = TextAlign.Center, lineHeight = 1.2f)
            if (sub.isNotEmpty()) T(sub, 30.tx, color = C.Sub, align = TextAlign.Center)
            Spacer(Modifier.weight(1f))
            if (musicOn) MusicBar()
        }
        Row(horizontalArrangement = Arrangement.spacedBy(16.px)) {
            if (recirculationOn) Chip(Pic.RECIRC, "바깥 공기 막음", Modifier.weight(1f))
            Row(
                Modifier
                    .weight(1f)
                    .background(C.Card, RoundedCornerShape(24.px))
                    .padding(horizontal = 26.px, vertical = 22.px),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                T("${cards.last().text.removeSuffix("에 도착")}까지 노래 ${songs}곡", 26.tx, weight = FontWeight.SemiBold)
                SongDots(songs, dot = 22, withNote = false)
            }
        }
    }
}

@Composable
private fun MusicBar() {
    Row(
        Modifier
            .fillMaxWidth()
            .background(C.Ink, RoundedCornerShape(32.px))
            .padding(horizontal = 32.px, vertical = 28.px),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(24.px),
    ) {
        Box(Modifier.size(80.px).background(C.Now, CircleShape), contentAlignment = Alignment.Center) {
            Picto(Pic.NOTE, 44.px, C.Ink, strokeWidth = 8f)
        }
        Column(verticalArrangement = Arrangement.spacedBy(6.px)) {
            T("좋아하는 노래를 틀었어요", 32.tx, color = C.Card, weight = FontWeight.ExtraBold)
            T("보호자가 골라 둔 3곡 중 1번째. 안내 음성이 나오면 잠깐 작아져요", 24.tx, color = C.OnInkSub)
        }
    }
}

/** 3. 길이 바뀌었을 때: 목적지가 그대로라는 말이 맨 위, 바뀐 칸만 보라색 점선 */
@Composable
fun RerouteScreen(change: RouteChange, current: Int, songs: Int, reason: String) {
    val place = change.cards.last().text.removeSuffix("에 도착")
    Column(
        Modifier
            .fillMaxSize()
            .background(C.Felt)
            .padding(start = 64.px, end = 64.px, top = 64.px, bottom = 56.px),
        verticalArrangement = Arrangement.spacedBy(32.px),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(14.px)) {
            T(if (change.sameDestination) "길이 바뀌었어\n그래도 ${place}에 가" else "가는 곳이 바뀌었어\n이제 ${place}에 가", 60.tx, weight = FontWeight.ExtraBold, lineHeight = 1.2f)
            T(reason, 28.tx, color = C.Sub)
        }
        VerticalStrip(
            change.cards,
            cardStates(change.cards.size, current, change.changed),
            compact = change.cards.size >= 5,
            label = { i -> if (i == change.cards.lastIndex && change.sameDestination) "그대로" else null },
        )
        Spacer(Modifier.weight(1f))
        if (change.extraSongs > 0) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .background(C.Card, RoundedCornerShape(28.px))
                    .padding(horizontal = 32.px, vertical = 28.px),
                verticalArrangement = Arrangement.spacedBy(20.px),
            ) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    T("노래 ${change.extraSongs}곡 더 걸려요", 34.tx, weight = FontWeight.ExtraBold)
                    T("${songs - change.extraSongs}곡이던 길이 ${songs}곡이 됐어요", 24.tx, color = C.Sub)
                }
                SongDots(songs, extra = change.extraSongs, dot = 56)
            }
        }
    }
}
