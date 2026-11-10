package com.botdue.daeum.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.botdue.daeum.engine.LeadPolicy
import com.botdue.daeum.engine.MomentLog
import com.botdue.daeum.engine.Situation
import com.botdue.daeum.engine.SituationRow

/**
 * 5. 이동 노트 (주차 중, 가로 2칸). 칸 하나가 그 상황을 한 번 지나간 것, 보라색 칸이 힘들어했던 순간.
 * 오른쪽은 기록 때문에 다음 이동에서 바뀌는 것. 진단이 아니라 관찰 기록이다.
 */
@Composable
fun NotesScreen(month: String, trips: Int, log: MomentLog, policy: LeadPolicy, tunnelName: String, onClose: () -> Unit) {
    val rows = log.rows()
    val hardTotal = rows.sumOf { it.hard }
    BoxWithConstraints(Modifier.fillMaxSize().background(C.Felt)) {
        val wide = maxWidth > 900.dp
        if (wide) {
            Row(
                Modifier.fillMaxSize().padding(start = 72.px, end = 72.px, top = 72.px, bottom = 60.px),
                horizontalArrangement = Arrangement.spacedBy(56.px),
            ) {
                Column(Modifier.weight(1.25f), verticalArrangement = Arrangement.spacedBy(32.px)) {
                    Record(month, trips, hardTotal, rows, fill = true)
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(28.px)) {
                    Changes(policy, tunnelName, fill = true, onClose = onClose)
                }
            }
        } else {
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(start = 64.px, end = 64.px, top = 64.px, bottom = 56.px),
                verticalArrangement = Arrangement.spacedBy(32.px),
            ) {
                Record(month, trips, hardTotal, rows, fill = false)
                Changes(policy, tunnelName, fill = false, onClose = onClose)
            }
        }
    }
}

@Composable
private fun ColumnScope.Record(month: String, trips: Int, hardTotal: Int, rows: List<SituationRow>, fill: Boolean) {
    Column(verticalArrangement = Arrangement.spacedBy(10.px)) {
        T("$month 이동 노트", 56.tx, weight = FontWeight.ExtraBold)
        T("이동 ${trips}번 중 힘들었던 순간 ${hardTotal}번. 칸 하나가 그 상황을 지나간 한 번이에요", 28.tx, color = C.Sub)
    }
    Column(
        (if (fill) Modifier.weight(1f) else Modifier).fillMaxWidth().background(C.Card, RoundedCornerShape(36.px)),
    ) {
        rows.forEachIndexed { i, row ->
            Row(
                (if (fill) Modifier.weight(1f) else Modifier).fillMaxWidth().padding(horizontal = 40.px, vertical = 24.px),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(20.px),
            ) {
                Picto(row.situation.pic(), 84.px)
                T(row.situation.label, 40.tx, weight = FontWeight.ExtraBold, modifier = Modifier.width(210.px))
                Squares(row, Modifier.weight(1f))
                T(if (row.situation == Situation.BELT) "${row.passed}번" else "${row.passed}번 중 ${row.hard}번", 32.tx, weight = FontWeight.SemiBold, align = TextAlign.End, modifier = Modifier.width(170.px))
            }
            if (i < rows.lastIndex) Box(Modifier.fillMaxWidth().height(3.px).background(C.Felt))
        }
    }
    Row(Modifier.padding(horizontal = 8.px), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(28.px)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.px)) {
            Box(Modifier.size(32.px).background(C.Changed, RoundedCornerShape(9.px)))
            T("힘들어했어요", 28.tx)
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.px)) {
            Box(Modifier.size(32.px).dashedBorder(C.Calm, 3.px, 9.px))
            T("괜찮게 지나갔어요", 28.tx)
        }
        Box(Modifier.weight(1f))
        T("화면 속 숫자는 예시", 22.tx, color = C.Sub)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Squares(row: SituationRow, modifier: Modifier) {
    FlowRow(modifier, horizontalArrangement = Arrangement.spacedBy(10.px), verticalArrangement = Arrangement.spacedBy(10.px)) {
        repeat(row.hard) { Box(Modifier.size(48.px).background(C.Changed, RoundedCornerShape(14.px))) }
        repeat(row.passed - row.hard) {
            Box(Modifier.size(48.px).padding(3.px).background(C.Card).dashedBorder(C.Calm, 3.px, 14.px))
        }
    }
}

@Composable
private fun ColumnScope.Changes(policy: LeadPolicy, tunnelName: String, fill: Boolean, onClose: () -> Unit) {
    T("다음 이동부터 이렇게 해요", 40.tx, weight = FontWeight.ExtraBold, modifier = Modifier.padding(top = 14.px))
    val reroute = policy.rerouteTurnLeadSec()
    if (reroute > LeadPolicy.REROUTE_DEFAULT_SEC) {
        Plan(
            "길이 바뀌면 ${reroute / 60}분 먼저 알려요",
            "전에는 ${LeadPolicy.REROUTE_DEFAULT_SEC}초 전에 알렸어요. 길 바뀜에서 ${LeadPolicy.REROUTE_HARD_LIMIT}번 넘게 힘들어해서 바꿨어요",
            highlight = true,
        )
    }
    Plan(
        "${tunnelName}은 ${LeadPolicy.TUNNEL_EARLY_SEC / 60}분 전부터 노래를 틀어요",
        "힘들었던 순간이 이 터널에서 있었어요. 다른 터널은 그대로예요",
        highlight = false,
    )
    policy.rerouteBeforeAfter()?.let { (before, after) ->
        Column(
            (if (fill) Modifier.weight(1f) else Modifier).fillMaxWidth().background(C.Strip, RoundedCornerShape(32.px)).padding(horizontal = 44.px, vertical = 40.px),
            verticalArrangement = Arrangement.spacedBy(22.px),
        ) {
            T("길 바뀜에서 힘들었던 순간", 32.tx, weight = FontWeight.ExtraBold)
            Row((if (fill) Modifier.weight(1f) else Modifier).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.px)) {
                Compare("일찍 알리기 전 2주", before, Modifier.weight(1f))
                Compare("일찍 알린 뒤 2주", after, Modifier.weight(1f))
            }
        }
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        T("기록은 이 차 안에만 있어요. 진단이 아니라 보호자가 \"힘들어했어\"라고 말한 순간을 모은 거예요", 26.tx, color = C.Sub, modifier = Modifier.weight(1f))
        DemoNext("처음으로", Modifier.padding(start = 16.px), onClose)
    }
}

@Composable
private fun Plan(title: String, sub: String, highlight: Boolean) {
    Column(
        (if (highlight) Modifier.nowRing(6.px, 32.px) else Modifier)
            .fillMaxWidth()
            .background(C.Card, RoundedCornerShape(32.px))
            .padding(44.px),
        verticalArrangement = Arrangement.spacedBy(16.px),
    ) {
        T(title, 42.tx, weight = FontWeight.ExtraBold)
        T(sub, 29.tx, color = C.Sub, lineHeight = 1.45f)
    }
}

@Composable
private fun Compare(label: String, count: Int, modifier: Modifier) {
    Column(
        modifier.fillMaxWidth().background(C.Card, RoundedCornerShape(22.px)).padding(horizontal = 32.px, vertical = 30.px),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        T(label, 28.tx, color = C.Sub)
        T("${count}번", 72.tx, weight = FontWeight.ExtraBold)
    }
}

private fun Situation.pic(): Pic = when (this) {
    Situation.REROUTE -> Pic.DETOUR
    Situation.TUNNEL -> Pic.TUNNEL
    Situation.TRAFFIC -> Pic.CAR
    Situation.BELT -> Pic.BELT
}
