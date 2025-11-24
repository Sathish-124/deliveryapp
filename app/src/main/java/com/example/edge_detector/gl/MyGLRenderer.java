package com.example.edge_detector.gl;

import android.opengl.GLES20;
import android.opengl.GLSurfaceView;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class MyGLRenderer implements GLSurfaceView.Renderer {

    private int mProgram;
    private int mTextureId;
    private int mPositionHandle;
    private int mTexCoordHandle;
    private int mTextureLocation;

    private FloatBuffer vertexBuffer;
    private FloatBuffer texCoordBuffer;

    // Full screen quad
    private static final float[] VERTICES = {
            -1.0f, 1.0f,
            -1.0f, -1.0f,
            1.0f, 1.0f,
            1.0f, -1.0f
    };

    // Texture coordinates (flipped vertically to match Android camera orientation usually)
    private static final float[] TEX_COORDS = {
            0.0f, 0.0f,
            0.0f, 1.0f,
            1.0f, 0.0f,
            1.0f, 1.0f
    };
    
    // Handles for image data update
    private ByteBuffer mImageData = null;
    private int mImageWidth = 0;
    private int mImageHeight = 0;
    private final Object mImageLock = new Object();
    private boolean mImageDirty = false;

    private final String vertexShaderCode =
            "attribute vec4 vPosition;" +
            "attribute vec2 a_TexCoord;" +
            "varying vec2 v_TexCoord;" +
            "void main() {" +
            "  gl_Position = vPosition;" +
            "  v_TexCoord = a_TexCoord;" +
            "}";

    private final String fragmentShaderCode =
            "precision mediump float;" +
            "uniform sampler2D u_Texture;" +
            "varying vec2 v_TexCoord;" +
            "void main() {" +
            "  gl_FragColor = texture2D(u_Texture, v_TexCoord);" +
            "}";

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);

        // Initialize buffers
        ByteBuffer bb = ByteBuffer.allocateDirect(VERTICES.length * 4);
        bb.order(ByteOrder.nativeOrder());
        vertexBuffer = bb.asFloatBuffer();
        vertexBuffer.put(VERTICES);
        vertexBuffer.position(0);

        ByteBuffer tb = ByteBuffer.allocateDirect(TEX_COORDS.length * 4);
        tb.order(ByteOrder.nativeOrder());
        texCoordBuffer = tb.asFloatBuffer();
        texCoordBuffer.put(TEX_COORDS);
        texCoordBuffer.position(0);

        // Create shader program
        int vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode);
        int fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode);

        mProgram = GLES20.glCreateProgram();
        GLES20.glAttachShader(mProgram, vertexShader);
        GLES20.glAttachShader(mProgram, fragmentShader);
        GLES20.glLinkProgram(mProgram);

        // Generate texture
        int[] textureNames = new int[1];
        GLES20.glGenTextures(1, textureNames, 0);
        mTextureId = textureNames[0];

        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTextureId);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        GLES20.glViewport(0, 0, width, height);
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);

        GLES20.glUseProgram(mProgram);

        mPositionHandle = GLES20.glGetAttribLocation(mProgram, "vPosition");
        GLES20.glEnableVertexAttribArray(mPositionHandle);
        GLES20.glVertexAttribPointer(mPositionHandle, 2, GLES20.GL_FLOAT, false, 0, vertexBuffer);

        mTexCoordHandle = GLES20.glGetAttribLocation(mProgram, "a_TexCoord");
        GLES20.glEnableVertexAttribArray(mTexCoordHandle);
        GLES20.glVertexAttribPointer(mTexCoordHandle, 2, GLES20.GL_FLOAT, false, 0, texCoordBuffer);

        mTextureLocation = GLES20.glGetUniformLocation(mProgram, "u_Texture");
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTextureId);

        // Update texture if needed
        synchronized (mImageLock) {
            if (mImageDirty && mImageData != null) {
                mImageData.position(0);
                // Set unpack alignment to 1 byte for robustness (Canny output is 1 byte per pixel)
                GLES20.glPixelStorei(GLES20.GL_UNPACK_ALIGNMENT, 1);
                
                // Using GL_LUMINANCE for grayscale image from OpenCV Canny
                GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_LUMINANCE, mImageWidth, mImageHeight, 0, GLES20.GL_LUMINANCE, GLES20.GL_UNSIGNED_BYTE, mImageData);
                mImageDirty = false;
            }
        }

        GLES20.glUniform1i(mTextureLocation, 0);

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);

        GLES20.glDisableVertexAttribArray(mPositionHandle);
        GLES20.glDisableVertexAttribArray(mTexCoordHandle);
    }

    public void updateImage(byte[] imageData, int width, int height) {
        synchronized (mImageLock) {
            if (mImageData == null || mImageData.capacity() != imageData.length) {
                mImageData = ByteBuffer.allocateDirect(imageData.length);
            }
            mImageData.position(0);
            mImageData.put(imageData);
            mImageWidth = width;
            mImageHeight = height;
            mImageDirty = true;
        }
    }

    private int loadShader(int type, String shaderCode) {
        int shader = GLES20.glCreateShader(type);
        GLES20.glShaderSource(shader, shaderCode);
        GLES20.glCompileShader(shader);
        return shader;
    }
}
