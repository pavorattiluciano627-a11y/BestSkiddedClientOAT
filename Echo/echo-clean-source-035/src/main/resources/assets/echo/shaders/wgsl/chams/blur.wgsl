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

struct PushConstants {
    dir : vec2<f32>
}

@group(0) @binding(0) var<uniform> cfg : Config;
@group(0) @binding(1) var tex : texture_2d<f32>;
@group(0) @binding(2) var smp : sampler;

var<push_constant> pc : PushConstants;

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
    let oneTexel = 1.0 / vec2<f32>(textureDimensions(tex));
    let radius = clamp(cfg.glowRadius, 1.0, 32.0);
    let sigma = radius * 0.5;
    let invTwoSigmaSq = 1.0 / (2.0 * sigma * sigma);

    var colorSum = vec3(0.0);
    var alphaSum = 0.0;
    var weightSum = 0.0;

    for (var x = -32.0; x <= 32.0; x += 1.0) {
        if (abs(x) > radius) {
            continue;
        }
        let w = exp(-x * x * invTwoSigmaSq);
        let s = textureSample(tex, smp, in.uv + pc.dir * oneTexel * x);
        colorSum += s.rgb * s.a * w;
        alphaSum += s.a * w;
        weightSum += w;
    }

    let alpha = alphaSum / weightSum;
    let color = select(vec3(0.0), colorSum / alphaSum, alphaSum > 1.0e-5);
    return vec4(color, alpha);
}
