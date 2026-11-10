package com.botdue.daeum.engine

import kotlin.math.ceil

/** 경로의 한 구간. 실제 차량에서는 NaviHelper 경로·TBT 정보에서 만든다. */
enum class LegKind { START, BIG_ROAD, LOCAL_ROAD, HIGHWAY, TUNNEL, REST, DESTINATION }

data class RouteLeg(
    val kind: LegKind,
    val name: String,
    /** 같은 터널·길을 다시 알아보기 위한 값. 실제로는 TBT 지점 좌표나 도로 이름 */
    val id: String = name,
)

/** 동승자에게 보여 주는 카드 한 칸. 쉬운 말 한 문장 + 덧붙임 한 줄 */
data class Card(
    val kind: LegKind,
    val text: String,
    val note: String? = null,
    val legId: String,
    /** 지나간 뒤 띠에 남기는 말 ("집에서 출발했어") */
    val doneText: String = text,
)

/**
 * 내비 경로를 3~5칸 카드로 줄인다.
 * 처음(출발)과 끝(도착)은 늘 남기고, 가운데는 동승자가 느끼는 변화가 큰 구간부터 남긴다.
 * 터널(어두워지고 소리가 울림) > 빠른 길·쉼터 > 큰 길 > 옆 길.
 */
class RouteCardBuilder(private val maxCards: Int = 5) {

    fun build(legs: List<RouteLeg>): List<Card> {
        require(maxCards >= 3) { "카드는 3칸 이상" }
        val start = legs.firstOrNull { it.kind == LegKind.START }
        val dest = legs.lastOrNull { it.kind == LegKind.DESTINATION }
        // 같은 종류가 이어지면 한 칸으로 (큰 길 → 큰 길은 동승자에게 같은 장면)
        val middle = legs
            .filter { it.kind != LegKind.START && it.kind != LegKind.DESTINATION }
            .fold(mutableListOf<RouteLeg>()) { acc, leg ->
                if (acc.lastOrNull()?.kind != leg.kind) acc += leg
                acc
            }
        while (middle.size > maxCards - 2) {
            // 우선순위가 가장 낮은 것 중 뒤쪽부터 뺀다
            val drop = middle.indices.minWith(compareBy<Int> { priority(middle[it].kind) }.thenByDescending { it })
            middle.removeAt(drop)
        }
        return listOfNotNull(start).map(::toCard) + middle.map(::toCard) + listOfNotNull(dest).map(::toCard)
    }

    private fun priority(kind: LegKind) = when (kind) {
        LegKind.TUNNEL -> 3
        LegKind.HIGHWAY, LegKind.REST -> 2
        LegKind.BIG_ROAD -> 1
        else -> 0
    }

    companion object {
        fun toCard(leg: RouteLeg): Card = when (leg.kind) {
            LegKind.START -> Card(leg.kind, "${leg.name}에서 출발해", legId = leg.id, doneText = "${leg.name}에서 출발했어")
            LegKind.BIG_ROAD -> Card(leg.kind, "큰 길을 달려", legId = leg.id, doneText = "큰 길을 달렸어")
            LegKind.LOCAL_ROAD -> Card(leg.kind, "옆 길로 돌아가", legId = leg.id, doneText = "옆 길로 돌아갔어")
            LegKind.HIGHWAY -> Card(leg.kind, "빠른 길에 들어가", "차가 조금 빨라져", leg.id, "빠른 길을 달렸어")
            LegKind.TUNNEL -> Card(leg.kind, "터널을 지나", "잠깐 어두워져. 좋아하는 노래가 나와", leg.id, "터널을 지나왔어")
            LegKind.REST -> Card(leg.kind, "${leg.name}에서 쉬어 가", legId = leg.id, doneText = "${leg.name}에서 쉬었어")
            LegKind.DESTINATION -> Card(leg.kind, "${leg.name}에 도착", legId = leg.id)
        }
    }
}

/**
 * 남은 시간을 분이 아니라 "노래 몇 곡"으로 바꾼다. 시계를 읽기 어려운 동승자도 셀 수 있게.
 * 한 곡 길이는 보호자가 정한다 (기본 3분 30초).
 */
class SongClock(private val songMinutes: Double = 3.5) {
    fun songs(remainingMinutes: Double): Int =
        if (remainingMinutes <= 0) 0 else maxOf(1, ceil(remainingMinutes / songMinutes - 1e-9).toInt())
}
