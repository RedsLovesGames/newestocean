package com.redslovesgames.newestocean;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;

import static org.junit.jupiter.api.Assertions.assertTrue;

class NewestOceanEntrypointTest {
    @Test
    void fabricMainEntrypointHasPublicNoArgConstructor() throws Exception {
        Constructor<NewestOcean> constructor = NewestOcean.class.getDeclaredConstructor();

        assertTrue(
            Modifier.isPublic(constructor.getModifiers()),
            "Fabric's default language adapter must be able to instantiate the main entrypoint"
        );
    }
}
