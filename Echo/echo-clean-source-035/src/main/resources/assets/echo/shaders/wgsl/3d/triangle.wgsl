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
    @location(0) pos1 : vec3<f32>,
    @location(1) pos2 : vec3<f32>,
    @location(2) pos3 : vec3<f32>,
    @location(3) color1 : i32,
    @location(4) color2 : i32,
    @location(5) color3 : i32
) -> VsOutput {
    let pos = select(select(pos1, pos2, id == 1u), pos3, id == 2u);
    let color = select(select(color1, color2, id == 1u), color3, id == 2u);

    var out : VsOutput;
    out.color = get_color(color);
    out.position = cam.proj * cam.view * vec4(pos, 1.0);
    return out;
}

@fragment
fn fs_main(in : VsOutput) -> @location(0) vec4<f32> {
    return in.color;
}
