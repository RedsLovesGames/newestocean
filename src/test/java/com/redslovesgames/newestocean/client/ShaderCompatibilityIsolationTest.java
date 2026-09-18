package com.redslovesgames.newestocean.client;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertFalse;

class ShaderCompatibilityIsolationTest {
    @Test
    void serverAndSharedCodeNeverDependOnClientShaderCompatibility() throws IOException {
        Path root = Path.of("src/main/java/com/redslovesgames/newestocean");
        try (Stream<Path> files = Files.walk(root)) {
            files.filter(path -> path.toString().endsWith(".java"))
                .filter(path -> !path.toString().replace('\\', '/').contains("/client/"))
                .forEach(path -> {
                    try {
                        String source = Files.readString(path);
                        assertFalse(source.contains("ShaderCompatibility"), path.toString());
                        assertFalse(source.contains("IrisCompatibilityBridge"), path.toString());
                    } catch (IOException error) {
                        throw new RuntimeException(error);
                    }
                });
        }
    }
}
