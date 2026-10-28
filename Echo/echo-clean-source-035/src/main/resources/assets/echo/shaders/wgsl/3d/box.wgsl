struct Camera {
    view : mat4x4<f32>,
    proj : mat4x4<f32>,
    pos : vec3<f32>
}

@group(0) @binding(0) var<uniform> cam : Camera;

struct VsOutput {
    @builtin(position) position : vec4<f32>,
    @location(0) color : vec4<f32>
}

const FACE = array<vec3<f32>, 6>(
    vec3(0.0, 0.0, 0.0),
    vec3(0.0, 0.0, 1.0),
    vec3(1.0, 0.0, 0.0),
    vec3(0.0, 0.0, 1.0),
    vec3(1.0, 0.0, 1.0),
    vec3(1.0, 0.0, 0.0)
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
    @location(1) size : vec3<f32>,
    @location(2) color : i32
) -> VsOutput {
    let face = id / 6u;
    var model = FACE[id % 6u];

    let winding = (0x29u & (1u << face)) != 0u;
    if (winding) {
        model = model.zyx;
    }

    if (face == 1u) {
        model.y += 1.0;
    } else if (face == 2u) {
        model = model.yxz;
    } else if (face == 3u) {
        model = model.yxz;
        model.x += 1.0;
    } else if (face == 4u) {
        model = model.xzy;
    } else if (face == 5u) {
        model = model.xzy;
        model.z += 1.0;
    }

    var out : VsOutput;
    out.color = get_color(color);
    out.position = cam.proj * cam.view * vec4(pos + model * size, 1.0);
    return out;
}

@fragment
fn fs_main(in : VsOutput) -> @location(0) vec4<f32> {
    return vec4(in.color.rgb * in.color.a, in.color.a);
}