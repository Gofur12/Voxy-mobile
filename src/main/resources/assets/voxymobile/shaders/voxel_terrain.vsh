#version 320 es
precision highp float;
layout(location = 0) in vec3 aPosition;
layout(location = 1) in float aMaterial;
uniform mat4 uMvp;
uniform vec3 uChunkOffset;
flat out float vMaterial;
void main() {
    vec3 worldPosition = aPosition + uChunkOffset;
    gl_Position = uMvp * vec4(worldPosition, 1.0);
    vMaterial = aMaterial;
}
