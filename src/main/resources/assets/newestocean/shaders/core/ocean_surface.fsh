#version 150

in float oceanLight;
out vec4 fragColor;

void main() {
    vec3 baseColor = vec3(0.055, 0.34, 0.58) * oceanLight;
    fragColor = vec4(baseColor, 0.72);
}
