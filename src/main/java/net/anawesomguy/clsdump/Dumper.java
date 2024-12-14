package net.anawesomguy.methodintercept;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;

public final class MethodInterceptor implements ClassFileTransformer {
    private final String targetClassName;
    private final String targetMethodName;

    public MethodInterceptor(String targetClassName, String targetMethodName) {
        this.targetClassName = targetClassName;
        this.targetMethodName = targetMethodName;
    }

    public static void premain(String args, Instrumentation inst) {
        String[] parts = args.split(",");
        if (parts.length != 2) {
            System.err.println("Usage: -javaagent:agent.jar=<className>,<methodName>");
            return;
        }
        String className = parts[0];
        String methodName = parts[1];
        System.out.println("Intercepting method " + methodName + " in class " + className);
        inst.addTransformer(new MethodInterceptor(className, methodName));
    }

    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
                            ProtectionDomain protectionDomain, byte[] classfileBuffer) {
        String dotClassName = className.replace('/', '.');
        if (!dotClassName.equals(targetClassName)) {
            return null;
        }

        try {
            ClassPool cp = ClassPool.getDefault();
            CtClass cc = cp.get(dotClassName);
            CtMethod method = cc.getDeclaredMethod(targetMethodName);

            method.setBody("{ /* no-op */ }");

            System.out.println("Intercepted method " + targetMethodName + " in class " + dotClassName);
            return cc.toBytecode();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
