package net.anawesomguy.clsdump;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.io.IOException;
import java.io.InputStream;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.ProtectionDomain;

public final class Dumper implements ClassFileTransformer {
    private static final String TARGET_METHOD_NAME = "yourPrivateMethod"; // Replace with your private method name
    private static final String TARGET_METHOD_DESC = "()V"; // Replace with your method descriptor
    private static final String STATIC_RESPONSE = "Static Response"; // The response you want to return

    public static void premain(String args, Instrumentation inst) {
        System.out.println("Intercepting method calls");
        inst.addTransformer(new Dumper());
    }

    @Override
    public byte[] transform(ClassLoader classLoader, String name, Class<?> clazz, ProtectionDomain protectionDomain, byte[] buf) {
        if (name.equals("your/package/ClassName")) { // Replace with the full class name containing the target method
            return interceptMethod(buf);
        }
        return null; // No transformation for other classes
    }

    private byte[] interceptMethod(byte[] classfileBuffer) {
        ClassReader classReader = new ClassReader(classfileBuffer);
        ClassWriter classWriter = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
        ClassVisitor classVisitor = new ClassVisitor(Opcodes.ASM9, classWriter) {
            @Override
            public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
                MethodVisitor mv = super.visitMethod(access, name, descriptor, signature, exceptions);
                if (name.equals(TARGET_METHOD_NAME) && descriptor.equals(TARGET_METHOD_DESC)) {
                    return new MethodVisitor(Opcodes.ASM9, mv) {
                        @Override
                        public void visitCode() {
                            super.visitCode();
                            // Return a static response
                            mv.visitLdcInsn(STATIC_RESPONSE);
                            mv.visitInsn(Opcodes.POP); // Pop the response if not used
                            mv.visitInsn(Opcodes.RETURN); // Return from the method
                        }
                    };
                }
                return mv; // No modification for other methods
            }
        };
        classReader.accept(classVisitor, 0);
        return classWriter.toByteArray();
    }
}
