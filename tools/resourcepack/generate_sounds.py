import numpy as np
import soundfile
from scipy import signal

from common import asset, write_json

RATE = 44100
RNG = np.random.default_rng(1337)


def timeline(seconds):
    return np.arange(int(RATE * seconds)) / RATE


def silence(seconds):
    return np.zeros(int(RATE * seconds))


def envelope(length, attack=0.005, decay=0.1, sustain=0.0, release=0.1, hold=0.0):
    total = length
    a = int(attack * RATE)
    d = int(decay * RATE)
    h = int(hold * RATE)
    r = int(release * RATE)
    env = np.zeros(total)
    position = 0
    for segment, start, end in ((a, 0.0, 1.0), (d, 1.0, sustain)):
        n = min(segment, total - position)
        if n > 0:
            env[position:position + n] = np.linspace(start, end, n, endpoint=False)
        position += n
    n = min(h, total - position)
    if n > 0:
        env[position:position + n] = sustain
        position += n
    remaining = total - position
    if remaining > 0:
        tail = np.full(remaining, sustain)
        rn = min(r, remaining)
        tail[-rn:] = np.linspace(sustain, 0.0, rn) if rn > 0 else tail[-rn:]
        env[position:] = tail
    return env


def exp_decay(length, seconds):
    return np.exp(-np.arange(length) / (seconds * RATE))


def sine(freq, seconds, phase=0.0):
    t = timeline(seconds)
    if callable(freq):
        f = freq(t)
        return np.sin(2 * np.pi * np.cumsum(f) / RATE + phase)
    return np.sin(2 * np.pi * freq * t + phase)


def bell(freq, seconds, decay=0.6, brightness=1.0):
    t = timeline(seconds)
    partials = [(1.0, 1.0), (2.0, 0.45 * brightness), (3.01, 0.22 * brightness), (4.2, 0.12 * brightness), (5.43, 0.07 * brightness)]
    out = np.zeros_like(t)
    for ratio, amp in partials:
        out += amp * np.sin(2 * np.pi * freq * ratio * t) * np.exp(-t / (decay / ratio ** 0.6))
    attack = np.minimum(1.0, t / 0.002)
    return out * attack


def noise(seconds):
    return RNG.uniform(-1, 1, int(RATE * seconds))


def pink(seconds):
    white = RNG.standard_normal(int(RATE * seconds))
    b, a = [0.049922035, -0.095993537, 0.050612699, -0.004408786], [1, -2.494956002, 2.017265875, -0.522189400]
    return signal.lfilter(b, a, white) * 0.25


def bandpass(data, low, high, order=2):
    sos = signal.butter(order, [low, high], btype="bandpass", fs=RATE, output="sos")
    return signal.sosfilt(sos, data)


def lowpass(data, cutoff, order=2):
    sos = signal.butter(order, cutoff, btype="lowpass", fs=RATE, output="sos")
    return signal.sosfilt(sos, data)


def highpass(data, cutoff, order=2):
    sos = signal.butter(order, cutoff, btype="highpass", fs=RATE, output="sos")
    return signal.sosfilt(sos, data)


def sweep_filter(data, start, end, q=4.0, steps=64):
    out = np.zeros_like(data)
    chunk = len(data) // steps + 1
    zi = None
    for index in range(steps):
        segment = data[index * chunk:(index + 1) * chunk]
        if len(segment) == 0:
            break
        frequency = start * (end / start) ** (index / max(1, steps - 1))
        b, a = signal.iirpeak(min(frequency, RATE / 2 - 100), q, fs=RATE)
        if zi is None:
            zi = signal.lfilter_zi(b, a) * 0
        filtered, zi = signal.lfilter(b, a, segment, zi=zi)
        out[index * chunk:index * chunk + len(segment)] = filtered
    return out


def reverb(data, seconds=1.2, mix=0.25, damping=0.5):
    delays = [0.0297, 0.0371, 0.0411, 0.0437, 0.0503, 0.0571]
    tail = np.concatenate([data, silence(seconds)])
    wet = np.zeros_like(tail)
    for delay in delays:
        d = int(delay * RATE)
        gain = 0.001 ** (delay / seconds)
        b = np.zeros(d + 1)
        b[0] = 1.0
        a = np.zeros(d + 1)
        a[0] = 1.0
        a[d] = -gain
        comb = signal.lfilter(b, a, tail)
        wet += lowpass(comb, 6000 * (1 - damping) + 1500)
    wet /= len(delays)
    return tail * (1 - mix) + wet * mix


def place(target, sound, at):
    start = int(at * RATE)
    end = start + len(sound)
    if end > len(target):
        target = np.concatenate([target, np.zeros(end - len(target))])
    target[start:end] += sound
    return target


def finish(data, gain=0.9, fade=0.02):
    data = data - np.mean(data)
    peak = np.max(np.abs(data)) or 1.0
    data = data / peak * gain
    n = int(fade * RATE)
    if n > 0 and len(data) > n:
        data[-n:] *= np.linspace(1, 0, n)
    return data.astype(np.float32)


NOTE = {name: 440.0 * 2 ** ((index - 9) / 12) for index, name in enumerate(["C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B"])}


def note(name, octave):
    return NOTE[name] * 2 ** (octave - 4)


def countdown_tick():
    tone = bell(note("E", 6), 0.25, decay=0.08, brightness=0.6)
    click = highpass(noise(0.01), 3000) * exp_decay(int(0.01 * RATE), 0.002)
    return finish(place(tone, click * 0.6, 0), 0.7)


def countdown_go():
    out = silence(1.4)
    for i, n in enumerate([("C", 5), ("E", 5), ("G", 5), ("C", 6)]):
        out = place(out, bell(note(*n), 1.2, decay=0.5) * (0.7 + 0.1 * i), i * 0.035)
    shimmer = bandpass(noise(1.0), 6000, 12000) * envelope(int(RATE * 1.0), 0.01, 0.6, 0.0, 0.2) * 0.25
    out = place(out, shimmer, 0.0)
    return finish(reverb(out, 1.0, 0.3), 0.85)


def ui_click():
    tone = sine(2200, 0.05) * exp_decay(int(0.05 * RATE), 0.008)
    tick = bandpass(noise(0.02), 2000, 8000) * exp_decay(int(0.02 * RATE), 0.003)
    return finish(place(tone, tick, 0), 0.6)


def ui_deny():
    out = silence(0.35)
    for at in (0.0, 0.14):
        t = timeline(0.11)
        buzz = signal.square(2 * np.pi * 180 * t) * 0.5 + np.sin(2 * np.pi * 90 * t)
        buzz = lowpass(buzz, 1800) * envelope(len(t), 0.005, 0.02, 0.8, 0.04, 0.05)
        out = place(out, buzz, at)
    return finish(out, 0.6)


def team_join():
    out = silence(0.8)
    out = place(out, bell(note("G", 5), 0.7, decay=0.35), 0.0)
    out = place(out, bell(note("C", 6), 0.7, decay=0.4), 0.09)
    return finish(reverb(out, 0.6, 0.2), 0.7)


def host_assigned():
    out = silence(1.2)
    for i, n in enumerate([("C", 5), ("E", 5), ("G", 5)]):
        out = place(out, bell(note(*n), 0.9, decay=0.45), i * 0.11)
    out = place(out, bell(note("C", 6), 1.0, decay=0.6) * 1.1, 0.33)
    return finish(reverb(out, 0.8, 0.25), 0.8)


def transition():
    length = 1.4
    whoosh = pink(length) * 3
    whoosh = sweep_filter(whoosh, 180, 3500, q=1.2)
    env = np.concatenate([np.linspace(0, 1, int(RATE * 0.7)) ** 2, np.linspace(1, 0, int(RATE * 0.7)) ** 1.5])
    whoosh = whoosh[:len(env)] * env
    boom = sine(lambda t: 70 * np.exp(-t * 3) + 32, 1.2) * exp_decay(int(RATE * 1.2), 0.35)
    out = place(whoosh, boom * 1.4, 0.62)
    return finish(reverb(out, 1.4, 0.3), 0.9)


def boarding_mount():
    flap = bandpass(noise(0.3), 300, 1600) * envelope(int(RATE * 0.3), 0.01, 0.12, 0.2, 0.1)
    flap2 = bandpass(noise(0.25), 250, 1300) * envelope(int(RATE * 0.25), 0.01, 0.1, 0.1, 0.1)
    thump = sine(110, 0.15) * exp_decay(int(RATE * 0.15), 0.04)
    out = place(flap, flap2 * 0.8, 0.12)
    out = place(out, thump * 0.7, 0.0)
    return finish(out, 0.75)


def boarding_rise():
    length = 3.0
    wind = sweep_filter(pink(length) * 3, 200, 1800, q=0.9)
    tone = sine(lambda t: 140 + 380 * (t / length) ** 1.5, length) * 0.35
    tone += sine(lambda t: 210 + 570 * (t / length) ** 1.5, length) * 0.2
    env = np.minimum(1.0, timeline(length) / 1.2) * np.minimum(1.0, (length - timeline(length)) / 0.4)
    out = (wind + lowpass(tone, 2000)) * env
    return finish(reverb(out, 1.0, 0.25), 0.85)


def flight_wind():
    length = 4.0
    base = lowpass(pink(length) * 4, 900)
    gust = 0.6 + 0.4 * np.sin(2 * np.pi * timeline(length) / length * 2) ** 2
    whistle = bandpass(pink(length) * 4, 900, 1400) * 0.25 * (0.5 + 0.5 * np.sin(2 * np.pi * timeline(length) * 0.7))
    out = (base + whistle) * gust
    fade = int(RATE * 0.5)
    out[:fade] *= np.linspace(0, 1, fade)
    out[-fade:] *= np.linspace(1, 0, fade)
    return finish(out, 0.7)


def flight_jump():
    length = 0.9
    whoosh = sweep_filter(pink(length) * 4, 3000, 250, q=1.4)
    env = envelope(int(RATE * length), 0.02, 0.25, 0.5, 0.4, 0.2)
    return finish(whoosh * env, 0.85)


def parachute_open():
    length = 0.6
    cloth = bandpass(noise(length), 180, 2500) * envelope(int(RATE * length), 0.005, 0.08, 0.25, 0.35)
    snap = highpass(noise(0.03), 1500) * exp_decay(int(RATE * 0.03), 0.006)
    thump = sine(lambda t: 90 * np.exp(-t * 6) + 50, 0.35) * exp_decay(int(RATE * 0.35), 0.08)
    out = place(cloth, snap * 1.2, 0.02)
    out = place(out, thump, 0.0)
    return finish(out, 0.85)


def parachute_land():
    thud = sine(lambda t: 120 * np.exp(-t * 10) + 45, 0.4) * exp_decay(int(RATE * 0.4), 0.07)
    rustle = bandpass(noise(0.5), 400, 3000) * envelope(int(RATE * 0.5), 0.01, 0.2, 0.15, 0.25) * 0.6
    return finish(place(thud, rustle, 0.03), 0.8)


def game_kill():
    out = silence(1.0)
    out = place(out, bell(note("A", 6), 0.9, decay=0.35, brightness=1.4), 0.0)
    out = place(out, bell(note("E", 7), 0.6, decay=0.25, brightness=0.8) * 0.5, 0.0)
    sub = sine(lambda t: 120 * np.exp(-t * 20) + 55, 0.3) * exp_decay(int(RATE * 0.3), 0.06)
    out = place(out, sub * 1.3, 0.0)
    crack = bandpass(noise(0.04), 2500, 9000) * exp_decay(int(RATE * 0.04), 0.008)
    out = place(out, crack * 0.8, 0.0)
    return finish(reverb(out, 0.6, 0.18), 0.9)


def game_assist():
    out = silence(0.7)
    out = place(out, bell(note("E", 6), 0.6, decay=0.25, brightness=1.0), 0.0)
    out = place(out, bell(note("B", 6), 0.5, decay=0.2, brightness=0.6) * 0.5, 0.06)
    return finish(reverb(out, 0.5, 0.18), 0.7)


def game_death():
    length = 2.0
    boom = sine(lambda t: 80 * np.exp(-t * 2) + 28, length) * exp_decay(int(RATE * length), 0.6)
    fall = sine(lambda t: 420 * np.exp(-t * 1.6) + 60, length) * exp_decay(int(RATE * length), 0.5) * 0.35
    rumble = lowpass(noise(length), 300) * exp_decay(int(RATE * length), 0.5) * 0.8
    out = boom * 1.3 + lowpass(fall, 1500) + rumble
    return finish(reverb(out, 1.5, 0.35), 0.95)


def game_elimination():
    out = silence(1.0)
    thud = sine(lambda t: 90 * np.exp(-t * 8) + 40, 0.5) * exp_decay(int(RATE * 0.5), 0.12)
    out = place(out, thud, 0.0)
    out = place(out, bell(note("D", 5), 0.8, decay=0.4, brightness=0.5) * 0.4, 0.04)
    return finish(reverb(lowpass(out, 3500), 1.0, 0.35), 0.6)


def chord(notes, start, seconds, decay=0.8, brightness=1.0, spread=0.0):
    out = silence(seconds + start + spread * len(notes))
    for i, n in enumerate(notes):
        out = place(out, bell(note(*n), seconds, decay=decay, brightness=brightness), start + i * spread)
    return out


def pad(notes, seconds, attack=0.4, release=0.8):
    t = timeline(seconds)
    out = np.zeros_like(t)
    for n in notes:
        f = note(*n)
        for detune in (-0.004, 0.0, 0.004):
            out += signal.sawtooth(2 * np.pi * f * (1 + detune) * t) * 0.2
    out = lowpass(out, 2200)
    env = np.minimum(1.0, t / attack) * np.minimum(1.0, (seconds - t) / release)
    return out * env


def game_victory():
    out = silence(4.0)
    steps = [("C", 5), ("E", 5), ("G", 5), ("C", 6), ("E", 6), ("G", 6)]
    for i, n in enumerate(steps):
        out = place(out, bell(note(*n), 1.4, decay=0.6), i * 0.09)
    out = place(out, pad([("C", 4), ("G", 4), ("C", 5), ("E", 5)], 3.2) * 0.8, 0.5)
    out = place(out, chord([("C", 6), ("E", 6), ("G", 6)], 0.0, 2.4, decay=1.2), 0.7)
    shimmer = bandpass(noise(2.5), 6000, 14000) * envelope(int(RATE * 2.5), 0.3, 1.5, 0.0, 0.6) * 0.15
    out = place(out, shimmer, 0.6)
    return finish(reverb(out, 1.8, 0.35), 0.9)


def game_defeat():
    out = silence(3.0)
    for i, n in enumerate([("A", 4), ("F", 4), ("D", 4), ("A", 3)]):
        out = place(out, bell(note(*n), 1.4, decay=0.7, brightness=0.6), i * 0.28)
    out = place(out, pad([("D", 3), ("A", 3), ("F", 4)], 2.4) * 0.6, 0.5)
    return finish(reverb(out, 1.6, 0.35), 0.8)


def zone_warning():
    out = silence(1.6)
    for i in range(3):
        for j, f in enumerate((880, 660)):
            t = timeline(0.18)
            tone = (np.sin(2 * np.pi * f * t) + 0.3 * signal.square(2 * np.pi * f * t)) * envelope(len(t), 0.005, 0.05, 0.6, 0.05, 0.08)
            out = place(out, lowpass(tone, 4000) * 0.7, i * 0.45 + j * 0.2)
    return finish(reverb(out, 0.6, 0.2), 0.75)


def zone_shrink():
    length = 3.0
    rumble = lowpass(noise(length), 160, order=4) * 3
    drone = sine(lambda t: 42 + 6 * np.sin(2 * np.pi * 0.6 * t), length) * 0.8
    energy = bandpass(pink(length) * 3, 1500, 4000) * 0.15 * (0.5 + 0.5 * np.sin(2 * np.pi * 9 * timeline(length)))
    env = np.minimum(1.0, timeline(length) / 0.8) * np.minimum(1.0, (length - timeline(length)) / 1.0)
    return finish((rumble + drone + energy) * env, 0.85)


def zone_damage():
    length = 0.25
    zap = highpass(noise(length), 2500) * exp_decay(int(RATE * length), 0.05)
    buzz = signal.sawtooth(2 * np.pi * 120 * timeline(length)) * exp_decay(int(RATE * length), 0.06) * 0.6
    crackle = (RNG.random(int(RATE * length)) > 0.985).astype(float) * RNG.uniform(-1, 1, int(RATE * length))
    return finish(zap + lowpass(buzz, 2500) + crackle * 0.8, 0.6)


SOUNDS = {
    "ui.countdown.tick": ("ui/countdown_tick", countdown_tick, "Countdown"),
    "ui.countdown.go": ("ui/countdown_go", countdown_go, "Los!"),
    "ui.click": ("ui/click", ui_click, "Klick"),
    "ui.deny": ("ui/deny", ui_deny, "Abgelehnt"),
    "ui.team_join": ("ui/team_join", team_join, "Team beigetreten"),
    "ui.host": ("ui/host", host_assigned, "Host zugewiesen"),
    "game.transition": ("game/transition", transition, "Übergang"),
    "boarding.mount": ("boarding/mount", boarding_mount, "Phantom bestiegen"),
    "boarding.rise": ("boarding/rise", boarding_rise, "Phantome steigen auf"),
    "flight.wind": ("flight/wind", flight_wind, "Flugwind"),
    "flight.jump": ("flight/jump", flight_jump, "Absprung"),
    "parachute.open": ("parachute/open", parachute_open, "Fallschirm öffnet sich"),
    "parachute.land": ("parachute/land", parachute_land, "Landung"),
    "game.kill": ("game/kill", game_kill, "Eliminierung"),
    "game.assist": ("game/assist", game_assist, "Assist"),
    "game.death": ("game/death", game_death, "Eliminiert"),
    "game.elimination": ("game/elimination", game_elimination, "Spieler eliminiert"),
    "game.victory": ("game/victory", game_victory, "Sieg"),
    "game.defeat": ("game/defeat", game_defeat, "Niederlage"),
    "zone.warning": ("zone/warning", zone_warning, "Zonenwarnung"),
    "zone.shrink": ("zone/shrink", zone_shrink, "Zone schrumpft"),
    "zone.damage": ("zone/damage", zone_damage, "Zonenschaden"),
}


def main():
    definitions = {}
    for event, (path, factory, subtitle) in SOUNDS.items():
        data = factory()
        target = asset("rift", "sounds", path + ".ogg")
        soundfile.write(str(target), data, RATE, format="OGG", subtype="VORBIS")
        definitions[event] = {"subtitle": subtitle, "sounds": [{"name": f"rift:{path}", "stream": False}]}
        print(f"{event}: {len(data) / RATE:.2f}s")
    write_json(asset("rift", "sounds.json"), definitions)


if __name__ == "__main__":
    main()
