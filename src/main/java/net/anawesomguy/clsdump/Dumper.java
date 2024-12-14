package net.anawesomguy.methodintercept;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

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
            return modifyClassBytes(classfileBuffer);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private byte[] modifyClassBytes(byte[] originalBytes) throws IOException {
        DataInputStream dis = new DataInputStream(new ByteArrayInputStream(originalBytes));
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(bos);

        // Copy the class file header
        dos.writeInt(dis.readInt());  // magic number
        dos.writeShort(dis.readShort());  // minor version
        dos.writeShort(dis.readShort());  // major version
        int constantPoolCount = dis.readUnsignedShort();
        dos.writeShort(constantPoolCount);

        // Copy the constant pool
        for (int i = 1; i < constantPoolCount; i++) {
            int tag = dis.readUnsignedByte();
            dos.writeByte(tag);
            switch (tag) {
                case 7: case 8: case 16: case 19: case 20:
                    dos.writeShort(dis.readUnsignedShort());
                    break;
                case 3: case 4: case 9: case 10: case 11: case 12: case 18:
                    dos.writeInt(dis.readInt());
                    break;
                case 5: case 6:
                    dos.writeLong(dis.readLong());
                    i++;
                    break;
                case 1:
                    dos.writeUTF(dis.readUTF());
                    break;
                case 15:
                    dos.writeByte(dis.readByte());
                    dos.writeShort(dis.readUnsignedShort());
                    break;
            }
        }

        // Copy access flags, this class, super class
        dos.writeShort(dis.readUnsignedShort());
        dos.writeShort(dis.readUnsignedShort());
        dos.writeShort(dis.readUnsignedShort());

        // Copy interfaces
        int interfacesCount = dis.readUnsignedShort();
        dos.writeShort(interfacesCount);
        for (int i = 0; i < interfacesCount; i++) {
            dos.writeShort(dis.readUnsignedShort());
        }

        // Copy fields
        int fieldsCount = dis.readUnsignedShort();
        dos.writeShort(fieldsCount);
        for (int i = 0; i < fieldsCount; i++) {
            copyMemberInfo(dis, dos);
        }

        // Modify methods
        int methodsCount = dis.readUnsignedShort();
        dos.writeShort(methodsCount);
        for (int i = 0; i < methodsCount; i++) {
            int accessFlags = dis.readUnsignedShort();
            int nameIndex = dis.readUnsignedShort();
            int descriptorIndex = dis.readUnsignedShort();
            
            dos.writeShort(accessFlags);
            dos.writeShort(nameIndex);
            dos.writeShort(descriptorIndex);

            if (isTargetMethod(originalBytes, nameIndex)) {
                // Replace method body with no-op
                dos.writeShort(2);  // attributesCount
                dos.writeShort(nameIndex);  // attribute_name_index (Code)
                dos.writeInt(14);  // attribute_length
                dos.writeShort(1);  // max_stack
                dos.writeShort(1);  // max_locals
                dos.writeInt(1);  // code_length
                dos.writeByte(0xB1);  // return
                dos.writeShort(0);  // exception_table_length
                dos.writeShort(0);  // attributes_count
                
                // Skip the original method body
                skipMethodBody(dis);
                
                System.out.println("Intercepted method " + targetMethodName + " in class " + targetClassName);
            } else {
                // Copy the original method body
                copyMethodBody(dis, dos);
            }
        }

        // Copy the rest of the class file
        byte[] remaining = new byte[dis.available()];
        dis.readFully(remaining);
        dos.write(remaining);

        return bos.toByteArray();
    }

    private boolean isTargetMethod(byte[] classBytes, int nameIndex) throws IOException {
        DataInputStream dis = new DataInputStream(new ByteArrayInputStream(classBytes));
        dis.skipBytes(10);  // Skip magic, version
        int constantPoolCount = dis.readUnsignedShort();
        for (int i = 1; i < constantPoolCount; i++) {
            int tag = dis.readUnsignedByte();
            if (tag == 1) {  // CONSTANT_Utf8
                if (i == nameIndex) {
                    return dis.readUTF().equals(targetMethodName);
                } else {
                    dis.skipBytes(dis.readUnsignedShort());
                }
            } else if (tag == 5 || tag == 6) {
                dis.skipBytes(8);
                i++;
            } else if (tag == 15) {
                dis.skipBytes(3);
            } else {
                dis.skipBytes(tag == 8 ? 2 : 4);
            }
        }
        return false;
    }

    private void copyMemberInfo(DataInputStream dis, DataOutputStream dos) throws IOException {
        dos.writeShort(dis.readUnsignedShort());  // access_flags
        dos.writeShort(dis.readUnsignedShort());  // name_index
        dos.writeShort(dis.readUnsignedShort());  // descriptor_index
        int attributesCount = dis.readUnsignedShort();
        dos.writeShort(attributesCount);
        for (int j = 0; j < attributesCount; j++) {
            copyAttribute(dis, dos);
        }
    }

    private void copyMethodBody(DataInputStream dis, DataOutputStream dos) throws IOException {
        int attributesCount = dis.readUnsignedShort();
        dos.writeShort(attributesCount);
        for (int j = 0; j < attributesCount; j++) {
            copyAttribute(dis, dos);
        }
    }

    private void skipMethodBody(DataInputStream dis) throws IOException {
        int attributesCount = dis.readUnsignedShort();
        for (int j = 0; j < attributesCount; j++) {
            dis.skipBytes(2);  // attribute_name_index
            int attributeLength = dis.readInt();
            dis.skipBytes(attributeLength);
        }
    }

    private void copyAttribute(DataInputStream dis, DataOutputStream dos) throws IOException {
        dos.writeShort(dis.readUnsignedShort());  // attribute_name_index
        int attributeLength = dis.readInt();
        dos.writeInt(attributeLength);
        byte[] attributeInfo = new byte[attributeLength];
        dis.readFully(attributeInfo);
        dos.write(attributeInfo);
    }
}
