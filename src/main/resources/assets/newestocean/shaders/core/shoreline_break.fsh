#version 150

uniform float ShorelineTime;
uniform float ShorelineQuality;

in vec2 shoreWorldXZ;
in float shoreBreaker;
in float shoreWash;

out vec4 fragColor;

void main() {
    float broadNoise = 0.5 + 0.5 * sin(shoreWorldXZ.x * 1.31 + shoreWorldXZ.y * 0.77 + ShorelineTime * 1.55);
    float fineNoise = 0.5 + 0.5 * sin(shoreWorldXZ.x * 3.71 - shoreWorldXZ.y * 2.43 - ShorelineTime * 2.25);
    float detailMix = clamp(ShorelineQuality, 0.0, 1.0);
    float breakup = mix(0.72 + 0.28 * broadNoise, 0.55 + 0.45 * broadNoise * fineNoise, detailMix);
    float washBand = 0.72 + 0.28 * clamp(shoreWash, 0.0, 1.0);
    float alpha = clamp(shoreBreaker * breakup * washBand, 0.0, 0.78);

    if (alpha < 0.025) {
        discard;
    }

    vec3 foamColor = mix(vec3(0.80, 0.91, 0.98), vec3(0.96, 0.99, 1.0), clamp(alpha * 1.4, 0.0, 1.0));
    fragColor = vec4(foamColor, alpha);
}
