package com.botdue.daeum.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.intl.LocaleList
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.em

@Composable
fun T(
    text: String,
    size: TextUnit,
    modifier: Modifier = Modifier,
    weight: FontWeight = FontWeight.Normal,
    color: Color = C.Ink,
    lineHeight: Float = 1.3f,
    align: TextAlign? = null,
) {
    Text(
        text = text,
        modifier = modifier,
        style = TextStyle(
            fontFamily = LocalFont.current,
            fontSize = size,
            fontWeight = weight,
            color = color,
            lineHeight = lineHeight.em,
            letterSpacing = if (size.value >= 30) (-0.02).em else 0.em,
            textAlign = align ?: TextAlign.Unspecified,
            // 한국어를 어절 단위로 줄바꿈 (기기 언어와 상관없이 ko로 지정해야 적용된다)
            localeList = LocaleList("ko-KR"),
            lineBreak = LineBreak(
                strategy = LineBreak.Strategy.HighQuality,
                strictness = LineBreak.Strictness.Strict,
                wordBreak = LineBreak.WordBreak.Phrase,
            ),
        ),
    )
}

/** 점선 테두리 (지난 칸 = 회녹색, 바뀐 칸 = 보라색) */
fun Modifier.dashedBorder(color: Color, width: Dp, radius: Dp): Modifier = drawWithContent {
    // 배경 위에 그려야 보이므로 내용 다음에 그린다
    drawContent()
    val w = width.toPx()
    drawRoundRect(
        color = color,
        topLeft = androidx.compose.ui.geometry.Offset(w / 2, w / 2),
        size = androidx.compose.ui.geometry.Size(size.width - w, size.height - w),
        cornerRadius = CornerRadius(radius.toPx()),
        style = Stroke(width = w, pathEffect = PathEffect.dashPathEffect(floatArrayOf(w * 2.2f, w * 1.6f))),
    )
}

/** 지금 칸: 겨자색 테두리를 카드 바깥에 두른다 */
fun Modifier.nowRing(width: Dp, radius: Dp): Modifier =
    border(width, C.Now, RoundedCornerShape(radius))

@Composable
fun PrimaryButton(text: String, modifier: Modifier = Modifier, height: Int = 104, size: Int = 36, onClick: () -> Unit) {
    Box(
        modifier
            .height(height.px)
            .background(C.Ink, RoundedCornerShape(50))
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { T(text, size.tx, color = C.Card, weight = FontWeight.ExtraBold) }
}

@Composable
fun SecondaryButton(text: String, modifier: Modifier = Modifier, height: Int = 84, size: Int = 28, onClick: () -> Unit) {
    Box(
        modifier
            .height(height.px)
            .border(3.px, C.Ink, RoundedCornerShape(50))
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { T(text, size.tx, weight = FontWeight.SemiBold) }
}

/** 남은 노래 수. 꽉 찬 원 = 남은 곡, 보라 점선 원 = 길이 바뀌어 늘어난 곡 */
@Composable
fun SongDots(songs: Int, extra: Int = 0, dot: Int = 44, withNote: Boolean = true) {
    Row(horizontalArrangement = Arrangement.spacedBy((dot / 4).px)) {
        repeat(songs - extra) {
            Box(Modifier.size(dot.px).background(C.Ink, CircleShape), contentAlignment = Alignment.Center) {
                if (withNote) Picto(Pic.NOTE, (dot * 0.55f).toInt().px, C.Card, strokeWidth = 9f)
            }
        }
        repeat(extra) {
            Box(Modifier.size(dot.px).dashedBorder(C.Changed, 3.px, (dot / 2).px))
        }
    }
}

/** 시연 조작판: 다음 장면으로 넘기는 작은 버튼. 실제 차량에서는 SDK 콜백이 이 역할을 한다 */
@Composable
fun DemoNext(label: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier
            .background(C.Ink.copy(alpha = 0.08f), RoundedCornerShape(50))
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 22.px, vertical = 12.px),
    ) { T("시연: $label", 22.tx, color = C.Sub, weight = FontWeight.SemiBold) }
}

@Composable
fun Chip(pic: Pic, text: String, modifier: Modifier = Modifier) {
    Row(
        modifier
            .background(C.Card, RoundedCornerShape(24.px))
            .padding(horizontal = 26.px, vertical = 22.px),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.px),
    ) {
        Picto(pic, 36.px, strokeWidth = 8f)
        T(text, 26.tx, weight = FontWeight.SemiBold)
    }
}

/**
 * 보호자에게 하는 존댓말 한 줄. 그림 카드 밖에, 보조 색으로 둔다.
 * 카드 안 굵은 반말 = 동승자에게 하는 말, 카드 밖 이 줄 = 보호자에게 하는 말로 나눠 보이게.
 */
@Composable
fun GuardianLine(text: String, modifier: Modifier = Modifier, size: Int = 26) {
    Row(modifier.padding(horizontal = 8.px), horizontalArrangement = Arrangement.spacedBy(16.px)) {
        Box(Modifier.padding(top = 6.px).width(6.px).height((size + 6).px).background(C.Done, RoundedCornerShape(3.px)))
        T(text, size.tx, color = C.Sub)
    }
}

/**
 * 운전 중 "표시해" 뒤 구석에 몇 초 뜨는 작은 칩. 경고색(빨강·보라)은 쓰지 않고,
 * "힘들어" 같은 말 없이 붙은 상황 이름만 쓴다 ("표시됨 · 방금 터널").
 */
@Composable
fun MarkChip(situation: String?, modifier: Modifier = Modifier) {
    Row(
        modifier
            .background(C.Card, RoundedCornerShape(50))
            .border(2.px, C.Ink.copy(alpha = 0.35f), RoundedCornerShape(50))
            .padding(horizontal = 22.px, vertical = 8.px),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.px),
    ) {
        Picto(Pic.CHECK, 26.px, C.DoneInk, strokeWidth = 10f)
        T(if (situation != null) "표시됨 · 방금 $situation" else "표시됨 · 상황 없음", 22.tx, color = C.Ink, weight = FontWeight.SemiBold)
    }
}
