struct Config {
    lineWidth : f32,
    scale : f32,
    fillAlpha : f32,
    outlineAlpha : f32,
    useEntityColor : f32,
    animTime : f32,
    glowRadius : f32,
    glowIntensity : f32,
    color1 : vec4<f32>,
    color2 : vec4<f32>
}

@group(0) @binding(0) var<uniform> cfg : Config;
@group(0) @binding(1) var outlineTex : texture_2d<f32>;
@group(0) @binding(2) var glowTex : texture_2d<f32>;
@group(0) @binding(3) var smp : sampler;

struct VsOutput {
    @builtin(position) position : vec4<f32>,
    @location(0) uv : vec2<f32>
}

const CLIP_QUAD = array<vec2<f32>, 4>(
    vec2(-1.0,  1.0),
    vec2( 1.0,  1.0),
    vec2(-1.0, -1.0),
    vec2( 1.0, -1.0)
);

const UV_QUAD = array<vec2<f32>, 4>(
    vec2(0.0, 0.0),
    vec2(1.0, 0.0),
    vec2(0.0, 1.0),
    vec2(1.0, 1.0)
);

@vertex
fn vs_main(@builtin(vertex_index) id : u32) -> VsOutput {
    var out : VsOutput;
    out.position = vec4(CLIP_QUAD[id], 0.0, 1.0);
    out.uv = UV_QUAD[id];
    return out;
}

@fragment
fn fs_main(in : VsOutput) -> @location(0) vec4<f32> {
    let outline = textureSample(outlineTex, smp, in.uv);
    let glow = textureSample(glowTex, smp, in.uv);

    let glowAlpha = pow(clamp(glow.a, 0.0, 1.0), 0.6) * cfg.glowIntensity;
    let alpha = outline.a + glowAlpha * (1.0 - outline.a);
    if (alpha <= 0.001) {
        return vec4(0.0);
    }

    let color = (outline.rgb * outline.a + glow.rgb * glowAlpha * (1.0 - outline.a)) / alpha;
    return vec4(color, alpha);
}
