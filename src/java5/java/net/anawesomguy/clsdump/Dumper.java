package net.anawesomguy.clsdump;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;

public final class Dumper implements ClassFileTransformer {
    private static final File DIR = new File("_classdump");

    static {
        if (DIR.exists())
            rm(DIR);
    }

    private final boolean debug;

    public Dumper(boolean debug) {
        this.debug = debug;
    }

    private static void rm(File f) {
        if (f.isDirectory())
            //noinspection DataFlowIssue
            for (File c : f.listFiles())
                rm(c);
        if (!f.delete())
            throw new RuntimeException("Failed to delete: ".concat(f.getPath()));
    }

    public static void premain(String args, Instrumentation inst) {
        System.out.println("Dumping classes to ".concat(DIR.getAbsolutePath()));
        boolean b = "debug".equalsIgnoreCase(args);
        inst.addTransformer(new Dumper(b));

        //already loaded classes
        for (Class<?> c : inst.getAllLoadedClasses()) {
            if (c.isArray())
                continue;
            String name = c.getName().replace('.', '/');
            try {
                String cls = name.concat(".class");
                InputStream in = c.getResourceAsStream(cls.substring(name.lastIndexOf('/') + 1));
                if (in != null) {
                    try {
                        File f = new File(DIR, cls);
                        //noinspection ResultOfMethodCallIgnored ???????? (what)
                        f.getParentFile().mkdirs();
                        FileOutputStream o = new FileOutputStream(f);
                        try {
                            byte[] buf = new byte[8192];
                            for (int i; (i = in.read(buf)) != -1;)
                                o.write(buf, 0, i);
                        } finally {
                            o.close();
                        }
                    } finally {
                        in.close();
                    }
                    if (b)
                        System.out.println("Dumped class ".concat(name));
                } else if (b)
                    System.out.println("Unable to get bytecode for ".concat(name));
            } catch (Exception e) {
                System.err.println("Unable to dump class ".concat(name));
                //noinspection CallToPrintStackTrace
                e.printStackTrace();
            }
        }
    }

    public byte[] transform(ClassLoader l, String name, Class<?> c, ProtectionDomain d, byte[] b) {
        try {
            File f = new File(DIR, name.concat(".class"));
            //noinspection ResultOfMethodCallIgnored ???????? (what)
            f.getParentFile().mkdirs();
            FileOutputStream o = new FileOutputStream(f);
            try {
                o.write(b);
            } finally {
                o.close();
            }
            if (debug)
                System.out.println("Dumped class ".concat(name));
        } catch (Exception e) {
            System.err.println("Unable to dump class ".concat(name));
            //noinspection CallToPrintStackTrace
            e.printStackTrace();
        }
        return null;
    }
}