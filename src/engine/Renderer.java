package engine;

import org.lwjgl.BufferUtils;

import java.nio.FloatBuffer;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;

public class Renderer {
    private int vao, vbo, ebo;
    private Shader shader;

    public Renderer() {
//        shader = new Shader("src/shaders/vertex.glsl", "src/shaders/fragment.glsl");
          shader = new Shader("shaders/vertex.glsl", "shaders/fragment.glsl");

        // Quad vertices
        float[] vertices = {
            // positions    // texture coords
            1.0f,  1.0f,   1.0f, 1.0f,   // top right
            1.0f, -1.0f,   1.0f, 0.0f,   // bottom right
           -1.0f, -1.0f,   0.0f, 0.0f,   // bottom left
           -1.0f,  1.0f,   0.0f, 1.0f    // top left
        };

        int[] indices = {
            0, 1, 3,   // first triangle
            1, 2, 3    // second triangle
        };

        vao = glGenVertexArrays();
        glBindVertexArray(vao);

        vbo = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        FloatBuffer buffer = BufferUtils.createFloatBuffer(vertices.length);
        buffer.put(vertices).flip();
        glBufferData(GL_ARRAY_BUFFER, buffer, GL_STATIC_DRAW);

        ebo = glGenBuffers();
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, ebo);
        glBufferData(GL_ELEMENT_ARRAY_BUFFER, indices, GL_STATIC_DRAW);

        glVertexAttribPointer(0, 2, GL_FLOAT, false, 4 * Float.BYTES, 0);
        glEnableVertexAttribArray(0);

        glVertexAttribPointer(1, 2, GL_FLOAT, false, 4 * Float.BYTES, 2 * Float.BYTES);
        glEnableVertexAttribArray(1);

        glBindBuffer(GL_ARRAY_BUFFER, 0);
        glBindVertexArray(0);
    }

    public void renderTexture(Texture texture, float x, float y, float width, float height) {
        shader.use();
        texture.bind();

        // For simplicity, render full screen or scaled. Adjust vertices if needed.
        // This is a basic full quad render. For positioned rendering, need to modify.

        glBindVertexArray(vao);
        glDrawElements(GL_TRIANGLES, 6, GL_UNSIGNED_INT, 0);
        glBindVertexArray(0);

        texture.unbind();
    }
}
