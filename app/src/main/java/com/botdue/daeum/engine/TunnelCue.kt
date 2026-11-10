package com.botdue.daeum.engine

/** 엔진이 차에 시키는 일. 화면·소리·공조는 앱 쪽에서 실제로 한다. */
sealed interface CueAction {
    data class Speak(val text: String) : CueAction
    data object PlayMusic : CueAction
    data object StopMusic : CueAction
    data class Recirculation(val on: Boolean) : CueAction
}

enum class TunnelStage { NONE, FIRST, SECOND, INSIDE, PASSED }

/**
 * 터널 앞 단계 예고.
 * NaviHelper onTBTInfo가 다음 지점이 터널(TURN_TUNNEL)이라고 알려 주면, 남은 거리 ÷ 차 속도로
 * 몇 초 뒤에 들어가는지 계산해 1분 전(첫 예고 + 노래 + 내기순환), 30초 전(한 번 더) 두 번 말한다.
 * 힘들어했던 터널은 [leadSec]가 더 길게 준다 ([LeadPolicy]).
 */
class TunnelCue(
    private val leadSec: (tunnelId: String) -> Int = { LeadPolicy.TUNNEL_DEFAULT_SEC },
    private val secondSec: Int = 30,
) {
    private val stages = mutableMapOf<String, TunnelStage>()

    fun stage(tunnelId: String): TunnelStage = stages[tunnelId] ?: TunnelStage.NONE

    /** 터널까지 몇 초 남았는지. 거의 서 있으면(정체) 시속 20km로 가정해 너무 이르게 말하지 않게 한다 */
    fun secondsTo(distanceM: Int, speedKmh: Double): Int {
        val v = maxOf(speedKmh, 20.0) / 3.6
        return (distanceM / v).toInt()
    }

    fun onTbt(tunnelId: String, distanceM: Int, speedKmh: Double): List<CueAction> {
        val now = stage(tunnelId)
        if (now >= TunnelStage.INSIDE) return emptyList()
        val sec = secondsTo(distanceM, speedKmh)
        return when {
            distanceM <= 0 -> {
                stages[tunnelId] = TunnelStage.INSIDE
                if (now < TunnelStage.FIRST) startActions() else emptyList()
            }
            sec <= secondSec && now < TunnelStage.SECOND -> {
                stages[tunnelId] = TunnelStage.SECOND
                // 첫 예고를 건너뛸 만큼 가까워졌으면(늦게 안내받음) 노래·공조도 같이 시작한다
                (if (now < TunnelStage.FIRST) startActions().filter { it !is CueAction.Speak } else emptyList()) +
                    CueAction.Speak("이제 터널에 들어가. 노래 들으면서 가자")
            }
            sec <= leadSec(tunnelId) && now < TunnelStage.FIRST -> {
                stages[tunnelId] = TunnelStage.FIRST
                startActions()
            }
            else -> emptyList()
        }
    }

    /**
     * 터널을 빠져나옴. 실제로는 TBT의 다음 지점이 터널 뒤로 넘어가거나 예상 통과 시간이 지났을 때.
     * 노래·공조는 이 앱이 켠 경우에만 원래대로 돌린다.
     */
    fun onPassed(tunnelId: String): List<CueAction> {
        val started = stage(tunnelId) in TunnelStage.FIRST..TunnelStage.INSIDE
        stages[tunnelId] = TunnelStage.PASSED
        if (!started) return emptyList()
        return listOf(CueAction.Speak("다 지나왔어. 이제 밝아"), CueAction.StopMusic, CueAction.Recirculation(false))
    }

    private fun startActions() = listOf(
        CueAction.Speak("곧 터널이야. 금방 밝아져"),
        CueAction.PlayMusic,
        CueAction.Recirculation(true),
    )
}
