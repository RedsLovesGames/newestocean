package com.redslovesgames.newestocean.client.shore;

import com.redslovesgames.newestocean.ocean.ShoreAttenuation;
import com.redslovesgames.newestocean.ocean.ShoreDistanceGrid;

import java.util.Arrays;

/**
 * Backend-neutral one-byte shoreline texture payload and allocation controller.
 *
 * <p>Distance 0 maps to 0 and the full-strength 15-block distance maps to 255. Renderer-specific
 * OpenGL upload code is supplied through {@link Backend}, keeping this class unit-testable.</p>
 */
public final class ShoreDistanceTexture implements AutoCloseable {
    private final Backend backend;
    private Object handle;
    private int width = -1;
    private int height = -1;
    private Payload payload;

    public ShoreDistanceTexture(Backend backend) {
        if (backend == null) {
            throw new IllegalArgumentException("backend cannot be null");
        }
        this.backend = backend;
    }

    public static byte encodeDistance(double distanceBlocks) {
        if (!Double.isFinite(distanceBlocks)) {
            throw new IllegalArgumentException("distanceBlocks must be finite");
        }
        double normalized = Math.max(
            0.0,
            Math.min(1.0, distanceBlocks / ShoreAttenuation.FULL_STRENGTH_DISTANCE)
        );
        return (byte) Math.round(normalized * 255.0);
    }

    public static Payload encode(ShoreDistanceGrid grid) {
        if (grid == null) {
            throw new IllegalArgumentException("grid cannot be null");
        }
        byte[] data = new byte[grid.width() * grid.height()];
        int index = 0;
        for (int z = 0; z < grid.height(); z++) {
            int worldZ = grid.originZ() + z;
            for (int x = 0; x < grid.width(); x++) {
                int worldX = grid.originX() + x;
                data[index++] = encodeDistance(grid.distanceToLand(worldX, worldZ));
            }
        }
        return new Payload(
            grid.originX(),
            grid.originZ(),
            grid.width(),
            grid.height(),
            data
        );
    }

    public void upload(Payload next) {
        if (next == null) {
            throw new IllegalArgumentException("payload cannot be null");
        }
        if (handle == null || next.width() != width || next.height() != height) {
            if (handle != null) {
                backend.release(handle);
            }
            handle = backend.allocate(next.width(), next.height());
            if (handle == null) {
                throw new IllegalStateException("shore texture backend returned a null handle");
            }
            width = next.width();
            height = next.height();
        }
        byte[] uploadData = next.data();
        backend.upload(handle, width, height, uploadData);
        payload = next;
    }

    public Object handle() {
        return handle;
    }

    public Payload payload() {
        return payload;
    }

    @Override
    public void close() {
        if (handle != null) {
            backend.release(handle);
            handle = null;
            width = -1;
            height = -1;
            payload = null;
        }
    }

    public interface Backend {
        Object allocate(int width, int height);

        void upload(Object handle, int width, int height, byte[] normalizedDistances);

        void release(Object handle);
    }

    public record Payload(int originX, int originZ, int width, int height, byte[] data) {
        public Payload {
            if (width <= 0 || height <= 0) {
                throw new IllegalArgumentException("payload dimensions must be positive");
            }
            if (data == null || data.length != width * height) {
                throw new IllegalArgumentException("payload data length must equal width * height");
            }
            data = data.clone();
        }

        @Override
        public byte[] data() {
            return data.clone();
        }

        public double texelScaleX() {
            return 1.0 / width;
        }

        public double texelScaleZ() {
            return 1.0 / height;
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) return true;
            if (!(other instanceof Payload payload)) return false;
            return originX == payload.originX
                && originZ == payload.originZ
                && width == payload.width
                && height == payload.height
                && Arrays.equals(data, payload.data);
        }

        @Override
        public int hashCode() {
            int result = Integer.hashCode(originX);
            result = 31 * result + Integer.hashCode(originZ);
            result = 31 * result + Integer.hashCode(width);
            result = 31 * result + Integer.hashCode(height);
            result = 31 * result + Arrays.hashCode(data);
            return result;
        }
    }
}
