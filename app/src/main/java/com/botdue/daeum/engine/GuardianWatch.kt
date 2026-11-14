package com.botdue.daeum.engine

enum class Seat(val rear: Boolean) {
    DRIVER(false), PASSENGER(false), REAR_LEFT(true), REAR_CENTER(true), REAR_RIGHT(true)
}

/**
 * 보호자 알림. 소리는 일반 TTS라 차 안 모두에게 들린다 (운전석에만 보낼 수 없다).
 * 그래서 소리로 하는 말([spoken])은 뒷자리가 들어도 괜찮은 말로 하고, 무슨 일인지([title])는 화면에만 쓴다.
 */
data class GuardianAlert(
    val kind: Kind,
    val seat: Seat,
    /** 화면에 쓰는 한 줄 (보호자가 볼 것) */
    val title: String,
    /** 세울 곳 안내. 근처 쉼터를 모르면 null */
    val stopHint: String?,
    /** 차 스피커로 하는 말. 뒷자리를 탓하거나 놀라게 하는 말은 넣지 않는다 */
    val spoken: String,
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
        return say(GuardianAlert.Kind.BELT, seat, timeMs, "뒷자리 벨트를 한번 봐 주세요", restAheadM)
    }

    fun onDoor(timeMs: Long, seat: Seat, open: Boolean, moving: Boolean, restAheadM: Int?): GuardianAlert? {
        if (!open || !moving || !seat.rear) return null
        return say(GuardianAlert.Kind.DOOR, seat, timeMs, "뒷문을 한번 봐 주세요", restAheadM)
    }

    /** 창문은 조금 여는 건 괜찮다. 절반 넘게 열렸을 때만 */
    fun onWindow(timeMs: Long, seat: Seat, openPercent: Int, moving: Boolean, restAheadM: Int?): GuardianAlert? {
        if (openPercent < 50 || !moving || !seat.rear) return null
        return say(GuardianAlert.Kind.WINDOW, seat, timeMs, "뒷자리 창문을 한번 봐 주세요", restAheadM)
    }

    private fun say(kind: GuardianAlert.Kind, seat: Seat, timeMs: Long, title: String, restAheadM: Int?): GuardianAlert? {
        val key = kind to seat
        val last = lastSaid[key]
        if (last != null && timeMs - last < quietMs) return null
        lastSaid[key] = timeMs
        val hint = restAheadM?.let { "${it}m 앞에 쉬어 갈 곳이 있어요" }
        // 문은 바로 세워야 하니 문이라고 말한다. 벨트·창문은 쉬어 가자는 말로만 (뒷자리가 혼난다고 느끼지 않게)
        val ask = if (kind == GuardianAlert.Kind.DOOR) "뒷문을 한번 확인해 주세요." else "잠깐 쉬었다 갈까요?"
        val spoken = listOfNotNull(hint?.let { "$it." }, ask).joinToString(" ")
        return GuardianAlert(kind, seat, title, hint, spoken, timeMs)
    }
}

/** 보호자가 말로 하는 명령. STT ON_DEVICE 결과 문장을 받는다. [MARK] = 지금 순간 표시 */
enum class GuardianCommand { REST, MARK }

object GuardianSpeech {
    /** 표시 말. "표시해"가 기본이고 예전 말 "힘들어했어"도 알아듣는다 */
    private val markWords = listOf("표시해", "표시하자", "표시할게", "힘들어했", "힘들었", "힘들어해")
    /** "안 힘들었어", "힘들지 않았어", "표시하지 마"는 표시가 아니다 */
    private val notMarkWords = listOf("안힘들", "힘들지않", "힘들진않", "힘들지는않", "표시하지", "표시안", "안표시")

    fun parse(text: String): GuardianCommand? {
        val t = text.replace(" ", "")
        return when {
            notMarkWords.any { it in t } -> null
            markWords.any { it in t } -> GuardianCommand.MARK
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
