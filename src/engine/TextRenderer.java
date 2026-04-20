package engine;

import org.lwjgl.nanovg.NVGColor;
import org.lwjgl.nanovg.NVGPaint;
import org.lwjgl.nanovg.NanoVG;
import org.lwjgl.nanovg.NanoVGGL3;
import org.lwjgl.system.MemoryUtil;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

import static org.lwjgl.nanovg.NanoVG.*;
import static org.lwjgl.opengl.GL11.*;

public class TextRenderer {
    private long vg;
    private NVGColor color;
    private int sliderImage;

    private int ins1Image;
    private int ins2Image;
    private int mistake6Image;
    private int mistakeNoResponseImage;

    private ByteBuffer fontData;
    private ByteBuffer boldFontData;

    public TextRenderer() {
        vg = NanoVGGL3.nvgCreate(NanoVGGL3.NVG_ANTIALIAS | NanoVGGL3.NVG_STENCIL_STROKES);
        if (vg == 0) {
            throw new RuntimeException("Could not init NanoVG");
        }
        color = NVGColor.create();

        fontData = loadFontBuffer("assets/fonts/ARIAL.TTF");
        if (nvgCreateFontMem(vg, "arial", fontData, false) == -1) {
            throw new RuntimeException("Could not load font: assets/fonts/ARIAL.TTF");
        }

        boldFontData = loadFontBuffer("assets/fonts/ARIALBD.TTF");
        if (nvgCreateFontMem(vg, "arial-bold", boldFontData, false) == -1) {
            throw new RuntimeException("Could not load font: assets/fonts/ARIALBD.TTF");
        }

        sliderImage = ImageAssetLoader.loadNanoVGImage(vg, "assets/slider/slider.png");
        ins1Image = ImageAssetLoader.loadNanoVGImage(vg, "assets/instructions/instructions_1.jpg");
        ins2Image = ImageAssetLoader.loadNanoVGImage(vg, "assets/instructions/instructions_2.jpg");
        mistake6Image = ImageAssetLoader.loadNanoVGImage(vg, "assets/errors/mistake_6.jpg");
        mistakeNoResponseImage = ImageAssetLoader.loadNanoVGImage(vg, "assets/errors/mistake_noresponse.jpg");

    }

    public void beginFrame(int width, int height) {
        NanoVG.nvgBeginFrame(vg, width, height, 1.0f);
    }

    public void endFrame() {
        NanoVG.nvgEndFrame(vg);
    }

    public void drawText(String text, float x, float y, float fontSize) {
        nvgFontSize(vg, fontSize);
        nvgFontFace(vg, "arial");
        nvgFillColor(vg, nvgRGBA((byte)255, (byte)255, (byte)255, (byte)255, color)); // White text
        nvgText(vg, x, y, text);
    }

    public void drawText(String text, float x, float y, float fontSize, NVGColor textColor) {
        nvgFontSize(vg, fontSize);
        nvgFontFace(vg, "arial");
        nvgFillColor(vg, textColor);
        nvgText(vg, x, y, text);
    }

    public void drawBoldText(String text, float x, float y, float fontSize) {
        nvgFontSize(vg, fontSize);
        nvgFontFace(vg, "arial-bold");
        nvgFillColor(vg, nvgRGBA((byte)255, (byte)255, (byte)255, (byte)255, color));
        nvgTextBox(vg, x, y, 900.0f, text);
    }

    public void drawRect(float x, float y, float w, float h, NVGColor color) {
        nvgBeginPath(vg);
        nvgRect(vg, x, y, w, h);
        nvgFillColor(vg, color);
        nvgFill(vg);
    }

    public void drawImage(int image, float x, float y, float w, float h) {
        NVGPaint paint = NVGPaint.create();
        nvgImagePattern(vg, x, y, w, h, 0.0f, image, 1.0f, paint);
        nvgBeginPath(vg);
        nvgRect(vg, x, y, w, h);
        nvgFillPaint(vg, paint);
        nvgFill(vg);
    }

    public int getSliderImage() {
        return sliderImage;
    }

    public int getIns1Image() {
        return ins1Image;
    }

    public int getIns2Image() {
        return ins2Image;
    }

    public int getMistake6Image() {
        return mistake6Image;
    }

    public int getMistakeNoResponseImage() {
        return mistakeNoResponseImage;
    }

    private ByteBuffer loadFontBuffer(String resourcePath) {
        try (InputStream fontStream = getClass().getClassLoader().getResourceAsStream(resourcePath)) {
            if (fontStream == null) {
                throw new RuntimeException("Could not load font: " + resourcePath);
            }
            byte[] fontBytes = readFully(fontStream);
            ByteBuffer loadedFontData = MemoryUtil.memAlloc(fontBytes.length);
            loadedFontData.put(fontBytes).flip();
            return loadedFontData;
        } catch (IOException e) {
            throw new RuntimeException("Failed to load font: " + resourcePath, e);
        }
    }

    private byte[] readFully(InputStream inputStream) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        byte[] buffer = new byte[4096];
        int bytesRead;
        while ((bytesRead = inputStream.read(buffer)) != -1) {
            outputStream.write(buffer, 0, bytesRead);
        }
        return outputStream.toByteArray();
    }

    public void cleanup() {
        nvgDeleteImage(vg, sliderImage);
        nvgDeleteImage(vg, ins1Image);
        nvgDeleteImage(vg, ins2Image);
        nvgDeleteImage(vg, mistake6Image);
        nvgDeleteImage(vg, mistakeNoResponseImage);
        NanoVGGL3.nvgDelete(vg);
        if (fontData != null) {
            MemoryUtil.memFree(fontData);
            fontData = null;
        }
        if (boldFontData != null) {
            MemoryUtil.memFree(boldFontData);
            boldFontData = null;
        }
    }
}
