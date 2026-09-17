#version 150

in float oceanLight;
in float oceanFoam;
out vec4 fragColor;

void main() {
    vec3 baseColor = vec3(0.055, 0.34, 0.58) * oceanLight;
    vec3 foamColor = vec3(0.93, 0.97, 1.0);
    float foamMix = smoothstep(0.06, 0.90, oceanFoam);
    vec3 color = mix(baseColor, foamColor, foamMix);
    float alpha = mix(0.72, 0.88, foamMix);
    fragColor = vec4(color, alpha);
}
