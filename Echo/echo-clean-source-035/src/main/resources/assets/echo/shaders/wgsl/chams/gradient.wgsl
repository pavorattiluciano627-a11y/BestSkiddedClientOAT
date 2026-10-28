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
    srcYUp : f32
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

const TWO_PI = 6.28318530718;

@vertex
fn vs_main(@builtin(vertex_index) id : u32) -> VsOutput {
    var out : VsOutput;
    out.position = vec4(CLIP_QUAD[id], 0.0, 1.0);
    out.uv = UV_QUAD[id];
    return out;
}

@fragment
fn fs_main(in : VsOutput) -> @location(0) vec4<f32> {
    let uv = select(in.uv, vec2(in.uv.x, 1.0 - in.uv.y), pc.srcYUp > 0.5);
    let oneTexel = 1.0 / vec2<f32>(textureDimensions(tex));
    let center = textureSample(tex, smp, uv);

    let radius = max(cfg.lineWidth, 1.0);
    var neighbor = 0.0;
    var neighborColor = vec3(0.0);
    for (var i = 0; i < 8; i++) {
        let angle = TWO_PI * f32(i) / 8.0;
        let dir = vec2(cos(angle), sin(angle)) * oneTexel;
        let s1 = textureSample(tex, smp, uv + dir * radius);
        let s2 = textureSample(tex, smp, uv + dir * (radius * 0.5));
        if (s1.a > neighbor) {
            neighbor = s1.a;
            neighborColor = s1.rgb;
        }
        if (s2.a > neighbor) {
            neighbor = s2.a;
            neighborColor = s2.rgb;
        }
    }

    let outline = (1.0 - center.a) * neighbor * cfg.outlineAlpha;
    let fill = center.a * cfg.fillAlpha;
    let alpha = max(outline, fill);
    if (alpha <= 0.001) {
        return vec4(0.0);
    }

    let phase = (in.uv.x + in.uv.y) * 0.5 * cfg.scale - cfg.animTime;
    let t = 0.5 + 0.5 * sin(TWO_PI * phase);
    var grad = mix(cfg.color1, cfg.color2, t);
    if (cfg.useEntityColor > 0.5) {
        grad = vec4(select(neighborColor, center.rgb, center.a > 0.0), 1.0);
    }

    return vec4(grad.rgb, alpha * grad.a);
}
