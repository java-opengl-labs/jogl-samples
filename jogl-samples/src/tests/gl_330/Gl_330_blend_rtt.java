/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tests.gl_330;

import com.jogamp.opengl.GL;
import static com.jogamp.opengl.GL2GL3.*;
import com.jogamp.opengl.GL3;
import com.jogamp.opengl.util.GLBuffers;
import com.jogamp.opengl.util.glsl.ShaderCode;
import com.jogamp.opengl.util.glsl.ShaderProgram;
import framework.BufferUtils;
import glm.glm;
import glm.mat._4.Mat4;
import framework.Profile;
import framework.Semantic;
import framework.Test;
import glf.Vertex_v2fv2f;
import glm.vec._2.Vec2;
import java.io.IOException;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.logging.Level;
import java.util.logging.Logger;
import jgli.Texture2d;
import jglm.Vec4i;

/**
 *
 * @author GBarbieri
 */
public class Gl_330_blend_rtt extends Test {

    public static void main(String[] args) {
        Gl_330_blend_rtt gl_330_blend_rtt = new Gl_330_blend_rtt();
    }

    public Gl_330_blend_rtt() {
        super("gl-330-blend-rtt", Profile.CORE, 3, 3);
    }

    private final String SHADERS_SOURCE = "image-2d";
    private final String SHADERS_ROOT = "src/data/gl_330";
    private final String TEXTURE_DIFFUSE = "kueken7_rgba8_srgb.dds";

    // With DDS textures, v texture coordinate are reversed, from top to bottom
    private int vertexCount = 6;
    private int vertexSize = vertexCount * Vertex_v2fv2f.SIZE;
    private float[] vertexData = {
        -1.0f, -1.0f,/**/ 0.0f, 1.0f,
        +1.0f, -1.0f,/**/ 1.0f, 1.0f,
        +1.0f, +1.0f,/**/ 1.0f, 0.0f,
        +1.0f, +1.0f,/**/ 1.0f, 0.0f,
        -1.0f, +1.0f,/**/ 0.0f, 0.0f,
        -1.0f, -1.0f,/**/ 0.0f, 1.0f};

    private class Texture {

        public static final int RGB8 = 0;
        public static final int R = 1;
        public static final int G = 2;
        public static final int B = 3;
        public static final int MAX = 4;
    };

    private class Shader {

        public static final int VERT = 0;
        public static final int FRAG = 1;
        public static final int MAX = 2;
    }

    private IntBuffer framebufferName = GLBuffers.newDirectIntBuffer(1),
            vertexArrayName = GLBuffers.newDirectIntBuffer(1), bufferName = GLBuffers.newDirectIntBuffer(1),
            texture2dName = GLBuffers.newDirectIntBuffer(Texture.MAX), samplerName = GLBuffers.newDirectIntBuffer(1);
    private int programNameSingle, uniformMvpSingle, uniformDiffuseSingle;
    private Vec4i[] viewport = new Vec4i[Texture.MAX];

    @Override
    protected boolean begin(GL gl) {

        GL3 gl3 = (GL3) gl;

        viewport[Texture.RGB8] = new Vec4i(0, 0, windowSize.x >> 1, windowSize.y >> 1);
        viewport[Texture.R] = new Vec4i(windowSize.x >> 1, 0, windowSize.x >> 1, windowSize.y >> 1);
        viewport[Texture.G] = new Vec4i(windowSize.x >> 1, windowSize.y >> 1, windowSize.x >> 1, windowSize.y >> 1);
        viewport[Texture.B] = new Vec4i(0, windowSize.y >> 1, windowSize.x >> 1, windowSize.y >> 1);

        boolean validated = true;

        if (validated) {
            validated = initBlend(gl3);
        }
        if (validated) {
            validated = initProgram(gl3);
        }
        if (validated) {
            validated = initBuffer(gl3);
        }
        if (validated) {
            validated = initVertexArray(gl3);
        }
        if (validated) {
            validated = initSampler(gl3);
        }
        if (validated) {
            validated = initTexture(gl3);
        }
        if (validated) {
            validated = initFramebuffer(gl3);
        }

        return validated && checkError(gl3, "begin");
    }

    private boolean initProgram(GL3 gl3) {

        boolean validated = true;

        ShaderCode[] shaderCodes = new ShaderCode[Shader.MAX];

        shaderCodes[Shader.VERT] = ShaderCode.create(gl3, GL_VERTEX_SHADER, this.getClass(), SHADERS_ROOT, null,
                SHADERS_SOURCE, "vert", null, true);
        shaderCodes[Shader.FRAG] = ShaderCode.create(gl3, GL_FRAGMENT_SHADER, this.getClass(), SHADERS_ROOT, null,
                SHADERS_SOURCE, "frag", null, true);

        if (validated) {

            ShaderProgram shaderProgram = new ShaderProgram();

            shaderProgram.add(shaderCodes[Shader.VERT]);
            shaderProgram.add(shaderCodes[Shader.FRAG]);

            shaderProgram.init(gl3);

            programNameSingle = shaderProgram.program();

            shaderProgram.link(gl3, System.out);
        }

        if (validated) {

            uniformMvpSingle = gl3.glGetUniformLocation(programNameSingle, "mvp");
            uniformDiffuseSingle = gl3.glGetUniformLocation(programNameSingle, "diffuse");
        }

        return validated & checkError(gl3, "initProgram");
    }

    private boolean initBuffer(GL3 gl3) {

        FloatBuffer vertexBuffer = GLBuffers.newDirectFloatBuffer(vertexData);

        gl3.glGenBuffers(1, bufferName);

        gl3.glBindBuffer(GL_ARRAY_BUFFER, bufferName.get(0));
        gl3.glBufferData(GL_ARRAY_BUFFER, vertexSize, vertexBuffer, GL_STATIC_DRAW);
        gl3.glBindBuffer(GL_ARRAY_BUFFER, 0);

        BufferUtils.destroyDirectBuffer(vertexBuffer);

        return checkError(gl3, "initBuffer");
    }

    private boolean initSampler(GL3 gl3) {

        FloatBuffer borderColor = GLBuffers.newDirectFloatBuffer(new float[]{0.0f, 0.0f, 0.0f, 0.0f});

        gl3.glGenSamplers(1, samplerName);
        gl3.glSamplerParameteri(samplerName.get(0), GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        gl3.glSamplerParameteri(samplerName.get(0), GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        gl3.glSamplerParameteri(samplerName.get(0), GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        gl3.glSamplerParameteri(samplerName.get(0), GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
        gl3.glSamplerParameteri(samplerName.get(0), GL_TEXTURE_WRAP_R, GL_CLAMP_TO_EDGE);
        gl3.glSamplerParameterfv(samplerName.get(0), GL_TEXTURE_BORDER_COLOR, borderColor);
        gl3.glSamplerParameterf(samplerName.get(0), GL_TEXTURE_MIN_LOD, -1000.f);
        gl3.glSamplerParameterf(samplerName.get(0), GL_TEXTURE_MAX_LOD, 1000.f);
        gl3.glSamplerParameterf(samplerName.get(0), GL_TEXTURE_LOD_BIAS, 0.0f);
        gl3.glSamplerParameteri(samplerName.get(0), GL_TEXTURE_COMPARE_MODE, GL_NONE);
        gl3.glSamplerParameteri(samplerName.get(0), GL_TEXTURE_COMPARE_FUNC, GL_LEQUAL);

        BufferUtils.destroyDirectBuffer(borderColor);

        return checkError(gl3, "initSampler");
    }

    private boolean initTexture(GL3 gl3) {

        try {
            jgli.Texture2d texture = new Texture2d(jgli.Load.load(TEXTURE_ROOT + "/" + TEXTURE_DIFFUSE));
            jgli.Gl.Format format = jgli.Gl.translate(texture.format());

            gl3.glActiveTexture(GL_TEXTURE0);
            gl3.glGenTextures(Texture.MAX, texture2dName);

            gl3.glBindTexture(GL_TEXTURE_2D, texture2dName.get(Texture.RGB8));
            gl3.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
            gl3.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
            gl3.glTexImage2D(GL_TEXTURE_2D, 0,
                    format.internal.value,
                    texture.dimensions()[0], texture.dimensions()[1],
                    0,
                    format.external.value, format.type.value,
                    texture.data());

            gl3.glBindTexture(GL_TEXTURE_2D, texture2dName.get(Texture.R));
            gl3.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_SWIZZLE_R, GL_RED);
            gl3.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_SWIZZLE_G, GL_ZERO);
            gl3.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_SWIZZLE_B, GL_ZERO);
            gl3.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_SWIZZLE_A, GL_ZERO);

            gl3.glBindTexture(GL_TEXTURE_2D, texture2dName.get(Texture.G));
            gl3.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_SWIZZLE_R, GL_ZERO);
            gl3.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_SWIZZLE_G, GL_RED);
            gl3.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_SWIZZLE_B, GL_ZERO);
            gl3.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_SWIZZLE_A, GL_ZERO);

            gl3.glBindTexture(GL_TEXTURE_2D, texture2dName.get(Texture.B));
            gl3.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_SWIZZLE_R, GL_ZERO);
            gl3.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_SWIZZLE_G, GL_ZERO);
            gl3.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_SWIZZLE_B, GL_RED);
            gl3.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_SWIZZLE_A, GL_ZERO);

            for (int i = Texture.R; i <= Texture.B; ++i) {
                gl3.glBindTexture(GL_TEXTURE_2D, texture2dName.get(i));
                gl3.glTexImage2D(GL_TEXTURE_2D, 0,
                        GL_R8,
                        texture.dimensions()[0], texture.dimensions()[1],
                        0,
                        GL_RGB, GL_UNSIGNED_BYTE,
                        null);
            }

        } catch (IOException ex) {
            Logger.getLogger(Gl_330_blend_rtt.class.getName()).log(Level.SEVERE, null, ex);
        }
        return checkError(gl3, "initTexture");
    }

    private boolean initFramebuffer(GL3 gl3) {

        gl3.glGenFramebuffers(1, framebufferName);
        gl3.glBindFramebuffer(GL_FRAMEBUFFER, framebufferName.get(0));

        for (int i = Texture.R; i <= Texture.B; ++i) {
            gl3.glFramebufferTexture(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0 + (i - Texture.R), texture2dName.get(i), 0);
        }

        int[] drawBuffers = new int[3];
        drawBuffers[0] = GL_COLOR_ATTACHMENT0;
        drawBuffers[1] = GL_COLOR_ATTACHMENT1;
        drawBuffers[2] = GL_COLOR_ATTACHMENT2;

        gl3.glDrawBuffers(3, drawBuffers, 0);

        if (!isFramebufferComplete(gl3, framebufferName.get(0))) {
            return false;
        }

        return checkError(gl3, "initFramebuffer");
    }

    private boolean initVertexArray(GL3 gl3) {

        gl3.glGenVertexArrays(1, vertexArrayName);
        gl3.glBindVertexArray(vertexArrayName.get(0));
        {
            gl3.glBindBuffer(GL_ARRAY_BUFFER, bufferName.get(0));
            gl3.glVertexAttribPointer(Semantic.Attr.POSITION, 2, GL_FLOAT, false, Vertex_v2fv2f.SIZE, 0);
            gl3.glVertexAttribPointer(Semantic.Attr.TEXCOORD, 2, GL_FLOAT, false, Vertex_v2fv2f.SIZE, Vec2.SIZE);
            gl3.glBindBuffer(GL_ARRAY_BUFFER, 0);

            gl3.glEnableVertexAttribArray(Semantic.Attr.POSITION);
            gl3.glEnableVertexAttribArray(Semantic.Attr.TEXCOORD);
        }
        gl3.glBindVertexArray(0);

        return checkError(gl3, "initVertexArray");
    }

    private boolean initBlend(GL3 gl3) {

        gl3.glBlendEquationSeparate(GL_FUNC_ADD, GL_FUNC_ADD);
        gl3.glBlendFuncSeparate(GL_ONE, GL_ONE, GL_ONE, GL_ONE);

        gl3.glEnablei(GL_BLEND, 0);
        gl3.glEnablei(GL_BLEND, 1);
        gl3.glEnablei(GL_BLEND, 2);
        gl3.glEnablei(GL_BLEND, 3);

        gl3.glColorMaski(0, true, true, true, false);
        gl3.glColorMaski(1, true, false, false, false);
        gl3.glColorMaski(2, true, false, false, false);
        gl3.glColorMaski(3, true, false, false, false);

        return checkError(gl3, "initBlend");
    }

    @Override
    protected boolean render(GL gl) {

        GL3 gl3 = (GL3) gl;

        // Pass 1
        {
            gl3.glBindFramebuffer(GL_FRAMEBUFFER, framebufferName.get(0));
            gl3.glViewport(0, 0, windowSize.x >> 1, windowSize.y >> 1);
            gl3.glClearBufferfv(GL_COLOR, 0, clearColor.put(0, 1).put(1, 1).put(2, 1).put(3, 1));
            gl3.glClearBufferfv(GL_COLOR, 1, clearColor);
            gl3.glClearBufferfv(GL_COLOR, 2, clearColor);
            gl3.glClearBufferfv(GL_COLOR, 3, clearColor);
        }

        // Pass 2
        gl3.glBindFramebuffer(GL_FRAMEBUFFER, 0);
        gl3.glClearBufferfv(GL_COLOR, 0, clearColor.put(0, 0).put(1, 0).put(2, 0).put(3, 0));

        gl3.glUseProgram(programNameSingle);

        {
            Mat4 projection = glm.ortho_(-2.0f, 2.0f, -1.5f, 1.5f, -1.0f, 1.0f);
            Mat4 view = new Mat4(1.0f);
            Mat4 model = new Mat4(0.2f);
            Mat4 mvp = projection.mul(view).mul(model);

            gl3.glUniformMatrix4fv(uniformMvpSingle, 1, false, mvp.toFa_(), 0);
            gl3.glUniform1i(uniformDiffuseSingle, 0);
        }

        for (int i = 0; i < Texture.MAX; ++i) {

            gl3.glViewport(viewport[i].x, viewport[i].y, viewport[i].z, viewport[i].w);

            gl3.glActiveTexture(GL_TEXTURE0);
            gl3.glBindTexture(GL_TEXTURE_2D, texture2dName.get(i));
            gl3.glBindSampler(0, samplerName.get(0));
            gl3.glBindVertexArray(vertexArrayName.get(0));

            gl3.glDrawArraysInstanced(GL_TRIANGLES, 0, vertexCount, 1);
        }

        return true;
    }

    @Override
    protected boolean end(GL gl) {

        GL3 gl3 = (GL3) gl;

        gl3.glDeleteBuffers(1, bufferName);
        gl3.glDeleteProgram(programNameSingle);
        gl3.glDeleteTextures(Texture.MAX, texture2dName);
        gl3.glDeleteFramebuffers(1, framebufferName);
        gl3.glDeleteSamplers(1, samplerName);

        BufferUtils.destroyDirectBuffer(bufferName);
        BufferUtils.destroyDirectBuffer(texture2dName);
        BufferUtils.destroyDirectBuffer(framebufferName);
        BufferUtils.destroyDirectBuffer(samplerName);
        
        return checkError(gl3, "end");
    }
}
