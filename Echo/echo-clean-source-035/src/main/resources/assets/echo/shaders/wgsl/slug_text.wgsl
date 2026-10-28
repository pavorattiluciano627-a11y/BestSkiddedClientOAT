@group(0) @binding(0) var<uniform> cam : mat4x4<f32>;
@group(0) @binding(1) var curves : texture_2d<f32>;
@group(0) @binding(2) var band_table : texture_2d<f32>;
@group(0) @binding(3) var band_indices : texture_2d<f32>;

const BAND_COUNT : u32 = 16u;
const TEX_W : u32 = 2048u;
const STEM_DARKEN : f32 = 1.6;

struct VsOutput {
    @builtin(position) position : vec4<f32>,
    @location(0) local : vec2<f32>,
    @location(1) color : vec4<f32>,
    @location(2) @interpolate(flat) y_band_start : u32,
    @location(3) @interpolate(flat) x_band_start : u32,
}

const QUAD = array<vec2<f32>, 4>(
    vec2(0.0, 0.0),
    vec2(1.0, 0.0),
    vec2(0.0, 1.0),
    vec2(1.0, 1.0)
);

@vertex
fn vs_main(
    @builtin(vertex_index) id : u32,
    @location(0) pos : vec2<f32>,
    @location(1) size : vec2<f32>,
    @location(2) bands : vec2<i32>,
    @location(3) color : i32,
    @location(4) z : f32,
) -> VsOutput {
    let corner = QUAD[id];
    let xy = pos + corner * size;

    let c = bitcast<u32>(color);

    var out : VsOutput;
    out.position = cam * vec4(xy, 0.0, 1.0);
    out.position.z = z;
    out.local = corner;
    out.color = vec4(
        f32((c >> 16u) & 0xFFu) / 255.0,
        f32((c >> 8u) & 0xFFu) / 255.0,
        f32(c & 0xFFu) / 255.0,
        f32((c >> 24u) & 0xFFu) / 255.0
    );
    out.y_band_start = u32(bands.x);
    out.x_band_start = u32(bands.y);
    return out;
}

fn tc(i : u32) -> vec2<i32> {
    return vec2<i32>(i32(i % TEX_W), i32(i / TEX_W));
}

fn compute_coverage(inv : f32, p0 : vec2<f32>, p1 : vec2<f32>, p2 : vec2<f32>) -> f32 {
    var shift : u32 = 0u;
    if (p0.y > 0.0) { shift = shift + 2u; }
    if (p1.y > 0.0) { shift = shift + 4u; }
    if (p2.y > 0.0) { shift = shift + 8u; }
    let code = (0x2E74u >> shift) & 3u;
    if (code == 0u) {
        return 0.0;
    }

    let a = p0 - 2.0 * p1 + p2;
    let b = p0 - p1;
    let c = p0;

    var t0 : f32;
    var t1 : f32;
    if (abs(a.y) >= 1e-4) {
        let radical = b.y * b.y - a.y * c.y;
        if (radical < 0.0) {
            return 0.0;
        }
        let s = sqrt(radical);
        t0 = (b.y - s) / a.y;
        t1 = (b.y + s) / a.y;
    } else {
        let t = c.y / (2.0 * b.y);
        t0 = t;
        t1 = t;
    }

    var alpha = 0.0;
    if ((code & 1u) != 0u) {
        let x0 = (a.x * t0 - 2.0 * b.x) * t0 + c.x;
        alpha = alpha + clamp(x0 * inv + 0.5, 0.0, 1.0);
    }
    if ((code & 2u) != 0u) {
        let x1 = (a.x * t1 - 2.0 * b.x) * t1 + c.x;
        alpha = alpha - clamp(x1 * inv + 0.5, 0.0, 1.0);
    }
    return alpha;
}

fn coverage_horizontal(p : vec2<f32>, inv_x : f32, y_band_start : u32) -> f32 {
    let ly = clamp(p.y, 0.0, 1.0);
    let by = min(u32(ly * f32(BAND_COUNT)), BAND_COUNT - 1u);
    let band = vec2<u32>(textureLoad(band_table, tc(y_band_start + by), 0).xy);

    var cov = 0.0;
    for (var i : u32 = 0u; i < band.y; i = i + 1u) {
        let ci = u32(textureLoad(band_indices, tc(band.x + i), 0).x);
        let base = ci * 2u;
        let a = textureLoad(curves, tc(base), 0);
        let b = textureLoad(curves, tc(base + 1u), 0);
        let p0 = a.xy;
        let p1 = a.zw;
        let p2 = b.xy;
        if (max(max(p0.x, p1.x), p2.x) < p.x) {
            break;
        }
        cov = cov + compute_coverage(inv_x, p0 - p, p1 - p, p2 - p);
    }
    return cov;
}

fn coverage_vertical(p : vec2<f32>, inv_y : f32, x_band_start : u32) -> f32 {
    let lx = clamp(p.x, 0.0, 1.0);
    let bx = min(u32(lx * f32(BAND_COUNT)), BAND_COUNT - 1u);
    let band = vec2<u32>(textureLoad(band_table, tc(x_band_start + bx), 0).xy);

    var cov = 0.0;
    for (var i : u32 = 0u; i < band.y; i = i + 1u) {
        let ci = u32(textureLoad(band_indices, tc(band.x + i), 0).x);
        let base = ci * 2u;
        let a = textureLoad(curves, tc(base), 0);
        let b = textureLoad(curves, tc(base + 1u), 0);
        let p0 = a.xy;
        let p1 = a.zw;
        let p2 = b.xy;
        if (max(max(p0.y, p1.y), p2.y) < p.y) {
            break;
        }
        cov = cov + compute_coverage(inv_y, (p0 - p).yx, (p1 - p).yx, (p2 - p).yx);
    }
    return cov;
}

const PERP = array<f32, 3>(-0.3333, 0.0, 0.3333);

@fragment
fn fs_main(in : VsOutput) -> @location(0) vec4<f32> {
    let local = in.local;
    let fw = fwidth(local);
    let inv_x = 1.0 / max(fw.x, 1e-6);
    let inv_y = 1.0 / max(fw.y, 1e-6);

    var cov_x = 0.0;
    var cov_y = 0.0;
    for (var k = 0; k < 3; k = k + 1) {
        let hp = vec2(local.x, local.y + PERP[k] * fw.y);
        cov_x = cov_x + coverage_horizontal(hp, inv_x, in.y_band_start);
        let vp = vec2(local.x + PERP[k] * fw.x, local.y);
        cov_y = cov_y + coverage_vertical(vp, inv_y, in.x_band_start);
    }
    cov_x = cov_x / 3.0;
    cov_y = cov_y / 3.0;

    var coverage = clamp((abs(cov_x) + abs(cov_y)) * 0.5, 0.0, 1.0);
    coverage = pow(coverage, 1.0 / STEM_DARKEN);
    return vec4(in.color.rgb, coverage * in.color.a);
}
