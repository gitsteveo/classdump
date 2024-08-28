package net.anawesomguy.clsdump;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
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
            throw new RuntimeException("Failed to delete: ".concat(f.toString()));
    }

    public static void premain(String args, Instrumentation inst) {
        System.out.println("Initializing ClassDump at ".concat(DIR.getAbsolutePath()));
        inst.addTransformer(new Dumper(args != null && args.equalsIgnoreCase("debug")));
    }

    public byte[] transform(ClassLoader loader, String name, Class<?> clz, ProtectionDomain domain, byte[] buf) {
        try {
            File f = new File(DIR, name.concat(".class"));
            //noinspection ResultOfMethodCallIgnored ???????? (what)
            f.getParentFile().mkdirs();
            FileOutputStream o = new FileOutputStream(f);
            if (debug)
                System.out.println("Dumping: ".concat(name));
            o.write(buf);
            o.close();
        } catch (IOException e) {
            System.err.println("Unable to dump class: ".concat(name));
            //noinspection CallToPrintStackTrace
            e.printStackTrace();
        }
        return null;
    }
}