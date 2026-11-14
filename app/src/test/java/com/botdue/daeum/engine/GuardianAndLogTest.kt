package com.botdue.daeum.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class GuardianAndLogTest {

    @Test
    fun `달리는 중 뒷자리 벨트가 풀리면 쉼터 안내와 함께 알린다`() {
        val alert = GuardianWatch().onBelt(0, Seat.REAR_RIGHT, buckled = false, moving = true, restAheadM = 600)!!
        assertEquals("뒷자리 벨트를 한번 봐 주세요", alert.title)
        assertEquals("600m 앞에 쉬어 갈 곳이 있어요", alert.stopHint)
    }

    @Test
    fun `소리로 하는 말은 뒷자리가 들어도 괜찮은 말이다`() {
        val belt = GuardianWatch().onBelt(0, Seat.REAR_RIGHT, buckled = false, moving = true, restAheadM = 600)!!
        assertEquals("600m 앞에 쉬어 갈 곳이 있어요. 잠깐 쉬었다 갈까요?", belt.spoken)
        assertFalse("벨트" in belt.spoken)
        val window = GuardianWatch().onWindow(0, Seat.REAR_LEFT, 80, moving = true, restAheadM = null)!!
        assertEquals("잠깐 쉬었다 갈까요?", window.spoken)
    }

    @Test
    fun `서 있거나 앞자리면 알리지 않는다`() {
        val watch = GuardianWatch()
        assertNull(watch.onBelt(0, Seat.REAR_LEFT, buckled = false, moving = false, restAheadM = null))
        assertNull(watch.onBelt(0, Seat.PASSENGER, buckled = false, moving = true, restAheadM = null))
    }

    @Test
    fun `1분 안에 같은 알림은 한 번만`() {
        val watch = GuardianWatch()
        assertNotNull(watch.onBelt(0, Seat.REAR_LEFT, false, true, null))
        assertNull(watch.onBelt(30_000, Seat.REAR_LEFT, false, true, null))
        assertNotNull(watch.onBelt(61_000, Seat.REAR_LEFT, false, true, null))
    }

    @Test
    fun `창문은 절반 넘게 열렸을 때만`() {
        val watch = GuardianWatch()
        assertNull(watch.onWindow(0, Seat.REAR_LEFT, 30, true, null))
        assertNotNull(watch.onWindow(0, Seat.REAR_LEFT, 80, true, null))
    }

    @Test
    fun `보호자 말 알아듣기`() {
        assertEquals(GuardianCommand.REST, GuardianSpeech.parse("잠깐 쉬자"))
        assertEquals(GuardianCommand.MARK, GuardianSpeech.parse("표시해"))
        assertEquals(GuardianCommand.MARK, GuardianSpeech.parse("방금 표시 해 줘"))
        assertEquals(GuardianCommand.MARK, GuardianSpeech.parse("아까 길 바뀔 때 힘들어했어"))
        assertNull(GuardianSpeech.parse("라디오 켜 줘"))
    }

    @Test
    fun `안 힘들었다는 말은 표시하지 않는다`() {
        assertNull(GuardianSpeech.parse("오늘은 안 힘들었어"))
        assertNull(GuardianSpeech.parse("터널은 힘들지 않았어"))
        assertNull(GuardianSpeech.parse("힘들진 않았어"))
        assertNull(GuardianSpeech.parse("안 힘들어했어"))
        assertNull(GuardianSpeech.parse("표시하지 마"))
    }

    @Test
    fun `쉼터에 들르면 도착 앞에 쉼터 칸이 들어간다`() {
        val cards = RouteCardBuilder().build(Demo.route)
        val (withRest, line) = GuardianSpeech.addRest(cards, Demo.REST_NAME)
        assertEquals(LegKind.REST, withRest[withRest.lastIndex - 1].kind)
        assertEquals("잠깐 쉬었다가 병원에 가", line)
    }

    @Test
    fun `표시는 5분 안 가장 가까운 상황에 붙는다`() {
        val log = MomentLog(
            passages = listOf(Passage(0, Situation.TUNNEL, "t"), Passage(60_000, Situation.REROUTE)),
            marks = listOf(HardMark(120_000), HardMark(1_000_000)),
        )
        assertEquals(1, log.hardCount(Situation.REROUTE))
        assertEquals(0, log.hardCount(Situation.TUNNEL))
    }

    @Test
    fun `표시 직후 60초 안에 시작된 상황에도 붙는다`() {
        val log = MomentLog(
            passages = listOf(Passage(40_000, Situation.TUNNEL, "t"), Passage(500_000, Situation.REROUTE)),
            marks = listOf(HardMark(0), HardMark(300_000)),
        )
        // 0초 표시 → 40초 뒤 터널. 300초 표시 → 200초 뒤 길 바뀜은 60초를 넘어서 붙지 않고, 앞의 터널(260초 전)에 붙는다
        assertEquals(listOf(Situation.TUNNEL, Situation.TUNNEL), log.moments.map { it.passage?.situation })
        assertEquals(0, log.hardCount(Situation.REROUTE))
    }

    @Test
    fun `앞뒤에 다 있으면 시간이 더 가까운 쪽`() {
        val before = Passage(-120_000, Situation.TUNNEL, "t")
        val after = Passage(30_000, Situation.REROUTE)
        assertEquals(after, MomentLog(listOf(before, after), emptyList()).attach(HardMark(0)))
        // 거리가 같으면 앞서 지나간 쪽
        val sameBefore = Passage(-30_000, Situation.TRAFFIC)
        assertEquals(sameBefore, MomentLog(listOf(sameBefore, after), emptyList()).attach(HardMark(0)))
    }

    @Test
    fun `붙일 상황이 없는 표시는 버리지 않고 상황 없음으로 남긴다`() {
        val log = MomentLog(
            passages = listOf(Passage(0, Situation.TUNNEL, "t")),
            marks = listOf(HardMark(60_000), HardMark(3_600_000)),
        )
        assertEquals(2, log.moments.size)
        assertNull(log.moments[1].passage)
        assertEquals(1, log.unattached)
        // 상황별 숫자에는 들어가지 않는다
        assertEquals(1, log.rows().sumOf { it.hard })
    }

    @Test
    fun `시연 한 달 기록 숫자`() {
        val rows = Demo.monthLog().rows().associateBy { it.situation }
        assertEquals(4 to 3, rows.getValue(Situation.REROUTE).let { it.passed to it.hard })
        assertEquals(9 to 1, rows.getValue(Situation.TUNNEL).let { it.passed to it.hard })
        assertEquals(5 to 0, rows.getValue(Situation.TRAFFIC).let { it.passed to it.hard })
        assertEquals(1 to 1, rows.getValue(Situation.BELT).let { it.passed to it.hard })
    }

    @Test
    fun `길 바뀜에 두 번 표시하면 꺾기 예고를 1분 전으로, 전후 2주 비교`() {
        val policy = LeadPolicy(Demo.monthLog())
        assertEquals(60, policy.rerouteTurnLeadSec())
        assertEquals(2 to 1, policy.rerouteBeforeAfter())
        assertEquals(30, LeadPolicy(MomentLog(emptyList(), emptyList())).rerouteTurnLeadSec())
    }
}
