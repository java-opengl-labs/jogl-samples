/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tests.gl320;

import com.jogamp.opengl.GL;
import static com.jogamp.opengl.GL.GL_ARRAY_BUFFER;
import static com.jogamp.opengl.GL.GL_BLEND;
import static com.jogamp.opengl.GL.GL_CLAMP_TO_EDGE;
import static com.jogamp.opengl.GL.GL_COLOR_ATTACHMENT0;
import static com.jogamp.opengl.GL.GL_DRAW_FRAMEBUFFER;
import static com.jogamp.opengl.GL.GL_DYNAMIC_DRAW;
import static com.jogamp.opengl.GL.GL_ELEMENT_ARRAY_BUFFER;
import static com.jogamp.opengl.GL.GL_FLOAT;
import static com.jogamp.opengl.GL.GL_FRAMEBUFFER;
import static com.jogamp.opengl.GL.GL_FRAMEBUFFER_SRGB;
import static com.jogamp.opengl.GL.GL_LINEAR;
import static com.jogamp.opengl.GL.GL_LINEAR_MIPMAP_LINEAR;
import static com.jogamp.opengl.GL.GL_MAP_INVALIDATE_BUFFER_BIT;
import static com.jogamp.opengl.GL.GL_MAP_WRITE_BIT;
import static com.jogamp.opengl.GL.GL_ONE_MINUS_SRC_ALPHA;
import static com.jogamp.opengl.GL.GL_RGBA;
import static com.jogamp.opengl.GL.GL_SRC_ALPHA;
import static com.jogamp.opengl.GL.GL_SRGB;
import static com.jogamp.opengl.GL.GL_SRGB8_ALPHA8;
import static com.jogamp.opengl.GL.GL_STATIC_DRAW;
import static com.jogamp.opengl.GL.GL_TEXTURE0;
import static com.jogamp.opengl.GL.GL_TEXTURE_2D;
import static com.jogamp.opengl.GL.GL_TEXTURE_MAG_FILTER;
import static com.jogamp.opengl.GL.GL_TEXTURE_MIN_FILTER;
import static com.jogamp.opengl.GL.GL_TEXTURE_WRAP_S;
import static com.jogamp.opengl.GL.GL_TEXTURE_WRAP_T;
import static com.jogamp.opengl.GL.GL_TRIANGLES;
import static com.jogamp.opengl.GL.GL_UNPACK_ALIGNMENT;
import static com.jogamp.opengl.GL.GL_UNSIGNED_BYTE;
import static com.jogamp.opengl.GL.GL_UNSIGNED_SHORT;
import static com.jogamp.opengl.GL2ES2.GL_FRAGMENT_SHADER;
import static com.jogamp.opengl.GL2ES2.GL_FRAMEBUFFER_ATTACHMENT_COLOR_ENCODING;
import static com.jogamp.opengl.GL2ES2.GL_VERTEX_SHADER;
import static com.jogamp.opengl.GL2ES3.GL_COLOR;
import static com.jogamp.opengl.GL2ES3.GL_TEXTURE_BASE_LEVEL;
import static com.jogamp.opengl.GL2ES3.GL_TEXTURE_MAX_LEVEL;
import static com.jogamp.opengl.GL2ES3.GL_UNIFORM_BUFFER;
import static com.jogamp.opengl.GL2ES3.GL_UNIFORM_BUFFER_OFFSET_ALIGNMENT;
import static com.jogamp.opengl.GL2GL3.GL_BACK_LEFT;
import com.jogamp.opengl.GL3;
import com.jogamp.opengl.math.FloatUtil;
import com.jogamp.opengl.util.GLBuffers;
import com.jogamp.opengl.util.glsl.ShaderCode;
import com.jogamp.opengl.util.glsl.ShaderProgram;
import framework.Semantic;
import framework.Test;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author GBarbieri
 */
public class Gl_320_fbo_srgb_blend extends Test {

    public static void main(String[] args) {
        Gl_320_fbo_srgb_blend gl_320_fbo_srgb_blend = new Gl_320_fbo_srgb_blend();
    }

    public Gl_320_fbo_srgb_blend() {
        super("Gl-320-fbo-srgb-blend", 3, 2);
    }

    private final String SHADER_SOURCE_TEXTURE = "fbo-srgb-blend";
    private final String SHADER_SOURCE_SPLASH = "fbo-srgb-blit-blend";
    private final String SHADERS_ROOT = "src/data/gl_320";
    private final String TEXTURE_DIFFUSE = "kueken7_rgba8_srgb.dds";

    private int vertexCount = 4;
    private int vertexSize = vertexCount * 2 * 2 * Float.BYTES;
    private float[] VertexData = {
        -10.0f, -10.0f, 0.0f, 1.0f,
        +10.0f, -10.0f, 1.0f, 1.0f,
        +10.0f, 10.0f, 1.0f, 0.0f,
        -10.0f, 10.0f, 0.0f, 0.0f};

    private int elementCount = 6;
    private int elementSize = elementCount * Short.BYTES;
    private short[] elementData = {
        0, 1, 2,
        2, 3, 0};

    private enum Buffer {
        VERTEX,
        ELEMENT,
        TRANSFORM,
        MAX
    }

    private enum Texture {
        DIFFUSE,
        COLORBUFFER,
        RENDERBUFFER,
        MAX
    }

    private enum Program {
        TEXTURE,
        SPLASH,
        MAX
    }

    private enum Shader {
        VERT_TEXTURE,
        FRAG_TEXTURE,
        VERT_SPLASH,
        FRAG_SPLASH,
        MAX
    }

    private int[] programName = new int[Program.MAX.ordinal()], vertexArrayName = new int[Program.MAX.ordinal()],
            bufferName = new int[Buffer.MAX.ordinal()], textureName = new int[Texture.MAX.ordinal()],
            uniformDiffuse = new int[Program.MAX.ordinal()], framebufferName = new int[1];
    private int framebufferScale = 2, uniformTransform;
    private float[] projection = new float[16];

    @Override
    protected boolean begin(GL gl) {

        GL3 gl3 = (GL3) gl;

        boolean validated = true;

        // Explicitly convert linear pixel color to sRGB color space, as FramebufferName is a sRGB FBO
        // Shader execution is done with linear color to get correct linear algebra working.
        gl3.glEnable(GL_FRAMEBUFFER_SRGB);

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
            validated = initTexture(gl3);
        }
        if (validated) {
            validated = initFramebuffer(gl3);
        }

        return validated;
    }

    private boolean initProgram(GL3 gl3) {

        boolean validated = true;

        ShaderCode[] shaderCodes = new ShaderCode[Shader.MAX.ordinal()];

        if (validated) {

            shaderCodes[Shader.VERT_TEXTURE.ordinal()] = ShaderCode.create(gl3, GL_VERTEX_SHADER,
                    this.getClass(), SHADERS_ROOT, null, SHADER_SOURCE_TEXTURE, "vert", null, true);
            shaderCodes[Shader.FRAG_TEXTURE.ordinal()] = ShaderCode.create(gl3, GL_FRAGMENT_SHADER,
                    this.getClass(), SHADERS_ROOT, null, SHADER_SOURCE_TEXTURE, "frag", null, true);

            ShaderProgram program = new ShaderProgram();
            programName[Program.TEXTURE.ordinal()] = program.program();

            program.add(shaderCodes[Shader.VERT_TEXTURE.ordinal()]);
            program.add(shaderCodes[Shader.FRAG_TEXTURE.ordinal()]);

            program.init(gl3);

            programName[Program.TEXTURE.ordinal()] = program.program();

            gl3.glBindAttribLocation(programName[Program.TEXTURE.ordinal()], Semantic.Attr.POSITION, "position");
            gl3.glBindAttribLocation(programName[Program.TEXTURE.ordinal()], Semantic.Attr.TEXCOORD, "texCoord");
            gl3.glBindFragDataLocation(programName[Program.TEXTURE.ordinal()], Semantic.Frag.COLOR, "color");

            program.link(gl3, System.out);
        }

        if (validated) {

            shaderCodes[Shader.VERT_SPLASH.ordinal()] = ShaderCode.create(gl3, GL_VERTEX_SHADER,
                    this.getClass(), SHADERS_ROOT, null, SHADER_SOURCE_SPLASH, "vert", null, true);
            shaderCodes[Shader.FRAG_SPLASH.ordinal()] = ShaderCode.create(gl3, GL_FRAGMENT_SHADER,
                    this.getClass(), SHADERS_ROOT, null, SHADER_SOURCE_SPLASH, "frag", null, true);

            ShaderProgram program = new ShaderProgram();
            programName[Program.SPLASH.ordinal()] = program.program();

            program.add(shaderCodes[Shader.VERT_SPLASH.ordinal()]);
            program.add(shaderCodes[Shader.FRAG_SPLASH.ordinal()]);

            program.init(gl3);

            programName[Program.SPLASH.ordinal()] = program.program();

            gl3.glBindFragDataLocation(programName[Program.SPLASH.ordinal()], Semantic.Frag.COLOR, "color");

            program.link(gl3, System.out);
        }

        if (validated) {

            uniformTransform = gl3.glGetUniformBlockIndex(programName[Program.TEXTURE.ordinal()], "Transform");
            uniformDiffuse[Program.TEXTURE.ordinal()]
                    = gl3.glGetUniformLocation(programName[Program.TEXTURE.ordinal()], "diffuse");
            uniformDiffuse[Program.SPLASH.ordinal()]
                    = gl3.glGetUniformLocation(programName[Program.SPLASH.ordinal()], "diffuse");

            gl3.glUseProgram(programName[Program.TEXTURE.ordinal()]);
            gl3.glUniform1i(uniformDiffuse[Program.TEXTURE.ordinal()], 0);
            gl3.glUniformBlockBinding(programName[Program.TEXTURE.ordinal()], uniformTransform,
                    Semantic.Uniform.TRANSFORM0);

            gl3.glUseProgram(programName[Program.SPLASH.ordinal()]);
            gl3.glUniform1i(uniformDiffuse[Program.SPLASH.ordinal()], 0);
        }

        return validated & checkError(gl3, "initProgram");
    }

    private boolean initBuffer(GL3 gl3) {

        gl3.glGenBuffers(Buffer.MAX.ordinal(), bufferName, 0);

        gl3.glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, bufferName[Buffer.ELEMENT.ordinal()]);
        ShortBuffer elementBuffer = GLBuffers.newDirectShortBuffer(elementData);
        gl3.glBufferData(GL_ELEMENT_ARRAY_BUFFER, elementSize, elementBuffer, GL_STATIC_DRAW);
        gl3.glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, 0);

        gl3.glBindBuffer(GL_ARRAY_BUFFER, bufferName[Buffer.VERTEX.ordinal()]);
        FloatBuffer vertexBuffer = GLBuffers.newDirectFloatBuffer(VertexData);
        gl3.glBufferData(GL_ARRAY_BUFFER, vertexSize, vertexBuffer, GL_STATIC_DRAW);
        gl3.glBindBuffer(GL_ARRAY_BUFFER, 0);

        int[] uniformBufferOffset = {0};
        gl3.glGetIntegerv(GL_UNIFORM_BUFFER_OFFSET_ALIGNMENT, uniformBufferOffset, 0);
        int uniformBlockSize = Math.max(16 * Float.BYTES, uniformBufferOffset[0]);

        gl3.glBindBuffer(GL_UNIFORM_BUFFER, bufferName[Buffer.TRANSFORM.ordinal()]);
        gl3.glBufferData(GL_UNIFORM_BUFFER, uniformBlockSize, null, GL_DYNAMIC_DRAW);
        gl3.glBindBuffer(GL_UNIFORM_BUFFER, 0);

        return true;
    }

    private boolean initTexture(GL3 gl3) {

        boolean validated = true;

        try {

            jgli.Texture texture = jgli.Load.load(TEXTURE_ROOT + "/" + TEXTURE_DIFFUSE);
            assert (!texture.empty());

            gl3.glPixelStorei(GL_UNPACK_ALIGNMENT, 1);

            gl3.glGenTextures(Texture.MAX.ordinal(), textureName, 0);

            gl3.glActiveTexture(GL_TEXTURE0);
            gl3.glBindTexture(GL_TEXTURE_2D, textureName[Texture.DIFFUSE.ordinal()]);
            gl3.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_BASE_LEVEL, 0);
            gl3.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAX_LEVEL, texture.levels() - 1);
            gl3.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR_MIPMAP_LINEAR);
            gl3.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
            gl3.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
            gl3.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);

            jgli.Gl.Format format = jgli.Gl.instance.translate(texture.format());
            for (int level = 0; level < texture.levels(); ++level) {
                gl3.glTexImage2D(GL_TEXTURE_2D, level,
                        format.internal.value,
                        texture.dimensions(level)[0], texture.dimensions(level)[1],
                        0,
                        format.external.value, format.type.value,
                        texture.data(0, 0, level));
            }

            gl3.glActiveTexture(GL_TEXTURE0);
            gl3.glBindTexture(GL_TEXTURE_2D, textureName[Texture.COLORBUFFER.ordinal()]);
            gl3.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_BASE_LEVEL, 0);
            gl3.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAX_LEVEL, 0);
            gl3.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
            gl3.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
            gl3.glTexImage2D(GL_TEXTURE_2D, 0, GL_SRGB8_ALPHA8, windowSize.x * framebufferScale,
                    windowSize.y * framebufferScale, 0, GL_RGBA, GL_UNSIGNED_BYTE, null);

            gl3.glPixelStorei(GL_UNPACK_ALIGNMENT, 4);

        } catch (IOException ex) {
            Logger.getLogger(Gl_320_fbo_srgb_blend.class.getName()).log(Level.SEVERE, null, ex);
        }
        return validated;
    }

    private boolean initVertexArray(GL3 gl3) {

        gl3.glGenVertexArrays(Program.MAX.ordinal(), vertexArrayName, 0);
        gl3.glBindVertexArray(vertexArrayName[Program.TEXTURE.ordinal()]);
        {
            gl3.glBindBuffer(GL_ARRAY_BUFFER, bufferName[Buffer.VERTEX.ordinal()]);
            gl3.glVertexAttribPointer(Semantic.Attr.POSITION, 2, GL_FLOAT, false, 2 * 2 * Float.BYTES, 0);
            gl3.glVertexAttribPointer(Semantic.Attr.TEXCOORD, 2, GL_FLOAT, false, 2 * 2 * Float.BYTES, 2 * Float.BYTES);
            gl3.glBindBuffer(GL_ARRAY_BUFFER, 0);

            gl3.glEnableVertexAttribArray(Semantic.Attr.POSITION);
            gl3.glEnableVertexAttribArray(Semantic.Attr.TEXCOORD);

            gl3.glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, bufferName[Buffer.ELEMENT.ordinal()]);
        }
        gl3.glBindVertexArray(0);

        gl3.glBindVertexArray(vertexArrayName[Program.SPLASH.ordinal()]);
        gl3.glBindVertexArray(0);

        return true;
    }

    private boolean initFramebuffer(GL3 gl3) {

        gl3.glGenFramebuffers(1, framebufferName, 0);
        gl3.glBindFramebuffer(GL_FRAMEBUFFER, framebufferName[0]);
        gl3.glFramebufferTexture(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, textureName[Texture.COLORBUFFER.ordinal()], 0);

        if (!isFramebufferComplete(gl3, framebufferName[0])) {
            return false;
        }
        int encodingLinear = GL_LINEAR;
        int encodingSRGB = GL_SRGB;

        gl3.glBindFramebuffer(GL_FRAMEBUFFER, 0);
        int[] encoding = {0};
        gl3.glGetFramebufferAttachmentParameteriv(GL_DRAW_FRAMEBUFFER, GL_BACK_LEFT,
                GL_FRAMEBUFFER_ATTACHMENT_COLOR_ENCODING, encoding, 0);
        return true;
    }

    @Override
    protected boolean render(GL gl) {

        GL3 gl3 = (GL3) gl;

        {
            gl3.glBindBuffer(GL_UNIFORM_BUFFER, bufferName[Buffer.TRANSFORM.ordinal()]);
            ByteBuffer pointer = gl3.glMapBufferRange(GL_UNIFORM_BUFFER,
                    0, 16 * Float.BYTES, GL_MAP_WRITE_BIT | GL_MAP_INVALIDATE_BUFFER_BIT);

            FloatUtil.makePerspective(projection, 0, true, (float) Math.PI * 0.25f,
                    (float) windowSize.x / windowSize.y, 0.1f, 100.0f);
            FloatUtil.multMatrix(projection, view());

            for (float f : projection) {
                pointer.putFloat(f);
            }

            // Make sure the uniform buffer is uploaded
            gl3.glUnmapBuffer(GL_UNIFORM_BUFFER);
        }

        // Render a textured quad to a sRGB framebuffer object.
        {
            gl3.glViewport(0, 0, windowSize.x * framebufferScale, windowSize.y * framebufferScale);

            gl3.glEnable(GL_BLEND);
            gl3.glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

            gl3.glBindFramebuffer(GL_FRAMEBUFFER, framebufferName[0]);
            gl3.glEnable(GL_FRAMEBUFFER_SRGB);

            gl3.glClearBufferfv(GL_COLOR, 0, new float[]{0.0f, 0.0f, 0.0f, 1.0f}, 0);

            gl3.glUseProgram(programName[Program.TEXTURE.ordinal()]);

            gl3.glActiveTexture(GL_TEXTURE0);
            gl3.glBindTexture(GL_TEXTURE_2D, textureName[Texture.DIFFUSE.ordinal()]);
            gl3.glBindVertexArray(vertexArrayName[Program.TEXTURE.ordinal()]);
            gl3.glBindBufferBase(GL_UNIFORM_BUFFER, Semantic.Uniform.TRANSFORM0, bufferName[Buffer.TRANSFORM.ordinal()]);

            gl3.glDrawElementsInstancedBaseVertex(GL_TRIANGLES, elementCount, GL_UNSIGNED_SHORT, 0, 100, 0);

            gl3.glDisable(GL_BLEND);
        }

        // Blit the sRGB framebuffer to the default framebuffer back buffer.
        {
            gl3.glViewport(0, 0, windowSize.x, windowSize.y);

            gl3.glBindFramebuffer(GL_FRAMEBUFFER, 0);
            gl3.glDisable(GL_FRAMEBUFFER_SRGB);

            gl3.glUseProgram(programName[Program.SPLASH.ordinal()]);

            gl3.glActiveTexture(GL_TEXTURE0);
            gl3.glBindVertexArray(vertexArrayName[Program.SPLASH.ordinal()]);
            gl3.glBindTexture(GL_TEXTURE_2D, textureName[Texture.COLORBUFFER.ordinal()]);

            gl3.glDrawArraysInstanced(GL_TRIANGLES, 0, 3, 1);
        }

        return true;
    }

    @Override
    protected boolean end(GL gl) {

        GL3 gl3 = (GL3) gl;

        gl3.glDeleteFramebuffers(1, framebufferName, 0);
        gl3.glDeleteProgram(programName[Program.SPLASH.ordinal()]);
        gl3.glDeleteProgram(programName[Program.TEXTURE.ordinal()]);

        gl3.glDeleteBuffers(Buffer.MAX.ordinal(), bufferName, 0);
        gl3.glDeleteTextures(Texture.MAX.ordinal(), textureName, 0);
        gl3.glDeleteVertexArrays(Program.MAX.ordinal(), vertexArrayName, 0);

        return true;
    }
}