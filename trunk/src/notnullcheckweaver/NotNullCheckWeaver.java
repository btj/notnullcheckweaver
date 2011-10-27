// Adapted from the Adapt example in the ASM distribution. Original example by Eric Bruneton.

package notnullcheckweaver;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.HashMap;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

/**
 * Generates run-time checks that enforce the @NotNull annotations in a specified package and its subpackages.    
 */
public final class NotNullCheckWeaver {
    private NotNullCheckWeaver() {}
    
    static String classNameDesc(String className) { return "L"+className+";"; }
    
    static final String weaverPackageName = "notnullcheckweaver";
    static final String notNullAnnotationClassName = weaverPackageName+"/NotNull";
    static final String notNullAnnotationDesc = classNameDesc(notNullAnnotationClassName);
    static final String nullableAnnotationClassName = weaverPackageName+"/Nullable";
    static final String nullableAnnotationDesc = classNameDesc(nullableAnnotationClassName);
    
    /**
     * Called by the JVM when the weaver is used as a Java agent.
     * 
     * To use the weaver, start the JVM as follows:
     * <pre>java -javaagent:notnullcheckweaver.jar=my.root.package my.root.package.MyMainClass</pre>
     * If started this way, the weaver instruments all classes in the specified package
     * and its subpackages.
     * 
     * Note: the weaver ignores package annotations for packages not below the specified package. 
     */
    public static void premain(String agentArgs, Instrumentation inst) {
        final String classNamePrefix = agentArgs;
        inst.addTransformer(new NotNullClassFileTransformer(classNamePrefix));
    }
}

class NotNullClassFileTransformer implements ClassFileTransformer {
    private final String classNamePrefix;
    private HashMap<String, Boolean> packageNotNullMap = new HashMap<String, Boolean>();
    
    NotNullClassFileTransformer(String classNamePrefix) {
        this.classNamePrefix = classNamePrefix.replace('.', '/');
    }
    
    static String getPackageName(String className) {
        int slash = className.lastIndexOf('/');
        if (slash < 0)
            return null;
        return className.substring(0, slash);
    }
    
    boolean isPackageNotNull(ClassLoader loader, String packageName) {
        if (packageName == null)
            return false;
        if (!packageName.startsWith(classNamePrefix))
            return false;
        Boolean packageNotNull = packageNotNullMap.get(packageName);
        if (packageNotNull != null)
            return packageNotNull;
        boolean superpackageNotNull = isPackageNotNull(loader, getPackageName(packageName));
        InputStream is = loader.getResourceAsStream(packageName+"/package-info.class");
        if (is == null)
            packageNotNull = superpackageNotNull;
        else {
            try {
                ClassReader cr = new ClassReader(is);
                PackageNotNullVisitor v = new PackageNotNullVisitor();
                cr.accept(v, ClassReader.SKIP_DEBUG|ClassReader.SKIP_FRAMES|ClassReader.SKIP_CODE);
                is.close();
                packageNotNull = v.packageNotNull || superpackageNotNull && !v.packageNullable;
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        packageNotNullMap.put(packageName, packageNotNull);
        return packageNotNull;
    }
    
    @Override
    public byte[] transform(ClassLoader loader, String className,
            Class<?> classBeingRedefined, ProtectionDomain protectionDomain,
            byte[] classfileBuffer) throws IllegalClassFormatException {
        if (className.endsWith("package-info"))
            return null; // Avoid loops
        if (className.startsWith(classNamePrefix)) {
            boolean packageNotNull = isPackageNotNull(loader, getPackageName(className));
            NotNullClassInspector inspector = new NotNullClassInspector(packageNotNull);
            {
                ClassReader reader1 = new ClassReader(classfileBuffer);
                reader1.accept(inspector, ClassReader.SKIP_DEBUG|ClassReader.SKIP_FRAMES|ClassReader.SKIP_CODE);
            }
            
            ClassReader reader2 = new ClassReader(classfileBuffer);
            ClassWriter writer = new ClassWriter(0);
            ClassVisitor adapter = new NotNullClassAdapter(writer, inspector);
            reader2.accept(adapter, 0);
            byte[] result = writer.toByteArray();
            /*
            try {
                FileOutputStream fos = new FileOutputStream(className.replace('/', '_') + ".class");
                fos.write(result);
                fos.close();
            } catch (FileNotFoundException e) {
                throw new RuntimeException(e);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            */
            return result;
        }
        return null;
    }
}

class PackageNotNullVisitor extends ClassVisitor {
    boolean packageNotNull;
    boolean packageNullable;
    
    public PackageNotNullVisitor() {
        super(Opcodes.ASM4);
    }

    public AnnotationVisitor visitAnnotation(String classDescriptor, boolean visible) {
        if (classDescriptor.equals(NotNullCheckWeaver.notNullAnnotationDesc))
            packageNotNull = true;
        else if (classDescriptor.equals(NotNullCheckWeaver.nullableAnnotationDesc))
            packageNullable = true;
        return null;
    }
}

class FieldInfo {
    final String name;
    final String desc;
    
    FieldInfo(String name, String desc) {
        this.name = name;
        this.desc = desc;
    }
    
    @Override
    public boolean equals(Object other) {
        FieldInfo info = (FieldInfo)other;
        return name.equals(info.name) && desc.equals(info.desc);
    }
    
    @Override
    public int hashCode() {
        return name.hashCode() ^ desc.hashCode();
    }
}

class NotNullClassInspector extends ClassVisitor implements Opcodes {
    String owner;
    boolean isInterface;
    boolean packageNotNull;
    boolean classNotNull;
    boolean classNullable;
    ArrayList<FieldInfo> notNullInstanceFields = new ArrayList<FieldInfo>();
    ArrayList<FieldInfo> notNullStaticFields = new ArrayList<FieldInfo>();

    public NotNullClassInspector(boolean packageNotNull) {
        super(Opcodes.ASM4);
        this.packageNotNull = packageNotNull;
    }

    boolean isClassNotNull() {
        boolean result = classNotNull || packageNotNull && !classNullable;
        return result;
    }
    
    @Override
    public void visit(
        final int version,
        final int access,
        final String name,
        final String signature,
        final String superName,
        final String[] interfaces)
    {
        owner = name;
        isInterface = (access & ACC_INTERFACE) != 0;
        super.visit(version, access, name, signature, superName, interfaces);
    }

    @Override
    public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
        if (desc.equals(NotNullCheckWeaver.notNullAnnotationDesc))
            classNotNull = true;
        else if (desc.equals(NotNullCheckWeaver.nullableAnnotationDesc)) {
            classNullable = true;
        }
        return super.visitAnnotation(desc, visible);
    }
    
    @Override
    public FieldVisitor visitField(
        final int access,
        final String name,
        final String desc,
        final String signature,
        final Object value)
    {
        final boolean isStatic = (access & ACC_STATIC) != 0;
        Type t = Type.getType(desc);
        if (t.getSort() == Type.OBJECT) {
            return new FieldVisitor(Opcodes.ASM4) {
                boolean fieldNotNull;
                boolean fieldNullable;
                
                @Override
                public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
                    if (desc.equals(NotNullCheckWeaver.notNullAnnotationDesc))
                        fieldNotNull = true;
                    else if (desc.equals(NotNullCheckWeaver.nullableAnnotationDesc))
                        fieldNullable = true;
                    return null;
                }
                
                @Override
                public void visitEnd() {
                    if (fieldNotNull || isClassNotNull() && !fieldNullable)
                        if (isStatic)
                            notNullStaticFields.add(new FieldInfo(name, desc));
                        else
                            notNullInstanceFields.add(new FieldInfo(name, desc));
                }
            };
        } else
            return null;
    }
}

class NotNullClassAdapter extends ClassVisitor implements Opcodes {

    static final String initCheckMethodName = "$checkNotNullInstanceFieldsInitialized";
    static final String clinitCheckMethodName = "$checkNotNullStaticFieldsInitialized";
    
    NotNullClassInspector inspector;

    public NotNullClassAdapter(final ClassVisitor writer, NotNullClassInspector inspector) {
        super(Opcodes.ASM4, writer);
        this.inspector = inspector;
    }

    @Override
    public MethodVisitor visitMethod(
        final int access,
        final String name,
        final String desc,
        final String signature,
        final String[] exceptions)
    {
        MethodVisitor mv = cv.visitMethod(access,
                name,
                desc,
                signature,
                exceptions);
        return new NotNullCodeAdapter(mv, inspector.owner, (access & ACC_STATIC) != 0, name, desc, this);
    }
    
    @Override
    public void visitEnd() {
        if (inspector.notNullInstanceFields.size() > 0)
            generateInstanceFieldsInitializationCheckMethod();
        if (inspector.notNullStaticFields.size() > 0) {
            if (!inspector.isInterface)
                generateStaticFieldsInitializationCheckMethod();
        }
        
        super.visitEnd();
    }

    private void generateInstanceFieldsInitializationCheckMethod() {
        MethodVisitor mv = cv.visitMethod(ACC_PRIVATE, initCheckMethodName, "()V", null, null);
        mv.visitCode();
        for (FieldInfo field : inspector.notNullInstanceFields) {
            mv.visitVarInsn(ALOAD, 0); // this
            mv.visitFieldInsn(GETFIELD, inspector.owner, field.name, field.desc); // read the field
            mv.visitLdcInsn(field.name); // load string constant
            mv.visitMethodInsn(INVOKESTATIC, NotNullCodeAdapter.checkNotNullClass, "checkConstructorFieldNotNull", "(Ljava/lang/Object;Ljava/lang/String;)V");
        }
        mv.visitInsn(RETURN);
        mv.visitMaxs(2, 1); // Two operands of size 1; one local (the receiver)
        mv.visitEnd();
    }
    
    static void generateStaticFieldsInitializedChecks(MethodVisitor mv, NotNullClassInspector inspector) {
        for (FieldInfo field : inspector.notNullStaticFields) {
            mv.visitFieldInsn(GETSTATIC, inspector.owner, field.name, field.desc);
            mv.visitLdcInsn(field.name);
            mv.visitMethodInsn(INVOKESTATIC, NotNullCodeAdapter.checkNotNullClass, "checkStaticInitializerFieldNotNull", "(Ljava/lang/Object;Ljava/lang/String;)V");
        }
    }
    
    private void generateStaticFieldsInitializationCheckMethod() {
        MethodVisitor mv = cv.visitMethod(ACC_PRIVATE | ACC_STATIC, clinitCheckMethodName, "()V", null, null);
        mv.visitCode();
        generateStaticFieldsInitializedChecks(mv, inspector);
        mv.visitInsn(RETURN);
        mv.visitMaxs(2, 1); // Two operands of size 1; one local (the receiver)
        mv.visitEnd();
    }
}

class NotNullCodeAdapter extends MethodVisitor implements Opcodes {

    static final String checkNotNullClass = "notnullcheckweaver/NotNullHelper";
    
    private String owner;
    private String name;
    private Type[] argTypes;
    private boolean isStatic;
    private NotNullClassAdapter classAdapter;
    private boolean resultNotNull;
    private boolean resultNullable;
    private boolean[] paramsNotNull;
    private boolean[] paramsNullable;
    
    boolean isParameterNotNull(int index) {
        return paramsNotNull[index] || classAdapter.inspector.isClassNotNull() && !paramsNullable[index];
    }
    
    boolean isResultNotNull() {
        return resultNotNull || classAdapter.inspector.isClassNotNull() && !resultNullable;
    }

    public NotNullCodeAdapter(final MethodVisitor mv, final String owner, boolean isStatic, String name, String desc, NotNullClassAdapter classAdapter) {
        super(Opcodes.ASM4, mv);
        this.owner = owner;
        this.name = name;
        this.isStatic = isStatic;
        this.classAdapter = classAdapter;
        argTypes = Type.getArgumentTypes(desc);
        paramsNotNull = new boolean[argTypes.length];
        paramsNullable = new boolean[argTypes.length];
    }
    
    @Override
    public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
        if (desc.equals(NotNullCheckWeaver.notNullAnnotationDesc))
            resultNotNull = true;
        else if (desc.equals(NotNullCheckWeaver.nullableAnnotationDesc))
            resultNullable = true;
        return super.visitAnnotation(desc, visible);
    }
    
    @Override
    public AnnotationVisitor visitParameterAnnotation(int index, String desc, boolean visible) {
        if (desc.equals(NotNullCheckWeaver.notNullAnnotationDesc))
            paramsNotNull[index] = true;
        else if (desc.equals(NotNullCheckWeaver.nullableAnnotationDesc))
            paramsNullable[index] = true;
        return super.visitParameterAnnotation(index, desc, visible);
    }
    
    @Override
    public void visitCode() {
        super.visitCode();
        int j = isStatic ? 0 : 1;
        for (int i = 0; i < argTypes.length; i++) {
            if (argTypes[i].getSort() == Type.OBJECT && isParameterNotNull(i)) {
                mv.visitIntInsn(ALOAD, j);
                mv.visitLdcInsn(i);
                mv.visitMethodInsn(INVOKESTATIC, checkNotNullClass, "checkArgumentNotNull", "(Ljava/lang/Object;I)V");
            }
            j += argTypes[i].getSize();
        }
    }

    @Override
    public void visitFieldInsn(
        final int opcode,
        final String owner,
        final String name,
        final String desc)
    {
        if (owner.equals(this.owner)) {
            switch (opcode) {
            case GETFIELD:
                if (classAdapter.inspector.notNullInstanceFields.contains(new FieldInfo(name, desc))) {
                    mv.visitFieldInsn(opcode, owner, name, desc);
                    mv.visitInsn(DUP); // Duplicate the value
                    mv.visitMethodInsn(INVOKESTATIC, checkNotNullClass, "checkGetFieldNotNull", "(Ljava/lang/Object;)V");
                    return;
                }
                break;
            case PUTFIELD:
                if (classAdapter.inspector.notNullInstanceFields.contains(new FieldInfo(name, desc))) {
                    mv.visitInsn(DUP); // Duplicate the value
                    mv.visitMethodInsn(INVOKESTATIC, checkNotNullClass, "checkPutFieldNotNull", "(Ljava/lang/Object;)V");
                    mv.visitFieldInsn(opcode, owner, name, desc);
                    return;
                }
                break;
            case GETSTATIC:
                if (classAdapter.inspector.notNullStaticFields.contains(new FieldInfo(name, desc))) {
                    mv.visitFieldInsn(opcode, owner, name, desc);
                    mv.visitInsn(DUP); // Duplicate the value
                    mv.visitMethodInsn(INVOKESTATIC, checkNotNullClass, "checkGetFieldNotNull", "(Ljava/lang/Object;)V");
                    return;
                }
                break;
            case PUTSTATIC:
                if (classAdapter.inspector.notNullStaticFields.contains(new FieldInfo(name, desc))) {
                    mv.visitInsn(DUP); // Duplicate the value
                    mv.visitMethodInsn(INVOKESTATIC, checkNotNullClass, "checkPutFieldNotNull", "(Ljava/lang/Object;)V");
                    mv.visitFieldInsn(opcode, owner, name, desc);
                    return;
                }
                break;
            }
        }
        super.visitFieldInsn(opcode, owner, name, desc);
    }
    
    @Override
    public void visitInsn(int opcode) {
        if (opcode == ARETURN && isResultNotNull()) {
            mv.visitInsn(DUP);
            mv.visitMethodInsn(INVOKESTATIC, checkNotNullClass, "checkResultNotNull", "(Ljava/lang/Object;)V");
        } else if (opcode == RETURN && classAdapter.inspector.notNullInstanceFields.size() > 0 && name.equals("<init>")) {
            mv.visitIntInsn(ALOAD, 0); // this
            mv.visitMethodInsn(INVOKESPECIAL, owner, NotNullClassAdapter.initCheckMethodName, "()V");
        } else if (opcode == RETURN && classAdapter.inspector.notNullStaticFields.size() > 0 && name.equals("<clinit>")) {
            if (classAdapter.inspector.isInterface)
                NotNullClassAdapter.generateStaticFieldsInitializedChecks(mv, classAdapter.inspector);
            else
                mv.visitMethodInsn(INVOKESTATIC, owner, NotNullClassAdapter.clinitCheckMethodName, "()V");
        }
        super.visitInsn(opcode);
    }
    
    @Override
    public void visitMaxs(int maxStack, int maxLocals) {
        super.visitMaxs(maxStack + 2, maxLocals);
    }
}
