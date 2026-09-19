#version 150

uniform float WakeTime;

in vec2 wakeWorldXZ;
in float wakeStrength;
in float wakeEdge;
in float wakeCenter;

out vec4 fragColor;

float proceduralBreakup(vec2 position) {
    float first = sin(position.x * 2.31 + position.y * 1.73 + WakeTime * 1.55);
    float second = sin(position.x * -1.27 + position.y * 2.87 - WakeTime * 0.92);
    float third = sin((position.x + position.y) * 4.15 + WakeTime * 0.48);
    return 0.50 + first * 0.20 + second * 0.18 + third * 0.12;
}

void main() {
    float edgeFade = smoothstep(0.02, 0.92, wakeEdge);
    float breakup = proceduralBreakup(wakeWorldXZ);
    float turbulenceBias = mix(0.05, 0.20, clamp(wakeCenter, 0.0, 1.0));
    float pattern = smoothstep(0.12, 0.78, breakup + wakeStrength * 0.28 + turbulenceBias);
    float alpha = clamp(wakeStrength * edgeFade * mix(0.46, 0.92, pattern), 0.0, 0.92);

    if (alpha < 0.015) {
        discard;
    }

    vec3 foam = mix(vec3(0.74, 0.88, 0.94), vec3(0.96, 0.99, 1.0), pattern);
    fragColor = vec4(foam, alpha);
}
