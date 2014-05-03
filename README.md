Pandorica
=========

This is currently a pretty hacked up Java application.  My code is really thrown together but I'll slowly clean things up.  A basic GUI is in place for now, it won't save any password you enter so you'll have to re-enter them everytime you run the application.

To compile and run on Windows:
------------------------------

	javac -cp .;gson-2.2.3.jar;jl1.0.1.jar *.java
	java -cp .;gson-2.2.3.jar;jl1.0.1.jar PandoraGUI

To compile and run on Unix (Mac or Linux):
------------------------------------------

	javac -cp .:gson-2.2.3.jar:jl1.0.1.jar *.java
	java -cp .:gson-2.2.3.jar:jl1.0.1.jar PandoraGUI