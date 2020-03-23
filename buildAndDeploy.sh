#!/bin/sh
mvn clean package
mkdir build-artifacts
$JAVA_HOME/bin/native-image -jar target/native-function-jar-with-dependencies.jar build-artifacts/native-function.o
chmod +x build-artifacts/native-function.o
cp $JAVA_HOME/lib/libsunec.so build-artifacts/libsunec.so
cp $JAVA_HOME/lib/security/cacerts build-artifacts/cacerts
cp bootstrap build-artifacts/bootstrap
cd build-artifacts
zip -r ../target/native-lambda.zip *
cd ..
rm -rf build-artifacts

aws lambda update-function-code --function-name $1 --zip-file fileb://target/native-lambda.zip