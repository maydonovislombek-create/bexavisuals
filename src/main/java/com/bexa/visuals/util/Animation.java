package com.bexa.visuals.util;

/** Maqsadga qarab yumshoq harakatlanuvchi qiymat (toggle, hover, panel animatsiyalari uchun). */
public class Animation {
    private float value;
    private float target;
    private long lastNanos = System.nanoTime();
    private final float speed; // kattaroq = tezroq

    public Animation(float initial, float speed) {
        this.value = initial;
        this.target = initial;
        this.speed = speed;
    }

    public void setTarget(float t) { this.target = t; }

    public float getTarget() { return target; }

    public void snap(float v) { this.value = v; this.target = v; }

    /** Har kadrda bir marta chaqiriladi; joriy qiymatni qaytaradi. */
    public float update() {
        long now = System.nanoTime();
        float dt = Math.min(0.1f, (now - lastNanos) / 1_000_000_000f);
        lastNanos = now;
        float diff = target - value;
        if (Math.abs(diff) < 0.0005f) {
            value = target;
        } else {
            value += diff * Math.min(1f, dt * speed);
        }
        return value;
    }

    public float get() { return value; }

    public boolean isDone() { return value == target; }
}
