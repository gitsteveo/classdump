# Class Dump

A tiny Java agent to dump all classes loaded by the JVM (excluding arrays), made using the instrumentation API.

There are two "versions" of this project: one is the normal one (can be compiled with Java 7+, uses NIO),
and the other one, which is compatible with all Java versions 5 and above.

To use, simply add `-javaagent:path/to/classdump-version.jar` as a JVM arg. (adding `=debug` at the end enables logging each class dumped)

Classes are dumped to `_classdump`, relative to the working directory.

### Building

This project builds like any normal Gradle project, just run `./gradlew build`.

However, to compile the Java 5 version (`./gradlew java5Jar`), you will need to use Java 8 or below, as Java 9+ does not support compiling with compatibility for Java 5.

There are also GitHub Actions setup for this repo, allowing you to get a jar for every commit!