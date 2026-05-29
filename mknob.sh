rm -rf -- ./build/.nob/classes/*

javac -d build/.nob/classes -cp ".:build/libs/*" src/nob/*.java
jar cf ./Nob.jar -C build/.nob/classes .
