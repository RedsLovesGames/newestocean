package com.redslovesgames.newestocean.client.water;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.redslovesgames.newestocean.client.shore.ShoreDistanceTexture;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL30;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;

/** OpenGL R8 backend for the compact camera-centered shoreline distance texture. */
public final class VanillaShoreTextureBackend implements ShoreDistanceTexture.Backend {
    @Override
    public Object allocate(int width, int height) {
        requireDimensions(width, height);
        RenderSystem.assertOnRenderThread();

        int texture = GlStateManager._genTexture();
        GlStateManager._bindTexture(texture);
        GlStateManager._texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
        GlStateManager._texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
        GlStateManager._texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
        GlStateManager._texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);
        GlStateManager._texImage2D(
            GL11.GL_TEXTURE_2D,
            0,
            GL30.GL_R8,
            width,
            height,
            0,
            GL11.GL_RED,
            GL11.GL_UNSIGNED_BYTE,
            null
        );
        return texture;
    }

    @Override
    public void upload(Object handle, int width, int height, byte[] data) {
        int texture = textureId(handle);
        requireDimensions(width, height);
        if (data == null || data.length != width * height) {
            throw new IllegalArgumentException("shore texture payload must contain exactly width * height bytes");
        }
        RenderSystem.assertOnRenderThread();

        ByteBuffer buffer = MemoryUtil.memAlloc(data.length);
        try {
            buffer.put(data).flip();
            GlStateManager._bindTexture(texture);
            GL11.glPixelStorei(GL11.GL_UNPACK_ALIGNMENT, 1);
            GL11.glTexSubImage2D(
                GL11.GL_TEXTURE_2D,
                0,
                0,
                0,
                width,
                height,
                GL11.GL_RED,
                GL11.GL_UNSIGNED_BYTE,
                buffer
            );
        } finally {
            MemoryUtil.memFree(buffer);
        }
    }

    @Override
    public void release(Object handle) {
        if (handle == null) {
            return;
        }
        RenderSystem.assertOnRenderThread();
        GlStateManager._deleteTexture(textureId(handle));
    }

    private static int textureId(Object handle) {
        if (!(handle instanceof Integer texture) || texture <= 0) {
            throw new IllegalArgumentException("shore texture handle must be a positive OpenGL texture id");
        }
        return texture;
    }

    private static void requireDimensions(int width, int height) {
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("shore texture dimensions must be positive");
        }
    }
}
