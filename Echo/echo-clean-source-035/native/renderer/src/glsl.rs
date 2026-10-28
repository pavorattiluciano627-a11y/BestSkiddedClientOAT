//! Custom DTO recovered from emitGlsl RVA 0x1000, especially 0x5181..0x5c9b.
use naga::back::glsl;
use naga::valid::ModuleInfo;
use naga::{Module, ScalarKind, ShaderStage, TypeInner};
use serde::Serialize;
use std::collections::HashMap;

#[derive(Serialize)]
struct TextureMapping {
    texture_binding: u32,
    sampler_binding: Option<u32>,
}

#[derive(Serialize)]
struct PushConstantItem {
    access_path: String,
    offset: u32,
    kind: &'static str,
    scalar_kind: &'static str,
    components: u32,
    columns: u32,
}

#[derive(Serialize)]
struct Reflection {
    texture_mapping: HashMap<String, TextureMapping>,
    push_constant_items: Vec<PushConstantItem>,
}

#[derive(Serialize)]
struct Output {
    source: String,
    reflection: Reflection,
}

fn scalar_name(kind: ScalarKind) -> &'static str {
    // The original six-entry lookup at RVA 0x11dfa8/0x11dfd8 folds abstract kinds.
    match kind {
        ScalarKind::Sint | ScalarKind::AbstractInt => "Sint",
        ScalarKind::Uint => "Uint",
        ScalarKind::Float | ScalarKind::AbstractFloat => "Float",
        ScalarKind::Bool => "Bool",
    }
}

fn reflect(module: &Module, reflection: glsl::ReflectionInfo) -> Reflection {
    let texture_mapping = reflection
        .texture_mapping
        .into_iter()
        .map(|(name, mapping)| {
            // Bind group numbers and arena indices are deliberately not returned.
            // Missing image binding falls back to zero (VA 0x180005203..0x180005215).
            let texture_binding = module.global_variables[mapping.texture]
                .binding
                .as_ref()
                .map_or(0, |binding| binding.binding);
            let sampler_binding = mapping.sampler.and_then(|sampler| {
                module.global_variables[sampler]
                    .binding
                    .as_ref()
                    .map(|binding| binding.binding)
            });
            (
                name,
                TextureMapping {
                    texture_binding,
                    sampler_binding,
                },
            )
        })
        .collect();
    let push_constant_items = reflection
        .push_constant_items
        .into_iter()
        .map(|item| {
            let (kind, scalar, components, columns) = match module.types[item.ty].inner {
                TypeInner::Scalar(scalar) => ("Scalar", scalar, 1, 1),
                TypeInner::Vector { size, scalar } => ("Vector", scalar, size as u32, 1),
                TypeInner::Matrix {
                    columns,
                    rows,
                    scalar,
                } => ("Matrix", scalar, rows as u32, columns as u32),
                _ => unreachable!("push constant item type must be scalar/vector/matrix"),
            };
            PushConstantItem {
                access_path: item.access_path,
                offset: item.offset,
                kind,
                scalar_kind: scalar_name(scalar.kind),
                components,
                columns,
            }
        })
        .collect();
    Reflection {
        texture_mapping,
        push_constant_items,
    }
}

pub(crate) fn emit(
    module: &Module,
    info: &ModuleInfo,
    version: u16,
    stage: ShaderStage,
    entry_point: String,
) -> Result<String, String> {
    // Do not use Options::default(): the original explicitly zeroes writer flags
    // and workgroup initialization at VA 0x180001093 and 0x1800010ad.
    let options = glsl::Options {
        version: glsl::Version::Desktop(version),
        writer_flags: glsl::WriterFlags::empty(),
        binding_map: Default::default(),
        zero_initialize_workgroup_memory: false,
    };
    let pipeline = glsl::PipelineOptions {
        shader_stage: stage,
        entry_point,
        multiview: None,
    };
    let mut source = String::new();
    let mut writer = glsl::Writer::new(
        &mut source,
        module,
        info,
        &options,
        &pipeline,
        naga::proc::BoundsCheckPolicies::default(),
    )
    .map_err(|error| format!("{error:?}"))?;
    let reflection = writer.write().map_err(|error| format!("{error:?}"))?;
    serde_json::to_string(&Output {
        source,
        reflection: reflect(module, reflection),
    })
    .map_err(|error| error.to_string())
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn reflects_unbound_resources_using_original_fallbacks() {
        // A valid WGSL handle type without a binding is intentionally not validated:
        // this tests the DTO's actual fallback branch independently of the backend.
        let module =
            naga::front::wgsl::parse_str("var image: texture_2d<f32>; var image_sampler: sampler;")
                .unwrap();
        let globals: Vec<_> = module
            .global_variables
            .iter()
            .map(|(handle, _)| handle)
            .collect();
        let raw = glsl::ReflectionInfo {
            texture_mapping: [(
                "unbound".into(),
                glsl::TextureMapping {
                    texture: globals[0],
                    sampler: Some(globals[1]),
                },
            )]
            .into_iter()
            .collect(),
            uniforms: Default::default(),
            varying: Default::default(),
            push_constant_items: Vec::new(),
            clip_distance_count: 0,
        };
        let dto = reflect(&module, raw);
        assert_eq!(dto.texture_mapping["unbound"].texture_binding, 0);
        assert_eq!(dto.texture_mapping["unbound"].sampler_binding, None);
    }

    #[test]
    fn abstract_scalar_labels_match_dll_table() {
        assert_eq!(scalar_name(ScalarKind::AbstractInt), "Sint");
        assert_eq!(scalar_name(ScalarKind::AbstractFloat), "Float");
    }
}
