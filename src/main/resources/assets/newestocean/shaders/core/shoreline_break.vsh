#version 150

in vec3 Position;
in vec4 Color;

uniform mat4 ModelViewMat;
uniform mat4 ProjMat;
uniform float ShorelineTime;
uniform float ShorelineWaveScale;
uniform float ShorelineWaterHeight;
uniform vec2 ShorelineCameraXZ;
uniform float ShorelineWaveCount;
uniform float ShorelineStormStrength;
uniform float ShorelineQuality;
uniform vec4 ShorelineWaveA0;
uniform vec4 ShorelineWaveB0;
uniform vec4 ShorelineWaveA1;
uniform vec4 ShorelineWaveB1;
uniform vec4 ShorelineWaveA2;
uniform vec4 ShorelineWaveB2;
uniform vec4 ShorelineWaveA3;
uniform vec4 ShorelineWaveB3;
uniform vec4 ShorelineWaveA4;
uniform vec4 ShorelineWaveB4;
uniform vec4 ShorelineWaveA5;
uniform vec4 ShorelineWaveB5;

out vec2 shoreWorldXZ;
out float shoreBreaker;
out float shoreWash;

void applyShoreWave(
    vec4 waveA,
    vec4 waveB,
    vec2 worldXZ,
    vec2 shoreDirection,
    inout float height,
    inout vec2 displacement,
    inout float incomingWeighted,
    inout float incomingWeight,
    inout float crestEnergy,
    inout float totalEnergy
) {
    float amplitude = waveA.z * ShorelineWaveScale;
    float theta = waveA.w * dot(waveA.xy, worldXZ) - waveB.y * ShorelineTime + waveB.x;
    float waveSin = sin(theta);
    float waveCos = cos(theta);
    height += amplitude * waveSin;
    displacement += waveB.z * amplitude * waveCos * waveA.xy;

    float weight = abs(amplitude) * (0.35 + 0.65 * clamp(waveB.z, 0.0, 1.0));
    float incoming = max(0.0, dot(waveA.xy, shoreDirection));
    incomingWeighted += incoming * weight;
    incomingWeight += weight;
    crestEnergy += max(0.0, waveSin) * abs(amplitude);
    totalEnergy += abs(amplitude);
}

void main() {
    vec2 worldXZ = Position.xz + ShorelineCameraXZ;
    vec2 shoreDirection = Color.rg * 2.0 - 1.0;
    float directionLength = length(shoreDirection);
    if (directionLength > 0.0001) {
        shoreDirection /= directionLength;
    } else {
        shoreDirection = vec2(0.0);
    }

    float shoreInfluence = clamp(Color.b, 0.0, 1.0);
    float shallowFactor = clamp(Color.a, 0.0, 1.0);
    float height = 0.0;
    vec2 displacement = vec2(0.0);
    float incomingWeighted = 0.0;
    float incomingWeight = 0.0;
    float crestEnergy = 0.0;
    float totalEnergy = 0.0;

    if (ShorelineWaveCount > 0.5) applyShoreWave(ShorelineWaveA0, ShorelineWaveB0, worldXZ, shoreDirection, height, displacement, incomingWeighted, incomingWeight, crestEnergy, totalEnergy);
    if (ShorelineWaveCount > 1.5) applyShoreWave(ShorelineWaveA1, ShorelineWaveB1, worldXZ, shoreDirection, height, displacement, incomingWeighted, incomingWeight, crestEnergy, totalEnergy);
    if (ShorelineWaveCount > 2.5) applyShoreWave(ShorelineWaveA2, ShorelineWaveB2, worldXZ, shoreDirection, height, displacement, incomingWeighted, incomingWeight, crestEnergy, totalEnergy);
    if (ShorelineWaveCount > 3.5) applyShoreWave(ShorelineWaveA3, ShorelineWaveB3, worldXZ, shoreDirection, height, displacement, incomingWeighted, incomingWeight, crestEnergy, totalEnergy);
    if (ShorelineWaveCount > 4.5) applyShoreWave(ShorelineWaveA4, ShorelineWaveB4, worldXZ, shoreDirection, height, displacement, incomingWeighted, incomingWeight, crestEnergy, totalEnergy);
    if (ShorelineWaveCount > 5.5) applyShoreWave(ShorelineWaveA5, ShorelineWaveB5, worldXZ, shoreDirection, height, displacement, incomingWeighted, incomingWeight, crestEnergy, totalEnergy);

    float incoming = incomingWeight > 0.0001 ? clamp(incomingWeighted / incomingWeight, 0.0, 1.0) : 0.0;
    float waveEnergy = totalEnergy > 0.0001 ? clamp(crestEnergy / totalEnergy, 0.0, 1.0) : 0.0;
    float shoal = shoreInfluence * shallowFactor * incoming;

    // Visual-only shoaling: lift positive crests slightly without changing physical ocean samples.
    height += crestEnergy * shoal * 0.15;

    shoreBreaker = clamp(
        shoreInfluence
            * incoming
            * (0.35 + 0.65 * waveEnergy)
            * (0.75 + 0.45 * clamp(ShorelineStormStrength, 0.0, 1.0))
            * clamp(ShorelineQuality, 0.0, 1.0),
        0.0,
        1.0
    );
    shoreWash = (0.5 + 0.5 * sin(dot(worldXZ, shoreDirection) * 1.4 - ShorelineTime * 1.6)) * shoreInfluence;
    shoreWorldXZ = worldXZ;

    vec3 displaced = vec3(
        Position.x + displacement.x,
        ShorelineWaterHeight + height + Position.y,
        Position.z + displacement.y
    );
    gl_Position = ProjMat * ModelViewMat * vec4(displaced, 1.0);
}
