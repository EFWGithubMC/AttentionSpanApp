package engine;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_1;
import static org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_2;

public class Debug {

    public static void InputDebug() {
        if(KeyListener.isKeyPressed(GLFW_KEY_SPACE)) {
            System.out.println("Space key pressed");
        }
        if (MouseListener.mouseButtonDown(GLFW_MOUSE_BUTTON_1)) {
            System.out.println("L mouse button pressed");
        }
        if (MouseListener.mouseButtonDown(GLFW_MOUSE_BUTTON_2)) {
            System.out.println("R mouse button pressed");
        }
        if(MouseListener.isDragging()) {
            System.out.println("Dragging");
        }
    }
}
