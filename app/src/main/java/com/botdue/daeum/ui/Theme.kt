package com.botdue.daeum.ui

import android.content.res.AssetManager
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 특수교육에서 쓰는 "그림 일정표 띠"에서 가져온 색.
 * 자극이 적은 세이지색 펠트 바탕에 흰 그림 카드. 강조는 두 가지만:
 * 겨자색 = 지금 칸, 보라색 점선 = 바뀐 칸. 경고에도 빨강은 쓰지 않는다 (동승자를 놀라게 하지 않게).
 */
object C {
    val Felt = Color(0xFFDDE6DC)
    /** 카드를 붙이는 띠 */
    val Strip = Color(0xFFC7D3C6)
    val Card = Color(0xFFFFFFFF)
    val Ink = Color(0xFF1C3A37)
    val Sub = Color(0xFF4E6763)
    /** 지난 칸 테두리·체크 */
    val Done = Color(0xFF9FB0A0)
    val DoneInk = Color(0xFF5F7562)
    val Now = Color(0xFFF3C969)
    val Changed = Color(0xFF6E5AA8)
    /** 괜찮게 지나간 칸 */
    val Calm = Color(0xFFA7B7A9)
    /** 어두운 띠 위 보조 글자 */
    val OnInkSub = Color(0xFFC9D8D5)
}

/**
 * Pleos 공식 서체 Asta Sans. 가변 글꼴 하나를 굵기별로 불러 쓴다.
 * res/font로 넣으면 굵기(wght) 축이 적용되지 않아 assets에서 읽는다.
 */
@OptIn(ExperimentalTextApi::class)
fun astaSans(assets: AssetManager): FontFamily = FontFamily(
    listOf(FontWeight.Normal, FontWeight.SemiBold, FontWeight.Bold, FontWeight.ExtraBold).map { w ->
        Font(
            path = "fonts/AstaSans.ttf",
            assetManager = assets,
            weight = w,
            variationSettings = FontVariation.Settings(FontVariation.weight(w.weight)),
        )
    }
)

val LocalFont = staticCompositionLocalOf<FontFamily> { FontFamily.Default }

/** 시안(920x1330 px 창 기준) 수치를 그대로 옮기기 위한 변환. 글자는 Pleos 최소 18sp 아래로 내리지 않는다 */
val Int.px: Dp get() = (this / 1.5f).dp
val Int.tx: TextUnit get() = maxOf(this / 1.5f, 18f).sp
