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

/**
 * This class contains the meat behind playing and queuing songs.  It provides the
 * ability to manipulate the play status of the current song, change the currently
 * playing station as well as receive current song information.
 **/
public class QueueManager{

	// --------------------------------------------------------------------------------------\\
	// ------------------------------------ GLOBAL VARS ------------------------------------ \\
	// --------------------------------------------------------------------------------------\\
	
	// Receives Singleton Object (And creates it if it doesn't exist)
	private MainPandora pandoraBackEnd = MainPandora.getInstance();
	
	// The Curent Song (Can we combine these into the PandoraSong class?)
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
	
	// The singleton object instance
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
	 * Returns the singleton QueueManager instance.  If it doesn't already exist
	 * it creates one. 
	 * 
	 * @return The new or existing QueueManager instance
	 **/
	public static synchronized QueueManager getInstance(){
		if(INSTANCE == null){
			// Initiate Object
			INSTANCE = new QueueManager();
		}
		
		return INSTANCE;
	}
	
	/**
	 * Pauses the current song.  This feature is deprecated and shouldn't be used 
	 * if possible. Use toggle() instead.
	 **/
	public void pause(){
		synchronized(threadLock){
			currentSongInfo.setSongStatus(PlayerStatus.PAUSED);
			liveSong.pause();
		}
	}
	
	/**
	 * Resumes the current song.  This feature is deprecated and shouldn't be used 
	 * if possible. Use toggle() instead.
	 **/
	public void resume(){
		synchronized(threadLock){
			currentSongInfo.setSongStatus(PlayerStatus.PLAYING);
			liveSong.resume();
			threadLock.notifyAll();
		}
	}
	
	/**
	 * Toggles the current song.  If the current song is playing, it pauses it.  
	 * If it's paused it resumes it.  It does nothing if the current song isn't
	 * either.
	 **/
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
	
	/**
	 * Skips the current song and procedes to the next one in queue.
	 **/
	public void nextSong(){
		synchronized(threadLock){
			currentSongInfo.setSongStatus(PlayerStatus.FINISHED);
			liveSong.stop();
		}
	}
	
	/**
	 * This completely stops the player and ends the ThreadedQueue thread.
	 * Once executed, nothing should be playing or paused and you can start 
	 * another station if you wish. 
	 **/
	public void stop(){
		synchronized(threadLock){
			if(liveSong != null){
				currentSongInfo.setSongStatus(PlayerStatus.STOPPED);
				liveSong.stop();
			}
		}
	}
	
	/**
	 * Starts a station with the tempStationId parameter.  The player will
	 * continue to play that station unless stopped by the stop() method. 
	 * 
	 * @param stationId the station ID to play
	 **/
	public void playStation(String stationId){
		ThreadedQueue songQueue = new ThreadedQueue(stationId);
		Thread queueThread = new Thread(songQueue);
		queueThread.start();
	}
	
	/**
	 * If set true, the player will save the songs as MP3's.  Otherwise it
	 * wont. 
	 *
	 * @param value whether to save the songs as MP3's
	 **/
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
	
	/**
	 * Returns the total estimated buffer percentage of the current song.  This will 
	 * probably be off +/- 1-3%;
	 *
	 * @return The total estimated buffer amount
	 **/
	public int getBufferPercentage(){
		synchronized(threadLock){
			return estPercentDone;
		}
	}
	
	/**
	 * Returns the current songs length in mm:ss
	 *
	 * @return The current songs parsed length
	 **/
	public String getCurrentSongLength(){
		synchronized(threadLock){
			return currentSongInfoLength;
		}
	}
	
	/**
	 * Returns the current songs position in mm:ss
	 *
	 * @return The current songs parsed position
	 **/
	public String getCurrentSongPosition(){
		synchronized(threadLock){
			return currentSongInfoPosition;
		}
	}
	
	/**
	 * Returns the current songs PandoraSong object. This object allows you 
	 * to access information about the song such as song name, artist name,
	 * etc.
	 *
	 * @return The current songs PandoraSong object
	 **/
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
	 * It only stops when the stop() method is called which sets 
	 * playerStatus = PlayerStatus.STOPPED where it breaks through the 
	 * while(true) loop and the thread naturally ends.
	 **/
	private class ThreadedQueue implements Runnable{
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
						
						// Commenting out until we can incorporate a proper way to deal with the "42 second song of death"
						
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
	 * This class will buffer the current song in memory, estimate the current 
	 * buffer percentage, and save the file to MP3 (if set).
	 **/
	private class BufferManager implements Runnable{
		
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
	
	/**
	 * This class plays the ByteArrayInputStream (stream generated from the buffered song). 
	 **/
	private class SongPlayer extends PandoraPlayer implements Runnable{
		
		
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
	
	/**
	 * This class updates the main class with the current songs length, as well as updates
	 * the length progress of the song every second.
	 **/
	private class ProgressManager implements Runnable{
		
		public void run(){
			
			try{
			
				SimpleDateFormat formatTime = new SimpleDateFormat("mm:ss");
				
				synchronized(threadLock){
					currentSongInfoLength = formatTime.format(time * 1000);
				}
				
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
					
						currentSongInfoPosition = formatTime.format(i * 1000);
					}
					
					try{
						Thread.sleep(1000);
					}catch(Exception e){}

				}
			}catch(Exception e){}
		}
	}
	
}