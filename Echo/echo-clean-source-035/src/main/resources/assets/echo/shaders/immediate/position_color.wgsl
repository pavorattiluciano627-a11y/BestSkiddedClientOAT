@group(0) @binding(0) var<uniform> cam : mat4x4<f32>;

struct VsInput {
    @location(0) position : vec4<f32>,
    @location(1) color : i32
}

struct VsOutput {
    @builtin(position) position : vec4<f32>,
    @location(0) color : vec4<f32>,
}

@vertex
fn vs_main(input : VsInput) -> VsOutput {
    let c = unpack4x8unorm(bitcast<u32>(input.color));

    var out : VsOutput;
    out.position = cam * vec4(input.position.xy, 0.0, 1.0);
    out.position.z = input.position.z;
    out.color = vec4(c.z, c.y, c.x, c.w);
    return out;
}

@fragment
fn fs_main(in : VsOutput) -> @location(0) vec4<f32> {
    return in.color;
}
