/**
 * Evan Reichard
 * https://github.com/evreichard
 * evan@evanreichard.com
 * 2013 - 2014
 **/

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.util.ArrayList;
import java.io.*;
import java.net.*;
import javax.imageio.*;
import java.util.Scanner;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;


public class PandoraGUI{

	public final static int NOTSTARTED = 0;
	public final static int PLAYING = 1;
    public final static int PAUSED = 2;
	public final static int FINISHED = 3;
	public final static int STOPPED = 4;
	
	MainPandora pandoraBackEnd = new MainPandora();
	QueueManager queueMan = new QueueManager(pandoraBackEnd);
	Scanner in = new Scanner(System.in);
	
	PandoraSong currentSong = new PandoraSong();
	private final Object threadLock = new Object();

	JComboBox<ComboItem> stationCB = new JComboBox<ComboItem>();
	JLabel songTitle = new JLabel();
	JLabel artistName = new JLabel();
	JLabel songProgress = new JLabel();
	JLabel albumArt = new JLabel();
	
	public PandoraGUI(){
		PromptPassword();
	}
	
	public void LoadMainScreen(){
		JFrame mainFrame = new JFrame("Pandorica");
		mainFrame.setSize(800, 370);
		mainFrame.setLayout(new BorderLayout());
		
		songTitle.setFont(new Font("Serif", Font.PLAIN, 24));
		artistName.setFont(new Font("Serif", Font.BOLD, 26));
		songProgress.setFont(new Font("Serif", Font.PLAIN, 18));
		albumArt.setBorder(new EmptyBorder(10, 10, 10, 10));
		
		JButton pausePlayButton = new JButton("||");
		JButton nextButton = new JButton(">>");
		
		pausePlayButton.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent ae){
				queueMan.toggle();
			}
		});
		
		nextButton.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent ae){
				queueMan.nextSong();
			}
		});
		
		stationCB.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent ae){
				queueMan.stop();
				ComboItem tempItem = (ComboItem)stationCB.getSelectedItem();
				queueMan.playStation(tempItem.getValue());
			}
		});
		
		JPanel rightContent = new JPanel(new GridLayout(7, 0));
		JPanel buttonPanel = new JPanel();
		
		rightContent.setBorder(new EmptyBorder(30, 20, 20, 30));
		
		buttonPanel.add(pausePlayButton);
		buttonPanel.add(nextButton);
		buttonPanel.add(stationCB);
		
		rightContent.add(songTitle);
		rightContent.add(artistName);
		rightContent.add(songProgress);
		rightContent.add(new JPanel());
		rightContent.add(new JPanel());
		rightContent.add(new JPanel());
		rightContent.add(buttonPanel);
		
		mainFrame.add(albumArt, BorderLayout.WEST);
		mainFrame.add(rightContent, BorderLayout.CENTER);
		mainFrame.setLocationRelativeTo(null);
		mainFrame.setVisible(true);
		mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		// We lock here
		SongContentUpdate songClass = new SongContentUpdate();
		Thread songPlayingThread = new Thread(songClass);
		songPlayingThread.start();
	}
	
	public void PromptPassword(){
		final JFrame passwordFrame = new JFrame("Pandorica Login");
		passwordFrame.setSize(350, 200);
		passwordFrame.setLayout(new GridLayout(3, 0));
		
		JLabel emailLabel = new JLabel("Email:");
		JLabel passwordLabel = new JLabel("Password:");
		
		final JTextField emailField = new JTextField();
		final JPasswordField passwordField = new JPasswordField();
		
		JButton loginButton = new JButton("Login");
		
		loginButton.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent ae){
				if(pandoraBackEnd.pandoraLogin(emailField.getText(), new String(passwordField.getPassword()))){
					passwordFrame.setVisible(false);
					PopulateStations();
					LoadMainScreen();
					// queueMan.saveAsMP3(true);
				}
			}
		});
		
		JPanel emailPanel = new JPanel(new GridLayout(0, 2));
		JPanel passwordPanel = new JPanel(new GridLayout(0, 2));
		
		emailPanel.add(emailLabel);
		emailPanel.add(emailField);
		
		passwordPanel.add(passwordLabel);
		passwordPanel.add(passwordField);
		
		passwordFrame.add(emailPanel);
		passwordFrame.add(passwordPanel);
		passwordFrame.add(loginButton);
		
		passwordFrame.setLocationRelativeTo(null);
		passwordFrame.setVisible(true);
		passwordFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	}
	
	class SongContentUpdate implements Runnable{
		public void run(){
			synchronized(threadLock){
				while(true){

					// On first start, we wait for song to start playing
					while(queueMan.getCurrentSong().getSongStatus() != PLAYING){						
						
						// Sleep for 100ms
						try{
							Thread.sleep(100);
						}catch(Exception e){}
					}
					
					// Gather playing song
					currentSong = queueMan.getCurrentSong();
					
					// Set song title & artist
					songTitle.setText(currentSong.getSongName());
					artistName.setText(currentSong.getArtistName());
					
					// Set album art
					try{
						URL sourceImage = new URL(currentSong.getAlbumArtUrl());
						Image AAImage = ImageIO.read(sourceImage).getScaledInstance(300, 300,  java.awt.Image.SCALE_SMOOTH);  
						albumArt.setIcon(new ImageIcon(AAImage));
						
					}catch(Exception e){}
					
					// Loop while our cached song is the same as the one in queue
					while(currentSong.getAudioUrl().equals(queueMan.getCurrentSong().getAudioUrl())){
						
						// Set song progress
						songProgress.setText(queueMan.getCurrentSongPosition() + " / " + queueMan.getCurrentSongLength());
						
						// Sleep for 100ms
						try{
							Thread.sleep(100);
						}catch(Exception e){}
					}
					
				}
			}
		}
	}
	
	class ComboItem{
	
		private String key;
		private String value;

		public ComboItem(String key, String value)
		{
			this.key = key;
			this.value = value;
		}

		@Override
		public String toString()
		{
			return key;
		}

		public String getKey()
		{
			return key;
		}

		public String getValue()
		{
			return value;
		}
	}
	
	public void PopulateStations(){
		// Get Station List
		ArrayList<ArrayList<String>> stationList = pandoraBackEnd.getStationList();
		
		// Print and choose station
		for(ArrayList<String> tempAL : stationList){
			// stationCB.addItem(tempAL.get(0));
			stationCB.addItem(new ComboItem(tempAL.get(0), tempAL.get(1)));
		}
	}
	
	public static void main(String args[]){
		new PandoraGUI();
	}
}