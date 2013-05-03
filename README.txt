(It is assumed that you are in this working directory)


** There are still some bugs.  PandoraReceiver must be running first.  
** When a new song comes along, PandoraReceiver does not reset appropriately
** PandoraPlayer MAY freak out if on a slow connection, have not experienced this on RIT's network, though.
** MainPandora does not have much input validation
**	- Cannot pause (Implemented byte array buffer in preperation for this, though)
**	- Cannot change station, requires reboot
**	- If logged in to too many times may result in 42 seconds songs until an unknown amount of time has passed
**	  (around an hour)  Change stations if this happens. (I have it tell you that it's skipping songs)




To compile and run on WINDOWS:

javac -cp .;gson-2.2.3.jar;commons-codec-1.7.jar;jl1.0.1.jar *.java

java -cp .;gson-2.2.3.jar;commons-codec-1.7.jar;jl1.0.1.jar MainPandora
java -cp .;gson-2.2.3.jar;commons-codec-1.7.jar;jl1.0.1.jar PandoraReceiver



To compile and tun on UNIX (Mac or Linux):

javac -cp .:gson-2.2.3.jar:commons-codec-1.7.jar:jl1.0.1.jar *.java

java -cp .:gson-2.2.3.jar:commons-codec-1.7.jar:jl1.0.1.jar MainPandora
java -cp .:gson-2.2.3.jar:commons-codec-1.7.jar:jl1.0.1.jar PandoraReceiver