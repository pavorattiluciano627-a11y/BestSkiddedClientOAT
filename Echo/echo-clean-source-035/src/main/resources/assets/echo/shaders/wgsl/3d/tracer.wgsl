struct Camera {
    view : mat4x4<f32>,
    proj : mat4x4<f32>,
    pos : vec3<f32>,
    size : vec2<f32>
}

@group(0) @binding(0) var<uniform> cam : Camera;

const WIDTH = 1.25;
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
    @location(0) pos : vec3<f32>,
    @location(1) color : i32
) -> VsOutput {
    var clip = cam.proj * cam.view * vec4(pos, 1.0);
    if (clip.w < NEAR) {
        var dir = clip.xy;
        let m = max(abs(dir.x), abs(dir.y));
        if (m < 1e-6) {
            dir = vec2(0.0, 1.0);
        } else {
            dir /= m;
        }
        clip = vec4(dir, 0.0, 1.0);
    }

    let na = clip.xyz / clip.w;
    let nb = vec3(0.0, 0.0, 0.0);
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

    var out : VsOutput;
    out.color = get_color(color);
    out.position = vec4(n.xy + normal * side * reach / half_size, n.z, 1.0);
    out.offset = side * reach;
    return out;
}

@fragment
fn fs_main(in : VsOutput) -> @location(0) vec4<f32> {
    let cov = clamp(WIDTH * 0.5 - abs(in.offset) + 0.5, 0.0, 1.0);
    return vec4(in.color.rgb, in.color.a * cov);
}
