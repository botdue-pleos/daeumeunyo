package com.botdue.daeum.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TunnelCueTest {

    @Test
    fun `1분 전에 노래와 내기순환을 켜고 30초 전에 한 번 더 말한다`() {
        val cue = TunnelCue()
        assertTrue(cue.onTbt("t", 2_000, 55.0).isEmpty())
        val first = cue.onTbt("t", Demo.tunnelFirst.first, Demo.tunnelFirst.second)
        assertTrue(CueAction.PlayMusic in first)
        assertTrue(CueAction.Recirculation(true) in first)
        assertEquals(TunnelStage.FIRST, cue.stage("t"))
        val second = cue.onTbt("t", Demo.tunnelSecond.first, Demo.tunnelSecond.second)
        assertEquals(listOf(CueAction.Speak("이제 터널에 들어가. 노래 들으면서 가자")), second)
        // 같은 단계는 다시 말하지 않는다
        assertTrue(cue.onTbt("t", 350, 55.0).isEmpty())
    }

    @Test
    fun `빠져나오면 노래와 공조를 원래대로`() {
        val cue = TunnelCue()
        cue.onTbt("t", 900, 55.0)
        val out = cue.onPassed("t")
        assertTrue(CueAction.StopMusic in out)
        assertTrue(CueAction.Recirculation(false) in out)
    }

    @Test
    fun `예고하지 않은 터널을 지나면 아무것도 끄지 않는다`() {
        val cue = TunnelCue()
        assertTrue(cue.onPassed("t").isEmpty())
    }

    @Test
    fun `늦게 안내받아 바로 30초 안이면 노래를 같이 켠다`() {
        val cue = TunnelCue()
        val actions = cue.onTbt("t", 300, 60.0)
        assertTrue(CueAction.PlayMusic in actions)
        assertEquals(1, actions.count { it is CueAction.Speak })
    }

    @Test
    fun `정체로 거의 서 있으면 너무 이르게 말하지 않는다`() {
        val cue = TunnelCue()
        // 시속 3km로 900m면 실제론 18분이지만, 시속 20km로 가정해 162초 → 아직 말하지 않음
        assertTrue(cue.onTbt("t", 900, 3.0).isEmpty())
    }

    @Test
    fun `힘들어했던 터널은 2분 전부터`() {
        val policy = LeadPolicy(Demo.monthLog())
        val cue = TunnelCue(leadSec = policy::tunnelLeadSec)
        // 1700m, 55km/h → 약 111초: 보통 터널은 아직, 집 앞 터널은 첫 예고
        assertTrue(cue.onTbt("other", 1_700, 55.0).isEmpty())
        assertTrue(CueAction.PlayMusic in cue.onTbt(Demo.HOME_TUNNEL, 1_700, 55.0))
    }
}
