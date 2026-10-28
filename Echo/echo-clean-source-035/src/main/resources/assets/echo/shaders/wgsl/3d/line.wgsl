struct Camera {
    view : mat4x4<f32>,
    proj : mat4x4<f32>,
    pos : vec3<f32>,
    size : vec2<f32>
}

@group(0) @binding(0) var<uniform> cam : Camera;

const WIDTH = 2.0;
const FEATHER = 1.0;
const NEAR = 1e-4;

struct VsOutput {
    @builtin(position) position : vec4<f32>,
    @location(0) color : vec4<f32>,
    @location(1) offset : f32
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
    @location(0) start : vec3<f32>,
    @location(1) end : vec3<f32>,
    @location(2) color : i32
) -> VsOutput {
    var out : VsOutput;
    out.color = get_color(color);
    out.offset = 0.0;

    var a = cam.proj * cam.view * vec4(start, 1.0);
    var b = cam.proj * cam.view * vec4(end, 1.0);
    if (a.w < NEAR && b.w < NEAR) {
        out.position = vec4(2.0, 2.0, 2.0, 1.0);
        return out;
    }
    if (a.w < NEAR) {
        a = mix(a, b, (NEAR - a.w) / (b.w - a.w));
    } else if (b.w < NEAR) {
        b = mix(b, a, (NEAR - b.w) / (a.w - b.w));
    }

    let na = a.xyz / a.w;
    let nb = b.xyz / b.w;
    let half_size = cam.size * 0.5;

    var dir = nb.xy * half_size - na.xy * half_size;
    let len = length(dir);
    if (len < 1e-6) {
        dir = vec2(1.0, 0.0);
    } else {
        dir /= len;
    }

    let normal = vec2(-dir.y, dir.x);
    let reach = WIDTH * 0.5 + FEATHER;
    let side = select(-1.0, 1.0, (id & 1u) == 1u);
    let n = select(na, nb, (id >> 1u) == 1u);

    out.position = vec4(n.xy + normal * side * reach / half_size, n.z, 1.0);
    out.offset = side * reach;
    return out;
}

@fragment
fn fs_main(in : VsOutput) -> @location(0) vec4<f32> {
    let cov = clamp(WIDTH * 0.5 - abs(in.offset) + 0.5, 0.0, 1.0);
    return vec4(in.color.rgb, in.color.a * cov);
}
