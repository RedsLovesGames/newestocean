package com.redslovesgames.newestocean.client.shore;

import com.redslovesgames.newestocean.ocean.ShoreDistanceGrid;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class ShoreDistanceTextureTest {
    @Test
    void normalizedByteEncodingMapsZeroToZeroAndFifteenOrMoreToFullScale() {
        assertEquals(0, Byte.toUnsignedInt(ShoreDistanceTexture.encodeDistance(0.0)));
        assertEquals(128, Byte.toUnsignedInt(ShoreDistanceTexture.encodeDistance(7.5)));
        assertEquals(255, Byte.toUnsignedInt(ShoreDistanceTexture.encodeDistance(15.0)));
        assertEquals(255, Byte.toUnsignedInt(ShoreDistanceTexture.encodeDistance(100.0)));
    }

    @Test
    void payloadUsesOneBytePerCellAndPreservesWorldOriginAndTexelScale() {
        boolean[][] land = new boolean[4][5];
        land[1][2] = true;
        ShoreDistanceGrid grid = ShoreDistanceGrid.fromLandMask(40, -12, land);

        ShoreDistanceTexture.Payload payload = ShoreDistanceTexture.encode(grid);

        assertEquals(40, payload.originX());
        assertEquals(-12, payload.originZ());
        assertEquals(5, payload.width());
        assertEquals(4, payload.height());
        assertEquals(20, payload.data().length);
        assertEquals(1.0 / 5.0, payload.texelScaleX(), 1.0e-12);
        assertEquals(1.0 / 4.0, payload.texelScaleZ(), 1.0e-12);
    }

    @Test
    void uploadControllerReusesAllocationWhenDimensionsDoNotChange() {
        RecordingBackend backend = new RecordingBackend();
        ShoreDistanceTexture texture = new ShoreDistanceTexture(backend);
        ShoreDistanceTexture.Payload first = payload(8, 8, 1);
        ShoreDistanceTexture.Payload second = payload(8, 8, 2);

        texture.upload(first);
        Object handle = texture.handle();
        texture.upload(second);

        assertEquals(1, backend.allocations);
        assertEquals(2, backend.uploads);
        assertSame(handle, texture.handle());
    }

    @Test
    void dimensionChangeReallocatesAndReleasesOldTexture() {
        RecordingBackend backend = new RecordingBackend();
        ShoreDistanceTexture texture = new ShoreDistanceTexture(backend);

        texture.upload(payload(8, 8, 1));
        Object first = texture.handle();
        texture.upload(payload(16, 8, 2));

        assertEquals(2, backend.allocations);
        assertEquals(1, backend.releases);
        assertNotEquals(first, texture.handle());
    }

    private static ShoreDistanceTexture.Payload payload(int width, int height, int fill) {
        byte[] data = new byte[width * height];
        java.util.Arrays.fill(data, (byte) fill);
        return new ShoreDistanceTexture.Payload(0, 0, width, height, data);
    }

    private static final class RecordingBackend implements ShoreDistanceTexture.Backend {
        private int nextHandle;
        private int allocations;
        private int uploads;
        private int releases;

        @Override
        public Object allocate(int width, int height) {
            allocations++;
            return ++nextHandle;
        }

        @Override
        public void upload(Object handle, int width, int height, byte[] normalizedDistances) {
            uploads++;
        }

        @Override
        public void release(Object handle) {
            releases++;
        }
    }
}
