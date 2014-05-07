/**
 * Evan Reichard
 * https://github.com/evreichard
 * evan@evanreichard.com
 * 2013 - 2014
 **/

import java.io.Console;
import java.util.ArrayList;
import java.util.Scanner;

public class PandoricaTerminal{
	// Receives Singleton Objects (And creates it if they don't already exist)
	private MainPandora pandoraBackEnd = MainPandora.getInstance();
	private QueueManager queueMan = QueueManager.getInstance();
	
	// The lock object
	private final Object threadLock = new Object();
	
	// Only created when we're using the '-nogui' flag
	private Scanner in;

	public PandoricaTerminal(){
		LoadPasswordTerminal();
	}
	
	// --------------------------------------------------------------------------------------\\
	// --------------------------------- TERMINAL LOADERS ---------------------------------- \\
	// --------------------------------------------------------------------------------------\\
	
	public void LoadPasswordTerminal(){
		
		in = new Scanner(System.in);
		System.out.print("Email: ");
		String username = in.nextLine();
		Console console = System.console();
		char passwordArray[] = console.readPassword("Password: ");

		// Login with password
		pandoraBackEnd.pandoraLogin(username, new String(passwordArray));
		
		LoadPlayerTerminal();
	}

	public void LoadPlayerTerminal(){
		queueMan.saveAsMP3(true);

		queueMan.playStation(LoadTerminalStations());

		SongTerminalContentUpdate songClass = new SongTerminalContentUpdate();
		Thread songPlayingThread = new Thread(songClass);
		songPlayingThread.start();
		
		String tempIn;

		while(true){
			tempIn = in.next();

			if(tempIn.equals("p")){
				queueMan.pause();
			}else if(tempIn.equals("r")){
				queueMan.resume();
			}else if(tempIn.equals("n")){
				queueMan.nextSong();
			}else if(tempIn.equals("s")){
				// Stop everything here
				queueMan.stop();
				queueMan.playStation(LoadTerminalStations());
			}else if(tempIn.equals("h")){
				System.out.println("p - pause\nr - resume\nn - next\ns - stations");
			}
		}
	
	}
	
	public String LoadTerminalStations(){
		// Get Station List
		ArrayList<ArrayList<String>> tempStationList = pandoraBackEnd.getStationList();

		// Print and choose station
		for(ArrayList<String> tempAL : tempStationList){
			System.out.println("(" + (tempStationList.indexOf(tempAL) + 1) + ") " + tempAL.get(0) + ", " + tempAL.get(1));
		}

		System.out.print("\nPlease enter a station: ");
		int selection = in.nextInt() - 1;
		String stationId = tempStationList.get(selection).get(1);

		return stationId;
	}
	
	// --------------------------------------------------------------------------------------\\
	// --------------------------------- EMBEDDED CLASSES ---------------------------------- \\
	// --------------------------------------------------------------------------------------\\
	
	
	
	class SongTerminalContentUpdate implements Runnable{
		public void run(){
			synchronized(threadLock){
				while(true){

					// On first start, we wait for song to start playing
					while(queueMan.getCurrentSong().getSongStatus() != PlayerStatus.PLAYING){						
						
						// Sleep for 100ms
						try{
							Thread.sleep(100);
						}catch(Exception e){}
					}
					
					// Gather playing song
					PandoraSong currentSongInfo = queueMan.getCurrentSong();
					
					
					String songName = currentSongInfo.getSongName();
					String artistName = currentSongInfo.getArtistName(); 
					String currentSongLength = queueMan.getCurrentSongLength();
					
					// Loop while our cached song is the same as the one in queue
					while(currentSongInfo.getAudioUrl().equals(queueMan.getCurrentSong().getAudioUrl())){
						String currentSongPosition = queueMan.getCurrentSongPosition();
						Integer bufferPercentage = queueMan.getBufferPercentage();
						
						System.out.print("\r" + "[" + bufferPercentage + "%] " + artistName + " - " + songName + " (" + currentSongPosition + " / " + currentSongLength + ")");
						
						// Sleep for 100ms
						try{
							Thread.sleep(100);
						}catch(Exception e){}
					}
					
					System.out.println("\r" + artistName + " - " + songName + " (PLAYED)                            ");
					
				}
			}
		}
	}
}