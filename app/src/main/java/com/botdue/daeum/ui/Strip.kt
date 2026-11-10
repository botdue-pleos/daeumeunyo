package com.botdue.daeum.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.botdue.daeum.engine.Card

enum class CardState { DONE, NOW, NEXT, CHANGED }

fun cardStates(count: Int, current: Int, changed: Set<Int>): List<CardState> = List(count) { i ->
    when {
        i < current -> CardState.DONE
        // 바뀐 칸은 지금 칸이어도 보라색 점선으로 (무엇이 바뀌었는지가 더 중요)
        i in changed -> CardState.CHANGED
        i == current -> CardState.NOW
        else -> CardState.NEXT
    }
}

/** 띠에 붙은 카드 목록 (세로). 출발 전과 길이 바뀐 화면에서 쓴다 */
@Composable
fun VerticalStrip(
    cards: List<Card>,
    states: List<CardState>,
    modifier: Modifier = Modifier,
    /** 5칸이면 카드를 조금 작게 (세로 1칸 창에 다 들어가게) */
    compact: Boolean = false,
    label: (Int) -> String? = { null },
) {
    val icon = if (compact) 80 else 104
    val title = if (compact) 34 else 38
    val vpad = if (compact) 12 else 20
    Column(
        modifier
            .fillMaxWidth()
            .background(C.Strip, RoundedCornerShape(36.px))
            .padding(20.px),
        verticalArrangement = Arrangement.spacedBy((if (compact) 14 else 18).px),
    ) {
        cards.forEachIndexed { i, card ->
            when (states[i]) {
                CardState.DONE -> Row(
                    Modifier
                        .fillMaxWidth()
                        .dashedBorder(C.Done, 3.px, 22.px)
                        .padding(horizontal = 28.px, vertical = 14.px),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(24.px),
                ) {
                    Picto(Pic.CHECK, 48.px, C.DoneInk, strokeWidth = 8f)
                    T(card.doneText, 28.tx, color = C.Sub, weight = FontWeight.SemiBold)
                }
                else -> {
                    val state = states[i]
                    var m = Modifier.fillMaxWidth()
                    m = when (state) {
                        CardState.NOW -> m.nowRing(6.px, 26.px)
                        CardState.CHANGED -> m.dashedBorder(C.Changed, 5.px, 26.px)
                        else -> m
                    }
                    Row(
                        m.background(C.Card, RoundedCornerShape(26.px)).padding(horizontal = 28.px, vertical = vpad.px),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(28.px),
                    ) {
                        Picto(card.kind.pic(), icon.px)
                        Column(verticalArrangement = Arrangement.spacedBy(6.px)) {
                            val tag = label(i) ?: if (state == CardState.CHANGED) "바뀐 칸" else null
                            if (tag != null) T(tag, 22.tx, weight = FontWeight.ExtraBold, color = if (state == CardState.CHANGED) C.Changed else C.Sub)
                            T(card.text, title.tx, weight = FontWeight.ExtraBold)
                            card.note?.let { T(it, 24.tx, color = C.Sub) }
                        }
                    }
                }
            }
        }
    }
}

/** 주행 중 위쪽의 작은 띠 (가로). 지난 칸은 체크만, 지금 칸은 겨자색 */
@Composable
fun MiniStrip(cards: List<Card>, states: List<CardState>, modifier: Modifier = Modifier) {
    Row(
        modifier
            .fillMaxWidth()
            .background(C.Strip, RoundedCornerShape(30.px))
            .padding(14.px),
        horizontalArrangement = Arrangement.spacedBy(12.px),
    ) {
        cards.forEachIndexed { i, card ->
            val state = states[i]
            val base = Modifier.weight(1f).height(112.px)
            val m = when (state) {
                CardState.DONE -> base.dashedBorder(C.Done, 3.px, 20.px)
                CardState.NOW -> base.nowRing(6.px, 20.px).background(C.Card, RoundedCornerShape(20.px))
                CardState.CHANGED -> base.dashedBorder(C.Changed, 4.px, 20.px).background(C.Card, RoundedCornerShape(20.px))
                CardState.NEXT -> base.background(C.Card, RoundedCornerShape(20.px))
            }
            Box(m, contentAlignment = Alignment.Center) {
                if (state == CardState.DONE) Picto(Pic.CHECK, 56.px, C.DoneInk, strokeWidth = 8f)
                else Picto(card.kind.pic(), 68.px)
            }
        }
    }
}

@Composable
fun Gap(h: Int) = Box(Modifier.height(h.px).width(1.px))
