package net.anawesomguy.clsdump;

import java.io.IOException;
import java.io.InputStream;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.ProtectionDomain;
import java.util.Comparator;
import java.util.stream.Stream;

public final class Dumper implements ClassFileTransformer {
    private static final Path DIR = Paths.get("_classdump");

    static {
        if (Files.exists(DIR))
            try {
                try (Stream<Path> s = Files.walk(DIR)) {
                    s.sorted(Comparator.reverseOrder()).forEach(Dumper::rm);
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
    }

    private static void rm(Path p) {
        try {
            Files.delete(p);
        } catch (IOException e) {
            throw new RuntimeException(p.toString(), e);
        }
    }

    private final boolean debug;

    public Dumper(boolean debug) {
        this.debug = debug;
    }

    public static void premain(String args, Instrumentation inst) {
        System.out.println("Dump classes to " + DIR.toAbsolutePath());
        boolean b = "debug".equalsIgnoreCase(args);
        inst.addTransformer(new Dumper(b));

        //already loaded classes
        for (Class<?> c : inst.getAllLoadedClasses()) {
            if (c.isArray())
                continue;
            String name = c.getName().replace('.', '/');
            try {
                String cls = name + ".class";
                InputStream in = c.getResourceAsStream(cls.substring(name.lastIndexOf('/') + 1));
                if (in != null) {
                    try {
                        Path p = DIR.resolve(cls);
                        Files.createDirectories(p.getParent());
                        Files.copy(in, p, StandardCopyOption.REPLACE_EXISTING);
                    } finally {
                        in.close();
                    }
                    if (b)
                        System.out.println("Dumped class " + name);
                } else if (b)
                    System.out.println("Couldn't get bytes for " + name);
            } catch (Exception e) {
                System.err.println("Unable to dump class " + name);
                //noinspection CallToPrintStackTrace
                e.printStackTrace();
            }
        }
    }

    public byte[] transform(ClassLoader cl, String name, Class<?> c, ProtectionDomain pd, byte[] b) {
        try {
            Path p = DIR.resolve(name + ".class");
            Files.createDirectories(p.getParent());
            Files.write(p, b);
            if (debug)
                System.out.println("Dumped class " + name);
        } catch (Exception e) {
            System.err.println("Unable to dump class " + name);
            //noinspection CallToPrintStackTrace
            e.printStackTrace();
        }
        return null;
    }
}