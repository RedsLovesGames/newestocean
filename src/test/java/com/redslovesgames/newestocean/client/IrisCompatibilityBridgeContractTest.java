package com.redslovesgames.newestocean.client;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IrisCompatibilityBridgeContractTest {
    @Test
    void bridgeUsesOptionalReflectionWithoutHardIrisImports() throws IOException {
        Path path = Path.of("src/main/java/com/redslovesgames/newestocean/client/IrisCompatibilityBridge.java");
        String source = Files.readString(path);

        assertTrue(source.contains("isModLoaded(\"iris\")"));
        assertTrue(source.contains("net.irisshaders.iris.api.v0.IrisApi"));
        assertTrue(source.contains("isShaderPackInUse"));
        assertTrue(source.contains("isRenderingShadowPass"));
        assertTrue(source.contains("getCurrentPackName"));
        assertFalse(source.contains("import net.irisshaders.iris"));
    }
}
