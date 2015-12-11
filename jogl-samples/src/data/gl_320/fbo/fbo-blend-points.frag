#version 150 core

precision highp float;
precision highp int;
layout(std140, column_major) uniform;

in vec2 gl_PointCoord;

in Block
{
    vec4 color;
} inBlock;

out vec4 color;
/*
vec3 detail_rgbToSrgb(in vec3 ColorRGB, in float GammaCorrection)
{
        vec3 ClampedColorRGB = clamp(ColorRGB, 0.0, 1.0);

        return mix(
                pow(ClampedColorRGB, vec3(GammaCorrection)) * 1.055 - 0.055,
                ClampedColorRGB * 12.92,
                lessThan(ClampedColorRGB, vec3(0.0031308)));
}

vec4 convertRgbToSrgb(in vec4 ColorRGB)
{
        return vec4(detail_rgbToSrgb(ColorRGB.rgb, 0.41666), ColorRGB.a);
}*/

void main()
{
    vec4 colorRgb = vec4(inBlock.color.rgb, (1.0 - smoothstep(0.0, 1.0, length((gl_PointCoord - 0.5) * 2.0))) * 1.0);

    color = colorRgb;//convertRgbToSrgb(ColorRGB);
}
