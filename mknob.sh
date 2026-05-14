rm build/.out/*

javac -d build/.out Nob.java
jar cf Nob.jar -C build/.out .
