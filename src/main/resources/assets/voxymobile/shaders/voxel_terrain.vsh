#version 320 es
precision highp float;
layout(location = 0) in vec3 aPosition;
layout(location = 1) in float aMaterial;
uniform mat4 uMvp;
uniform vec3 uChunkOffset;
flat out float vMaterial;
void main() {
    vec3 worldPosition = aPosition + uChunkOffset;
    vec4 clipPosition = uMvp * vec4(worldPosition, 1.0);
    // LOD is selected strictly outside vanilla view distance. Keep it at the far depth
    // plane so it fills sky pixels but cannot cover nearer vanilla terrain/entities.
    clipPosition.z = clipPosition.w * 0.99998;
    gl_Position = clipPosition;
    vMaterial = aMaterial;
}
