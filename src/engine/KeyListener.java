package engine;


import static org.lwjgl.glfw.GLFW.GLFW_PRESS;
import static org.lwjgl.glfw.GLFW.GLFW_RELEASE;

import java.util.ArrayList;
import java.util.List;

public class KeyListener {

    private static KeyListener instance;
    private  boolean keyPressed[] = new boolean[350];
    private static int lastKeyPressed = -1;
    private static long lastKeyPressTime = -1; // Timestamp of last key press

    private KeyListener() {

    }

    public static KeyListener get() {
        if (KeyListener.instance == null) {
            KeyListener.instance = new KeyListener();
        }

        return KeyListener.instance;
    }

    public static void keyCallback(long window, int key, int scancode, int action, int mods) {
        if (action == GLFW_PRESS) {
            get().keyPressed[key] = true;
            lastKeyPressed = key;
            lastKeyPressTime = System.nanoTime(); // Record the timestamp in nanoseconds
        } else if (action == GLFW_RELEASE) {
            get().keyPressed[key] = false;
        }
    }

    public static boolean isKeyPressed(int keyCode) {
        return get().keyPressed[keyCode];
    }

    public static int getLastKeyPressed() {
        return lastKeyPressed;
    }

    public static long getLastKeyPressTime() {
        return lastKeyPressTime;
    }

    public static void clearLastKey() {
        lastKeyPressed = -1;
        lastKeyPressTime = -1; // Clear the timestamp
    }
}
