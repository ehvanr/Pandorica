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
		String stationId = tempStationList.get(selection).get(1);
		
		QueueManager queueMan = new QueueManager(pandoraBackEnd);
		queueMan.saveAsMP3(true);
		
		queueMan.playStation(stationId);
		
		selection = in.nextInt() - 1;
		
		queueMan.pause();
		
		selection = in.nextInt() - 1;
		
		queueMan.resume();
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