if [ "$1" ]; then
    file=$(find src/test/nob -name "$1.java")
    if javac -d build/classes -cp ".:build/libs/*:build/classes" "$file"; then
        echo "Recompiled $file"

        file="${file#src/test/}"
        file="${file%.java}"
        file="${file//\//.}"

        java -cp ".:./build/libs/*:./build/classes" org.junit.runner.JUnitCore "$file"
    fi
fi
