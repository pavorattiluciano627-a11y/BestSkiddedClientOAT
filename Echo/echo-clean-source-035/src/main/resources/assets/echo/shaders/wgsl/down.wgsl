@group(0) @binding(0) var tex : texture_2d<f32>;
@group(0) @binding(1) var smp : sampler;

struct PushConstants {
    offset : f32,
    srcYUp : f32,
}

var<push_constant> pc : PushConstants;

struct VsOutput {
    @builtin(position) position : vec4<f32>,
    @location(0) uv : vec2<f32>,
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
    let size = vec2<f32>(textureDimensions(tex));
    let texelSize = 1.0 / size;
    let halfPixel = texelSize / 2.0;

    let uv = select(in.uv, vec2(in.uv.x, 1.0 - in.uv.y), pc.srcYUp > 0.5);
    var sum = textureSample(tex, smp, uv) * 4.0;

    sum += textureSample(tex, smp, uv - halfPixel * pc.offset);
    sum += textureSample(tex, smp, uv + halfPixel * pc.offset);
    sum += textureSample(tex, smp, uv + vec2<f32>(halfPixel.x, -halfPixel.y) * pc.offset);
    sum += textureSample(tex, smp, uv - vec2<f32>(halfPixel.x, -halfPixel.y) * pc.offset);

    return sum / 8.0;
}
