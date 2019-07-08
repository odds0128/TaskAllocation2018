#!/usr/local/bin/fish

sourcepath="src/main/research/Manager.java"
outputpath="out/production/"

packagepath="src/:src/main/:src/main/research"
librarypath="src/libs/commons-collections4-4.1/commons-collections4-4-1.jar:src/libs/guava-19.0.jar:src/libs/poi-3.17/poi-3.17.jar:src/libs/poi-3.17/poi-ooxml-3.17.jar, src/libs/hamcrest-all-1.3.jar"
classpath="$packagepath":"$librarypath"

echo $classpath
javac -d $outputpath -cp $classpath $sourcepath

executionpath="out/production/:out/production/libs/commons-collections4-4.1/commons-collections4-4-1.jar:out/production/libs/guava-19.0.jar:out/production/libs/poi-3.17/poi-3.17.jar:out/production/libs/poi-3.17/poi-ooxml-3.17.jar"

echo $executionpath
java -cp $executionpath main/research/Manager

