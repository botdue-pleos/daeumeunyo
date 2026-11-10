package com.botdue.daeum.engine

/** 길이 바뀐 뒤 동승자에게 할 말과, 카드에서 다르게 보여 줄 칸 */
data class RouteChange(
    val cards: List<Card>,
    /** 새로 생긴 칸 (보라색 점선) */
    val changed: Set<Int>,
    /** 이번엔 지나지 않게 된 칸 */
    val removed: List<Card>,
    val sameDestination: Boolean,
    val extraSongs: Int,
    /** 차가 소리 내어 말하는 문장 */
    val line: String,
)

/**
 * 내비가 경로를 다시 찾았을 때(onReRouteEnded) 바뀐 점을 쉬운 말로 만든다.
 * 원칙: 목적지가 그대로라는 말을 가장 먼저 한다. 무엇이 바뀌었는지는 그다음.
 */
class ChangeAnnouncer(private val songClock: SongClock = SongClock()) {

    fun compare(
        before: List<Card>,
        after: List<Card>,
        minutesBefore: Double,
        minutesAfter: Double,
    ): RouteChange? {
        val key = { c: Card -> c.kind to c.legId }
        val oldKeys = before.map(key).toSet()
        val newKeys = after.map(key).toSet()
        val changed = after.indices.filter { key(after[it]) !in oldKeys }.toSet()
        val removed = before.filter { key(it) !in newKeys }
        val extra = songClock.songs(minutesAfter) - songClock.songs(minutesBefore)
        if (changed.isEmpty() && removed.isEmpty() && extra <= 0) return null

        val dest = after.last()
        val sameDestination = before.lastOrNull()?.legId == dest.legId
        val place = dest.text.removeSuffix("에 도착")
        val line = buildString {
            if (sameDestination) append("길이 바뀌었어. 그래도 ${place}에 가.") else append("가는 곳이 바뀌었어. 이제 ${place}에 가.")
            if (extra > 0) append(" 노래 ${extra}곡 더.")
        }
        // 지나지 않게 된 터널은 바뀐 칸에 한 줄로 알려 준다 (불안해하던 구간이 없어졌다는 것도 정보)
        val skippedTunnel = removed.any { it.kind == LegKind.TUNNEL }
        val cards = after.mapIndexed { i, c ->
            if (i in changed && skippedTunnel && c.note == null) c.copy(note = "터널은 이번엔 안 지나가요") else c
        }
        return RouteChange(cards, changed, removed, sameDestination, extra, line)
    }
}
