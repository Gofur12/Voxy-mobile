#version 320 es
precision highp float;
flat in float vMaterial;
layout(location = 0) out vec4 fragColor;
vec3 materialColor(float id) {
    float n = mod(id * 0.1031, 1.0);
    vec3 p = fract(vec3(n, n + 0.37, n + 0.71) * vec3(17.0, 31.0, 13.0));
    p += dot(p, p.yzx + 19.19);
    return 0.28 + 0.62 * fract((p.xxy + p.yzz) * p.zyx);
}
void main() {
    vec3 color = materialColor(vMaterial);
    fragColor = vec4(color, 1.0);
}
