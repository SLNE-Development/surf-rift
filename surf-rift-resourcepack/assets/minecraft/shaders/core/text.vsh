#version 330

#if !defined(IS_GUI) && !defined(IS_SEE_THROUGH)
#moj_import <minecraft:fog.glsl>
#moj_import <minecraft:sample_lightmap.glsl>
#endif

#moj_import <minecraft:dynamictransforms.glsl>
#moj_import <minecraft:projection.glsl>

const float CD_OVERSHOOT    = 1.9;
const float CD_SLIDE_DIST   = 45.0;
const float CD_START_SCALE  = 2.2;
const float CD_START_ANGLE  = -14.0;
const float CD_FLASH        = 0.6;
const float CD_SHADOW_DARK  = 0.25;
const float CD_PIVOT_Y      = 26.0;
const vec3  CD_COLOR_NUM    = vec3(1.00, 1.00, 1.00);
const vec3  CD_COLOR_GO     = vec3(0.35, 1.00, 0.45);

in vec3 Position;
in vec4 Color;
in vec2 UV0;
#if !defined(IS_GUI) && !defined(IS_SEE_THROUGH)
in ivec2 UV2;
#endif

#if !defined(IS_GUI) && !defined(IS_SEE_THROUGH)
uniform sampler2D Sampler2;
out float sphericalVertexDistance;
out float cylindricalVertexDistance;
#endif

out vec4 vertexColor;
out vec2 texCoord0;
out float riftGrayscale;

float easeOutBack(float t) {
    float c1 = CD_OVERSHOOT;
    float c3 = c1 + 1.0;
    float u = t - 1.0;
    return 1.0 + c3 * u * u * u + c1 * u * u;
}

void main() {
    vec3 pos = Position;
    vec4 color = Color;
    vec4 clip = ProjMat * ModelViewMat * vec4(pos, 1.0);
    riftGrayscale = 0.0;

    ivec3 c = ivec3(round(Color.rgb * 255.0));
    int code = (c.r << 16) | (c.g << 8) | c.b;
    bool isText = code == 0xA3F13C || code == 0xA3F13D;
    bool isShadow = code == 0x5B2E08 || code == 0x5B2E09;

    if (code == 0x7E7E7F) {
        riftGrayscale = 1.0;
        color = vec4(1.0, 1.0, 1.0, Color.a);
    }

    if (isText || isShadow) {
        int palette = code & 1;
        float t = clamp(Color.a, 0.0, 1.0);
        float k = easeOutBack(t);

#ifdef IS_GUI
        vec2 ndc = clip.xy / clip.w;
        vec2 unit = vec2(ProjMat[0][0], abs(ProjMat[1][1]));
        vec2 pivot = vec2(0.0, CD_PIVOT_Y * unit.y);
        vec2 g = (ndc - pivot) / unit;

        float s = mix(CD_START_SCALE, 1.0, k);
        float a = radians(CD_START_ANGLE) * (1.0 - k);
        g = mat2(cos(a), sin(a), -sin(a), cos(a)) * (g * s);
        g.x -= CD_SLIDE_DIST * (1.0 - k);

        clip.xy = (pivot + g * unit) * clip.w;
#endif
        vec3 rgb = palette == 1 ? CD_COLOR_GO : CD_COLOR_NUM;
        rgb = mix(rgb, vec3(1.0), CD_FLASH * (1.0 - smoothstep(0.0, 0.6, t)));
        if (isShadow) rgb *= CD_SHADOW_DARK;
        color = vec4(rgb, smoothstep(0.0, 0.3, t));
    }

    gl_Position = clip;

#if !defined(IS_GUI) && !defined(IS_SEE_THROUGH)
    sphericalVertexDistance = fog_spherical_distance(pos);
    cylindricalVertexDistance = fog_cylindrical_distance(pos);
    vertexColor = color * sample_lightmap(Sampler2, UV2);
#else
    vertexColor = color;
#endif
    texCoord0 = UV0;
}
