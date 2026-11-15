package com.botdue.daeum

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.botdue.daeum.car.DemoCarControls
import com.botdue.daeum.car.MarkTone
import com.botdue.daeum.car.Speaker
import com.botdue.daeum.car.TunnelMusic
import com.botdue.daeum.engine.ChangeAnnouncer
import com.botdue.daeum.engine.CueAction
import com.botdue.daeum.engine.Demo
import com.botdue.daeum.engine.GuardianAlert
import com.botdue.daeum.engine.GuardianCommand
import com.botdue.daeum.engine.HardMark
import com.botdue.daeum.engine.MarkedMoment
import com.botdue.daeum.engine.MomentLog
import com.botdue.daeum.engine.Passage
import com.botdue.daeum.engine.Situation
import com.botdue.daeum.engine.GuardianSpeech
import com.botdue.daeum.engine.GuardianWatch
import com.botdue.daeum.engine.LeadPolicy
import com.botdue.daeum.engine.LegKind
import com.botdue.daeum.engine.RouteCardBuilder
import com.botdue.daeum.engine.RouteChange
import com.botdue.daeum.engine.Seat
import com.botdue.daeum.engine.SongClock
import com.botdue.daeum.engine.TunnelCue
import com.botdue.daeum.engine.TunnelStage
import com.botdue.daeum.ui.ArrivedScreen
import com.botdue.daeum.ui.DemoNext
import com.botdue.daeum.ui.DrivingScreen
import com.botdue.daeum.ui.GuardianScreen
import com.botdue.daeum.ui.LocalFont
import com.botdue.daeum.ui.MarkChip
import com.botdue.daeum.ui.NotesScreen
import com.botdue.daeum.ui.RerouteScreen
import com.botdue.daeum.ui.StartScreen
import com.botdue.daeum.ui.astaSans
import com.botdue.daeum.ui.px
import kotlinx.coroutines.delay
import com.botdue.daeum.ui.FitWindow

/** 표시 칩을 띄워 두는 시간 */
private const val MARK_CHIP_MS = 4_000L

/**
 * 시연 장면. MARKED = 길이 바뀐 직후 보호자가 "표시해"라고 말한 장면 (구석에 표시 칩).
 * 실제 차량에서는 NaviHelper·Vehicle SDK 콜백이 들어올 때마다 다음 상태로 넘어가고,
 * 시연에서는 화면 구석 "시연: 다음" 버튼이 그 콜백을 대신한다.
 */
enum class Step { START, ROAD, TUNNEL_FIRST, TUNNEL_SECOND, TUNNEL_PASSED, REROUTE, MARKED, BELT, REST, ARRIVED, NOTES }

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 화면 캡처·영상용: adb shell am start -n com.botdue.daeum/.MainActivity --es step TUNNEL_FIRST
        val start = intent.getStringExtra("step")?.let { runCatching { Step.valueOf(it) }.getOrNull() } ?: Step.START
        // 캡처할 때는 소리를 내지 않는다
        val quiet = intent.getBooleanExtra("quiet", false)
        // 캡처 모드(quiet 또는 clean): "시연: 다음" 칩을 숨기고, 표시 칩도 사라지지 않게 둔다
        val capture = quiet || intent.getBooleanExtra("clean", false)
        setContent { FitWindow { DaeumApp(start, quiet, capture) } }
    }
}

/** 한 번의 이동 상태. 엔진 결과를 모아 화면에 넘긴다 */
private class Trip(val say: (String) -> Unit, val music: (Boolean) -> Unit, val tone: () -> Unit) {
    val builder = RouteCardBuilder()
    val clock = SongClock()
    val log = Demo.monthLog()
    val policy = LeadPolicy(log)
    val cue = TunnelCue(leadSec = policy::tunnelLeadSec)
    val guardian = GuardianWatch()

    var step by mutableStateOf(Step.START)
    var cards by mutableStateOf(builder.build(Demo.route))
    var current by mutableStateOf(0)
    var changed by mutableStateOf(emptySet<Int>())
    var songs by mutableStateOf(clock.songs(Demo.TOTAL_MINUTES))
    var musicOn by mutableStateOf(false)
    var recirculationOn by mutableStateOf(false)
    var change by mutableStateOf<RouteChange?>(null)
    var alert by mutableStateOf<GuardianAlert?>(null)
    var songPicked by mutableStateOf(true)

    /** 오늘 지나간 상황(차량 신호로 자동)과 보호자가 "표시해"라고 한 순간 */
    var todayPassages by mutableStateOf(emptyList<Passage>())
    var todayMarks by mutableStateOf(emptyList<HardMark>())
    /** 도착 화면에서 보호자가 맞다고 한 표시 */
    var confirmed by mutableStateOf(emptySet<HardMark>())
    /** 구석에 잠깐 띄우는 표시 칩 */
    var markChip by mutableStateOf<MarkedMoment?>(null)
    val todayLog: MomentLog get() = MomentLog(todayPassages, todayMarks)

    private val car = DemoCarControls { recirculationOn = it }
    private var clockMs = Demo.todayStartMs

    val tunnelStage: TunnelStage get() = cue.stage(Demo.HOME_TUNNEL)

    private fun apply(actions: List<CueAction>) = actions.forEach {
        when (it) {
            is CueAction.Speak -> say(it.text)
            CueAction.PlayMusic -> { musicOn = true; music(true) }
            CueAction.StopMusic -> { musicOn = false; music(false) }
            is CueAction.Recirculation -> car.setRecirculation(it.on)
        }
    }

    private fun indexOf(kind: LegKind) = cards.indexOfFirst { it.kind == kind }

    private fun passed(situation: Situation, key: String = "") {
        todayPassages = todayPassages + Passage(clockMs, situation, key)
    }

    /**
     * 보호자 말 (실제로는 STT ON_DEVICE 결과). "표시해"는 말로 되읽지 않고 짧은 소리 하나만 낸다:
     * 스피커는 차 안 모두에게 들리므로 동승자가 자기 얘기를 듣지 않게.
     */
    fun hear(text: String) {
        when (GuardianSpeech.parse(text)) {
            GuardianCommand.MARK -> {
                val mark = HardMark(clockMs)
                todayMarks = todayMarks + mark
                markChip = MarkedMoment(mark, todayLog.attach(mark))
                tone()
            }
            GuardianCommand.REST -> if (step != Step.REST && step != Step.ARRIVED && step != Step.NOTES) go(Step.REST)
            null -> Unit
        }
    }

    /** 도착 화면: 놓친 표시를 오늘 지나간 상황에서 골라 더한다 (그 상황 시각에 표시) */
    fun addMissed(p: Passage) {
        val mark = HardMark(p.timeMs)
        todayMarks = todayMarks + mark
        confirmed = confirmed + mark
    }

    fun reject(mark: HardMark) {
        todayMarks = todayMarks - mark
        confirmed = confirmed - mark
    }

    fun go(next: Step) {
        clockMs += 60_000
        if (next != Step.MARKED) markChip = null
        when (next) {
            Step.START -> {
                if (musicOn) apply(listOf(CueAction.StopMusic))
                cards = builder.build(Demo.route); current = 0; changed = emptySet(); change = null; alert = null
                todayPassages = emptyList(); todayMarks = emptyList(); confirmed = emptySet(); clockMs = Demo.todayStartMs
                songs = clock.songs(Demo.TOTAL_MINUTES); car.setRecirculation(false)
            }
            Step.ROAD -> { current = 1; say("출발해. 노래 ${songs}곡만큼 가") }
            Step.TUNNEL_FIRST -> {
                current = indexOf(LegKind.TUNNEL)
                songs = clock.songs(13.0)
                apply(cue.onTbt(Demo.HOME_TUNNEL, Demo.tunnelFirst.first, Demo.tunnelFirst.second))
            }
            Step.TUNNEL_SECOND -> apply(cue.onTbt(Demo.HOME_TUNNEL, Demo.tunnelSecond.first, Demo.tunnelSecond.second))
            Step.TUNNEL_PASSED -> {
                passed(Situation.TUNNEL, Demo.HOME_TUNNEL)
                apply(cue.onTbt(Demo.HOME_TUNNEL, 0, 50.0))
                apply(cue.onPassed(Demo.HOME_TUNNEL))
                current += 1
                songs = clock.songs(Demo.MINUTES_BEFORE_REROUTE)
            }
            Step.REROUTE -> {
                val c = ChangeAnnouncer(clock).compare(cards, builder.build(Demo.rerouted), Demo.MINUTES_BEFORE_REROUTE, Demo.MINUTES_AFTER_REROUTE)
                if (c != null) {
                    passed(Situation.REROUTE)
                    change = c; cards = c.cards; changed = c.changed; current = c.changed.minOrNull() ?: current
                    songs = clock.songs(Demo.MINUTES_AFTER_REROUTE)
                    say(c.line)
                }
            }
            // 길이 바뀐 화면을 이어서 보여 주고, 보호자가 "표시해"라고 말한다
            Step.MARKED -> hear("표시해")
            Step.BELT -> {
                alert = guardian.onBelt(clockMs, Seat.REAR_RIGHT, buckled = false, moving = true, restAheadM = Demo.REST_AHEAD_M)
                alert?.let { a -> passed(Situation.BELT); say(a.spoken) }
            }
            Step.REST -> {
                val (withRest, line) = GuardianSpeech.addRest(cards, Demo.REST_NAME)
                cards = withRest; current = withRest.lastIndex - 1; changed = setOf(current)
                say(line)
            }
            Step.ARRIVED -> { current = cards.lastIndex; songs = 0; say("${Demo.route.last().name}에 도착했어") }
            Step.NOTES -> Unit
        }
        step = next
    }

    /** 각 장면에서 "시연: 다음"을 누르면 갈 곳 */
    fun nextOf(s: Step): Step = when (s) {
        Step.START -> Step.ROAD
        Step.ROAD -> Step.TUNNEL_FIRST
        Step.TUNNEL_FIRST -> Step.TUNNEL_SECOND
        Step.TUNNEL_SECOND -> Step.TUNNEL_PASSED
        Step.TUNNEL_PASSED -> Step.REROUTE
        Step.REROUTE -> Step.MARKED
        Step.MARKED -> Step.BELT
        Step.BELT -> Step.REST
        Step.REST -> Step.ARRIVED
        Step.ARRIVED -> Step.NOTES
        Step.NOTES -> Step.START
    }

    /** 캡처용으로 중간 장면에서 시작할 때, 앞 장면들을 차례로 지나간 상태를 만든다 */
    fun fastForward(target: Step) {
        var s = Step.START
        while (s != target) { s = nextOf(s); go(s) }
    }
}

@Composable
fun DaeumApp(start: Step = Step.START, quiet: Boolean = false, capture: Boolean = quiet) {
    val context = LocalContext.current
    val font = remember { astaSans(context.assets) }
    val speaker = remember { Speaker(context) }
    val music = remember { TunnelMusic(context) }
    val markTone = remember { MarkTone() }
    DisposableEffect(Unit) { onDispose { music.stop(); speaker.release(); markTone.release() } }

    val trip = remember {
        Trip(
            say = { if (!quiet) speaker.say(it) },
            music = { on -> if (quiet) Unit else if (on) music.play() else music.stop() },
            tone = { if (!quiet) markTone.play() },
        ).also { it.fastForward(start) }
    }
    // 표시 칩은 몇 초만. 캡처 모드에서는 남겨 둔다
    LaunchedEffect(trip.markChip) {
        if (trip.markChip != null && !capture) {
            delay(MARK_CHIP_MS)
            trip.markChip = null
        }
    }
    CompositionLocalProvider(LocalFont provides font) {
        Box(Modifier.fillMaxSize()) {
            when (trip.step) {
                Step.START -> StartScreen(
                    destination = Demo.route.last().name,
                    cards = trip.cards,
                    songs = trip.songs,
                    tunnelSongPicked = trip.songPicked,
                    onDepart = { trip.go(Step.ROAD) },
                    onPickSong = { trip.songPicked = !trip.songPicked },
                )
                Step.ROAD, Step.TUNNEL_FIRST, Step.TUNNEL_SECOND, Step.TUNNEL_PASSED, Step.MARKED, Step.REST -> DrivingScreen(
                    cards = trip.cards,
                    current = trip.current,
                    changed = trip.changed,
                    songs = trip.songs,
                    tunnel = trip.tunnelStage,
                    musicOn = trip.musicOn,
                    recirculationOn = trip.recirculationOn,
                )
                Step.REROUTE -> trip.change?.let { RerouteScreen(it, trip.current, trip.songs, Demo.REROUTE_REASON) }
                Step.BELT -> trip.alert?.let {
                    GuardianScreen(it, Demo.route.last().name, onRest = { trip.go(Step.REST) }, onDismiss = { trip.go(Step.ARRIVED) })
                }
                Step.ARRIVED -> {
                    val today = trip.todayLog
                    ArrivedScreen(
                        destination = Demo.route.last().name,
                        moments = today.moments,
                        confirmed = trip.confirmed,
                        missed = today.passages.filter { it !in today.hardPassages },
                        onConfirm = { trip.confirmed = trip.confirmed + it },
                        onReject = trip::reject,
                        onAdd = trip::addMissed,
                        onDone = { trip.go(Step.NOTES) },
                    )
                }
                Step.NOTES -> NotesScreen("11월", Demo.MONTH_TRIPS, trip.log, trip.policy, "집 앞 터널", showClose = !capture, onClose = { trip.go(Step.START) })
            }
            trip.markChip?.let { MarkChip(it.passage?.situation?.label, Modifier.align(Alignment.TopStart).padding(top = 4.px, start = 64.px)) }
            if (!capture && trip.step != Step.NOTES && trip.step != Step.START && trip.step != Step.BELT && trip.step != Step.ARRIVED) {
                DemoNext("다음", Modifier.align(Alignment.TopEnd).padding(top = 2.px, end = 16.px)) { trip.go(trip.nextOf(trip.step)) }
            }
        }
    }
}
