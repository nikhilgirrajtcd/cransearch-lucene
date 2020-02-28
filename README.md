# Indexing and Searching Cranfield documents using Lucene on Java
## Author - Nikhil Girraj khandeln@tcd.ie 

## Steps to compile and run

### Compile code
> `mvn package`

### Execute the jar file
> `java -jar target/test-1.0.0-SNAPSHOT.jar`

### Compile trec (if not already compiled)
> `cd trec_eval-9.0.7/trec_eval`
> `make`
> `chmod 777 trec_eval`

### Results
> `./trec_eval ../../res/io/cranqrel ../../res/io/cranqrel-output`
> `cd ../..`
