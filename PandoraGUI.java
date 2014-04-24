import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.io.*;
import java.util.Scanner;

public class PandoraGUI{
	public PandoraGUI(){
		MainPandora pandoraBackEnd = new MainPandora();
		
		// Prompt Password
		String tempPassArr[] = promptPassword();
		
		// Login with password
		pandoraBackEnd.pandoraLogin(tempPassArr[0], tempPassArr[1]);
		
		// Get Station List
		ArrayList<ArrayList<String>> tempStationList = pandoraBackEnd.getStationList();
		
		// Print and choose station
		for(ArrayList<String> tempAL : tempStationList){
			System.out.println("(" + (tempStationList.indexOf(tempAL) + 1) + ") " + tempAL.get(0) + ", " + tempAL.get(1));
		}
		
		Scanner in = new Scanner(System.in);
		System.out.print("\nPlease enter a station: ");
		int selection = in.nextInt() - 1;
		
		while(true){
			// Get Playlist of selected station
			ArrayList<PandoraSong> tempPlaylist = pandoraBackEnd.getPlaylist(tempStationList.get(selection).get(1));

			// Sends songListArray JsonObject ArrayList object to PandoraPlayer
			PandoraPlayer playTrack = new PandoraPlayer(tempPlaylist);
			
			// Plays the player, it will cycle through until it completes the playlist
			try{
				playTrack.play();
			}catch(NoSongInQueueException nsiqe){
				nsiqe.printStackTrace();
			}
		}
	}
	
	// [TEMPORARY] This prompts for the password, implement GUI in this.
	public String[] promptPassword(){
		Scanner in = new Scanner(System.in);
		System.out.print("Email: ");
		String username = in.nextLine();
		Console console = System.console();
		char passwordArray[] = console.readPassword("Password: ");
		String tempStrArr[] = {username, new String(passwordArray)};
		
		return tempStrArr;
	}
	
	public static void main(String args[]){
		new PandoraGUI();
	}
}