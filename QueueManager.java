/**
 * Evan Reichard
 * https://github.com/evreichard
 * evan@evanreichard.com
 * 2013 - 2014
 **/

import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import javazoom.jl.decoder.*;
import javazoom.jl.player.Player;

public class QueueManager{

	// --------------------------------------------------------------------------------------\\
	// ------------------------------------ GLOBAL VARS ------------------------------------ \\
	// --------------------------------------------------------------------------------------\\
	
	// Receives Singleton Object (And creates it if it doesn't exist)
	private MainPandora pandoraBackEnd = MainPandora.getInstance();
	
	// The Curent Song (Can we combine these?)
	private PandoraSong currentSongInfo = new PandoraSong();
	private SongPlayer liveSong;
	private byte[] currentFile;
	
	private final Object threadLock = new Object();
	
	// On a per-object level?
	private boolean saveToMP3 = false;
	
	private int estPercentDone = 0;
	private String mp3DIRString;
	
	// Set in PandoraSong with accessors / setters
	private int time;
	private String currentSongInfoLength;
	private String currentSongInfoPosition;
	
	private static QueueManager INSTANCE;
	
	// --------------------------------------------------------------------------------------\\
	// ------------------------------------ CONSTRUCTOR ------------------------------------ \\
	// --------------------------------------------------------------------------------------\\
	
	// Can only be accessed from the getInstance() method (This prevents accidentally spawning another QueueManager object)
	private QueueManager(){}

	// --------------------------------------------------------------------------------------\\
	// ---------------------------------- CONTROL METHODS ---------------------------------- \\
	// --------------------------------------------------------------------------------------\\
	
	/**
	 * Singleton Object
	 **/
	public static synchronized QueueManager getInstance(){
		if(INSTANCE == null){
			// Initiate Object
			INSTANCE = new QueueManager();
		}
		
		return INSTANCE;
	}
	
	public void pause(){
		synchronized(threadLock){
			currentSongInfo.setSongStatus(PlayerStatus.PAUSED);
			liveSong.pause();
		}
	}
	
	public void resume(){
		synchronized(threadLock){
			currentSongInfo.setSongStatus(PlayerStatus.PLAYING);
			liveSong.resume();
			threadLock.notifyAll();
		}
	}
	
	public void toggle(){
		synchronized(threadLock){
			if(currentSongInfo.getSongStatus() == PlayerStatus.PLAYING){
				currentSongInfo.setSongStatus(PlayerStatus.PAUSED);
				liveSong.pause();
			}else if(currentSongInfo.getSongStatus() == PlayerStatus.PAUSED){
				currentSongInfo.setSongStatus(PlayerStatus.PLAYING);
				liveSong.resume();
				threadLock.notifyAll();
			}
		}
	}
	
	public void nextSong(){
		synchronized(threadLock){
			currentSongInfo.setSongStatus(PlayerStatus.FINISHED);
			liveSong.stop();
		}
	}
	
	public void stop(){
		synchronized(threadLock){
			if(liveSong != null){
				currentSongInfo.setSongStatus(PlayerStatus.STOPPED);
				liveSong.stop();
			}
		}
	}
	
	/**
	 * This method will execute the ThreadedQueue with the stationId param.
	 **/
	public void playStation(String tempStationId){
		ThreadedQueue playQueue = new ThreadedQueue(tempStationId);
		playQueue.start();
	}
	
	// This should be set in a setting.  
	public void saveAsMP3(boolean value){
		saveToMP3 = value;
		
		if(value){
			mp3DIRString = System.getProperty("user.home") + "//MP3//";
			File mp3DIR = new File(mp3DIRString);

			if (!mp3DIR.exists()){
				mp3DIR.mkdir();
			}
		}
	}
	
	// --------------------------------------------------------------------------------------\\
	// ---------------------------------- ACCESSOR METHODS ----------------------------------\\
	// --------------------------------------------------------------------------------------\\
	
	public int getBufferPercentage(){
		synchronized(threadLock){
			return estPercentDone;
		}
	}
	
	public String getCurrentSongLength(){
		synchronized(threadLock){
			return currentSongInfoLength;
		}
	}
	
	public String getCurrentSongPosition(){
		synchronized(threadLock){
			return currentSongInfoPosition;
		}
	}
	
	public PandoraSong getCurrentSong(){
		synchronized(threadLock){
			return currentSongInfo;
		}
	}
	
	// --------------------------------------------------------------------------------------\\
	// ------------------------------- INNER THREADED CLASSES -------------------------------\\
	// --------------------------------------------------------------------------------------\\
	
	/**
	 * This class will infinitely call and loop through stationId songs.  
	 * It is only broken when the stop() method is called which sets
	 * playerStatus = STOPPED.
	 **/
	class ThreadedQueue extends Thread{
		ArrayList<PandoraSong> songPlaylist = new ArrayList<PandoraSong>();
		String stationId;
		
		public ThreadedQueue(String tempStationId){
			stationId = tempStationId;
		}
	
		public void run(){
			
			while(true){
			
				// If queue is less than 2
				if(songPlaylist.size() < 2){
					
					// Request new songs for current stationId
					ArrayList<PandoraSong> tempPlaylist = pandoraBackEnd.getPlaylist(stationId);
					
					// Appends those songs on the playlist queue
					for(PandoraSong tempSong : tempPlaylist){
						songPlaylist.add(tempSong);
					}
				}else{
					
					String streamURL;
					
					synchronized(threadLock){
						if(currentSongInfo.getSongStatus() == PlayerStatus.STOPPED){
							songPlaylist.removeAll(songPlaylist);
							
							// We set this so we no longer get stuck in this loop if we choose another station
							currentSongInfo.setSongStatus(PlayerStatus.FINISHED);
							break;
						}
						
						currentSongInfo = null;
						currentSongInfo = songPlaylist.get(0);
					
						streamURL = currentSongInfo.getAudioUrl();
					}
					
					// Play file
					try{
						URL url = new URL(streamURL);
						HttpURLConnection urlConnect = (HttpURLConnection)url.openConnection();
						int size = urlConnect.getContentLength();
						urlConnect.disconnect();

						InputStream is = url.openStream();
						
						Bitstream stream = new Bitstream(is);
						
						Header header = stream.readFrame();
						stream.unreadFrame();
						
						time = (int)(header.total_ms(size) / 1000);
						
						int tempEstFileSizeKB = (time * 128) / 8;
						
						// if(time == 42){
						//	System.out.print("Skipping song...");
						//}else{

							currentFile = new byte[size];
							
							ByteArrayInputStream readFromArray = new ByteArrayInputStream(currentFile);
				
							BufferManager liveBuffer = new BufferManager(is, tempEstFileSizeKB); 
							ProgressManager liveProgress = new ProgressManager();
							liveSong = new SongPlayer(readFromArray);
							
							Thread bufferThread = new Thread(liveBuffer);
							Thread progressThread = new Thread(liveProgress);
							Thread songThread = new Thread(liveSong);
							
							// If skipped previously, we need to switch from "FINISHED" to "PLAYING" before we start any threads
							// to make sure they don't prematurely end
							synchronized(threadLock){
								currentSongInfo.setSongStatus(PlayerStatus.PLAYING);
							}
							
							progressThread.start();
							songThread.start();
							bufferThread.start();

							bufferThread.join();
							progressThread.join();
							songThread.join();
							
							currentFile = null;
							System.gc();
						//}
						
						stream.close();
						is.close();
						
					}catch(Exception e){
						e.printStackTrace();
					}
					songPlaylist.remove(0);
				}
			}
		}
	}
	
	/**
	 * THE FOLLOWING ARE THE THREADS FOR THE BUFFER, PLAYER, AND PROGRESS (WHICH WILL BE DEPRECATED SOON)
	 **/
	
	class BufferManager implements Runnable{
		
		int estFileSizeKB;
		InputStream tempIS;
		
		public BufferManager(InputStream parentIS, int tempEstFileSizeKB){
			estFileSizeKB = tempEstFileSizeKB;
			tempIS = parentIS;
		}
		
		public void run(){
		
				try{
						int byteLength = 0;
						int byteRead = 0;
						byte[] byteArray = new byte[512];
						
						// Loops while there is still input stream data
						while((byteLength = tempIS.read(byteArray)) != -1){
							
							synchronized(threadLock){
								// Appends bytes to currentFile index 0 byte array.  Hard to read, but it's quick.
								System.arraycopy(byteArray, 0, currentFile, byteRead, byteLength);
								byteRead += byteLength;
							
								// Caclulate Buffer % done (Should this be updated on that actual song object? And then 
								// accessed via the object received from this class?)
								estPercentDone = (int)((((double)byteRead / 1024.0) / (double)estFileSizeKB) * 100.0);
								
								// If estimation is above 100, set to 100% (We'll be done in no time at this point)
								if(estPercentDone > 100){
									estPercentDone = 100;
								}
							
								if(currentSongInfo.getSongStatus() == PlayerStatus.FINISHED || currentSongInfo.getSongStatus() == PlayerStatus.STOPPED){
									break;
								}
								
								// WE SHOULD IMPLEMENT AN ACTUAL BUFFER THAT PAUSES AND RESUMES THE CURRENT SONG
								if(byteRead > 49152){
									threadLock.notifyAll();
								}
							}
						}
						
						synchronized(threadLock){
							
							estPercentDone = 100;
						
							// This is what determines if we are going to save the song or not (Dependent on whether the song is finished and 
							// whether we previously set the saveToMP3 boolean value to true)
							if(saveToMP3 && currentSongInfo.getSongStatus() != PlayerStatus.FINISHED){
								OutputStream writeStream = new FileOutputStream(new File(mp3DIRString + currentSongInfo.getArtistName() + " - " + currentSongInfo.getSongName() + ".mp3"));
								writeStream.write(currentFile);
								writeStream.flush();
								writeStream.close();
							}
						}
						
						// Nulls out byteArray in prep for next song
						byteArray = null;
						
				}catch(Exception e){
					e.printStackTrace();
				}
		}
	}
	
	class SongPlayer extends PandoraPlayer implements Runnable{
		
		public SongPlayer(InputStream inputStream) throws JavaLayerException{
			super(inputStream);
		}
		
		public void run() {
			
			try{
				synchronized(threadLock){
					threadLock.wait();
					currentSongInfo.setSongStatus(PlayerStatus.PLAYING);
				}
				
				play();
				
			}catch(Exception e){
				e.printStackTrace();
			}
		}
	}
	
	class ProgressManager implements Runnable{
		
		public void run(){
			
			try{
			
				SimpleDateFormat formatTime = new SimpleDateFormat("mm:ss");
				currentSongInfoLength = formatTime.format(time * 1000);
				
				for(int i = 0; i <= time; i++){
					synchronized (threadLock) {
						while (currentSongInfo.getSongStatus() == PlayerStatus.PAUSED) {
							try {
								threadLock.wait();
							}catch(Exception e){}
						}
						
						if(currentSongInfo.getSongStatus() == PlayerStatus.FINISHED || currentSongInfo.getSongStatus() == PlayerStatus.STOPPED){
							break;
						}
					}
					
					currentSongInfoPosition = formatTime.format(i * 1000);
					
					try{
						Thread.sleep(1000);
					}catch(Exception e){}

				}
			}catch(Exception e){}
			
		}
	}
	
}