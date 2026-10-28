@group(0) @binding(0) var<uniform> cam : mat4x4<f32>;

const KIND_RECT = 0;
const KIND_SHADOW = 1;
const KIND_HUE = 2;
const KIND_SB = 3;
const KIND_ALPHA = 4;

struct VsOutput {
    @builtin(position) position : vec4<f32>,
    @location(0) uv : vec2<f32>,
    @location(1) color : vec4<f32>,
    @location(2) size : vec2<f32>,
    @location(3) radius : vec4<f32>,
    @location(4) data : vec4<f32>,
    @location(5) border_color : vec4<f32>,
    @location(6) @interpolate(flat) kind : i32,
}

const QUAD = array<vec2<f32>, 4>(
    vec2(0.0, 0.0),
    vec2(1.0, 0.0),
    vec2(0.0, 1.0),
    vec2(1.0, 1.0)
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
    @location(0) rect : vec4<f32>,
    @location(1) radius : vec4<f32>,
    @location(2) color : vec4<i32>,
    @location(3) data : vec4<f32>,
    @location(4) extra : vec2<i32>,
) -> VsOutput {
    let corner = QUAD[id];
    let kind = extra.x;

    var pad = 0.0;
    if (kind == KIND_SHADOW) {
        pad = max(data.y, 0.3) * 3.0;
    }
    let center = rect.xy + rect.zw * 0.5;
    let local = center + (corner - vec2(0.5)) * (rect.zw + vec2(pad * 2.0));

    var cols = array<i32, 4>(color.x, color.y, color.z, color.w);

    var out : VsOutput;
    out.position = cam * vec4(local, 0.0, 1.0);
    out.position.z = data.x;
    out.uv = corner;
    out.color = get_color(cols[id]);
    out.size = rect.zw;
    out.radius = radius;
    out.data = data;
    out.border_color = get_color(extra.y);
    out.kind = kind;
    return out;
}

fn sdf(p : vec2<f32>, b : vec2<f32>, radius : vec4<f32>) -> f32 {
    let pair = select(radius.yw, radius.xz, p.x > 0.0);
    let r = select(pair.y, pair.x, p.y > 0.0);
    let q = abs(p) - b + r;
    return min(max(q.x, q.y), 0.0) + length(max(q, vec2(0.0))) - r;
}

fn gaussian(x : f32, sigma : f32) -> f32 {
    let pi = 3.141592653589793;
    return exp(-(x * x) / (2.0 * sigma * sigma)) / (sqrt(2.0 * pi) * sigma);
}

fn erf(x : vec2<f32>) -> vec2<f32> {
    let s = sign(x);
    let a = abs(x);
    var v = 1.0 + (0.278393 + (0.230389 + 0.078108 * (a * a)) * a) * a;
    v *= v;
    return s - s / (v * v);
}

fn shadow_x(x : f32, y : f32, sigma : f32, corner : f32, half_size : vec2<f32>) -> f32 {
    let delta = min(half_size.y - corner - abs(y), 0.0);
    let curved = half_size.x - corner + sqrt(max(0.0, corner * corner - delta * delta));
    let integral = 0.5 + 0.5 * erf((vec2(x) + vec2(-curved, curved)) * (sqrt(0.5) / sigma));
    return integral.y - integral.x;
}

fn hsv2rgb(c : vec3<f32>) -> vec3<f32> {
    let K = vec4(1.0, 2.0 / 3.0, 1.0 / 3.0, 3.0);
    let p = abs(fract(vec3(c.x) + K.xyz) * 6.0 - vec3(K.w));
    return c.z * mix(vec3(K.x), clamp(p - vec3(K.x), vec3(0.0), vec3(1.0)), c.y);
}

fn fill_rect(in : VsOutput) -> vec4<f32> {
    let half_size = in.size * 0.5;
    let local_pos = half_size - (in.uv * in.size);
    let dist = sdf(local_pos, half_size, in.radius);
    let gradient = vec2(dpdx(dist), dpdy(dist));
    let edge = max(length(gradient), 1e-4);
    let outer_coverage = clamp(0.5 - dist / edge, 0.0, 1.0);
    let border_width = in.data.y;
    if (border_width > 0.0) {
        let inner_coverage = clamp(0.5 - (dist + border_width) / edge, 0.0, 1.0);
        let border_coverage = max(outer_coverage - inner_coverage, 0.0);
        let premul = in.border_color.rgb * in.border_color.a * border_coverage
            + in.color.rgb * in.color.a * inner_coverage;
        let alpha = in.border_color.a * border_coverage + in.color.a * inner_coverage;
        return vec4(premul, alpha);
    }
    let alpha = in.color.a * outer_coverage;
    return vec4(in.color.rgb * alpha, alpha);
}

fn fill_shadow(in : VsOutput) -> vec4<f32> {
    let half_size = in.size * 0.5;
    let sigma = max(in.data.y, 0.3);
    let corner = min(in.radius.x, min(half_size.x, half_size.y));
    let pad = sigma * 3.0;
    let point = (in.uv - vec2(0.5)) * (in.size + vec2(pad * 2.0));

    let low = point.y - half_size.y;
    let high = point.y + half_size.y;
    let start = clamp(-3.0 * sigma, low, high);
    let end = clamp(3.0 * sigma, low, high);

    let step = (end - start) / 4.0;
    var y = start + step * 0.5;
    var value = 0.0;
    for (var i = 0; i < 4; i++) {
        value += shadow_x(point.x, point.y - y, sigma, corner, half_size) * gaussian(y, sigma) * step;
        y += step;
    }
    let alpha = in.color.a * value;
    return vec4(in.color.rgb * alpha, alpha);
}

fn rounded_coverage(in : VsOutput) -> f32 {
    if (in.radius.x <= 0.0 && in.radius.y <= 0.0
            && in.radius.z <= 0.0 && in.radius.w <= 0.0) {
        return 1.0;
    }
    let half_size = in.size * 0.5;
    let local_pos = half_size - (in.uv * in.size);
    let dist = sdf(local_pos, half_size, in.radius);
    let edge = max(length(vec2(dpdx(dist), dpdy(dist))), 1e-4);
    return clamp(0.5 - dist / edge, 0.0, 1.0);
}

@fragment
fn fs_main(in : VsOutput) -> @location(0) vec4<f32> {
    var color : vec4<f32>;
    switch (in.kind) {
        case KIND_RECT: {
            color = fill_rect(in);
        }
        case KIND_SHADOW: {
            color = fill_shadow(in);
        }
        case KIND_HUE: {
            let alpha = in.data.z;
            color = vec4(hsv2rgb(vec3(in.uv.x, 1.0, 1.0)) * alpha, alpha);
        }
        case KIND_SB: {
            let alpha = in.data.z;
            color = vec4(hsv2rgb(vec3(in.data.y, in.uv.x, 1.0 - in.uv.y)) * alpha, alpha);
        }
        default: {
            let alpha = in.uv.x * in.data.z;
            color = vec4(in.color.rgb * alpha, alpha);
        }
    }


    if (in.kind == KIND_HUE || in.kind == KIND_SB || in.kind == KIND_ALPHA) {
        color *= rounded_coverage(in);
    }

    if (color.a <= 0.003) {
        discard;
    }
    return color;
}
