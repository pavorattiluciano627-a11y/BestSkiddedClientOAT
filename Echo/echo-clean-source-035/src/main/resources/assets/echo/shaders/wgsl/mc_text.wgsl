@group(0) @binding(0) var<uniform> cam : mat4x4<f32>;
@group(0) @binding(1) var tex : texture_2d<f32>;
@group(0) @binding(2) var smp : sampler;

struct VsOutput {
    @builtin(position) position : vec4<f32>,
    @location(0) uv : vec2<f32>,
    @location(1) color : vec4<f32>,
    @location(2) @interpolate(flat) flags : i32,
}

const QUAD = array<vec2<f32>, 4>(
    vec2(0.0, 0.0),
    vec2(1.0, 0.0),
    vec2(0.0, 1.0),
    vec2(1.0, 1.0)
);

fn get_color(c : i32) -> vec4<f32> {
    let u = bitcast<u32>(c);
    return vec4(
        f32((u >> 16u) & 0xFFu) / 255.0,
        f32((u >> 8u) & 0xFFu) / 255.0,
        f32(u & 0xFFu) / 255.0,
        f32((u >> 24u) & 0xFFu) / 255.0
    );
}

@vertex
fn vs_main(
    @builtin(vertex_index) id : u32,
    @location(0) bounds : vec4<f32>,
    @location(1) uv : vec4<f32>,
    @location(2) extra : vec2<i32>,
    @location(3) z : f32,
) -> VsOutput {
    let corner = QUAD[id];

    var out : VsOutput;
    out.position = cam * vec4(bounds.xy + corner * bounds.zw, 0.0, 1.0);
    out.position.z = z;
    out.uv = mix(uv.xy, uv.zw, corner);
    out.color = get_color(extra.x);
    out.flags = extra.y;
    return out;
}

@fragment
fn fs_main(in : VsOutput) -> @location(0) vec4<f32> {
    var s = textureSample(tex, smp, in.uv);
    if ((in.flags & 1) != 0) {
        s = vec4(1.0, 1.0, 1.0, s.x);
    }
    let color = s * in.color;
    if (color.a <= 0.01) {
        discard;
    }
    return color;
}
