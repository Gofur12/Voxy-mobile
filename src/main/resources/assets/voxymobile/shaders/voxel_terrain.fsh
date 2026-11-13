#version 320 es
precision highp float;
flat in float vMaterial;
layout(location = 0) out vec4 fragColor;
void main() {
    // Stage 28 heightfield LOD uses a stable terrain tint. Do not hash raw block IDs:
    // the previous pseudo-random palette produced bright pink/yellow/ red overlays.
    vec3 terrain = vec3(0.30, 0.48, 0.22);
    fragColor = vec4(terrain, 1.0);
}
