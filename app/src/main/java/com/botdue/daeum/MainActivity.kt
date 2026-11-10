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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.botdue.daeum.car.DemoCarControls
import com.botdue.daeum.car.Speaker
import com.botdue.daeum.car.TunnelMusic
import com.botdue.daeum.engine.ChangeAnnouncer
import com.botdue.daeum.engine.CueAction
import com.botdue.daeum.engine.Demo
import com.botdue.daeum.engine.GuardianAlert
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
import com.botdue.daeum.ui.NotesScreen
import com.botdue.daeum.ui.RerouteScreen
import com.botdue.daeum.ui.StartScreen
import com.botdue.daeum.ui.astaSans
import com.botdue.daeum.ui.px

/**
 * 시연 장면. 실제 차량에서는 NaviHelper·Vehicle SDK 콜백이 들어올 때마다 다음 상태로 넘어가고,
 * 시연에서는 화면 구석 "시연: 다음" 버튼이 그 콜백을 대신한다.
 */
enum class Step { START, ROAD, TUNNEL_FIRST, TUNNEL_SECOND, TUNNEL_PASSED, REROUTE, BELT, REST, ARRIVED, NOTES }

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 화면 캡처·영상용: adb shell am start -n com.botdue.daeum/.MainActivity --es step TUNNEL_FIRST
        val start = intent.getStringExtra("step")?.let { runCatching { Step.valueOf(it) }.getOrNull() } ?: Step.START
        // 캡처할 때는 소리를 내지 않는다
        val quiet = intent.getBooleanExtra("quiet", false)
        setContent { DaeumApp(start, quiet) }
    }
}

/** 한 번의 이동 상태. 엔진 결과를 모아 화면에 넘긴다 */
private class Trip(val say: (String) -> Unit, val music: (Boolean) -> Unit) {
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
    private val car = DemoCarControls { recirculationOn = it }
    private var clockMs = 0L

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

    fun go(next: Step) {
        clockMs += 60_000
        when (next) {
            Step.START -> {
                if (musicOn) apply(listOf(CueAction.StopMusic))
                cards = builder.build(Demo.route); current = 0; changed = emptySet(); change = null; alert = null
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
                apply(cue.onTbt(Demo.HOME_TUNNEL, 0, 50.0))
                apply(cue.onPassed(Demo.HOME_TUNNEL))
                current += 1
                songs = clock.songs(Demo.MINUTES_BEFORE_REROUTE)
            }
            Step.REROUTE -> {
                val c = ChangeAnnouncer(clock).compare(cards, builder.build(Demo.rerouted), Demo.MINUTES_BEFORE_REROUTE, Demo.MINUTES_AFTER_REROUTE)
                if (c != null) {
                    change = c; cards = c.cards; changed = c.changed; current = c.changed.minOrNull() ?: current
                    songs = clock.songs(Demo.MINUTES_AFTER_REROUTE)
                    say(c.line)
                }
            }
            Step.BELT -> {
                alert = guardian.onBelt(clockMs, Seat.REAR_RIGHT, buckled = false, moving = true, restAheadM = Demo.REST_AHEAD_M)
                alert?.let { a -> say(listOfNotNull(a.title, a.stopHint).joinToString(". ")) }
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
        Step.REROUTE -> Step.BELT
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
fun DaeumApp(start: Step = Step.START, quiet: Boolean = false) {
    val context = LocalContext.current
    val font = remember { astaSans(context.assets) }
    val speaker = remember { Speaker(context) }
    val music = remember { TunnelMusic(context) }
    DisposableEffect(Unit) { onDispose { music.stop(); speaker.release() } }

    val trip = remember {
        Trip(
            say = { if (!quiet) speaker.say(it) },
            music = { on -> if (quiet) Unit else if (on) music.play() else music.stop() },
        ).also { it.fastForward(start) }
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
                Step.ROAD, Step.TUNNEL_FIRST, Step.TUNNEL_SECOND, Step.TUNNEL_PASSED, Step.REST -> DrivingScreen(
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
                Step.ARRIVED -> ArrivedScreen(Demo.route.last().name, onHard = { trip.go(Step.NOTES) }, onFine = { trip.go(Step.NOTES) })
                Step.NOTES -> NotesScreen("11월", Demo.MONTH_TRIPS, trip.log, trip.policy, "집 앞 터널", onClose = { trip.go(Step.START) })
            }
            if (trip.step != Step.NOTES && trip.step != Step.START && trip.step != Step.BELT && trip.step != Step.ARRIVED) {
                DemoNext("다음", Modifier.align(Alignment.TopEnd).padding(top = 2.px, end = 16.px)) { trip.go(trip.nextOf(trip.step)) }
            }
        }
    }
}
