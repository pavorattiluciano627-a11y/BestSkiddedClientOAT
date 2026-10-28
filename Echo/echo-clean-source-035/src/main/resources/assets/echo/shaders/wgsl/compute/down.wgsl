@group(0) @binding(0) var srcTex : texture_2d<f32>;
@group(0) @binding(1) var smp : sampler;
@group(0) @binding(2) var dstTex : texture_storage_2d<rgba8unorm, write>;

struct PushConstants {
    offset : f32,
    srcYUp : f32,
}

var<push_constant> pc : PushConstants;

@compute @workgroup_size(8, 8, 1)
fn cs_main(@builtin(global_invocation_id) id : vec3<u32>) {
    let dstSize = textureDimensions(dstTex);
    if (id.x >= dstSize.x || id.y >= dstSize.y) {
        return;
    }

    var uv = (vec2<f32>(id.xy) + 0.5) / vec2<f32>(dstSize);
    uv = select(uv, vec2(uv.x, 1.0 - uv.y), pc.srcYUp > 0.5);

    let srcSize = vec2<f32>(textureDimensions(srcTex));
    let texelSize = 1.0 / srcSize;
    let halfPixel = texelSize / 2.0;

    var sum = textureSampleLevel(srcTex, smp, uv, 0.0) * 4.0;

    sum += textureSampleLevel(srcTex, smp, uv - halfPixel * pc.offset, 0.0);
    sum += textureSampleLevel(srcTex, smp, uv + halfPixel * pc.offset, 0.0);
    sum += textureSampleLevel(srcTex, smp, uv + vec2<f32>(halfPixel.x, -halfPixel.y) * pc.offset, 0.0);
    sum += textureSampleLevel(srcTex, smp, uv - vec2<f32>(halfPixel.x, -halfPixel.y) * pc.offset, 0.0);

    textureStore(dstTex, vec2<i32>(id.xy), sum / 8.0);
}
