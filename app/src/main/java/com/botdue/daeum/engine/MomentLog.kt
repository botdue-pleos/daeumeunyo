package com.botdue.daeum.engine

/** 동승자가 겪는 상황. 차량 신호로 자동 기록된다 */
enum class Situation(val label: String) {
    REROUTE("길 바뀜"),
    TUNNEL("터널"),
    TRAFFIC("막힘"),
    BELT("벨트 풀림"),
}

/** 상황을 한 번 지나감. [key]는 같은 터널을 다시 알아보는 값 (터널 id) */
data class Passage(val timeMs: Long, val situation: Situation, val key: String = "")

/** 보호자가 "힘들어했어"라고 말한 순간 */
data class HardMark(val timeMs: Long)

data class SituationRow(val situation: Situation, val passed: Int, val hard: Int)

/**
 * 힘들었던 순간 기록. 진단이 아니라 보호자가 말한 순간을 그 직전 상황과 묶어 둘 뿐이다.
 * "힘들어했어"는 보통 상황이 끝난 뒤에 말하므로, [windowMs] 안에 있었던 가장 가까운 상황 하나에 붙인다.
 */
class MomentLog(
    val passages: List<Passage>,
    val marks: List<HardMark>,
    private val windowMs: Long = 5 * 60_000,
) {
    /** 각 표시가 어느 상황에 붙었는지 */
    val hardPassages: Set<Passage> by lazy {
        marks.mapNotNull { m ->
            passages.filter { it.timeMs <= m.timeMs && m.timeMs - it.timeMs <= windowMs }.maxByOrNull { it.timeMs }
        }.toSet()
    }

    fun rows(): List<SituationRow> = Situation.entries.map { s ->
        SituationRow(s, passages.count { it.situation == s }, hardPassages.count { it.situation == s })
    }

    fun hardCount(situation: Situation, key: String? = null, fromMs: Long = Long.MIN_VALUE, untilMs: Long = Long.MAX_VALUE): Int =
        hardPassages.count {
            it.situation == situation && (key == null || it.key == key) && it.timeMs in fromMs..untilMs
        }

    /** 어느 상황이 몇 번째 힘들었는지 시간순으로 */
    fun hardTimes(situation: Situation): List<Long> =
        hardPassages.filter { it.situation == situation }.map { it.timeMs }.sorted()
}

/**
 * 기록을 다음 이동의 예고 시점에 반영하는 규칙. 단순하고 설명할 수 있게 둔다.
 * - 터널: 그 터널에서 한 번이라도 힘들어했으면 그 터널만 1분 → 2분 전부터
 * - 길 바뀜: 두 번 힘들어했으면 바뀐 길에서 꺾기 전 예고를 30초 → 1분 전으로
 */
class LeadPolicy(private val log: MomentLog) {

    fun tunnelLeadSec(tunnelId: String): Int =
        if (log.hardCount(Situation.TUNNEL, key = tunnelId) >= 1) TUNNEL_EARLY_SEC else TUNNEL_DEFAULT_SEC

    fun rerouteTurnLeadSec(): Int =
        if (log.hardCount(Situation.REROUTE) >= REROUTE_HARD_LIMIT) REROUTE_EARLY_SEC else REROUTE_DEFAULT_SEC

    /** 길 바뀜 예고를 앞당긴 때. 그 전후 같은 기간 동안 힘들었던 횟수를 나란히 보여 주는 데 쓴다 */
    fun rerouteChangedAt(): Long? = log.hardTimes(Situation.REROUTE).getOrNull(REROUTE_HARD_LIMIT - 1)

    /** 앞당기기 전 [days]일(바꾼 날 포함)과 뒤 [days]일의 길 바뀜 힘든 횟수 */
    fun rerouteBeforeAfter(days: Int = 14): Pair<Int, Int>? {
        val at = rerouteChangedAt() ?: return null
        val span = days * DAY_MS
        return log.hardCount(Situation.REROUTE, fromMs = at - span + 1, untilMs = at) to
            log.hardCount(Situation.REROUTE, fromMs = at + 1, untilMs = at + span)
    }

    companion object {
        const val TUNNEL_DEFAULT_SEC = 60
        const val TUNNEL_EARLY_SEC = 120
        const val REROUTE_DEFAULT_SEC = 30
        const val REROUTE_EARLY_SEC = 60
        const val REROUTE_HARD_LIMIT = 2
        const val DAY_MS = 24 * 60 * 60_000L
    }
}
