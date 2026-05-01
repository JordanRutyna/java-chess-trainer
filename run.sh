#!/bin/bash
mkdir -p out
javac -d out src/core/*.java src/app/*.java src/openings/*.java src/ui/*.java
java -cp out:src ui.MainWindow