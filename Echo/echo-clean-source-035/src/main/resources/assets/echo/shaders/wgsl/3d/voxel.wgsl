struct Camera {
    view : mat4x4<f32>,
    proj : mat4x4<f32>,
    pos : vec3<f32>
}

struct Push {
    rel : vec4<f32>
}

@group(0) @binding(0) var<uniform> cam : Camera;
@group(0) @binding(1) var<uniform> palette : array<vec4<u32>, 1024>;
var<push_constant> pc : Push;

struct VsOutput {
    @builtin(position) position : vec4<f32>,
    @location(0) color : vec4<f32>,
    @location(1) local_pos : vec3<f32>,
    @location(2) @interpolate(flat) axis : u32,
    @location(3) @interpolate(flat) outline : u32
}

const CORNERS = array<vec3<f32>, 4>(
    vec3(0.0, 0.0, 0.0),
    vec3(0.0, 0.0, 1.0),
    vec3(1.0, 0.0, 0.0),
    vec3(1.0, 0.0, 1.0)
);

const DIR_TO_AXIS = array<u32, 3>(1u, 0u, 2u);

fn get_color(c : u32) -> vec4<f32> {
    return vec4(
        f32((c >> 16u) & 0xFFu) / 255.0,
        f32((c >> 8u) & 0xFFu) / 255.0,
        f32(c & 0xFFu) / 255.0,
        f32((c >> 24u) & 0xFFu) / 255.0
    );
}

fn get_model(corner : u32, face : u32, w : f32, h : f32) -> vec3<f32> {
    var model = CORNERS[corner];
    if ((0x29u & (1u << face)) != 0u) {
        model = model.zyx;
    }
    model.x *= w;
    model.z *= h;
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
    return model;
}

@vertex
fn vs_main(
    @builtin(vertex_index) id : u32,
    @location(0) data : vec2<i32>
) -> VsOutput {
    let corner = id % 4u;

    let w0 = bitcast<u32>(data.x);
    let block = bitcast<u32>(data.y) & 0xFFFu;

    let x = f32(w0 & 0xFu);
    let y = f32((w0 >> 4u) & 0xFu);
    let z = f32((w0 >> 8u) & 0xFu);
    let w = f32((w0 >> 12u) & 0xFu) + 1.0;
    let h = f32((w0 >> 16u) & 0xFu) + 1.0;
    let face = (w0 >> 20u) & 0x7u;

    let local = get_model(corner, face, w, h) + vec3(x, y, z);

    let raw = palette[block >> 2u][block & 3u];

    var out : VsOutput;
    out.color = get_color(raw);
    out.local_pos = local;
    out.axis = DIR_TO_AXIS[face / 2u];
    out.outline = (raw >> 24u) & 1u;
    out.position = cam.proj * cam.view * vec4(pc.rel.xyz + local, 1.0);
    return out;
}

@fragment
fn fs_main(in : VsOutput) -> @location(0) vec4<f32> {
    let f = fract(in.local_pos);
    let d = fwidth(in.local_pos);
    var edge = smoothstep(vec3(0.0), d * 1.5, f) * smoothstep(vec3(0.0), d * 1.5, 1.0 - f);
    edge[in.axis] = 1.0;

    let is_outline = min(edge.x, min(edge.y, edge.z)) < 0.5;

    if (is_outline && in.outline != 0u) {
        return vec4(in.color.rgb, 1.0);
    }
    return vec4(in.color.rgb * in.color.a, in.color.a);
}
