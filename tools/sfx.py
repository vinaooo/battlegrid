# Makes the game sounds in app/src/main/res/raw: python3 tools/sfx.py app/src/main/res/raw
import wave, math, random, struct, sys
RATE = 22050
def write(path, samples):
    peak = max(abs(s) for s in samples) or 1
    with wave.open(path, 'wb') as w:
        w.setnchannels(1); w.setsampwidth(2); w.setframerate(RATE)
        w.writeframes(b''.join(struct.pack('<h', int(s / peak * 0.8 * 32767)) for s in samples))
def lowpass(xs, a):
    y = 0; out = []
    for x in xs:
        y += a * (x - y); out.append(y)
    return out
random.seed(7)
def noise(n): return [random.uniform(-1, 1) for _ in range(n)]
# Miss: a short splash, bright noise falling off fast.
n = int(0.35 * RATE)
splash = lowpass(noise(n), 0.35)
miss = [s * math.exp(-t / (0.07 * RATE)) * min(1, t / 80) for t, s in enumerate(splash)]
# Hit: a boom, a falling low tone with a noise crack.
n = int(0.55 * RATE)
crack = lowpass(noise(n), 0.2)
hit = []
phase = 0
for t in range(n):
    f = 45 + 70 * math.exp(-t / (0.08 * RATE)); phase += 2 * math.pi * f / RATE
    env = math.exp(-t / (0.12 * RATE)) * min(1, t / 40)
    hit.append(env * (math.sin(phase) + 0.6 * crack[t] * math.exp(-t / (0.03 * RATE))))
# Sunk: a longer rumble going down, with a second boom.
n = int(1.1 * RATE)
rumble = lowpass(noise(n), 0.04)
sunk = []
phase = 0
for t in range(n):
    f = 38 + 50 * math.exp(-t / (0.3 * RATE)); phase += 2 * math.pi * f / RATE
    env = math.exp(-t / (0.35 * RATE)) * min(1, t / 60)
    sunk.append(env * (0.8 * math.sin(phase) + 1.6 * rumble[t]))
out = sys.argv[1]
write(f"{out}/sfx_miss.wav", miss); write(f"{out}/sfx_hit.wav", hit); write(f"{out}/sfx_sunk.wav", sunk)
