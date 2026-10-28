package compatcheck;

import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.*;
import java.util.zip.ZipFile;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import org.objectweb.asm.util.Textifier;
import org.objectweb.asm.util.TraceMethodVisitor;
import org.spongepowered.asm.launch.MixinBootstrap;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.Mixins;
import org.spongepowered.asm.mixin.transformer.IMixinTransformer;
import org.spongepowered.asm.service.MixinService;

/** Real Mixin transformations plus exhaustive member linkage; no game execution. */
public final class MinecraftLwjglCompatibilityAudit {
    private static final String RESIZE = "org/lwjgl/stb/STBImageResize";
    private static final String DIALOG = "org/lwjgl/util/tinyfd/TinyFileDialogs";
    private static final String RESIZE_DESC = "(JIIIJIIII)I";
    private static final String DIALOG_DESC = "(Ljava/lang/CharSequence;Ljava/lang/CharSequence;Ljava/lang/CharSequence;Ljava/lang/CharSequence;Z)Z";
    private static final Map<String, Integer> NAMED_TARGETS = Map.of(
            "net/minecraft/client/texture/NativeImage", 1,
            "net/minecraft/client/MinecraftClient", 2,
            "net/minecraft/client/util/NarratorManager", 1,
            "net/minecraft/client/util/Window", 1);

    private static Map<String, Integer> targetNames() throws Exception {
        String mappingFile = System.getProperty("echo.compatibility.targetMap");
        if (mappingFile == null) return NAMED_TARGETS;
        Properties mappings = new Properties();
        try (InputStream input = Files.newInputStream(Path.of(mappingFile))) { mappings.load(input); }
        if (!mappings.stringPropertyNames().equals(NAMED_TARGETS.keySet())) throw new AssertionError("Expected exactly four named target mappings");
        Map<String, Integer> result = new HashMap<>();
        for (var target : NAMED_TARGETS.entrySet()) result.put(mappings.getProperty(target.getKey()), target.getValue());
        if (result.size() != 4) throw new AssertionError("Target mapping collision");
        return result;
    }

    private record Reference(String owner, String name, String descriptor, boolean field, boolean isStatic) {}
    private static final Set<Reference> before = new HashSet<>();
    private static final Set<Reference> after = new HashSet<>();

    private static boolean removedCall(MethodInsnNode call) {
        return call.owner.equals(RESIZE) && call.name.equals("nstbir_resize_uint8") && call.desc.equals(RESIZE_DESC)
                || call.owner.equals(DIALOG) && call.name.equals("tinyfd_messageBox") && call.desc.equals(DIALOG_DESC);
    }

    private static ClassNode node(byte[] data) {
        ClassNode value = new ClassNode();
        // Mixin recomputes StackMapTable frames; compare executable instructions,
        // debug information, exception tables and maxima, not frame compression.
        new ClassReader(data).accept(value, ClassReader.SKIP_FRAMES);
        return value;
    }

    private static String methodText(MethodNode method) {
        Textifier text = new Textifier();
        method.accept(new TraceMethodVisitor(text));
        StringWriter output = new StringWriter();
        text.print(new PrintWriter(output));
        return output.toString();
    }

    private static void normalizeArgumentSpills(MethodNode method, MethodInsnNode call, int originalLocals) {
        Type[] arguments = Type.getArgumentTypes(call.desc);
        List<AbstractInsnNode> spill = new ArrayList<>();
        AbstractInsnNode cursor = call.getPrevious();
        for (int i = 0; i < arguments.length * 2; i++) {
            spill.add(cursor);
            cursor = cursor.getPrevious();
        }
        Collections.reverse(spill);
        int[] slots = new int[arguments.length];
        int next = -1;
        for (int i = 0; i < arguments.length; i++) {
            AbstractInsnNode instruction = spill.get(arguments.length + i);
            if (!(instruction instanceof VarInsnNode load) || load.getOpcode() != arguments[i].getOpcode(Opcodes.ILOAD)
                    || load.var < originalLocals || next >= 0 && load.var != next) {
                throw new AssertionError("Redirect argument reload changed: " + method.name);
            }
            slots[i] = load.var;
            next = load.var + arguments[i].getSize();
        }
        for (int i = 0; i < arguments.length; i++) {
            int argument = arguments.length - 1 - i;
            AbstractInsnNode instruction = spill.get(i);
            if (!(instruction instanceof VarInsnNode store) || store.getOpcode() != arguments[argument].getOpcode(Opcodes.ISTORE)
                    || store.var != slots[argument]) throw new AssertionError("Redirect argument spill changed: " + method.name);
        }
        // Only exact fresh-local reverse-store / forward-load identities are
        // discarded in this comparison copy. No actual transformed bytes change.
        spill.forEach(method.instructions::remove);
        method.maxLocals = originalLocals;
    }

    private static void proveOnlyRedirects(byte[] original, byte[] transformed, int expected) throws Exception {
        ClassNode oldClass = node(original), newClass = node(transformed);
        Map<String, MethodNode> merged = new HashMap<>();
        Set<String> originalMethods = new HashSet<>();
        oldClass.methods.forEach(method -> originalMethods.add(method.name + method.desc));
        for (MethodNode method : newClass.methods) if (!originalMethods.contains(method.name + method.desc)) {
            merged.put(method.name + method.desc, method);
        }
        if (merged.size() != 1) throw new AssertionError("Expected one merged redirect handler: " + newClass.name + merged.keySet());
        int calls = 0, forwards = 0;
        for (MethodNode method : merged.values()) for (AbstractInsnNode insn : method.instructions) {
            if (insn instanceof MethodInsnNode call && call.owner.startsWith("dev/echo/compat/lwjgl/Legacy")) {
                boolean resize = method.desc.equals(RESIZE_DESC);
                if (!call.owner.equals(resize ? "dev/echo/compat/lwjgl/LegacyImageResize" : "dev/echo/compat/lwjgl/LegacyTinyFileDialogs")
                        || !call.name.equals(resize ? "resizeUint8" : "messageBox")
                        || !call.desc.equals(resize ? RESIZE_DESC : DIALOG_DESC)) {
                    throw new AssertionError("Wrong adapter descriptor " + call.owner + call.name + call.desc);
                }
                forwards++;
            }
        }
        if (forwards != 1) throw new AssertionError("Handler must forward once");
        for (MethodNode oldMethod : oldClass.methods) {
            MethodNode actual = newClass.methods.stream().filter(m -> m.name.equals(oldMethod.name) && m.desc.equals(oldMethod.desc)).findFirst().orElseThrow();
            // Normalize ONLY the real Mixin-inserted invocation for comparison.
            // This node is never executed, exported, or used as an application artifact.
            for (AbstractInsnNode insn : actual.instructions) if (insn instanceof MethodInsnNode call
                    && call.owner.equals(oldClass.name) && merged.containsKey(call.name + call.desc)) {
                boolean resize = call.desc.equals(RESIZE_DESC);
                normalizeArgumentSpills(actual, call, oldMethod.maxLocals);
                call.owner = resize ? RESIZE : DIALOG;
                call.name = resize ? "nstbir_resize_uint8" : "tinyfd_messageBox";
                calls++;
            }
            if (!methodText(oldMethod).equals(methodText(actual))) {
                System.err.println("BEFORE\n" + methodText(oldMethod) + "AFTER\n" + methodText(actual));
                throw new AssertionError("Non-redirect method changes: " + oldClass.name + "#" + oldMethod.name + oldMethod.desc);
            }
        }
        if (calls != expected) throw new AssertionError("Wrong redirect count " + oldClass.name + ": " + calls);
        System.out.println("TRANSFORMED " + oldClass.name + " redirects=" + calls + " original_instructions_unchanged_after_verified_argument_spills=true frames=recomputed sha256="
                + HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(transformed)));
    }

    private static void inspect(byte[] data, Set<Reference> references) {
        new ClassReader(data).accept(new ClassVisitor(Opcodes.ASM9) {
            private void add(String owner, String name, String desc, boolean field, boolean isStatic) {
                if (owner.startsWith("org/lwjgl/") || owner.startsWith("dev/echo/compat/lwjgl/Legacy"))
                    references.add(new Reference(owner, name, desc, field, isStatic));
            }
            @Override public MethodVisitor visitMethod(int access, String name, String desc, String sig, String[] exceptions) {
                return new MethodVisitor(Opcodes.ASM9) {
                    @Override public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
                        add(owner, name, desc, false, opcode == Opcodes.INVOKESTATIC);
                    }
                    @Override public void visitFieldInsn(int opcode, String owner, String name, String desc) {
                        add(owner, name, desc, true, opcode == Opcodes.GETSTATIC || opcode == Opcodes.PUTSTATIC);
                    }
                    @Override public void visitInvokeDynamicInsn(String name, String desc, Handle bootstrap, Object... args) {
                        for (Object arg : args) if (arg instanceof Handle handle) add(handle.getOwner(), handle.getName(), handle.getDesc(),
                                handle.getTag() <= Opcodes.H_PUTSTATIC, handle.getTag() == Opcodes.H_INVOKESTATIC
                                        || handle.getTag() == Opcodes.H_GETSTATIC || handle.getTag() == Opcodes.H_PUTSTATIC);
                    }
                };
            }
        }, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
    }

    private static boolean resolves(Class<?> type, Reference ref) {
        if (type == null) return false;
        if (ref.name.equals("<init>")) {
            for (var constructor : type.getDeclaredConstructors())
                if (Type.getConstructorDescriptor(constructor).equals(ref.descriptor)
                        && !ref.isStatic && !Modifier.isPrivate(constructor.getModifiers())) return true;
            return false;
        }
        if (ref.field) {
            for (var field : type.getDeclaredFields()) if (field.getName().equals(ref.name) && Type.getDescriptor(field.getType()).equals(ref.descriptor)
                    && Modifier.isStatic(field.getModifiers()) == ref.isStatic && Modifier.isPublic(field.getModifiers())) return true;
        } else {
            for (var method : type.getDeclaredMethods()) if (method.getName().equals(ref.name) && Type.getMethodDescriptor(method).equals(ref.descriptor)
                    && Modifier.isStatic(method.getModifiers()) == ref.isStatic
                    && (Modifier.isPublic(method.getModifiers()) || ref.owner.startsWith("dev/echo/compat/lwjgl/Legacy"))) return true;
        }
        for (Class<?> parent : type.getInterfaces()) if (resolves(parent, ref)) return true;
        return resolves(type.getSuperclass(), ref);
    }

    private static Set<Reference> failures(Set<Reference> values) {
        Set<Reference> result = new HashSet<>();
        for (Reference ref : values) {
            try {
                if (!resolves(Class.forName(ref.owner.replace('/', '.'), false, MinecraftLwjglCompatibilityAudit.class.getClassLoader()), ref)) result.add(ref);
            } catch (ReflectiveOperationException | LinkageError failure) { result.add(ref); }
        }
        return result;
    }

    public static void main(String[] args) throws Exception {
        Map<String, Integer> targets = targetNames();
        MixinBootstrap.init();
        MixinEnvironment.getDefaultEnvironment().setSide(MixinEnvironment.Side.CLIENT);
        MixinEnvironment.getCurrentEnvironment().setSide(MixinEnvironment.Side.CLIENT);
        Mixins.addConfiguration("echo.lwjgl-compat.mixins.json");
        IMixinTransformer transformer = ((OfflineMixinService) MixinService.getService()).transformer();
        int classes = 0, transformedCount = 0;
        try (ZipFile game = new ZipFile(args[0])) {
            var entries = game.entries();
            while (entries.hasMoreElements()) {
                var entry = entries.nextElement();
                if (!entry.getName().endsWith(".class")) continue;
                byte[] bytes;
                try (InputStream input = game.getInputStream(entry)) { bytes = input.readAllBytes(); }
                inspect(bytes, before);
                String name = entry.getName().substring(0, entry.getName().length() - 6);
                if (targets.containsKey(name)) {
                    byte[] transformed = transformer.transformClassBytes(name.replace('/', '.'), name.replace('/', '.'), bytes);
                    proveOnlyRedirects(bytes, transformed, targets.get(name));
                    inspect(transformed, after);
                    transformedCount++;
                } else inspect(bytes, after);
                classes++;
            }
        }
        for (int index = 1; index < args.length; index++) {
            byte[] provider = Files.readAllBytes(Path.of(args[index]));
            inspect(provider, before); inspect(provider, after); classes++;
        }
        Set<Reference> oldFailures = failures(before);
        if (oldFailures.size() != 2 || oldFailures.stream().noneMatch(ref -> ref.owner.equals(RESIZE) && ref.descriptor.equals(RESIZE_DESC))
                || oldFailures.stream().noneMatch(ref -> ref.owner.equals(DIALOG) && ref.descriptor.equals(DIALOG_DESC))) {
            throw new AssertionError("Original linkage frontier changed: " + oldFailures);
        }
        // Audit the adapter's real 3.4.3 method-handle target as well as MC calls.
        for (String name : List.of("LegacyTinyFileDialogs", "LegacyImageResize")) {
            try (InputStream input = MinecraftLwjglCompatibilityAudit.class.getClassLoader().getResourceAsStream("dev/echo/compat/lwjgl/" + name + ".class")) {
                inspect(Objects.requireNonNull(input).readAllBytes(), after);
            }
        }
        Set<Reference> newFailures = failures(after);
        if (transformedCount != 4 || !newFailures.isEmpty()) throw new AssertionError("Transformed compatibility failure: " + newFailures);
        System.out.println("MINECRAFT_COMPATIBILITY classes=" + classes + " original_members=" + before.size()
                + " original_failures=" + oldFailures.size() + " transformed_members=" + after.size()
                + " transformed_failures=" + newFailures.size() + " target_classes=" + transformedCount + " redirects=5");
    }
}
