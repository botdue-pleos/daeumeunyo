package com.botdue.daeum.engine

import java.time.LocalDateTime
import java.time.ZoneId

/**
 * 시연용 데이터. 실제 차량에서는 PleosSignalSource가 NaviHelper·Vehicle SDK 콜백을 같은 모양으로 넘긴다.
 * 시나리오: 보호자(어머니)가 12세 자녀를 뒷자리에 태우고 병원에 간다.
 */
object Demo {
    const val HOME_TUNNEL = "home-tunnel"

    val route = listOf(
        RouteLeg(LegKind.START, "집"),
        RouteLeg(LegKind.BIG_ROAD, "큰 길", "big-road"),
        RouteLeg(LegKind.TUNNEL, "집 앞 터널", HOME_TUNNEL),
        RouteLeg(LegKind.HIGHWAY, "강변 빠른 길", "river-highway"),
        RouteLeg(LegKind.DESTINATION, "병원"),
    )

    /** 빠른 길 앞에 사고가 나서 내비가 옆 길로 다시 찾은 경로 */
    val rerouted = listOf(
        RouteLeg(LegKind.START, "집"),
        RouteLeg(LegKind.BIG_ROAD, "큰 길", "big-road"),
        RouteLeg(LegKind.TUNNEL, "집 앞 터널", HOME_TUNNEL),
        RouteLeg(LegKind.LOCAL_ROAD, "강변 옆 길", "river-side"),
        RouteLeg(LegKind.DESTINATION, "병원"),
    )
    const val REROUTE_REASON = "앞에 사고가 나서 옆 길로 돌아가요"

    const val TOTAL_MINUTES = 17.0
    const val MINUTES_BEFORE_REROUTE = 10.0
    const val MINUTES_AFTER_REROUTE = 17.0
    const val REST_NAME = "강변 쉼터"
    const val REST_AHEAD_M = 600

    /** 터널 앞 TBT: (남은 거리 m, 차 속도 km/h). 1분 전, 30초 전 */
    val tunnelFirst = 900 to 55.0
    val tunnelSecond = 400 to 55.0

    private val zone = ZoneId.of("Asia/Seoul")
    private fun at(day: Int, hour: Int, minute: Int) =
        LocalDateTime.of(2026, 11, day, hour, minute).atZone(zone).toInstant().toEpochMilli()

    const val MONTH_TRIPS = 12

    /**
     * 11월 한 달 이동 기록. 화면 속 숫자는 예시다.
     * 길 바뀜 4번 중 3번, 터널 9번 중 1번(집 앞 터널), 막힘 5번 중 0번, 벨트 풀림 1번.
     * 길 바뀜 두 번째 힘듦(11/8) 뒤로 예고를 앞당겼고, 그 뒤 2주 동안은 1번.
     */
    fun monthLog(): MomentLog {
        val passages = mutableListOf<Passage>()
        val marks = mutableListOf<HardMark>()
        fun pass(day: Int, h: Int, m: Int, s: Situation, key: String = "", hard: Boolean = false) {
            val t = at(day, h, m)
            passages += Passage(t, s, key)
            if (hard) marks += HardMark(t + 2 * 60_000)
        }
        pass(3, 10, 10, Situation.REROUTE, hard = true)
        pass(8, 15, 20, Situation.REROUTE, hard = true)
        pass(12, 10, 40, Situation.REROUTE)
        pass(17, 16, 5, Situation.REROUTE, hard = true)
        listOf(2, 3, 5, 8, 10, 12, 15, 17, 19).forEach { d ->
            pass(d, 9, 30, Situation.TUNNEL, key = if (d == 5) HOME_TUNNEL else "tunnel-$d", hard = d == 5)
        }
        listOf(4, 9, 11, 16, 18).forEach { d -> pass(d, 17, 50, Situation.TRAFFIC) }
        pass(14, 11, 15, Situation.BELT, hard = true)
        return MomentLog(passages, marks)
    }
}
