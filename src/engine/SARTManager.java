package engine;

import engine.KeyListener;
import engine.Renderer;
import util.Calculations;

import java.util.Random;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.nanovg.NVGColor;

import static org.lwjgl.glfw.GLFW.GLFW_KEY_SPACE;
import static org.lwjgl.nanovg.NanoVG.nvgRGBA;

public class SARTManager {

    private static Logger logger = LogManager.getLogger(SARTManager.class);

    public static void logTrial(float fontSize, int stimulus, long responseTime, String responseStatus) {
        if(responseTime == -1) {
            String result = String.format(fontSize + "; " + stimulus + "; " + responseStatus + "; " + "N/A");
            logger.info(result);
        } else {
            String result = String.format(fontSize + "; " + stimulus + "; " + responseStatus + "; " + responseTime);
            logger.info(result);
        }
    }

    private Renderer renderer;
    private TextRenderer textRenderer;
    private Random random = new Random();
    private long stimulusOnset;
    private boolean showStimulus = false;
    private boolean showFixation = false;
    private boolean showMistake = false;
    private int currentStimulus;
    private long lastResponseTime = 0;
    private float currentFontSize;
    private long displayDuration; // in nanoseconds
    private long fixationStart;
    private long mistakeStart;
    private MistakeType currentMistakeType = MistakeType.NONE;
    private boolean responded = false;
    private boolean spacePressedThisTrial = false; // Flag to track space press
    private long spacePressTime = -1; // Time when space was pressed during response window

    private enum MistakeType {
        NONE,
        PRESSED_ON_SIX,
        NO_RESPONSE
    }

    public SARTManager(Renderer renderer, TextRenderer textRenderer) {
        this.renderer = renderer;
        this.textRenderer = textRenderer;
    }

    public void startTrial() {
        if (!showStimulus && !showFixation && !showMistake) {
            // Generate random number 1-9
            currentStimulus = random.nextInt(9) + 1;
            stimulusOnset = System.nanoTime();
            showStimulus = true;
            showFixation = false;
            showMistake = false;
            currentMistakeType = MistakeType.NONE;
            responded = false;
            spacePressedThisTrial = false; // Reset space press flag
            spacePressTime = -1; // Reset space press time
            // Clear any leftover key presses from previous trials
            KeyListener.clearLastKey();
            // Randomize appearance
            currentFontSize = random.nextInt(100) + 50; // 50-150
            // Fixed display duration: 250 ms
            displayDuration = 250  * 1_000_000L; // in nanoseconds
        }
    }

    public void render(TextRenderer textRenderer) {
        if (showStimulus && System.nanoTime() - stimulusOnset < displayDuration) {
            // Draw the stimulus in the center
            String text = String.valueOf(currentStimulus);
            float x = 1280 / 2 - (text.length() * currentFontSize * 0.3f); // Rough center
            float y = 720 / 2 + currentFontSize / 2;
            textRenderer.drawText(text, x, y, currentFontSize);
        } else if (showFixation) {
            // Draw fixation "x"
            float fontSize = 100.0f;
            float x = 1280 / 2 - (fontSize * 3.0f);
            float y = 720 / 2 + fontSize / 2;
            NVGColor responseTextColour = NVGColor.create();
            textRenderer.drawText("Respond now!", x, y, fontSize, nvgRGBA((byte)0, (byte)255, (byte)0, (byte)100, responseTextColour));
        } else if (showMistake) {
            float imageWidth = 739.0f;
            float imageHeight = 533.0f;
            float imageX = (1280.0f - imageWidth) / 2.0f;
            float imageY = (720.0f - imageHeight) / 2.0f;
            if (currentMistakeType == MistakeType.PRESSED_ON_SIX) {
                textRenderer.drawImage(textRenderer.getMistake6Image(), imageX, imageY, imageWidth, imageHeight);
            } else if (currentMistakeType == MistakeType.NO_RESPONSE) {
                textRenderer.drawImage(textRenderer.getMistakeNoResponseImage(), imageX, imageY, imageWidth, imageHeight);
            }
        }
    }

    public void update() {
        long now = System.nanoTime();
        if (showStimulus && now - stimulusOnset >= displayDuration) {
            showStimulus = false;
            showFixation = true;
            fixationStart = now;
            KeyListener.clearLastKey(); // Clear any keys pressed during stimulus display
        } else if (showFixation) {
            // During the 900ms response window, monitor keyboard input but don't process responses yet
            // The "x" will be displayed for the full 900ms duration
            if (KeyListener.getLastKeyPressed() == GLFW_KEY_SPACE && !spacePressedThisTrial && spacePressTime == -1) {
                // Record the exact time when space was pressed (from KeyListener)
                spacePressedThisTrial = true;
                spacePressTime = KeyListener.getLastKeyPressTime();
                // Don't clear the key here - we need to keep the timestamp for later use
            }

            if (now - fixationStart >= 900_000_000L) {
                // Response window has elapsed, now process the outcome
                if (!responded) {
                    if (spacePressTime != -1 && spacePressTime >= fixationStart) {
                        // Space was pressed during the window and after fixation started
                        long responseTimeMs = (spacePressTime - fixationStart) / 1_000_000L;
                        // Number 6 is a "no-go" condition - should not respond
                        if (currentStimulus == 6) {
                            // System.out.println(currentFontSize + "; " + currentStimulus + "; " + responseTimeMs + "; Response unsuccessful");
                            // logStringTransfer = currentFontSize + "; " + currentStimulus + "; " + responseTimeMs + "; Response unsuccessful";
                            logTrial(currentFontSize, currentStimulus, responseTimeMs, "Response unsuccessful");
                            Calculations.recordResponse(false);
                            // logger.error("user failed");
                            responded = true;
                            showFixation = false;
                            showMistake = true;
                            currentMistakeType = MistakeType.PRESSED_ON_SIX;
                            mistakeStart = now;
                            lastResponseTime = now;
                        } else {
                            // System.out.println(currentFontSize + "; " + currentStimulus + "; " + responseTimeMs + "; Response successful");
                            // logStringTransfer = currentFontSize + "; " + currentStimulus + "; " + responseTimeMs + "; Response successful";
                            logTrial(currentFontSize, currentStimulus, responseTimeMs, "Response successful");
                            Calculations.recordResponse(true);
                            Calculations.recordCorrectGoResponseTime(responseTimeMs);
                            responded = true;
                            showFixation = false;
                            lastResponseTime = now;
                        }
                    } else {
                        // No valid response during the window
                        // Number 6 is no-go: not responding is correct
                        if (currentStimulus == 6) {
                            // System.out.println(currentFontSize + "; " + currentStimulus + "; " + "N/A; Response successful");
                            // logStringTransfer = currentFontSize + "; " + currentStimulus + "; " + "N/A; Response successful";
                            logTrial(currentFontSize, currentStimulus, -1, "Response successful");
                            Calculations.recordResponse(true);
                            responded = true;
                            showFixation = false;
                            lastResponseTime = now;
                        } else {
                            // For other numbers, not responding is a mistake
                            long responseTimeMs = 900L; // Full 900ms response window elapsed
                            // System.out.println(currentFontSize + "; " + currentStimulus + "; " + responseTimeMs + "; Response unsuccessful");
                            // logStringTransfer = currentFontSize + "; " + currentStimulus + "; " + responseTimeMs + "; Response unsuccessful";
                            logTrial(currentFontSize, currentStimulus, responseTimeMs, "Response unsuccessful");
                            Calculations.recordResponse(false);
                            showFixation = false;
                            showMistake = true;
                            currentMistakeType = MistakeType.NO_RESPONSE;
                            mistakeStart = now;
                            responded = true;
                        }
                    }
                }
                // Clear keys after processing the response
                KeyListener.clearLastKey();
            }
        } else if (showMistake && now - mistakeStart >= 3_000_000_000L) {
            showMistake = false;
            currentMistakeType = MistakeType.NONE;
            lastResponseTime = now;
        }
    }

    public boolean isWaiting() {
        return showStimulus || showFixation || showMistake;
    }

    public boolean isShowingStimulus() {
        return showStimulus;
    }

    public boolean isShowingMistake() {
        return showMistake;
    }
}
