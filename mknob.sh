rm -rf -- ./build/.nob/classes/*

javac -d build/.nob/classes src/nob/*.java
jar cf ./Nob.jar -C build/.nob/classes .
