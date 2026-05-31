rm -rf -- ./build/.nob/classes/*

javac -d build/.nob/classes -cp ".:build/libs/*" $(find src/nob -name "*.java")
jar cf ./Nob.jar -C build/.nob/classes .
