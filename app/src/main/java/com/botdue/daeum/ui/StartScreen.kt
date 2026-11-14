package com.botdue.daeum.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import com.botdue.daeum.engine.Card
import com.botdue.daeum.engine.HardMark
import com.botdue.daeum.engine.MarkedMoment
import com.botdue.daeum.engine.Passage
import com.botdue.daeum.engine.Situation
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

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

/**
 * 도착: 동승자가 보는 큰 카드는 "도착했어 / 잘 왔어"만.
 * 아래 작은 칸은 보호자용: 운전 중 "표시해"라고 한 순간을 붙은 상황과 함께 보여 주고 맞는지 묻는다.
 * 놓친 표시는 오늘 지나간 상황에서 골라 더한다.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ArrivedScreen(
    destination: String,
    moments: List<MarkedMoment>,
    confirmed: Set<HardMark>,
    missed: List<Passage>,
    onConfirm: (HardMark) -> Unit,
    onReject: (HardMark) -> Unit,
    onAdd: (Passage) -> Unit,
    onDone: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxSize()
            .background(C.Felt)
            .padding(start = 64.px, end = 64.px, top = 64.px, bottom = 56.px),
        verticalArrangement = Arrangement.spacedBy(28.px),
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(C.Card, RoundedCornerShape(40.px))
                .nowRing(8.px, 40.px)
                .padding(48.px),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.px, Alignment.CenterVertically),
        ) {
            Picto(Pic.HOSPITAL, 200.px, strokeWidth = 4.5f)
            T("${destination}에 도착했어", 60.tx, weight = FontWeight.ExtraBold)
            T("잘 왔어", 34.tx, color = C.Sub)
        }
        GuardianLine("여기부터는 보호자께 여쭤요")
        Column(
            Modifier
                .fillMaxWidth()
                .background(C.Card, RoundedCornerShape(32.px))
                .padding(horizontal = 36.px, vertical = 32.px),
            verticalArrangement = Arrangement.spacedBy(18.px),
        ) {
            T(if (moments.isEmpty()) "오늘 표시한 순간은 없어요" else "오늘 표시한 순간 ${moments.size}개 — 맞나요?", 32.tx, weight = FontWeight.ExtraBold)
            moments.forEach { m ->
                MomentRow(m, m.mark in confirmed, onConfirm = { onConfirm(m.mark) }, onReject = { onReject(m.mark) })
            }
            if (missed.isNotEmpty()) {
                T("놓친 순간이 있으면 골라 주세요 · 오늘 지나간 상황", 24.tx, color = C.Sub, weight = FontWeight.SemiBold)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(12.px), verticalArrangement = Arrangement.spacedBy(12.px)) {
                    missed.forEach { p ->
                        Box(
                            Modifier
                                .border(3.px, C.Done, RoundedCornerShape(50))
                                .clickable(role = Role.Button) { onAdd(p) }
                                .padding(horizontal = 22.px, vertical = 12.px),
                        ) { T("+ ${p.situation.label} ${clock(p.timeMs)}", 24.tx, weight = FontWeight.SemiBold) }
                    }
                }
            }
            T("맞다고 한 표시만 가까운 상황과 묶어 이 차에만 남겨요", 22.tx, color = C.Sub)
        }
        PrimaryButton("다 확인했어요", Modifier.fillMaxWidth(), height = 96, onClick = onDone)
    }
}

/** 표시 한 줄: "길 바뀜 10:20" + [맞아요] [아니에요]. 맞다고 하면 버튼 대신 체크 */
@Composable
private fun MomentRow(m: MarkedMoment, confirmed: Boolean, onConfirm: () -> Unit, onReject: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().background(C.Felt, RoundedCornerShape(22.px)).padding(horizontal = 24.px, vertical = 14.px),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.px),
    ) {
        Picto(m.passage?.situation?.pic() ?: Pic.MIC, 52.px, strokeWidth = 7f)
        T("${m.passage?.situation?.label ?: "상황 없음"} ${clock(m.mark.timeMs)}", 30.tx, weight = FontWeight.ExtraBold, modifier = Modifier.weight(1f))
        if (confirmed) {
            Picto(Pic.CHECK, 40.px, C.DoneInk, strokeWidth = 9f)
            T("맞아요", 26.tx, color = C.Sub, weight = FontWeight.SemiBold)
        } else {
            PrimaryButton("맞아요", Modifier.width(170.px), height = 72, size = 26, onClick = onConfirm)
            SecondaryButton("아니에요", Modifier.width(190.px), height = 72, size = 26, onClick = onReject)
        }
    }
}

private val hhmm = DateTimeFormatter.ofPattern("HH:mm")

/** 표시·상황 시각 (차 안 시계 기준) */
fun clock(timeMs: Long): String = Instant.ofEpochMilli(timeMs).atZone(ZoneId.of("Asia/Seoul")).format(hhmm)

/** 상황 그림 (이동 노트·도착 화면 공용) */
fun Situation.pic(): Pic = when (this) {
    Situation.REROUTE -> Pic.DETOUR
    Situation.TUNNEL -> Pic.TUNNEL
    Situation.TRAFFIC -> Pic.CAR
    Situation.BELT -> Pic.BELT
}
