JavaPandora
===========

This is currently a pretty hacked up implementation of accessing the Pandora API and playing songs.  Current task now is to implement methods that would be used in a GUI implementation.  I'm in the middle of the Consol-GUI transformation.

To compile and run on Windows:
------------------------------

	javac -cp .;gson-2.2.3.jar;commons-codec-1.7.jar;jl1.0.1.jar *.java
	java -cp .;gson-2.2.3.jar;commons-codec-1.7.jar;jl1.0.1.jar PandoraGUI

To compile and run on Unix (Mac or Linux):
------------------------------------------

	javac -cp .:gson-2.2.3.jar:commons-codec-1.7.jar:jl1.0.1.jar *.java
	java -cp .:gson-2.2.3.jar:commons-codec-1.7.jar:jl1.0.1.jar PandoraGUI