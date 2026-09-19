package com.redslovesgames.newestocean.client;

import com.redslovesgames.newestocean.NewestOcean;
import net.fabricmc.loader.api.FabricLoader;

import java.lang.reflect.Method;
import java.util.Optional;

/** Optional, reflection-only Iris bridge. Newest Ocean never hard-depends on Iris. */
final class IrisCompatibilityBridge {
    private static final String IRIS_API_CLASS = "net.irisshaders.iris.api.v0.IrisApi";
    private static final String IRIS_INTERNAL_CLASS = "net.irisshaders.iris.Iris";

    private static volatile boolean initialized;
    private static volatile boolean activeBridgeResolved;
    private static volatile boolean failureLogged;

    private static Method getInstance;
    private static Method isShaderPackInUse;
    private static Method isRenderingShadowPass;
    private static Method getConfig;
    private static Method configGetShaderPackName;
    private static Method getCurrentPackName;

    private IrisCompatibilityBridge() {
    }

    static ShaderCompatibility.IrisState currentState() {
        if (!FabricLoader.getInstance().isModLoaded("iris")) {
            return new ShaderCompatibility.IrisState(false, true, false, false, null);
        }

        initializeIfNeeded();
        if (!activeBridgeResolved) {
            return new ShaderCompatibility.IrisState(true, false, false, false, null);
        }

        try {
            Object api = getInstance.invoke(null);
            boolean packInUse = (boolean) isShaderPackInUse.invoke(api);
            boolean shadowPass = packInUse && (boolean) isRenderingShadowPass.invoke(api);
            String packName = packInUse ? shaderPackName(api) : null;
            return new ShaderCompatibility.IrisState(true, true, packInUse, shadowPass, packName);
        } catch (ReflectiveOperationException | RuntimeException error) {
            logFailureOnce(error);
            return new ShaderCompatibility.IrisState(true, false, false, false, null);
        }
    }

    private static synchronized void initializeIfNeeded() {
        if (initialized) {
            return;
        }
        initialized = true;

        try {
            ClassLoader loader = IrisCompatibilityBridge.class.getClassLoader();
            Class<?> apiClass = Class.forName(IRIS_API_CLASS, false, loader);
            getInstance = apiClass.getMethod("getInstance");
            isShaderPackInUse = apiClass.getMethod("isShaderPackInUse");
            isRenderingShadowPass = apiClass.getMethod("isRenderingShadowPass");
            activeBridgeResolved = true;

            try {
                getConfig = apiClass.getMethod("getConfig");
                configGetShaderPackName = getConfig.getReturnType().getMethod("getShaderPackName");
            } catch (ReflectiveOperationException ignored) {
                getConfig = null;
                configGetShaderPackName = null;
            }

            try {
                Class<?> irisClass = Class.forName(IRIS_INTERNAL_CLASS, false, loader);
                getCurrentPackName = irisClass.getMethod("getCurrentPackName");
            } catch (ReflectiveOperationException ignored) {
                getCurrentPackName = null;
            }
        } catch (ReflectiveOperationException | RuntimeException error) {
            activeBridgeResolved = false;
            logFailureOnce(error);
        }
    }

    private static String shaderPackName(Object api) {
        if (getConfig != null && configGetShaderPackName != null) {
            try {
                Object config = getConfig.invoke(api);
                String name = stringValue(configGetShaderPackName.invoke(config));
                if (name != null) {
                    return name;
                }
            } catch (ReflectiveOperationException | RuntimeException ignored) {
                // Pack-name discovery is optional; generic Iris mode remains safe.
            }
        }

        if (getCurrentPackName != null) {
            try {
                return stringValue(getCurrentPackName.invoke(null));
            } catch (ReflectiveOperationException | RuntimeException ignored) {
                // Pack-name discovery is optional; generic Iris mode remains safe.
            }
        }
        return null;
    }

    private static String stringValue(Object value) {
        if (value instanceof String string) {
            return string;
        }
        if (value instanceof Optional<?> optional) {
            Object contained = optional.orElse(null);
            return contained instanceof String string ? string : null;
        }
        return null;
    }

    private static void logFailureOnce(Throwable error) {
        if (!failureLogged) {
            failureLogged = true;
            NewestOcean.LOGGER.warn(
                "Iris is installed but Newest Ocean could not resolve its compatibility bridge; using CPU-safe rendering.",
                error
            );
        }
    }
}
