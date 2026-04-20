package engine;

import org.apache.logging.log4j.Level;
import org.lwjgl.Version;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.opengl.GL;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.nanovg.NVGColor;
import org.lwjgl.nanovg.NanoVG;

import java.nio.IntBuffer;

import static javax.print.attribute.standard.JobState.COMPLETED;
import static org.lwjgl.glfw.Callbacks.glfwFreeCallbacks;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.glfw.GLFW.glfwPollEvents;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL11.GL_DEPTH_BUFFER_BIT;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.system.MemoryUtil.NULL;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import util.Invocation;

public class Windows {

    private static Logger tutorialLogger = LogManager.getLogger(SARTManager.class);

    // The window handle
    private long window;

    private boolean debugStatus = false;
    private boolean inSettings = false;

    private Renderer renderer;
    private TextRenderer textRenderer;
    private SARTManager sartManager;
    private int width = 1280, height = 720;
    private int experimentDurationMinutes = 5;

    private enum State { START, EXPERIMENT, MENU, SETTINGS, COMPLETED, REMINDER, COUNTDOWN, TUTORIAL }
    private State state = State.START;
    // private boolean altPressed = false;
    private long experimentStart;
    private int tutorialPage = 1;
    private boolean spaceWasPressed = false;
    private boolean escapeWasPressed = false;
    private boolean leftMouseWasPressed = false;

    // Countdown state
    private long reminderStart;
    private static final long REMINDER_DURATION_NS = 4_000_000_000L; // Duration of the reminder
    private long countdownStart;
    private static final long COUNTDOWN_DURATION_NS = 3_000_000_000L; // 3 seconds in nanoseconds

    private boolean debugButtonPressed = false;

    public void run() {
        System.out.println("Hello LWJGL " + Version.getVersion() + "!");

        init();
        loop();

        // Free the window callbacks and destroy the window
        glfwFreeCallbacks(window);
        glfwDestroyWindow(window);

        // Cleanup renderers
        textRenderer.cleanup();

        // Terminate GLFW and free the error callback
        glfwTerminate();
        glfwSetErrorCallback(null).free();
    }


    private void init() {
        // Setup an error callback. The default implementation
        // will print the error message in System.err.
        GLFWErrorCallback.createPrint(System.err).set();

        // Initialize GLFW. Most GLFW functions will not work before doing this.
        if ( !glfwInit() )
            throw new IllegalStateException("Unable to initialize GLFW");

        // Configure GLFW
        glfwDefaultWindowHints(); // optional, the current window hints are already the default
        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE); // the window will stay hidden after creation
        glfwWindowHint(GLFW_RESIZABLE, GLFW_FALSE); // the window will be resizable

        // Create the window
        window = glfwCreateWindow(width, height, "Attention Span App", NULL, NULL);
        if ( window == NULL )
            throw new RuntimeException("Failed to create the GLFW window");

        glfwSetCursorPosCallback(window, MouseListener::mousePosCallback);
        glfwSetMouseButtonCallback(window, MouseListener::mouseButtonCallback);
        glfwSetScrollCallback(window, MouseListener::mouseScrollCallback);
        glfwSetKeyCallback(window, KeyListener::keyCallback);

        // Get the thread stack and push a new frame
        try ( MemoryStack stack = stackPush() ) {
            IntBuffer pWidth = stack.mallocInt(1); // int*
            IntBuffer pHeight = stack.mallocInt(1); // int*

            // Get the window size passed to glfwCreateWindow
            glfwGetWindowSize(window, pWidth, pHeight);

            // Get the resolution of the primary monitor
            GLFWVidMode vidmode = glfwGetVideoMode(glfwGetPrimaryMonitor());

            // Center the window
            glfwSetWindowPos(
                    window,
                    (vidmode.width() - pWidth.get(0)) / 2,
                    (vidmode.height() - pHeight.get(0)) / 2
            );
        } // the stack frame is popped automatically

        // Make the OpenGL context current
        glfwMakeContextCurrent(window);
        // Enable v-sync
        glfwSwapInterval(1);

        // Make the window visible
        glfwShowWindow(window);


    }

    private void loop() {
        // This line is critical for LWJGL's interoperation with GLFW's
        // OpenGL context, or any context that is managed externally.
        // LWJGL detects the context that is current in the current thread,
        // creates the GLCapabilities instance and makes the OpenGL
        // bindings available for use.
        GL.createCapabilities();

        // Initialize rendering components
        renderer = new Renderer();
        textRenderer = new TextRenderer();
        sartManager = new SARTManager(renderer, textRenderer);

        // Set the clear color
        glClearColor(0.0f, 0.0f, 0.0f, 0.0f);

        // Run the rendering loop until the user has attempted to close
        // the window or has pressed the ESCAPE key.
        while ( !glfwWindowShouldClose(window) ) {
            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT); // clear the framebuffer

            // Get current window size
            try (MemoryStack stack = stackPush()) {
                IntBuffer w = stack.mallocInt(1);
                IntBuffer h = stack.mallocInt(1);
                glfwGetWindowSize(window, w, h);
                width = w.get();
                height = h.get();
            }

            // Render
            textRenderer.beginFrame(width, height);
            switch (state) {
                case START:
                    renderStartScreen();
                    break;
                case EXPERIMENT:
                    sartManager.render(textRenderer);
                    int currentTime = (int)((System.nanoTime() - experimentStart) / 1_000_000_000L);
                    textRenderer.drawText(String.valueOf(currentTime) + "/" + (60 * experimentDurationMinutes), 10, 20, 20);
                    break;
                case MENU:
                    renderMenu();
                    break;
                case SETTINGS:
                    renderSettings();
                    break;
                case COMPLETED:
                    renderCompletedScreen();
                    break;
                case REMINDER:
                    renderReminderScreen();
                    break;
                case COUNTDOWN:
                    renderCountdownScreen();
                    break;
                case TUTORIAL:
                    renderTutorialScreen();
                    break;
            }
            textRenderer.endFrame();

            // Update
            switch (state) {
                case EXPERIMENT:
                    sartManager.update();
                    if (!sartManager.isWaiting()) {
                        sartManager.startTrial();
                    }
                    // Only check experiment duration when not showing mistake feedback
                    if (!sartManager.isShowingMistake()) {
                        if (System.nanoTime() - experimentStart > experimentDurationMinutes * 60 * 1_000_000_000L) {
                            tutorialLogger.log(Level.INFO, "Experiment completed!");
                            tutorialLogger.log(Level.INFO, "a = " + String.format("%.4f", util.Calculations.getAccuracyPercentage()) + "; " + "t = " + String.format("%.4f", util.Calculations.getAverageCorrectGoResponseTimeMs()));
                            state = State.COMPLETED;
                        }
                    }
                    break;
                case COUNTDOWN:
                    if (System.nanoTime() - countdownStart > COUNTDOWN_DURATION_NS) {
                        state = State.EXPERIMENT;
                        experimentStart = System.nanoTime(); // Restart experiment timer
                    }
                    break;
                case REMINDER:
                    if (System.nanoTime() - reminderStart > REMINDER_DURATION_NS) {
                        state = State.COUNTDOWN;
                        countdownStart = System.nanoTime();
                    }
                    break;
            }

            // Handle inputs
            handleInputs();

            glfwSwapBuffers(window); // swap the color buffers

            // Poll for window events. The key callback above will only be
            // invoked during this call.
            glfwPollEvents();
        }
    }

    private void renderStartScreen() {
        textRenderer.drawText("Welcome to the SART experiment", 10, 20, 20);
        textRenderer.drawText("Current experiment mode: " + experimentDurationMinutes + " minute(s)", 10, 60, 20);

        float submitBx = (float) width - 270.0f;
        float submitBy = 20.0f;
        float submitBw = 240.0f;
        float submitBh = 45.0f;
        NVGColor greyButton = NVGColor.create();
        NanoVG.nvgRGBA((byte)150, (byte)150, (byte)150, (byte)255, greyButton);
        textRenderer.drawRect(submitBx, submitBy, submitBw, submitBh, greyButton);
        textRenderer.drawText("Open browser to submit results", submitBx + 12.0f, submitBy + 28.0f, 16.0f);

        // Settings Button
        float settingsBx = (float)width / 2 - 200, settingsBy = (float)height / 2 - 200, settingsBw = 400, settingsBh = 100;
        NVGColor blueSettings = NVGColor.create();
        NanoVG.nvgRGBA((byte)173, (byte)216, (byte)230, (byte)255, blueSettings); // light blue
        textRenderer.drawRect(settingsBx, settingsBy, settingsBw, settingsBh, blueSettings);
        textRenderer.drawText("Settings", settingsBx + 120, settingsBy + 60, 40);

        // Start button
        float startBx = (float)width / 2 - 200, startBy = (float)height / 2 - 50, startBw = 400, startBh = 100;
        NVGColor blue = NVGColor.create();
        NanoVG.nvgRGBA((byte)173, (byte)216, (byte)230, (byte)255, blue); // light blue
        textRenderer.drawRect(startBx, startBy, startBw, startBh, blue);
        textRenderer.drawText("Start experiment", startBx + 50, startBy + 60, 40);

        // Tutorial button
        float tutorialBx = (float)width / 2 - 200, tutorialBy = (float)height / 2 + 100, tutorialBw = 400, tutorialBh = 100;
        textRenderer.drawRect(tutorialBx, tutorialBy, tutorialBw, tutorialBh, blue);
        textRenderer.drawText("Tutorial", tutorialBx + 120, tutorialBy + 60, 40);
    }

    private void renderMenu() {
        // Buttons: Close, Enable Debug, Settings
        float bx = (float)width / 2 - 150, bw = 300, bh = 50;
        float by1 = (float)height / 2 - 100, by2 = (float)height / 2, by3 = (float)height / 2 + 100;
        NVGColor blue = NVGColor.create();
        NanoVG.nvgRGBA((byte)173, (byte)216, (byte)230, (byte)255, blue);
        textRenderer.drawRect(bx, by1, bw, bh, blue);
        textRenderer.drawText("Close Application", bx + 20, by1 + 35, 30);
        textRenderer.drawRect(bx, by2, bw, bh, blue);
        if (debugButtonPressed) {
            textRenderer.drawText("Debug disabled", bx + 20, by2 + 35, 30);
        } else {
        textRenderer.drawText("Toggle debug", bx + 20, by2 + 35, 30); }
        textRenderer.drawRect(bx, by3, bw, bh, blue);
        textRenderer.drawText("Time Conf.", bx + 20, by3 + 35, 30);
    }

    private void renderSettings() {
        NVGColor grey = NVGColor.create();
        NanoVG.nvgRGBA((byte)128, (byte)128, (byte)128, (byte)255, grey);
        textRenderer.drawRect(0, 0, width, height, grey);
        textRenderer.drawText("Settings", (float)width / 2 - 100, (float)height / 2 - 100, 50);

        // Draw duration text
        textRenderer.drawText("Experiment Duration: " + experimentDurationMinutes + " minutes", (float)width / 2 - 200, (float)height / 2 - 50, 30);

        // Draw slider
        float sliderX = (float)width / 2 - 200;
        float sliderY = (float)height / 2;
        float sliderW = 400;
        float sliderH = 20;
        textRenderer.drawImage(textRenderer.getSliderImage(), sliderX, sliderY, sliderW, sliderH);

        // Draw knob
        float knobX = sliderX + ((experimentDurationMinutes - 1) / 4.0f) * sliderW - 10;
        float knobY = sliderY - 5;
        float knobW = 20;
        float knobH = 30;
        NVGColor blue = NVGColor.create();
        NanoVG.nvgRGBA((byte)173, (byte)216, (byte)230, (byte)255, blue);
        textRenderer.drawRect(knobX, knobY, knobW, knobH, blue);
    }

    private void renderCompletedScreen() {
        textRenderer.drawText("Experiment completed (press Esc to quit program", (float)width / 2 - 360, (float)height / 2, 40);
        textRenderer.drawText("Or press Space to return to the main menu)", (float)width / 2 - 330, (float)height / 2 + 60, 40);
        textRenderer.drawText("Accuracy: " + String.format("%.2f", util.Calculations.getAccuracyPercentage()) + "%", 10, 20, 20);
        textRenderer.drawText("t\u0305: " + String.format(String.valueOf((int)util.Calculations.getAverageCorrectGoResponseTimeMs())), 10, 60, 20);
    }

    private void renderReminderScreen() {
        String reminderText = "Remember to only press the space bar to respond when the text 'Respond now!' is on screen!";
        float textX = (float) width / 2 - 450.0f;
        float textY = (float) height / 2 - 80.0f;
        textRenderer.drawBoldText(reminderText, textX, textY, 42.0f);
    }

    private void renderCountdownScreen() {
         // Calculate elapsed time and remaining time
         long elapsedNS = System.nanoTime() - countdownStart;
         long remainingNS = COUNTDOWN_DURATION_NS - elapsedNS;
         int remainingSeconds = Math.max(1, (int)((remainingNS + 999_999_999) / 1_000_000_000L)); // Round up

         // Draw "Beginning in" text at top middle
         textRenderer.drawText("Beginning in", (float)width / 2 - 100, (float)height / 2 - 100, 40);

         // Draw countdown numbers 3, 2, 1 horizontally
         float numberY = (float)height / 2 + 50;
         float spacing = 150;

         // Draw numbers with opacity based on current countdown
         for (int i = 3; i >= 1; i--) {
             float posX = (float)width / 2 - spacing + (3 - i) * spacing;
             float opacity = (i == remainingSeconds) ? 1.0f : 0.3f;
             drawCountdownNumber(String.valueOf(i), posX, numberY, opacity);
         }
     }

    private void renderTutorialScreen() {
        int tutorialImage = tutorialPage == 1 ? textRenderer.getIns1Image() : textRenderer.getIns2Image();
        float imageWidth = 739.0f;
        float imageHeight = 533.0f;
        float imageX = ((float) width - imageWidth) / 2.0f;
        float imageY = ((float) height - imageHeight) / 2.0f;
        textRenderer.drawImage(tutorialImage, imageX, imageY, imageWidth, imageHeight);
    }

    private void drawCountdownNumber(String text, float x, float y, float opacity) {
         NVGColor color = NVGColor.create();
         byte alpha = (byte) (255 * opacity);
         NanoVG.nvgRGBA((byte)255, (byte)255, (byte)255, alpha, color);
         textRenderer.drawText(text, x - 25, y, 100, color);
     }
    
    /**
     * Called when the Tutorial button is pressed.
     * This method serves as an interface for integrating tutorial features later.
     * For now, it does nothing, but can be extended to show tutorial content.
     */
    private void onTutorialButtonPressed() {
        tutorialLogger.info("Tutorial beginning!");
        tutorialPage = 1;
        state = State.TUTORIAL;
        spaceWasPressed = true;
    }

    private void resetToStartState() {
        state = State.START;
        inSettings = false;
        debugStatus = false;
        debugButtonPressed = false;
    }

    private void handleInputs() {
        // Disable all inputs when stimulus is displayed during experiment
        if (state == State.EXPERIMENT && sartManager.isShowingStimulus()) {
            return;
        }

        // Alt key for menu
        /*
        if (KeyListener.isKeyPressed(GLFW_KEY_LEFT_ALT) || KeyListener.isKeyPressed(GLFW_KEY_RIGHT_ALT)) {
            if (!altPressed) {
                if (state == State.START) {
                    state = State.MENU;
                } else if (state == State.MENU) {
                    state = State.START;
                }
                altPressed = true;
            }
        } else {
            altPressed = false;
        }
        */

        /*
        // ESC key
        if (KeyListener.isKeyPressed(GLFW_KEY_ESCAPE)) {
            if (state == State.SETTINGS) {
                state = State.MENU;
            } else if (state == State.MENU) {
                state = State.START;
            } else if (state == State.COMPLETED) {
                glfwSetWindowShouldClose(window, true);
            }
        }
         */

        boolean escapePressed = KeyListener.isKeyPressed(GLFW_KEY_ESCAPE);
        if (escapePressed && !escapeWasPressed) {
            switch (state) {
                case SETTINGS:
                    inSettings = false;
                    state = State.MENU;
                    break;
                case MENU:
                    resetToStartState();
                    break;
                case COMPLETED:
                    tutorialLogger.info("Force close from Esc");
                    glfwSetWindowShouldClose(window, true);
                    break;
            }
        }
        escapeWasPressed = escapePressed;

        boolean spacePressed = KeyListener.isKeyPressed(GLFW_KEY_SPACE);
        if (state == State.TUTORIAL && spacePressed && !spaceWasPressed) {
            if (tutorialPage == 1) {
                tutorialPage = 2;
            } else {
                experimentDurationMinutes = 1; // Set a shorter duration for tutorial/demo purposes
                state = State.REMINDER;
                reminderStart = System.nanoTime();
            }
        } else if (state == State.COMPLETED && spacePressed && !spaceWasPressed) {
            resetToStartState();
            experimentDurationMinutes = 5;
        }
        spaceWasPressed = spacePressed;

        // Mouse click
        boolean leftMousePressed = MouseListener.mouseButtonDown(0);
        if (leftMousePressed && !leftMouseWasPressed) {
            float mx = MouseListener.getX();
            float my = MouseListener.getY();
            switch (state) {
                case START:
                    float submitBx = (float) width - 270.0f;
                    float submitBy = 20.0f;
                    float submitBw = 240.0f;
                    float submitBh = 45.0f;
                    if (mx >= submitBx && mx <= submitBx + submitBw && my >= submitBy && my <= submitBy + submitBh) {
                        Invocation.openResultsSubmissionPage();
                        break;
                    }
                    // Start button
                    if (mx >= (float)width/2 - 200 && mx <= (float)width/2 + 200 && my >= (float)height/2 - 50 && my <= (float)height/2 + 50) {
                        state = State.REMINDER;
                        reminderStart = System.nanoTime();
                    }
                    // Tutorial button
                    else if (mx >= (float)width/2 - 200 && mx <= (float)width/2 + 200 && my >= (float)height/2 + 100 && my <= (float)height/2 + 200) {
                        onTutorialButtonPressed();
                    }
                    else if (mx >= (float)width/2 - 200 && mx <= (float)width/2 + 200 && my >= (float)height/2 - 200 && my <= (float)height/2 - 100) {
                        state = State.MENU;
                        // System.out.println("Settings button pressed");
                    }
                    break;
                case MENU:
                    float bx = (float)width / 2 - 150, bw = 300, bh = 50;
                    float by1 = (float)height / 2 - 100, by2 = (float)height / 2, by3 = (float)height / 2 + 100;
                    if (mx >= bx && mx <= bx + bw) {
                        if (my >= by1 && my <= by1 + bh) {
                            // Close
                            tutorialLogger.info("Close from button");
                            glfwSetWindowShouldClose(window, true);
                        } else if (!inSettings && my >= by2 && my <= by2 + bh) {
                            if (!debugButtonPressed) {
                                tutorialLogger.debug("Debug pressed");
                                debugButtonPressed = true;
                                break;
                            }
                            // Enable Debug
                            /*
                            if (!debugStatus && !debugButtonToggled) {
                                debugStatus = true;
                                debugButtonToggled = true;
                                tutorialLogger.debug("Debug enabled");
                                break;
                            } else if(debugStatus && !debugButtonToggled) {
                                debugStatus = false;
                                debugButtonToggled = true;
                                tutorialLogger.debug("Debug disabled");
                                break;
                            }
                            */
                            // textRenderer.drawText("Feature disabled", bx + 20, by2 + 35, 30);
                            // tutorialLogger.debug("Debug disabled on release builds.");
                            // state = State.START; // or back to previous, but for now to start
                        } else if (my >= by3 && my <= by3 + bh) {
                            // Settings
                            state = State.SETTINGS;
                            inSettings = true;
                            break;
                        }
                    }
                case SETTINGS:
                    float sliderX = (float)width / 2 - 200;
                    float sliderY = (float)height / 2;
                    float sliderW = 400;
                    float sliderH = 20;
                    if (inSettings && mx >= sliderX && mx <= sliderX + sliderW && my >= sliderY - 10 && my <= sliderY + sliderH + 10) {
                        float ratio = (mx - sliderX) / sliderW;
                        int newDuration = 1 + (int) Math.round(ratio * 4);
                        experimentDurationMinutes = Math.max(1, Math.min(5, newDuration));
                    }
                    break;
            }
        }
        leftMouseWasPressed = leftMousePressed;

        if (debugStatus) {
            Debug.InputDebug();
        }
    }

}
