javac -cp .;gson-2.2.3.jar;commons-codec-1.7.jar;jl1.0.1.jar *.java
jar cfm JavaPandora.jar Manifest.txt *.class
java -jar JavaPandora.jar