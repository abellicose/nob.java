rm -rf -- ./build/api/classes
mkdir -p ./build/api/classes

javac -d build/api/classes $(find api/src/nob -name "*.java")
jar cf ./nob-api.jar -C build/api/classes .
