package net.anawesomguy.clsdump;

import java.io.IOException;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.ProtectionDomain;
import java.util.Comparator;
import java.util.stream.Stream;

public final class Dumper implements ClassFileTransformer {
    private static final Path DIR = Paths.get("_classdump");

    static {
        if (Files.exists(DIR))
            try {
                Stream<Path> walk = Files.walk(DIR);
                walk.sorted(Comparator.reverseOrder()).forEach(Dumper::rm);
                walk.close();
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
        System.out.println("Initializing Class Dump at " + DIR.toAbsolutePath());
        inst.addTransformer(new Dumper(args != null && args.equalsIgnoreCase("debug")));
    }

    public byte[] transform(ClassLoader loader, String name, Class<?> clz, ProtectionDomain domain, byte[] buf) {
        try {
            Path p = DIR.resolve(name + ".class");
            Files.createDirectories(p.getParent());
            if (debug)
                System.out.println("Dumping: " + name);
            Files.write(p, buf);
        } catch (IOException e) {
            System.err.println("Unable to dump class: " + name);
            //noinspection CallToPrintStackTrace
            e.printStackTrace();
        }
        return null;
    }
}