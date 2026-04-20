package engine;

import org.lwjgl.nanovg.NanoVG;
import org.lwjgl.stb.STBImage;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public final class ImageAssetLoader {

    private ImageAssetLoader() {
    }

    public static LoadedImage load(String assetPath) {
        return load(assetPath, 4);
    }

    public static LoadedImage load(String assetPath, int desiredChannels) {
        String normalizedPath = normalizePath(assetPath);
        byte[] imageBytes = readAssetBytes(normalizedPath);

        ByteBuffer encodedImage = MemoryUtil.memAlloc(imageBytes.length);
        encodedImage.put(imageBytes).flip();

        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer widthBuffer = stack.mallocInt(1);
            IntBuffer heightBuffer = stack.mallocInt(1);
            IntBuffer channelBuffer = stack.mallocInt(1);

            ByteBuffer pixels = STBImage.stbi_load_from_memory(
                    encodedImage,
                    widthBuffer,
                    heightBuffer,
                    channelBuffer,
                    desiredChannels
            );

            if (pixels == null) {
                throw new RuntimeException("Failed to load image '" + normalizedPath + "': " + STBImage.stbi_failure_reason());
            }

            int channels = desiredChannels != 0 ? desiredChannels : channelBuffer.get(0);
            return new LoadedImage(pixels, widthBuffer.get(0), heightBuffer.get(0), channels);
        } finally {
            MemoryUtil.memFree(encodedImage);
        }
    }

    public static int loadNanoVGImage(long vg, String assetPath) {
        return loadNanoVGImage(vg, assetPath, 4);
    }

    public static int loadNanoVGImage(long vg, String assetPath, int desiredChannels) {
        try (LoadedImage image = load(assetPath, desiredChannels)) {
            int imageHandle = NanoVG.nvgCreateImageRGBA(
                    vg,
                    image.getWidth(),
                    image.getHeight(),
                    0,
                    image.getPixels()
            );
            if (imageHandle == 0) {
                throw new RuntimeException("Failed to create NanoVG image for '" + assetPath + "'");
            }
            return imageHandle;
        }
    }

    private static byte[] readAssetBytes(String assetPath) {
        InputStream resourceStream = openResourceStream(assetPath);
        if (resourceStream != null) {
            try (InputStream inputStream = resourceStream) {
                return readFully(inputStream);
            } catch (IOException e) {
                throw new RuntimeException("Failed to read image resource '" + assetPath + "'", e);
            }
        }

        Path[] candidatePaths = new Path[] {
                Paths.get(assetPath),
                Paths.get("src").resolve(assetPath),
                Paths.get("resources").resolve(assetPath)
        };

        for (Path candidate : candidatePaths) {
            if (Files.exists(candidate)) {
                try {
                    return Files.readAllBytes(candidate);
                } catch (IOException e) {
                    throw new RuntimeException("Failed to read image file '" + candidate + "'", e);
                }
            }
        }

        throw new RuntimeException("Image asset not found: " + assetPath);
    }

    private static InputStream openResourceStream(String assetPath) {
        ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
        if (contextClassLoader != null) {
            InputStream inputStream = contextClassLoader.getResourceAsStream(assetPath);
            if (inputStream != null) {
                return inputStream;
            }
        }

        InputStream inputStream = ImageAssetLoader.class.getClassLoader().getResourceAsStream(assetPath);
        if (inputStream != null) {
            return inputStream;
        }

        if (assetPath.startsWith("/")) {
            return ImageAssetLoader.class.getResourceAsStream(assetPath);
        }

        return ImageAssetLoader.class.getResourceAsStream("/" + assetPath);
    }

    private static byte[] readFully(InputStream inputStream) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        byte[] buffer = new byte[4096];
        int bytesRead;
        while ((bytesRead = inputStream.read(buffer)) != -1) {
            outputStream.write(buffer, 0, bytesRead);
        }
        return outputStream.toByteArray();
    }

    private static String normalizePath(String assetPath) {
        String normalized = assetPath.replace('\\', '/');
        if (normalized.startsWith("/")) {
            return normalized.substring(1);
        }
        return normalized;
    }

    public static final class LoadedImage implements AutoCloseable {
        private final ByteBuffer pixels;
        private final int width;
        private final int height;
        private final int channels;

        private LoadedImage(ByteBuffer pixels, int width, int height, int channels) {
            this.pixels = pixels;
            this.width = width;
            this.height = height;
            this.channels = channels;
        }

        public ByteBuffer getPixels() {
            return pixels;
        }

        public int getWidth() {
            return width;
        }

        public int getHeight() {
            return height;
        }

        public int getChannels() {
            return channels;
        }

        @Override
        public void close() {
            STBImage.stbi_image_free(pixels);
        }
    }
}
