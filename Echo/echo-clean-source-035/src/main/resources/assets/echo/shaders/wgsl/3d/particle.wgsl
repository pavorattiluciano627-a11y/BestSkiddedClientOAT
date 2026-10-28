struct Camera {
    view : mat4x4<f32>,
    proj : mat4x4<f32>,
    pos : vec3<f32>
}

@group(0) @binding(0) var<uniform> cam : Camera;
@group(0) @binding(1) var tex : texture_2d<f32>;
@group(0) @binding(2) var smp : sampler;

struct VsOutput {
    @builtin(position) position : vec4<f32>,
    @location(0) uv : vec2<f32>,
    @location(1) color : vec4<f32>
}

const CORNERS = array<vec2<f32>, 4>(
    vec2(-0.5, -0.5),
    vec2(0.5, -0.5),
    vec2(-0.5, 0.5),
    vec2(0.5, 0.5)
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
    @location(0) pos : vec3<f32>,
    @location(1) size : f32,
    @location(2) color : i32,
    @location(3) uv_rect : vec4<f32>,
    @location(4) rotation : f32
) -> VsOutput {
    let right = vec3(cam.view[0][0], cam.view[1][0], cam.view[2][0]);
    let up = vec3(cam.view[0][1], cam.view[1][1], cam.view[2][1]);

    let base = CORNERS[id % 4u];
    let c = base * size;
    let cos_r = cos(rotation);
    let sin_r = sin(rotation);
    let corner = vec2(c.x * cos_r - c.y * sin_r, c.x * sin_r + c.y * cos_r);
    let world = pos + right * corner.x + up * corner.y;

    var out : VsOutput;
    out.color = get_color(color);
    out.uv = uv_rect.xy + (base + 0.5) * (uv_rect.zw - uv_rect.xy);
    out.position = cam.proj * cam.view * vec4(world, 1.0);
    return out;
}

@fragment
fn fs_main(in : VsOutput) -> @location(0) vec4<f32> {
    let t = textureSample(tex, smp, in.uv);
    let a = t.a * in.color.a;
    return vec4(t.rgb * in.color.rgb * a, a);
}
