struct Sky {
    mvp : mat4x4<f32>,
    colorA : vec4<f32>,
    colorB : vec4<f32>,
    params : vec4<f32>
}

@group(0) @binding(0) var<uniform> sky : Sky;
@group(0) @binding(1) var noiseTex : texture_3d<f32>;
@group(0) @binding(2) var noiseSmp : sampler;

const NOISE_SIZE = 64.0;

struct VsOutput {
    @builtin(position) position : vec4<f32>,
    @location(0) direction : vec3<f32>
}

const SIZE = 100.0;

const CORNERS = array<vec3<f32>, 24>(
    vec3(-1.0, -1.0, -1.0), vec3( 1.0, -1.0, -1.0), vec3( 1.0, -1.0,  1.0), vec3(-1.0, -1.0,  1.0),
    vec3(-1.0,  1.0, -1.0), vec3(-1.0,  1.0,  1.0), vec3( 1.0,  1.0,  1.0), vec3( 1.0,  1.0, -1.0),
    vec3(-1.0, -1.0, -1.0), vec3(-1.0,  1.0, -1.0), vec3( 1.0,  1.0, -1.0), vec3( 1.0, -1.0, -1.0),
    vec3(-1.0, -1.0,  1.0), vec3( 1.0, -1.0,  1.0), vec3( 1.0,  1.0,  1.0), vec3(-1.0,  1.0,  1.0),
    vec3(-1.0, -1.0, -1.0), vec3(-1.0, -1.0,  1.0), vec3(-1.0,  1.0,  1.0), vec3(-1.0,  1.0, -1.0),
    vec3( 1.0, -1.0, -1.0), vec3( 1.0,  1.0, -1.0), vec3( 1.0,  1.0,  1.0), vec3( 1.0, -1.0,  1.0)
);

fn hash1(seed : vec3<f32>) -> f32 {
    var p = fract(seed * 0.3183099 + vec3(0.1, 0.2, 0.3));
    p *= 17.0;
    return fract(p.x * p.y * p.z * (p.x + p.y + p.z));
}

fn hash3(p : vec3<f32>) -> vec3<f32> {
    return vec3(hash1(p), hash1(p + 31.7), hash1(p + 74.3));
}

fn noise3(p : vec3<f32>) -> f32 {
    return textureSampleLevel(noiseTex, noiseSmp, p / NOISE_SIZE, 0.0).r;
}

fn fbm(seed : vec3<f32>) -> f32 {
    var p = seed;
    var value = 0.0;
    var amplitude = 0.5;
    for (var i = 0; i < 5; i++) {
        value += amplitude * noise3(p);
        p = p * 2.03 + vec3(11.5, 7.2, 3.1);
        amplitude *= 0.5;
    }
    return value;
}

fn starField(nd : vec3<f32>, density : f32, time : f32) -> f32 {
    if (density <= 0.001) {
        return 0.0;
    }
    let p = nd * 90.0;
    let cell = floor(p);
    let f = fract(p) - 0.5;
    let rnd = hash3(cell);
    if (step(1.0 - density * 0.1, rnd.z) <= 0.0) {
        return 0.0;
    }
    let d = length(f - (rnd - 0.5) * 0.6);
    let star = pow(clamp(1.0 - d * 4.0, 0.0, 1.0), 6.0);
    let twinkle = 0.65 + 0.35 * sin(time * (2.0 + rnd.x * 3.0) + rnd.y * 12.0);
    return star * twinkle;
}

fn nebula(nd : vec3<f32>, colA : vec3<f32>, colB : vec3<f32>, time : f32) -> vec3<f32> {
    let drift = vec3(time * 0.008, time * 0.005, -time * 0.006);
    let n1 = fbm(nd * 2.4 + drift);
    let n2 = fbm(nd * 5.2 - drift * 1.7 + vec3(4.7));
    let cloud = smoothstep(0.38, 0.85, n1 * 0.65 + n2 * 0.45);
    let base = colB * 0.06;
    let clouds = mix(colB, colA, smoothstep(0.2, 0.9, n2));
    return base + clouds * cloud * 0.85;
}

fn aurora(nd : vec3<f32>, colA : vec3<f32>, colB : vec3<f32>, time : f32) -> vec3<f32> {
    let base = vec3(0.010, 0.015, 0.040);
    let alt = clamp(nd.y, 0.0, 1.0);
    var curtain = 0.0;
    for (var i = 0; i < 3; i++) {
        let fi = f32(i);
        let wave = fbm(vec3(nd.x * 2.5 + fi * 3.7, nd.z * 2.5 - fi * 1.9, time * 0.06 + fi));
        let center = 0.20 + 0.40 * wave + fi * 0.13;
        curtain += exp(-abs(alt - center) * (10.0 - fi * 2.0)) * (0.8 - fi * 0.22);
    }
    curtain *= smoothstep(0.0, 0.15, alt);
    let color = mix(colA, colB, clamp(alt * 1.8, 0.0, 1.0));
    return base + color * curtain * 0.9;
}

fn twilight(nd : vec3<f32>, colA : vec3<f32>, colB : vec3<f32>, time : f32) -> vec3<f32> {
    let h = clamp(nd.y * 0.5 + 0.5, 0.0, 1.0);
    var sky = mix(colB, colA, pow(h, 1.4));

    let uv = nd.xz / (abs(nd.y) + 0.35);
    let clouds = fbm(vec3(uv * 1.2 + vec2(time * 0.010, time * 0.004), time * 0.02));
    sky += colB * smoothstep(0.45, 0.80, clouds) * 0.30;

    let glowDir = normalize(vec3(-0.55, 0.18, -0.20));
    let glow = pow(max(dot(nd, glowDir), 0.0), 6.0);
    sky += vec3(0.90, 0.95, 0.75) * glow * 0.35;
    return sky;
}

@vertex
fn vs_main(@builtin(vertex_index) id : u32) -> VsOutput {
    let corner = CORNERS[id] * SIZE;

    var out : VsOutput;
    out.direction = corner;
    out.position = sky.mvp * vec4(corner, 1.0);
    return out;
}

@fragment
fn fs_main(in : VsOutput) -> @location(0) vec4<f32> {
    let nd = normalize(in.direction);
    let time = sky.params.x;
    let mode = i32(sky.params.y + 0.5);
    let colA = sky.colorA.rgb;
    let colB = sky.colorB.rgb;

    var color : vec3<f32>;
    if (mode == 1) {
        color = aurora(nd, colA, colB, time);
    } else if (mode == 2) {
        color = twilight(nd, colA, colB, time);
    } else {
        color = nebula(nd, colA, colB, time);
    }

    var stars = starField(nd, sky.colorB.a, time);
    if (mode == 2) {
        stars *= 0.25;
    }
    color += vec3(stars);

    return vec4(color * sky.colorA.a, 1.0);
}
