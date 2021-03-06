/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tests.gl_400;

import com.jogamp.opengl.GL;
import static com.jogamp.opengl.GL3ES3.*;
import com.jogamp.opengl.GL4;
import com.jogamp.opengl.util.GLBuffers;
import com.jogamp.opengl.util.glsl.ShaderCode;
import com.jogamp.opengl.util.glsl.ShaderProgram;
import glm.glm;
import glm.mat._4.Mat4;
import glm.vec._2.Vec2;
import dev.Vec4u8;
import framework.BufferUtils;
import framework.Profile;
import framework.Semantic;
import framework.Test;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;

/**
 *
 * @author GBarbieri
 */
public class Gl_400_primitive_smooth_shading extends Test {

    public static void main(String[] args) {
        Gl_400_primitive_smooth_shading gl_400_primitive_smooth_shading = new Gl_400_primitive_smooth_shading();
    }

    public Gl_400_primitive_smooth_shading() {
        super("gl-400-primitive-smooth-shading", Profile.CORE, 4, 0);
    }

    private final String SHADERS_SOURCE1 = "tess";
    private final String SHADERS_SOURCE2 = "smooth-shading-geom";
    private final String SHADERS_ROOT = "src/data/gl_400";

    private int vertexCount = 4;
    private int vertexSize = vertexCount * glf.Vertex_v2fc4ub.SIZE;
    private glf.Vertex_v2fc4ub[] vertexData = {
        new glf.Vertex_v2fc4ub(new Vec2(-1.0f, -1.0f), new Vec4u8(255, 0, 0, 255)),
        new glf.Vertex_v2fc4ub(new Vec2(1.0f, -1.0f), new Vec4u8(255, 255, 255, 255)),
        new glf.Vertex_v2fc4ub(new Vec2(1.0f, 1.0f), new Vec4u8(0, 255, 0, 255)),
        new glf.Vertex_v2fc4ub(new Vec2(-1.0f, 1.0f), new Vec4u8(0, 0, 255, 255))};

    private int elementCount = 6;
    private int elementSize = elementCount * Short.BYTES;
    private short[] elementData = {
        0, 1, 2,
        2, 3, 0};

    private class Program {

        public static final int TESS = 0;
        public static final int SMOOTH = 1;
        public static final int MAX = 2;
    }

    private class Buffer {

        public static final int ARRAY = 0;
        public static final int ELEMENT = 1;
        public static final int MAX = 2;
    }

    private int[] programName = new int[Program.MAX], uniformMvp = new int[Program.MAX];
    private IntBuffer bufferName = GLBuffers.newDirectIntBuffer(Buffer.MAX),
            vertexArrayName = GLBuffers.newDirectIntBuffer(1);

    @Override
    protected boolean begin(GL gl) {

        GL4 gl4 = (GL4) gl;

        boolean validated = true;

        if (validated) {
            validated = initProgram(gl4);
        }
        if (validated) {
            validated = initBuffer(gl4);
        }
        if (validated) {
            validated = initVertexArray(gl4);
        }

        return validated && checkError(gl4, "begin");
    }

    private boolean initProgram(GL4 gl4) {

        boolean validated = true;

        if (validated) {

            ShaderProgram shaderProgram = new ShaderProgram();

            ShaderCode vertShaderCode = ShaderCode.create(gl4, GL_VERTEX_SHADER, this.getClass(), SHADERS_ROOT,
                    null, SHADERS_SOURCE1, "vert", null, true);
            ShaderCode contShaderCode = ShaderCode.create(gl4, GL_TESS_CONTROL_SHADER, this.getClass(), SHADERS_ROOT,
                    null, SHADERS_SOURCE1, "cont", null, true);
            ShaderCode evalShaderCode = ShaderCode.create(gl4, GL_TESS_EVALUATION_SHADER, this.getClass(), SHADERS_ROOT,
                    null, SHADERS_SOURCE1, "eval", null, true);
            ShaderCode geomShaderCode = ShaderCode.create(gl4, GL_GEOMETRY_SHADER, this.getClass(), SHADERS_ROOT,
                    null, SHADERS_SOURCE1, "geom", null, true);
            ShaderCode fragShaderCode = ShaderCode.create(gl4, GL_FRAGMENT_SHADER, this.getClass(), SHADERS_ROOT,
                    null, SHADERS_SOURCE1, "frag", null, true);

            shaderProgram.init(gl4);

            programName[Program.TESS] = shaderProgram.program();

            shaderProgram.add(vertShaderCode);
            shaderProgram.add(geomShaderCode);
            shaderProgram.add(contShaderCode);
            shaderProgram.add(evalShaderCode);
            shaderProgram.add(fragShaderCode);

            shaderProgram.link(gl4, System.out);
        }

        if (validated) {

            ShaderProgram shaderProgram = new ShaderProgram();

            ShaderCode vertShaderCode = ShaderCode.create(gl4, GL_VERTEX_SHADER, this.getClass(), SHADERS_ROOT, null,
                    SHADERS_SOURCE2, "vert", null, true);
            ShaderCode geomShaderCode = ShaderCode.create(gl4, GL_GEOMETRY_SHADER, this.getClass(), SHADERS_ROOT, null,
                    SHADERS_SOURCE2, "geom", null, true);
            ShaderCode fragShaderCode = ShaderCode.create(gl4, GL_FRAGMENT_SHADER, this.getClass(), SHADERS_ROOT, null,
                    SHADERS_SOURCE2, "frag", null, true);

            shaderProgram.init(gl4);
            programName[Program.SMOOTH] = shaderProgram.program();

            shaderProgram.add(vertShaderCode);
            shaderProgram.add(geomShaderCode);
            shaderProgram.add(fragShaderCode);

            shaderProgram.link(gl4, System.out);
        }

        // Get variables locations
        if (validated) {

            uniformMvp[Program.TESS] = gl4.glGetUniformLocation(programName[Program.TESS], "mvp");
            uniformMvp[Program.SMOOTH] = gl4.glGetUniformLocation(programName[Program.SMOOTH], "mvp");
        }

        return validated & checkError(gl4, "initProgram");
    }

    private boolean initVertexArray(GL4 gl4) {

        gl4.glGenVertexArrays(1, vertexArrayName);
        gl4.glBindVertexArray(vertexArrayName.get(0));
        {
            gl4.glBindBuffer(GL_ARRAY_BUFFER, bufferName.get(Buffer.ARRAY));
            gl4.glVertexAttribPointer(Semantic.Attr.POSITION, 2, GL_FLOAT, false, glf.Vertex_v2fc4ub.SIZE, 0);
            gl4.glVertexAttribPointer(Semantic.Attr.COLOR, 4, GL_UNSIGNED_BYTE, true, glf.Vertex_v2fc4ub.SIZE, Vec2.SIZE);
            gl4.glEnableVertexAttribArray(Semantic.Attr.POSITION);
            gl4.glEnableVertexAttribArray(Semantic.Attr.COLOR);
            gl4.glBindBuffer(GL_ARRAY_BUFFER, 0);
        }
        gl4.glBindVertexArray(0);

        return checkError(gl4, "initVertexArray");
    }

    private boolean initBuffer(GL4 gl4) {

        ShortBuffer elementBuffer = GLBuffers.newDirectShortBuffer(elementData);
        ByteBuffer vertexBuffer = GLBuffers.newDirectByteBuffer(vertexSize);

        gl4.glGenBuffers(Buffer.MAX, bufferName);
        gl4.glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, bufferName.get(Buffer.ELEMENT));
        gl4.glBufferData(GL_ELEMENT_ARRAY_BUFFER, elementSize, elementBuffer, GL_STATIC_DRAW);
        gl4.glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, 0);

        gl4.glBindBuffer(GL_ARRAY_BUFFER, bufferName.get(Buffer.ARRAY));
        for (int i = 0; i < vertexCount; i++) {
            vertexData[i].toBb(vertexBuffer, i);
        }
        vertexBuffer.rewind();
        gl4.glBufferData(GL_ARRAY_BUFFER, vertexSize, vertexBuffer, GL_STATIC_DRAW);
        gl4.glBindBuffer(GL_ARRAY_BUFFER, 0);

        BufferUtils.destroyDirectBuffer(elementBuffer);
        BufferUtils.destroyDirectBuffer(vertexBuffer);

        return checkError(gl4, "initBuffer");
    }

    @Override
    protected boolean render(GL gl) {

        GL4 gl4 = (GL4) gl;

        Mat4 projection = glm.perspective_((float) Math.PI * 0.25f, windowSize.x * 0.5f / windowSize.y, 0.1f, 100.0f);
        Mat4 model = new Mat4(1.0f);
        Mat4 mvp = projection.mul(viewMat4()).mul(model);

        gl4.glViewport(0, 0, windowSize.x, windowSize.y);
        gl4.glClearBufferfv(GL_COLOR, 0, clearColor.put(0, 0).put(1, 0).put(2, 0).put(3, 1));

        gl4.glBindVertexArray(vertexArrayName.get(0));
        gl4.glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, bufferName.get(Buffer.ELEMENT));

        gl4.glViewport(0, 0, (int) (windowSize.x * 0.5f), windowSize.y);
        gl4.glUseProgram(programName[Program.TESS]);
        gl4.glUniformMatrix4fv(uniformMvp[Program.TESS], 1, false, mvp.toFa_(), 0);

        gl4.glPatchParameteri(GL_PATCH_VERTICES, vertexCount);
        gl4.glDrawArraysInstanced(GL_PATCHES, 0, vertexCount, 1);

        gl4.glViewport((int) (windowSize.x * 0.5f), 0, (int) (windowSize.x * 0.5f), windowSize.y);
        gl4.glUseProgram(programName[Program.SMOOTH]);
        gl4.glUniformMatrix4fv(uniformMvp[Program.SMOOTH], 1, false, mvp.toFa_(), 0);

        gl4.glDrawElementsInstancedBaseVertex(GL_TRIANGLES, elementCount, GL_UNSIGNED_SHORT, 0, 1, 0);

        return true;
    }

    @Override
    protected boolean end(GL gl) {

        GL4 gl4 = (GL4) gl;

        gl4.glDeleteVertexArrays(1, vertexArrayName);
        gl4.glDeleteBuffers(Buffer.MAX, bufferName);
        gl4.glDeleteProgram(programName[0]);
        gl4.glDeleteProgram(programName[1]);

        BufferUtils.destroyDirectBuffer(vertexArrayName);
        BufferUtils.destroyDirectBuffer(bufferName);

        return checkError(gl4, "end");
    }
}
