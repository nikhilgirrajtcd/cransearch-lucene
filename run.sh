echo "Compile"
mvn package
echo "Running the jar file"
java -jar target/test-1.0.0-SNAPSHOT.jar
echo "Compile trec"
cd trec_eval-9.0.7/trec_eval
make
chmod 777 trec_eval
echo "Results"
./trec_eval ../../res/io/cranqrel ../../res/io/cranqrel-output 
cd ../..
