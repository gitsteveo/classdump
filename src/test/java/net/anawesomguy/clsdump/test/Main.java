package net.anawesomguy.clsdump.test;

import java.io.PrintStream;

public final class Main {
    public static void main(String[] args) throws ReflectiveOperationException {
        long start = System.nanoTime();
        PrintStream.class.getMethod("println", String.class).invoke(
            Class.forName("java.lang.System").getField("out").get(null), "Hello World"
        );
        System.out.println("Test concluded in " + (System.nanoTime() - start) / 1000000.0 + "ms");
    }
}
