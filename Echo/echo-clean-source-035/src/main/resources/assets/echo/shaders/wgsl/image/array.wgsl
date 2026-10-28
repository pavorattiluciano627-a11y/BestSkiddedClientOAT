@group(0) @binding(0) var<uniform> cam : mat4x4<f32>;
@group(0) @binding(1) var textures : binding_array<texture_2d<f32>, 16>;
@group(0) @binding(2) var smp : sampler;

struct VsOutput {
    @builtin(position) position : vec4<f32>,
    @location(0) uv : vec2<f32>,
    @location(1) @interpolate(flat) tex_index : i32,
    @location(2) size : vec2<f32>,
    @location(3) @interpolate(flat) radius : f32,
    @location(4) @interpolate(flat) alpha : f32,
    @location(5) @interpolate(flat) tint : vec4<f32>,
    @location(6) @interpolate(flat) flags : i32,
    @location(7) tex_uv : vec2<f32>,
}

const QUAD = array<vec2<f32>, 4>(
    vec2(0.0, 0.0),
    vec2(1.0, 0.0),
    vec2(0.0, 1.0),
    vec2(1.0, 1.0)
);

@vertex
fn vs_main(
    @builtin(vertex_index) id : u32,
    @location(0) bounds : vec4<f32>,
    @location(1) tex_index : i32,
    @location(2) z : f32,
    @location(3) radius : f32,
    @location(4) alpha : f32,
    @location(5) tint : i32,
    @location(6) flags : i32,
    @location(7) uv_rect : vec4<f32>,
) -> VsOutput {
    let xy = bounds.xy + QUAD[id] * bounds.zw;

    var out : VsOutput;
    out.position = cam * vec4(xy, 0.0, 1.0);
    out.position.z = z;
    out.uv = QUAD[id];
    out.tex_uv = uv_rect.xy + QUAD[id] * uv_rect.zw;
    out.tex_index = tex_index;
    out.size = bounds.zw;
    out.radius = radius;
    out.alpha = alpha;
    let packed = bitcast<u32>(tint);
    out.tint = vec4(f32((packed >> 16u) & 0xFFu), f32((packed >> 8u) & 0xFFu),
        f32(packed & 0xFFu), f32((packed >> 24u) & 0xFFu)) / 255.0;
    out.flags = flags;
    return out;
}

fn sdf(p : vec2<f32>, b : vec2<f32>, r : f32) -> f32 {
    let q = abs(p) - b + r;
    return min(max(q.x, q.y), 0.0) + length(max(q, vec2(0.0))) - r;
}

@fragment
fn fs_main(in : VsOutput) -> @location(0) vec4<f32> {
    var a = 1.0;
    if (in.radius > 0.0) {
        let half_size = in.size * 0.5;
        let dist = sdf(half_size - in.uv * in.size, half_size, in.radius);
        let edge = max(length(vec2(dpdx(dist), dpdy(dist))), 1e-4);
        a = clamp(0.5 - dist / edge, 0.0, 1.0);
    }
    if (a <= 0.01) {
        discard;
    }
    let tex = textureSample(textures[in.tex_index], smp, in.tex_uv);
    let mask = (in.flags & 1) != 0;
    let rgb = select(tex.rgb, in.tint.rgb, mask);
    let tint_alpha = select(1.0, in.tint.a, mask);
    let scale = a * in.alpha * tint_alpha;
    if ((in.flags & 2) != 0) {
        return tex * scale;
    }
    let alpha = tex.a * scale;
    return vec4(rgb * alpha, alpha);
}
