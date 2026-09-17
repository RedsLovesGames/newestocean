#version 150

in vec3 Position;

uniform mat4 ModelViewMat;
uniform mat4 ProjMat;
uniform float OceanTime;
uniform float OceanWaveScale;
uniform float OceanWaterHeight;
uniform vec2 OceanCameraXZ;
uniform float OceanWaveCount;
uniform float OceanFoamQuality;
uniform float OceanStormStrength;
uniform vec4 OceanWaveA0;
uniform vec4 OceanWaveB0;
uniform vec4 OceanWaveA1;
uniform vec4 OceanWaveB1;
uniform vec4 OceanWaveA2;
uniform vec4 OceanWaveB2;
uniform vec4 OceanWaveA3;
uniform vec4 OceanWaveB3;
uniform vec4 OceanWaveA4;
uniform vec4 OceanWaveB4;
uniform vec4 OceanWaveA5;
uniform vec4 OceanWaveB5;

out float oceanLight;
out float oceanFoam;

void accumulateWave(
    vec4 a,
    vec4 b,
    vec2 worldXZ,
    inout float height,
    inout vec2 displacement,
    inout vec2 slope,
    inout float crestCurvature
) {
    vec2 direction = a.xy;
    float amplitude = a.z * OceanWaveScale;
    float waveNumber = a.w;
    float phase = b.x;
    float angularFrequency = b.y;
    float steepness = b.z;

    float theta = waveNumber * dot(direction, worldXZ) - angularFrequency * OceanTime + phase;
    float s = sin(theta);
    float c = cos(theta);

    height += amplitude * s;
    slope += amplitude * waveNumber * direction * c;
    displacement += steepness * amplitude * c * direction;
    crestCurvature += amplitude * waveNumber * waveNumber * max(s, 0.0);
}

void main() {
    vec2 worldXZ = Position.xz + OceanCameraXZ;
    float height = OceanWaterHeight;
    vec2 displacement = vec2(0.0);
    vec2 slope = vec2(0.0);
    float crestCurvature = 0.0;

    if (OceanWaveCount > 0.5) accumulateWave(OceanWaveA0, OceanWaveB0, worldXZ, height, displacement, slope, crestCurvature);
    if (OceanWaveCount > 1.5) accumulateWave(OceanWaveA1, OceanWaveB1, worldXZ, height, displacement, slope, crestCurvature);
    if (OceanWaveCount > 2.5) accumulateWave(OceanWaveA2, OceanWaveB2, worldXZ, height, displacement, slope, crestCurvature);
    if (OceanWaveCount > 3.5) accumulateWave(OceanWaveA3, OceanWaveB3, worldXZ, height, displacement, slope, crestCurvature);
    if (OceanWaveCount > 4.5) accumulateWave(OceanWaveA4, OceanWaveB4, worldXZ, height, displacement, slope, crestCurvature);
    if (OceanWaveCount > 5.5) accumulateWave(OceanWaveA5, OceanWaveB5, worldXZ, height, displacement, slope, crestCurvature);

    vec3 normal = normalize(vec3(-slope.x, 1.0, -slope.y));
    oceanLight = clamp(0.72 + normal.y * 0.20, 0.68, 0.94);

    float slopeFoam = smoothstep(0.24, 0.78, length(slope));
    float crestFoam = smoothstep(0.035, 0.30, crestCurvature);
    float formation = slopeFoam * (0.45 + 0.55 * crestFoam);
    float detail = clamp((OceanFoamQuality - 0.35) / 0.65, 0.0, 1.0);
    float breakupNoise = 0.5 + 0.5 * sin(
        worldXZ.x * 0.33
        + worldXZ.y * 0.29
        + sin(worldXZ.y * 0.11 - OceanTime * 0.19) * 1.7
    );
    float breakup = mix(0.82, 0.55 + 0.45 * breakupNoise, detail);
    float stormGain = 0.55 + 0.75 * OceanStormStrength;
    oceanFoam = clamp(formation * stormGain * OceanFoamQuality * breakup, 0.0, 1.0);

    vec3 displaced = vec3(Position.x + displacement.x, height, Position.z + displacement.y);
    gl_Position = ProjMat * ModelViewMat * vec4(displaced, 1.0);
}
