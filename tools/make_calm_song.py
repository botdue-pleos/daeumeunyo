"""터널 앞에서 트는 기본 음악을 합성한다 (저작권 없는 자체 제작 음원).
느린 5음계 멜로디 + 낮은 화음, 부드러운 시작·끝. 사용: python3 tools/make_calm_song.py app/src/main/res/raw/calm_song.wav"""
import math, struct, sys, wave

SR = 16000
BEAT = 60 / 66  # 66 bpm
# 다장조 5음계 (도레미솔라) 주파수
N = {"C4": 261.63, "D4": 293.66, "E4": 329.63, "G4": 392.00, "A4": 440.00, "C5": 523.25, "D5": 587.33, "E5": 659.25,
     "C3": 130.81, "F3": 174.61, "G3": 196.00, "A2": 110.00}
melody = [("E4", 1), ("G4", 1), ("A4", 2), ("G4", 1), ("E4", 1), ("D4", 2),
          ("C4", 1), ("D4", 1), ("E4", 2), ("G4", 1), ("E4", 1), ("D4", 2),
          ("E4", 1), ("G4", 1), ("A4", 2), ("C5", 1), ("A4", 1), ("G4", 2),
          ("E4", 1), ("D4", 1), ("C4", 2), ("D4", 1), ("E4", 1), ("C4", 2)] * 2
bass = [("C3", 4), ("A2", 4), ("F3", 4), ("G3", 4)] * 3

total_beats = sum(b for _, b in melody)
n = int(total_beats * BEAT * SR)
buf = [0.0] * n

def tone(note, start_beat, beats, amp, harmonics):
    f = N[note]
    s0 = int(start_beat * BEAT * SR)
    length = int(beats * BEAT * SR)
    for i in range(length):
        if s0 + i >= n:
            break
        t = i / SR
        env = min(1.0, t / 0.04) * math.exp(-2.2 * t / (beats * BEAT))  # 부드럽게 울렸다 사라짐
        v = sum(a * math.sin(2 * math.pi * f * k * t) for k, a in harmonics)
        buf[s0 + i] += amp * env * v

b = 0
for note, beats in melody:
    tone(note, b, beats, 0.32, [(1, 1.0), (2, 0.25), (3, 0.08)])
    b += beats
b = 0
while b < total_beats:
    for note, beats in bass:
        if b >= total_beats:
            break
        tone(note, b, beats, 0.22, [(1, 1.0), (2, 0.15)])
        b += beats

fade = int(1.5 * SR)
for i in range(fade):
    buf[i] *= i / fade
    buf[n - 1 - i] *= i / fade
peak = max(abs(x) for x in buf)
with wave.open(sys.argv[1], "wb") as w:
    w.setnchannels(1); w.setsampwidth(2); w.setframerate(SR)
    w.writeframes(b"".join(struct.pack("<h", int(x / peak * 0.7 * 32767)) for x in buf))
print("seconds", round(n / SR, 1))
