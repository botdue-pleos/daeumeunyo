package com.botdue.daeum.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RouteAndChangeTest {

    private val builder = RouteCardBuilder()

    @Test
    fun `시연 경로는 5칸이고 처음과 끝이 출발과 도착`() {
        val cards = builder.build(Demo.route)
        assertEquals(listOf("집에서 출발해", "큰 길을 달려", "터널을 지나", "빠른 길에 들어가", "병원에 도착"), cards.map { it.text })
    }

    @Test
    fun `같은 종류가 이어지면 한 칸으로 합친다`() {
        val legs = listOf(
            RouteLeg(LegKind.START, "집"),
            RouteLeg(LegKind.BIG_ROAD, "1번 도로"),
            RouteLeg(LegKind.BIG_ROAD, "2번 도로"),
            RouteLeg(LegKind.DESTINATION, "학교"),
        )
        assertEquals(3, builder.build(legs).size)
    }

    @Test
    fun `칸이 넘치면 터널은 남기고 옆 길부터 뺀다`() {
        val legs = listOf(
            RouteLeg(LegKind.START, "집"),
            RouteLeg(LegKind.LOCAL_ROAD, "골목"),
            RouteLeg(LegKind.BIG_ROAD, "큰 길"),
            RouteLeg(LegKind.TUNNEL, "터널"),
            RouteLeg(LegKind.LOCAL_ROAD, "골목2"),
            RouteLeg(LegKind.HIGHWAY, "고속도로"),
            RouteLeg(LegKind.DESTINATION, "복지관"),
        )
        val kinds = builder.build(legs).map { it.kind }
        assertEquals(5, kinds.size)
        assertEquals(listOf(LegKind.START, LegKind.BIG_ROAD, LegKind.TUNNEL, LegKind.HIGHWAY, LegKind.DESTINATION), kinds)
    }

    @Test
    fun `남은 시간은 노래 곡 수로 올림`() {
        val clock = SongClock(3.5)
        assertEquals(5, clock.songs(Demo.TOTAL_MINUTES))
        assertEquals(3, clock.songs(10.0))
        assertEquals(1, clock.songs(0.5))
        assertEquals(0, clock.songs(0.0))
    }

    @Test
    fun `길이 바뀌면 목적지가 그대로라는 말을 먼저 하고 바뀐 칸만 표시`() {
        val change = ChangeAnnouncer().compare(
            builder.build(Demo.route), builder.build(Demo.rerouted),
            Demo.MINUTES_BEFORE_REROUTE, Demo.MINUTES_AFTER_REROUTE,
        )!!
        assertTrue(change.line.startsWith("길이 바뀌었어. 그래도 병원에 가."))
        assertTrue(change.line.endsWith("노래 2곡 더."))
        assertEquals(setOf(3), change.changed)
        assertEquals(LegKind.HIGHWAY, change.removed.single().kind)
        assertTrue(change.sameDestination)
    }

    @Test
    fun `터널을 안 지나게 되면 바뀐 칸에 알려 준다`() {
        val before = builder.build(listOf(RouteLeg(LegKind.START, "집"), RouteLeg(LegKind.TUNNEL, "터널"), RouteLeg(LegKind.DESTINATION, "병원")))
        val after = builder.build(listOf(RouteLeg(LegKind.START, "집"), RouteLeg(LegKind.LOCAL_ROAD, "옆 길"), RouteLeg(LegKind.DESTINATION, "병원")))
        val change = ChangeAnnouncer().compare(before, after, 10.0, 10.0)!!
        assertEquals("터널은 이번엔 안 지나가요", change.cards[1].note)
    }

    @Test
    fun `목적지가 바뀌면 그렇다고 말한다`() {
        val before = builder.build(listOf(RouteLeg(LegKind.START, "집"), RouteLeg(LegKind.DESTINATION, "병원")))
        val after = builder.build(listOf(RouteLeg(LegKind.START, "집"), RouteLeg(LegKind.DESTINATION, "약국")))
        assertTrue(ChangeAnnouncer().compare(before, after, 5.0, 5.0)!!.line.startsWith("가는 곳이 바뀌었어. 이제 약국에 가."))
    }

    @Test
    fun `아무것도 안 바뀌면 말하지 않는다`() {
        val cards = builder.build(Demo.route)
        assertNull(ChangeAnnouncer().compare(cards, cards, 10.0, 9.0))
    }
}
