import java.io.InputStream;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.zip.ZipFile;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Handle;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

/** Resolves actual Minecraft/SDL-provider LWJGL member descriptors without executing application code. */
public final class LwjglLinkageAudit {
    private static final Set<String> checked = new TreeSet<>();
    private static final Set<String> failures = new TreeSet<>();
    private static final Map<String, Set<String>> callers = new TreeMap<>();
    private static String currentClass;
    private static String currentMethod;

    private LwjglLinkageAudit() {}

    private static boolean member(Class<?> type, String name, String descriptor, boolean field, boolean isStatic) {
        if (type == null) { return false; }
        if (name.equals("<init>")) {
            for (var value : type.getDeclaredConstructors()) {
                if (Type.getConstructorDescriptor(value).equals(descriptor)) { return true; }
            }
            return false;
        }
        if (field) {
            for (var value : type.getDeclaredFields()) {
                if (value.getName().equals(name) && Type.getDescriptor(value.getType()).equals(descriptor)
                        && Modifier.isStatic(value.getModifiers()) == isStatic) { return true; }
            }
        } else {
            for (var value : type.getDeclaredMethods()) {
                if (value.getName().equals(name) && Type.getMethodDescriptor(value).equals(descriptor)
                        && Modifier.isStatic(value.getModifiers()) == isStatic) { return true; }
            }
        }
        for (Class<?> parent : type.getInterfaces()) {
            if (member(parent, name, descriptor, field, isStatic)) { return true; }
        }
        return member(type.getSuperclass(), name, descriptor, field, isStatic);
    }

    private static void reference(String owner, String name, String descriptor, boolean field, boolean isStatic) {
        if (!owner.startsWith("org/lwjgl/")) { return; }
        String identity = owner + "." + name + descriptor + " static=" + isStatic;
        callers.computeIfAbsent(identity, ignored -> new TreeSet<>()).add(currentClass + "#" + currentMethod);
        if (!checked.add(identity)) { return; }
        try {
            Class<?> type = Class.forName(owner.replace('/', '.'), false, LwjglLinkageAudit.class.getClassLoader());
            if (!member(type, name, descriptor, field, isStatic)) { failures.add(identity); }
        } catch (ReflectiveOperationException | LinkageError error) { failures.add(identity + " " + error); }
    }

    private static void inspect(byte[] bytes) {
        new ClassReader(bytes).accept(new ClassVisitor(Opcodes.ASM9) {
            @Override
            public void visit(int version, int access, String name, String signature, String parent, String[] interfaces) {
                currentClass = name;
            }
            @Override
            public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
                currentMethod = name + descriptor;
                return new MethodVisitor(Opcodes.ASM9) {
                    @Override
                    public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean itf) {
                        reference(owner, name, descriptor, false, opcode == Opcodes.INVOKESTATIC);
                    }
                    @Override
                    public void visitFieldInsn(int opcode, String owner, String name, String descriptor) {
                        reference(owner, name, descriptor, true, opcode == Opcodes.GETSTATIC || opcode == Opcodes.PUTSTATIC);
                    }
                    @Override
                    public void visitInvokeDynamicInsn(String name, String descriptor, Handle bootstrap, Object... arguments) {
                        for (Object argument : arguments) {
                            if (argument instanceof Handle handle) {
                                boolean field = handle.getTag() <= Opcodes.H_PUTSTATIC;
                                boolean isStatic = handle.getTag() == Opcodes.H_INVOKESTATIC || handle.getTag() == Opcodes.H_GETSTATIC || handle.getTag() == Opcodes.H_PUTSTATIC;
                                reference(handle.getOwner(), handle.getName(), handle.getDesc(), field, isStatic);
                            }
                        }
                    }
                };
            }
        }, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
    }

    /** Checks all references in each supplied real JAR or class file.
     * @param args paths to ordinary public Minecraft JARs or source-compiled provider classes
     * @throws Exception if inputs cannot be read or linkage fails
     */
    public static void main(String[] args) throws Exception {
        int classes = 0;
        for (String input : args) {
            if (input.endsWith(".class")) { inspect(Files.readAllBytes(Path.of(input))); classes++; continue; }
            try (ZipFile jar = new ZipFile(input)) {
                var entries = jar.entries();
                while (entries.hasMoreElements()) {
                    var entry = entries.nextElement();
                    if (!entry.getName().endsWith(".class")) { continue; }
                    try (InputStream stream = jar.getInputStream(entry)) { inspect(stream.readAllBytes()); }
                    classes++;
                }
            }
        }
        checked.forEach(value -> System.out.println("REFERENCED " + value));
        failures.forEach(value -> System.err.println("UNRESOLVED " + value + " callers=" + callers.get(value)));
        System.out.println("LWJGL_LINKAGE classes=" + classes + " members=" + checked.size() + " failures=" + failures.size());
        if (checked.isEmpty() || !failures.isEmpty()) { throw new AssertionError("LWJGL compatibility check failed"); }
    }
}
