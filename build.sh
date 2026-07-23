rm -rf -- ./build/.nob/classes/*

javac -d build/classes -cp ".:build/libs/*" $(find src/nob -name "*.java")
jar cf ./Nob.jar -C build/classes .
