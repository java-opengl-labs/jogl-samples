/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tests.gl_400;

import com.jogamp.opengl.GL;
import static com.jogamp.opengl.GL2.GL_COMPARE_R_TO_TEXTURE;
import static com.jogamp.opengl.GL2ES3.*;
import com.jogamp.opengl.GL4;
import com.jogamp.opengl.util.GLBuffers;
import com.jogamp.opengl.util.glsl.ShaderCode;
import com.jogamp.opengl.util.glsl.ShaderProgram;
import dev.Vec4u8;
import framework.BufferUtils;
import glm.glm;
import glm.mat._4.Mat4;
import framework.Profile;
import framework.Semantic;
import framework.Test;
import glf.Vertex_v3fv4u8;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ShortBuffer;
import java.util.logging.Level;
import java.util.logging.Logger;
import jgli.Texture2d;
import glm.vec._2.Vec2;
import glm.vec._3.Vec3;
import java.nio.IntBuffer;
import jglm.Vec2i;

/**
 *
 * @author GBarbieri
 */
public class Gl_400_fbo_shadow extends Test {

    public static void main(String[] args) {
        Gl_400_fbo_shadow gl_400_fbo_shadow = new Gl_400_fbo_shadow();
    }

    public Gl_400_fbo_shadow() {
        super("gl-400-fbo-shadow", Profile.CORE, 4, 0, new Vec2(0.0f, -(float) Math.PI * 0.3f));
    }

    private final String SHADERS_SOURCE_DEPTH = "fbo-shadow-depth";
    private final String SHADERS_SOURCE_RENDER = "fbo-shadow-render";
    private final String SHADERS_ROOT = "src/data/gl_400";
    private final String TEXTURE_DIFFUSE = "kueken7_rgb_dxt1_unorm.dds";

    private int vertexCount = 8;
    private int vertexSize = vertexCount * Vertex_v3fv4u8.SIZE;
    private Vertex_v3fv4u8[] vertexData = {
        new Vertex_v3fv4u8(new Vec3(-1.0f, -1.0f, 0.0f), new Vec4u8(255, 127, 0, 255)),
        new Vertex_v3fv4u8(new Vec3(+1.0f, -1.0f, 0.0f), new Vec4u8(255, 127, 0, 255)),
        new Vertex_v3fv4u8(new Vec3(+1.0f, +1.0f, 0.0f), new Vec4u8(255, 127, 0, 255)),
        new Vertex_v3fv4u8(new Vec3(-1.0f, +1.0f, 0.0f), new Vec4u8(255, 127, 0, 255)),
        new Vertex_v3fv4u8(new Vec3(-0.1f, -0.1f, 0.2f), new Vec4u8(0, 127, 255, 255)),
        new Vertex_v3fv4u8(new Vec3(+0.1f, -0.1f, 0.2f), new Vec4u8(0, 127, 255, 255)),
        new Vertex_v3fv4u8(new Vec3(+0.1f, +0.1f, 0.2f), new Vec4u8(0, 127, 255, 255)),
        new Vertex_v3fv4u8(new Vec3(-0.1f, +0.1f, 0.2f), new Vec4u8(0, 127, 255, 255))};

    private int elementCount = 12;
    private int elementSize = elementCount * Short.BYTES;
    private short[] elementData = {
        0, 1, 2,
        2, 3, 0,
        4, 5, 6,
        6, 7, 4};

    private class Buffer {

        public static final int VERTEX = 0;
        public static final int ELEMENT = 1;
        public static final int TRANSFORM = 2;
        public static final int MAX = 3;
    }

    private class Texture {

        public static final int DIFFUSE = 0;
        public static final int COLORBUFFER = 1;
        public static final int RENDERBUFFER = 2;
        public static final int SHADOWMAP = 3;
        public static final int MAX = 4;
    }

    private class Program {

        public static final int DEPTH = 0;
        public static final int RENDER = 1;
        public static final int MAX = 2;
    }

    private class Framebuffer {

        public static final int FRAMEBUFFER = 0;
        public static final int SHADOW = 1;
        public static final int MAX = 2;
    }

    private IntBuffer framebufferName = GLBuffers.newDirectIntBuffer(Framebuffer.MAX),
            vertexArrayName = GLBuffers.newDirectIntBuffer(Program.MAX),
            bufferName = GLBuffers.newDirectIntBuffer(Buffer.MAX),
            textureName = GLBuffers.newDirectIntBuffer(Texture.MAX);
    private int[] programName = new int[Program.MAX], uniformTransform = new int[Program.MAX];
    private int uniformShadow;
    private Vec2i shadowSize = new Vec2i(64, 64);

    @Override
    protected boolean begin(GL gl) {

        GL4 gl4 = (GL4) gl;

        boolean validated = true;
        validated = validated && gl4.isExtensionAvailable("GL_ARB_ES2_compatibility");

        if (validated) {
            validated = initProgram(gl4);
        }
        if (validated) {
            validated = initBuffer(gl4);
        }
        if (validated) {
            validated = initVertexArray(gl4);
        }
        if (validated) {
            validated = initTexture(gl4);
        }
        if (validated) {
            validated = initFramebuffer(gl4);
        }

        return validated && checkError(gl4, "begin");
    }

    private boolean initProgram(GL4 gl4) {

        boolean validated = true;

        if (validated) {

            ShaderProgram shaderProgram = new ShaderProgram();

            ShaderCode vertShaderCode = ShaderCode.create(gl4, GL_VERTEX_SHADER, this.getClass(), SHADERS_ROOT, null,
                    SHADERS_SOURCE_RENDER, "vert", null, true);
            ShaderCode fragShaderCode = ShaderCode.create(gl4, GL_FRAGMENT_SHADER, this.getClass(), SHADERS_ROOT, null,
                    SHADERS_SOURCE_RENDER, "frag", null, true);

            shaderProgram.init(gl4);

            shaderProgram.add(vertShaderCode);
            shaderProgram.add(fragShaderCode);

            programName[Program.RENDER] = shaderProgram.program();

            gl4.glBindAttribLocation(programName[Program.RENDER], Semantic.Attr.POSITION, "position");
            gl4.glBindAttribLocation(programName[Program.RENDER], Semantic.Attr.COLOR, "color");
            gl4.glBindFragDataLocation(programName[Program.RENDER], Semantic.Frag.COLOR, "color");

            shaderProgram.link(gl4, System.out);
        }

        if (validated) {

            uniformTransform[Program.RENDER] = gl4.glGetUniformBlockIndex(programName[Program.RENDER], "Transform");
            uniformShadow = gl4.glGetUniformLocation(programName[Program.RENDER], "shadow");
        }

        if (validated) {

            ShaderProgram shaderProgram = new ShaderProgram();

            ShaderCode vertShaderCode = ShaderCode.create(gl4, GL_VERTEX_SHADER, this.getClass(), SHADERS_ROOT, null,
                    SHADERS_SOURCE_DEPTH, "vert", null, true);

            shaderProgram.init(gl4);

            shaderProgram.add(vertShaderCode);

            programName[Program.DEPTH] = shaderProgram.program();

            gl4.glBindAttribLocation(programName[Program.DEPTH], Semantic.Attr.POSITION, "position");

            shaderProgram.link(gl4, System.out);
        }

        if (validated) {

            uniformTransform[Program.DEPTH] = gl4.glGetUniformBlockIndex(programName[Program.DEPTH], "Transform");
        }

        return validated & checkError(gl4, "initProgram");
    }

    private boolean initBuffer(GL4 gl4) {

        ShortBuffer elementBuffer = GLBuffers.newDirectShortBuffer(elementData);
        ByteBuffer vertexBuffer = GLBuffers.newDirectByteBuffer(vertexSize);
        IntBuffer uniformBufferOffset = GLBuffers.newDirectIntBuffer(1);

        gl4.glGenBuffers(Buffer.MAX, bufferName);

        gl4.glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, bufferName.get(Buffer.ELEMENT));
        gl4.glBufferData(GL_ELEMENT_ARRAY_BUFFER, elementSize, elementBuffer, GL_STATIC_DRAW);
        gl4.glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, 0);

        gl4.glBindBuffer(GL_ARRAY_BUFFER, bufferName.get(Buffer.VERTEX));
        for (int i = 0; i < vertexCount; i++) {
            vertexData[i].toBb(vertexBuffer, i);
        }
        vertexBuffer.rewind();
        gl4.glBufferData(GL_ARRAY_BUFFER, vertexSize, vertexBuffer, GL_STATIC_DRAW);
        gl4.glBindBuffer(GL_ARRAY_BUFFER, 0);

        gl4.glGetIntegerv(GL_UNIFORM_BUFFER_OFFSET_ALIGNMENT, uniformBufferOffset);

        int uniformBlockSize = Math.max(Mat4.SIZE * 3, uniformBufferOffset.get(0));

        gl4.glBindBuffer(GL_UNIFORM_BUFFER, bufferName.get(Buffer.TRANSFORM));
        gl4.glBufferData(GL_UNIFORM_BUFFER, uniformBlockSize, null, GL_DYNAMIC_DRAW);
        gl4.glBindBuffer(GL_UNIFORM_BUFFER, 0);

        BufferUtils.destroyDirectBuffer(elementBuffer);
        BufferUtils.destroyDirectBuffer(vertexBuffer);
        BufferUtils.destroyDirectBuffer(uniformBufferOffset);

        return checkError(gl4, "initBuffer");
    }

    private boolean initTexture(GL4 gl4) {

        try {
            jgli.Texture2d texture = new Texture2d(jgli.Load.load(TEXTURE_ROOT + "/" + TEXTURE_DIFFUSE));
            assert (!texture.empty());

            gl4.glPixelStorei(GL_UNPACK_ALIGNMENT, 1);

            gl4.glGenTextures(Texture.MAX, textureName);

            gl4.glActiveTexture(GL_TEXTURE0);
            gl4.glBindTexture(GL_TEXTURE_2D, textureName.get(Texture.DIFFUSE));
            gl4.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_BASE_LEVEL, 0);
            gl4.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAX_LEVEL, texture.levels() - 1);
            gl4.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST_MIPMAP_NEAREST);
            gl4.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
            gl4.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
            gl4.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);

            for (int level = 0; level < texture.levels(); ++level) {
                gl4.glCompressedTexImage2D(
                        GL_TEXTURE_2D,
                        level,
                        GL_COMPRESSED_RGB_S3TC_DXT1_EXT,
                        texture.dimensions(level)[0],
                        texture.dimensions(level)[1],
                        0,
                        texture.size(level),
                        texture.data(level));
            }

            gl4.glActiveTexture(GL_TEXTURE0);
            gl4.glBindTexture(GL_TEXTURE_2D, textureName.get(Texture.COLORBUFFER));
            gl4.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_BASE_LEVEL, 0);
            gl4.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAX_LEVEL, 0);
            gl4.glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA8, windowSize.x, windowSize.y, 0, GL_RGBA, GL_UNSIGNED_BYTE, null);

            gl4.glActiveTexture(GL_TEXTURE0);
            gl4.glBindTexture(GL_TEXTURE_2D, textureName.get(Texture.RENDERBUFFER));
            gl4.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_BASE_LEVEL, 0);
            gl4.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAX_LEVEL, 0);
            gl4.glTexImage2D(GL_TEXTURE_2D, 0, GL_DEPTH_COMPONENT24, windowSize.x, windowSize.y, 0, GL_DEPTH_COMPONENT,
                    GL_FLOAT, null);

            gl4.glActiveTexture(GL_TEXTURE0);
            gl4.glBindTexture(GL_TEXTURE_2D, textureName.get(Texture.SHADOWMAP));
            gl4.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_BASE_LEVEL, 0);
            gl4.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAX_LEVEL, 0);
            gl4.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
            gl4.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
            gl4.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
            gl4.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
            gl4.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_COMPARE_FUNC, GL_LEQUAL);
            gl4.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_COMPARE_MODE, GL_COMPARE_R_TO_TEXTURE);
            gl4.glTexImage2D(GL_TEXTURE_2D, 0, GL_DEPTH_COMPONENT24, shadowSize.x, shadowSize.y, 0, GL_DEPTH_COMPONENT,
                    GL_FLOAT, null);

            gl4.glPixelStorei(GL_UNPACK_ALIGNMENT, 4);

        } catch (IOException ex) {
            Logger.getLogger(Gl_400_fbo_shadow.class.getName()).log(Level.SEVERE, null, ex);
        }
        return checkError(gl4, "initTexture");
    }

    private boolean initVertexArray(GL4 gl4) {

        gl4.glGenVertexArrays(Program.MAX, vertexArrayName);
        gl4.glBindVertexArray(vertexArrayName.get(Program.RENDER));
        {
            gl4.glBindBuffer(GL_ARRAY_BUFFER, bufferName.get(Buffer.VERTEX));
            gl4.glVertexAttribPointer(Semantic.Attr.POSITION, 3, GL_FLOAT, false, Vertex_v3fv4u8.SIZE, 0);
            gl4.glVertexAttribPointer(Semantic.Attr.COLOR, 4, GL_UNSIGNED_BYTE, true, Vertex_v3fv4u8.SIZE, Vec3.SIZE);
            gl4.glBindBuffer(GL_ARRAY_BUFFER, 0);

            gl4.glEnableVertexAttribArray(Semantic.Attr.POSITION);
            gl4.glEnableVertexAttribArray(Semantic.Attr.COLOR);

            gl4.glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, bufferName.get(Buffer.ELEMENT));
        }
        gl4.glBindVertexArray(0);

        return true;
    }

    private boolean initFramebuffer(GL4 gl4) {

        boolean validated = true;

        IntBuffer buffersRender = GLBuffers.newDirectIntBuffer(new int[]{GL_COLOR_ATTACHMENT0});

        gl4.glGenFramebuffers(Framebuffer.MAX, framebufferName);

        gl4.glBindFramebuffer(GL_FRAMEBUFFER, framebufferName.get(Framebuffer.FRAMEBUFFER));
        gl4.glFramebufferTexture(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, textureName.get(Texture.COLORBUFFER), 0);
        gl4.glFramebufferTexture(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, textureName.get(Texture.RENDERBUFFER), 0);
        gl4.glDrawBuffers(1, buffersRender);
        if (!isFramebufferComplete(gl4, framebufferName.get(Framebuffer.FRAMEBUFFER))) {
            return false;
        }

        gl4.glBindFramebuffer(GL_FRAMEBUFFER, framebufferName.get(Framebuffer.SHADOW));
        gl4.glFramebufferTexture(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, textureName.get(Texture.SHADOWMAP), 0);
        gl4.glDrawBuffer(GL_NONE);
        if (!isFramebufferComplete(gl4, framebufferName.get(Framebuffer.SHADOW))) {
            return false;
        }

        gl4.glBindFramebuffer(GL_FRAMEBUFFER, 0);
        gl4.glDrawBuffer(GL_BACK);
        if (!isFramebufferComplete(gl4, 0)) {
            return false;
        }

        BufferUtils.destroyDirectBuffer(buffersRender);

        return validated && checkError(gl4, "initFramebuffer");
    }

    @Override
    protected boolean render(GL gl) {

        GL4 gl4 = (GL4) gl;

        gl4.glBindBuffer(GL_UNIFORM_BUFFER, bufferName.get(Buffer.TRANSFORM));
        ByteBuffer pointer = gl4.glMapBufferRange(
                GL_UNIFORM_BUFFER, 0, Mat4.SIZE * 3,
                GL_MAP_WRITE_BIT | GL_MAP_INVALIDATE_BUFFER_BIT);

        // Update of the MVP matrix for the render pass
        {
            Mat4 projection = glm.perspective_((float) Math.PI * 0.25f, (float) windowSize.x / windowSize.y, 0.01f, 5.0f);
            Mat4 model = new Mat4(1.0f);
            
            projection.mul(viewMat4()).mul(model).toDbb(pointer);
        }

        // Update of the MVP matrix for the depth pass
        {
            Mat4 projection = glm.ortho_(-1.0f, 1.0f, -1.0f, 1.0f, -4.0f, 8.0f);
            Mat4 view = glm.lookAt_(new Vec3(0.5, 1.0, 2.0), new Vec3(0), new Vec3(0, 0, 1));
            Mat4 model = new Mat4(1.0f);
            Mat4 depthMVP = projection.mul(view).mul(model);

            depthMVP.toDbb(pointer, Mat4.SIZE);

            Mat4 biasMatrix = new Mat4(
                    0.5f, 0.0f, 0.0f, 0.0f,
                    0.0f, 0.5f, 0.0f, 0.0f,
                    0.0f, 0.0f, 0.5f, 0.0f,
                    0.5f, 0.5f, 0.5f, 1.0f);

            biasMatrix.mul(depthMVP).toDbb(pointer, Mat4.SIZE * 2);
        }
        gl4.glUnmapBuffer(GL_UNIFORM_BUFFER);

        gl4.glBindBufferBase(GL_UNIFORM_BUFFER, Semantic.Uniform.TRANSFORM0, bufferName.get(Buffer.TRANSFORM));

        renderShadow(gl4);
        renderFramebuffer(gl4);

        return checkError(gl4, "render");
    }

    private void renderShadow(GL4 gl4) {

        gl4.glEnable(GL_DEPTH_TEST);
        gl4.glDepthFunc(GL_LESS);

        gl4.glViewport(0, 0, shadowSize.x, shadowSize.y);

        gl4.glBindFramebuffer(GL_FRAMEBUFFER, framebufferName.get(Framebuffer.SHADOW));
        gl4.glClearBufferfv(GL_DEPTH, 0, clearDepth.put(0, 1));

        // Bind rendering objects
        gl4.glUseProgram(programName[Program.DEPTH]);
        gl4.glUniformBlockBinding(programName[Program.DEPTH], uniformTransform[Program.DEPTH],
                Semantic.Uniform.TRANSFORM0);

        gl4.glBindVertexArray(vertexArrayName.get(Program.RENDER));
        gl4.glDrawElementsInstancedBaseVertex(GL_TRIANGLES, elementCount, GL_UNSIGNED_SHORT, 0, 1, 0);

        gl4.glDisable(GL_DEPTH_TEST);

        checkError(gl4, "renderShadow");
    }

    private void renderFramebuffer(GL4 gl4) {

        gl4.glEnable(GL_DEPTH_TEST);
        gl4.glDepthFunc(GL_LESS);

        gl4.glViewport(0, 0, windowSize.x, windowSize.y);

        gl4.glBindFramebuffer(GL_FRAMEBUFFER, 0);
        gl4.glClearBufferfv(GL_DEPTH, 0, clearDepth);
        gl4.glClearBufferfv(GL_COLOR, 0, clearColor.put(0, 0).put(1, 0).put(2, 0).put(3, 1));

        gl4.glUseProgram(programName[Program.RENDER]);
        gl4.glUniform1i(uniformShadow, 0);
        gl4.glUniformBlockBinding(programName[Program.RENDER], uniformTransform[Program.RENDER],
                Semantic.Uniform.TRANSFORM0);

        gl4.glActiveTexture(GL_TEXTURE0);
        gl4.glBindTexture(GL_TEXTURE_2D, textureName.get(Texture.SHADOWMAP));

        gl4.glBindVertexArray(vertexArrayName.get(Program.RENDER));
        gl4.glDrawElementsInstancedBaseVertex(GL_TRIANGLES, elementCount, GL_UNSIGNED_SHORT, 0, 1, 0);

        gl4.glDisable(GL_DEPTH_TEST);

        checkError(gl4, "renderFramebuffer");
    }

    @Override
    protected boolean end(GL gl) {

        GL4 gl4 = (GL4) gl;

        gl4.glDeleteFramebuffers(Framebuffer.MAX, framebufferName);
        for (int i = 0; i < Program.MAX; ++i) {
            gl4.glDeleteProgram(programName[i]);
        }
        gl4.glDeleteBuffers(Buffer.MAX, bufferName);
        gl4.glDeleteTextures(Texture.MAX, textureName);
        gl4.glDeleteVertexArrays(Program.MAX, vertexArrayName);

        BufferUtils.destroyDirectBuffer(framebufferName);
        BufferUtils.destroyDirectBuffer(bufferName);
        BufferUtils.destroyDirectBuffer(textureName);
        BufferUtils.destroyDirectBuffer(vertexArrayName);

        return checkError(gl4, "end");
    }
}
