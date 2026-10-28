@group(0) @binding(0) var<uniform> cam : mat4x4<f32>;
@group(0) @binding(1) var tex : texture_2d<f32>;
@group(0) @binding(2) var smp : sampler;

struct VsInput {
    @location(0) position : vec4<f32>,
    @location(1) color : i32,
    @location(2) uv : vec2<f32>
}

struct VsOutput {
    @builtin(position) position : vec4<f32>,
    @location(0) color : vec4<f32>,
    @location(1) uv : vec2<f32>,
}

@vertex
fn vs_main(
    input : VsInput
) -> VsOutput {
    let c = unpack4x8unorm(bitcast<u32>(input.color));

    var out : VsOutput;
    out.position = cam * input.position;
    out.color = vec4(c.z, c.y, c.x, c.w);
    out.uv = input.uv;
    return out;
}

@fragment
fn fs_main(in : VsOutput) -> @location(0) vec4<f32> {
    return textureSample(tex, smp, in.uv) * in.color;
}
