#version 150

in vec3 Position;
in vec4 Color;

uniform mat4 ModelViewMat;
uniform mat4 ProjMat;
uniform float WakeTime;
uniform float WakeWaveScale;
uniform float WakeWaterHeight;
uniform vec2 WakeCameraXZ;
uniform float WakeWaveCount;
uniform vec4 WakeWaveA0;
uniform vec4 WakeWaveB0;
uniform vec4 WakeWaveA1;
uniform vec4 WakeWaveB1;
uniform vec4 WakeWaveA2;
uniform vec4 WakeWaveB2;
uniform vec4 WakeWaveA3;
uniform vec4 WakeWaveB3;
uniform vec4 WakeWaveA4;
uniform vec4 WakeWaveB4;
uniform vec4 WakeWaveA5;
uniform vec4 WakeWaveB5;

out vec2 wakeWorldXZ;
out float wakeStrength;
out float wakeEdge;
out float wakeCenter;

void applyWave(vec4 waveA, vec4 waveB, vec2 worldXZ, inout float height, inout vec2 displacement) {
    float amplitude = waveA.z * WakeWaveScale;
    float theta = waveA.w * dot(waveA.xy, worldXZ) - waveB.y * WakeTime + waveB.x;
    float waveSin = sin(theta);
    float waveCos = cos(theta);
    height += amplitude * waveSin;
    float horizontalAmount = waveB.z * amplitude * waveCos;
    displacement += horizontalAmount * waveA.xy;
}

void main() {
    vec2 worldXZ = Position.xz + WakeCameraXZ;
    float height = 0.0;
    vec2 displacement = vec2(0.0);

    if (WakeWaveCount > 0.5) applyWave(WakeWaveA0, WakeWaveB0, worldXZ, height, displacement);
    if (WakeWaveCount > 1.5) applyWave(WakeWaveA1, WakeWaveB1, worldXZ, height, displacement);
    if (WakeWaveCount > 2.5) applyWave(WakeWaveA2, WakeWaveB2, worldXZ, height, displacement);
    if (WakeWaveCount > 3.5) applyWave(WakeWaveA3, WakeWaveB3, worldXZ, height, displacement);
    if (WakeWaveCount > 4.5) applyWave(WakeWaveA4, WakeWaveB4, worldXZ, height, displacement);
    if (WakeWaveCount > 5.5) applyWave(WakeWaveA5, WakeWaveB5, worldXZ, height, displacement);

    vec3 displaced = vec3(
        Position.x + displacement.x,
        WakeWaterHeight + height + Position.y,
        Position.z + displacement.y
    );

    wakeWorldXZ = worldXZ;
    wakeStrength = Color.r;
    wakeEdge = Color.g;
    wakeCenter = Color.b;
    gl_Position = ProjMat * ModelViewMat * vec4(displaced, 1.0);
}
