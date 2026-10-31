package com.bexa.visuals.util;

import net.minecraft.client.MinecraftClient;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayDeque;

/** Sichqoncha tugmalari bosish tezligi (CPS) hisoblagichi. Har kadrda update() chaqiriladi. */
public final class Cps {
    private Cps() {}

    private static final ArrayDeque<Long> LEFT = new ArrayDeque<>(), RIGHT = new ArrayDeque<>();
    public static boolean leftDown, rightDown;
    private static boolean prevL, prevR;
    private static int lastFrame = -1;

    public static void update(int frameId) {
        if (frameId == lastFrame) return;
        lastFrame = frameId;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.getWindow() == null) return;
        long h = mc.getWindow().getHandle();
        leftDown = GLFW.glfwGetMouseButton(h, GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
        rightDown = GLFW.glfwGetMouseButton(h, GLFW.GLFW_MOUSE_BUTTON_RIGHT) == GLFW.GLFW_PRESS;
        long now = System.currentTimeMillis();
        if (leftDown && !prevL) LEFT.addLast(now);
        if (rightDown && !prevR) RIGHT.addLast(now);
        prevL = leftDown;
        prevR = rightDown;
        while (!LEFT.isEmpty() && now - LEFT.peekFirst() > 1000) LEFT.pollFirst();
        while (!RIGHT.isEmpty() && now - RIGHT.peekFirst() > 1000) RIGHT.pollFirst();
    }

    public static int left() { return LEFT.size(); }
    public static int right() { return RIGHT.size(); }
}
