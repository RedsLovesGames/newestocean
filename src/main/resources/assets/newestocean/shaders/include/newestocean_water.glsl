// NEWESTOCEAN_WATER_V1
uniform int newestocean_waveCount;
uniform float newestocean_time;
uniform float newestocean_waveScale;
uniform float newestocean_seaLevel;
uniform sampler2D newestocean_shoreTexture;
uniform vec2 newestocean_shoreOrigin;
uniform vec2 newestocean_shoreScale;
uniform vec4 newestocean_waterStillBounds;
uniform vec4 newestocean_waterFlowingBounds;
uniform float newestocean_foamStrength;

uniform vec4 newestocean_waveA0;
uniform vec4 newestocean_waveB0;
uniform vec4 newestocean_waveA1;
uniform vec4 newestocean_waveB1;
uniform vec4 newestocean_waveA2;
uniform vec4 newestocean_waveB2;
uniform vec4 newestocean_waveA3;
uniform vec4 newestocean_waveB3;
uniform vec4 newestocean_waveA4;
uniform vec4 newestocean_waveB4;
uniform vec4 newestocean_waveA5;
uniform vec4 newestocean_waveB5;
uniform vec4 newestocean_waveA6;
uniform vec4 newestocean_waveB6;
uniform vec4 newestocean_waveA7;
uniform vec4 newestocean_waveB7;
uniform vec4 newestocean_waveA8;
uniform vec4 newestocean_waveB8;
uniform vec4 newestocean_waveA9;
uniform vec4 newestocean_waveB9;
uniform vec4 newestocean_waveA10;
uniform vec4 newestocean_waveB10;
uniform vec4 newestocean_waveA11;
uniform vec4 newestocean_waveB11;
uniform vec4 newestocean_waveA12;
uniform vec4 newestocean_waveB12;
uniform vec4 newestocean_waveA13;
uniform vec4 newestocean_waveB13;
uniform vec4 newestocean_waveA14;
uniform vec4 newestocean_waveB14;
uniform vec4 newestocean_waveA15;
uniform vec4 newestocean_waveB15;
uniform vec4 newestocean_waveA16;
uniform vec4 newestocean_waveB16;
uniform vec4 newestocean_waveA17;
uniform vec4 newestocean_waveB17;
uniform vec4 newestocean_waveA18;
uniform vec4 newestocean_waveB18;
uniform vec4 newestocean_waveA19;
uniform vec4 newestocean_waveB19;
uniform vec4 newestocean_waveA20;
uniform vec4 newestocean_waveB20;
uniform vec4 newestocean_waveA21;
uniform vec4 newestocean_waveB21;
uniform vec4 newestocean_waveA22;
uniform vec4 newestocean_waveB22;
uniform vec4 newestocean_waveA23;
uniform vec4 newestocean_waveB23;

struct NewestOceanWaterSample {
    vec3 displacement;
    vec3 normal;
    float foam;
};

bool newestocean_uvInside(vec2 atlasUv, vec4 bounds) {
    return atlasUv.x >= bounds.x
        && atlasUv.y >= bounds.y
        && atlasUv.x <= bounds.z
        && atlasUv.y <= bounds.w;
}

bool newestocean_isWater(vec2 atlasUv) {
    return newestocean_uvInside(atlasUv, newestocean_waterStillBounds)
        || newestocean_uvInside(atlasUv, newestocean_waterFlowingBounds);
}

vec4 newestocean_waveA(int index) {
    if (index == 0) return newestocean_waveA0;
    if (index == 1) return newestocean_waveA1;
    if (index == 2) return newestocean_waveA2;
    if (index == 3) return newestocean_waveA3;
    if (index == 4) return newestocean_waveA4;
    if (index == 5) return newestocean_waveA5;
    if (index == 6) return newestocean_waveA6;
    if (index == 7) return newestocean_waveA7;
    if (index == 8) return newestocean_waveA8;
    if (index == 9) return newestocean_waveA9;
    if (index == 10) return newestocean_waveA10;
    if (index == 11) return newestocean_waveA11;
    if (index == 12) return newestocean_waveA12;
    if (index == 13) return newestocean_waveA13;
    if (index == 14) return newestocean_waveA14;
    if (index == 15) return newestocean_waveA15;
    if (index == 16) return newestocean_waveA16;
    if (index == 17) return newestocean_waveA17;
    if (index == 18) return newestocean_waveA18;
    if (index == 19) return newestocean_waveA19;
    if (index == 20) return newestocean_waveA20;
    if (index == 21) return newestocean_waveA21;
    if (index == 22) return newestocean_waveA22;
    if (index == 23) return newestocean_waveA23;
    return vec4(0.0);
}

vec4 newestocean_waveB(int index) {
    if (index == 0) return newestocean_waveB0;
    if (index == 1) return newestocean_waveB1;
    if (index == 2) return newestocean_waveB2;
    if (index == 3) return newestocean_waveB3;
    if (index == 4) return newestocean_waveB4;
    if (index == 5) return newestocean_waveB5;
    if (index == 6) return newestocean_waveB6;
    if (index == 7) return newestocean_waveB7;
    if (index == 8) return newestocean_waveB8;
    if (index == 9) return newestocean_waveB9;
    if (index == 10) return newestocean_waveB10;
    if (index == 11) return newestocean_waveB11;
    if (index == 12) return newestocean_waveB12;
    if (index == 13) return newestocean_waveB13;
    if (index == 14) return newestocean_waveB14;
    if (index == 15) return newestocean_waveB15;
    if (index == 16) return newestocean_waveB16;
    if (index == 17) return newestocean_waveB17;
    if (index == 18) return newestocean_waveB18;
    if (index == 19) return newestocean_waveB19;
    if (index == 20) return newestocean_waveB20;
    if (index == 21) return newestocean_waveB21;
    if (index == 22) return newestocean_waveB22;
    if (index == 23) return newestocean_waveB23;
    return vec4(0.0);
}

float newestocean_shoreFactor(vec2 worldXZ) {
    vec2 shoreUv = (worldXZ - newestocean_shoreOrigin) * newestocean_shoreScale;
    float t = clamp(texture(newestocean_shoreTexture, shoreUv).r, 0.0, 1.0);
    return t * t * (3.0 - 2.0 * t);
}

NewestOceanWaterSample newestocean_evaluateWater(vec3 worldPosition) {
    NewestOceanWaterSample sample;
    sample.displacement = vec3(0.0);
    sample.normal = vec3(0.0, 1.0, 0.0);
    sample.foam = 0.0;

    vec3 tangentX = vec3(1.0, 0.0, 0.0);
    vec3 tangentZ = vec3(0.0, 0.0, 1.0);
    vec2 slope = vec2(0.0);
    float crestCurvature = 0.0;
    float shoreFactor = newestocean_shoreFactor(worldPosition.xz);

    for (int i = 0; i < 24; i++) {
        if (i < newestocean_waveCount) {
            vec4 waveA = newestocean_waveA(i);
            vec4 waveB = newestocean_waveB(i);
            vec2 direction = waveA.xy;
            float amplitude = waveA.z * newestocean_waveScale * shoreFactor;
            float waveNumber = waveA.w;
            float angularFrequency = waveB.x;
            float phase = waveB.y;
            float steepness = waveB.z;

            float theta = waveNumber * dot(direction, worldPosition.xz)
                - angularFrequency * newestocean_time
                + phase;
            float s = sin(theta);
            float c = cos(theta);
            float steepnessAmplitude = steepness * amplitude;
            float horizontalDerivative = steepnessAmplitude * waveNumber * s;

            sample.displacement.x += steepnessAmplitude * direction.x * c;
            sample.displacement.y += amplitude * s;
            sample.displacement.z += steepnessAmplitude * direction.y * c;

            slope += amplitude * waveNumber * direction * c;

            tangentX.x -= horizontalDerivative * direction.x * direction.x;
            tangentX.y += amplitude * waveNumber * direction.x * c;
            tangentX.z -= horizontalDerivative * direction.x * direction.y;

            tangentZ.x -= horizontalDerivative * direction.x * direction.y;
            tangentZ.y += amplitude * waveNumber * direction.y * c;
            tangentZ.z -= horizontalDerivative * direction.y * direction.y;

            crestCurvature += amplitude * waveNumber * waveNumber * max(s, 0.0);
        }
    }

    vec3 normal = normalize(cross(tangentZ, tangentX));
    if (normal.y < 0.0) {
        normal = -normal;
    }
    sample.normal = normal;

    float slopeFoam = smoothstep(0.24, 0.78, length(slope));
    float crestFoam = smoothstep(0.035, 0.30, crestCurvature);
    sample.foam = clamp(
        slopeFoam * (0.45 + 0.55 * crestFoam) * shoreFactor * newestocean_foamStrength,
        0.0,
        1.0
    );
    return sample;
}
