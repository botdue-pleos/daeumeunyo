package com.botdue.daeum.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.Dp
import com.botdue.daeum.engine.LegKind

/**
 * 그림 카드의 그림. 의사소통 그림 카드처럼 굵은 선 하나로 그린다 (96x96 기준 선 굵기 6).
 * [fills]는 속을 칠하는 선 (터널 안쪽 어둠, 음표 머리).
 */
enum class Pic(val strokes: List<String>, val fills: List<String> = emptyList()) {
    HOME(listOf("M12 46 L48 16 L84 46", "M22 38 V82 H74 V38", "M40 82 V60 H56 V82")),
    ROAD(listOf("M26 86 L40 12", "M70 86 L56 12", "M48 20 V28", "M48 42 V52", "M48 66 V78")),
    HIGHWAY(listOf("M30 86 L42 12", "M78 86 L60 12", "M53 20 V28", "M55 42 V52", "M57 66 V78", "M6 36 H20", "M2 52 H16", "M6 68 H20")),
    DETOUR(listOf("M30 88 V60 C30 42 66 44 66 26 V12", "M54 22 L66 10 L78 22")),
    TUNNEL(listOf("M8 84 V50 A40 40 0 0 1 88 50 V84", "M4 84 H92"), listOf("M28 84 V58 A20 20 0 0 1 68 58 V84 Z")),
    HOSPITAL(listOf("M18 84 V20 H78 V84", "M48 34 V62", "M34 48 H62", "M8 84 H88")),
    REST(listOf(
        "M22 40 H66 V58 C66 70 58 78 44 78 C30 78 22 70 22 58 Z",
        "M66 46 H72 C78 46 80 50 80 54 C80 58 78 62 72 62 H66",
        "M36 30 C36 24 40 24 40 18", "M50 30 C50 24 54 24 54 18", "M14 86 H74",
    )),
    CHECK(listOf("M20 50 L40 70 L78 30")),
    /** 안전벨트 표준 그림처럼: 좌석에 앉은 사람 + 어깨에서 허리로 내려오는 사선 벨트 + 버클 */
    BELT(listOf(
        "M39 17 a9 9 0 1 0 18 0 a9 9 0 1 0 -18 0",
        "M30 84 V52 C30 41 38 34 48 34 C58 34 66 41 66 52 V84",
        "M18 84 V36 C18 29 22 25 28 25", "M78 84 V36 C78 29 74 25 68 25", "M10 84 H86",
        "M63 39 L42 66", "M36 66 H48 V76 H36 Z",
    )),
    CAR(listOf("M12 64 V50 L24 30 H72 L84 50 V64 Z", "M12 50 H84", "M21 68 a7 7 0 1 0 14 0 a7 7 0 1 0 -14 0", "M61 68 a7 7 0 1 0 14 0 a7 7 0 1 0 -14 0")),
    NOTE(listOf("M36 72 V20 L72 12 V64"), listOf("M16 72 a10 10 0 1 0 20 0 a10 10 0 1 0 -20 0", "M52 64 a10 10 0 1 0 20 0 a10 10 0 1 0 -20 0")),
    RECIRC(listOf("M16 48 A32 32 0 1 1 32 75.7", "M16 72 V48 H40")),
    MIC(listOf("M48 12 a12 12 0 0 1 12 12 V44 a12 12 0 0 1 -24 0 V24 a12 12 0 0 1 12 -12 Z", "M20 44 A28 28 0 0 0 76 44", "M48 72 V84")),
}

fun LegKind.pic(): Pic = when (this) {
    LegKind.START -> Pic.HOME
    LegKind.BIG_ROAD -> Pic.ROAD
    LegKind.LOCAL_ROAD -> Pic.DETOUR
    LegKind.HIGHWAY -> Pic.HIGHWAY
    LegKind.TUNNEL -> Pic.TUNNEL
    LegKind.REST -> Pic.REST
    LegKind.DESTINATION -> Pic.HOSPITAL
}

@Composable
fun Picto(pic: Pic, size: Dp, color: Color = C.Ink, strokeWidth: Float = 6f, modifier: Modifier = Modifier) {
    val vector = remember(pic, color, strokeWidth) {
        ImageVector.Builder(defaultWidth = size, defaultHeight = size, viewportWidth = 96f, viewportHeight = 96f).apply {
            pic.fills.forEach { addPath(addPathNodes(it), fill = SolidColor(color)) }
            pic.strokes.forEach {
                addPath(
                    addPathNodes(it),
                    stroke = SolidColor(color),
                    strokeLineWidth = strokeWidth,
                    strokeLineCap = StrokeCap.Round,
                    strokeLineJoin = StrokeJoin.Round,
                )
            }
        }.build()
    }
    Image(vector, contentDescription = null, modifier = modifier.size(size))
}
