build:
	mvn clean package -DskipTests --no-transfer-progress

run:
	mvn clean spring-boot:run

# See https://www.baeldung.com/java-application-remote-debugging#from-java9
# Remove "suspend=n" if JVM must wait for debugger to attach
run-jar:
	echo %JAVA_HOME%
	java -version
	java -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=127.0.0.1:8998 -jar target/app.jar

curl-get:
	curl localhost:8080/test/sendGet

curl-get-async:
	curl localhost:8080/test/sendAsyncGet

get-error:
	curl localhost:8080/test/sendAsyncGet
	curl localhost:8080/test/sendGet

get-success:
	curl localhost:8080/test/sendGet
	curl localhost:8080/test/sendAsyncGet
	