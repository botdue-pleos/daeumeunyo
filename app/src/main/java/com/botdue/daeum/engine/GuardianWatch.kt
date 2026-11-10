package com.botdue.daeum.engine

enum class Seat(val rear: Boolean) {
    DRIVER(false), PASSENGER(false), REAR_LEFT(true), REAR_CENTER(true), REAR_RIGHT(true)
}

/** 운전하는 보호자에게만 들려주는 알림. 운전자는 화면을 볼 필요 없이 이 한 줄만 들으면 된다 */
data class GuardianAlert(
    val kind: Kind,
    val seat: Seat,
    val title: String,
    /** 세울 곳 안내. 근처 쉼터를 모르면 null */
    val stopHint: String?,
    val timeMs: Long,
) {
    enum class Kind { BELT, DOOR, WINDOW }
}

/**
 * 뒷자리 상태 감시. Vehicle SDK Safety(안전벨트)·Door·Window 구독 값을 받는다.
 * 달리는 중에만 알리고, 같은 자리 같은 일은 [quietMs] 동안 한 번만 말한다 (벨트를 풀었다 맸다 반복할 때 계속 울리지 않게).
 */
class GuardianWatch(private val quietMs: Long = 60_000) {
    private val lastSaid = mutableMapOf<Pair<GuardianAlert.Kind, Seat>, Long>()

    fun onBelt(timeMs: Long, seat: Seat, buckled: Boolean, moving: Boolean, restAheadM: Int?): GuardianAlert? {
        if (buckled || !moving || !seat.rear) return null
        return say(GuardianAlert.Kind.BELT, seat, timeMs, "뒷자리 벨트가 풀렸어요", restAheadM)
    }

    fun onDoor(timeMs: Long, seat: Seat, open: Boolean, moving: Boolean, restAheadM: Int?): GuardianAlert? {
        if (!open || !moving || !seat.rear) return null
        return say(GuardianAlert.Kind.DOOR, seat, timeMs, "뒷문이 열렸어요", restAheadM)
    }

    /** 창문은 조금 여는 건 괜찮다. 절반 넘게 열렸을 때만 */
    fun onWindow(timeMs: Long, seat: Seat, openPercent: Int, moving: Boolean, restAheadM: Int?): GuardianAlert? {
        if (openPercent < 50 || !moving || !seat.rear) return null
        return say(GuardianAlert.Kind.WINDOW, seat, timeMs, "뒷자리 창문이 많이 열렸어요", restAheadM)
    }

    private fun say(kind: GuardianAlert.Kind, seat: Seat, timeMs: Long, title: String, restAheadM: Int?): GuardianAlert? {
        val key = kind to seat
        val last = lastSaid[key]
        if (last != null && timeMs - last < quietMs) return null
        lastSaid[key] = timeMs
        val hint = restAheadM?.let { "${it}m 앞에 세울 수 있는 쉼터가 있어요" }
        return GuardianAlert(kind, seat, title, hint, timeMs)
    }
}

/** 보호자가 말로 하는 명령. STT ON_DEVICE 결과 문장을 받는다 */
enum class GuardianCommand { REST, HARD_MOMENT }

object GuardianSpeech {
    fun parse(text: String): GuardianCommand? {
        val t = text.replace(" ", "")
        return when {
            listOf("힘들어했", "힘들었", "힘들어해").any { it in t } -> GuardianCommand.HARD_MOMENT
            listOf("쉬자", "쉬어가", "쉬었다", "쉼터").any { it in t } -> GuardianCommand.REST
            else -> null
        }
    }

    /** 쉼터에 들르기로 하면 동승자 카드에 쉼터 칸을 도착 바로 앞에 넣고, 미리 말해 준다 */
    fun addRest(cards: List<Card>, restName: String): Pair<List<Card>, String> {
        val rest = RouteCardBuilder.toCard(RouteLeg(LegKind.REST, restName))
        val dest = cards.last()
        val place = dest.text.removeSuffix("에 도착")
        return (cards.dropLast(1) + rest + dest) to "잠깐 쉬었다가 ${place}에 가"
    }
}
