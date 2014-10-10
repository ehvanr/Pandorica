Pandorica
=========

This is currently a pretty hacked up Java application.  My code is really thrown together but I'll slowly clean things up.  A basic GUI is in place for now, it won't save any password you enter so you'll have to re-enter them everytime you run the application.

![ScreenShot](http://i.imgur.com/Kx0iD37.png)


To compile and run on Windows:
------------------------------

	javac -cp .;lib/jl1.0.1.jar;lib/gson-2.2.3.jar *.java
	java -cp .;lib/jl1.0.1.jar;lib/gson-2.2.3.jar Pandorica

To compile and run on Unix (Mac or Linux):
------------------------------------------

	javac -cp .:lib/jl1.0.1.jar:lib/gson-2.2.3.jar *.java
	java -cp .:lib/jl1.0.1.jar:lib/gson-2.2.3.jar Pandorica