/**
 * Evan Reichard
 * https://github.com/evreichard
 * evan@evanreichard.com
 * 2013 - 2014
 **/

import java.util.ArrayList;
import java.io.*;
import java.net.*;
import javazoom.jl.player.Player;
import javazoom.jl.decoder.*;
import java.text.SimpleDateFormat;

public class QueueManager{

	// --------------------------------------------------------------------------------------\\
	// ------------------------------------ GLOBAL VARS ------------------------------------ \\
	// --------------------------------------------------------------------------------------\\
	
	public final static int NOTSTARTED = 0;
	public final static int PLAYING = 1;
    public final static int PAUSED = 2;
	public final static int FINISHED = 3;
	public final static int STOPPED = 4;
	
	// To access the backend, this object must be passed through in the constructor
	MainPandora pandoraBackEnd;
	
	// The object to notify threads
	private final Object threadLock = new Object();	
	
	// Current playing song
	PandoraSong currentSong = new PandoraSong();
	
	// InputStream for the current song, accessable via the threads
	InputStream is;
	
	// The actual song itself, accesses by the buffer and play thread
	byte[] currentFile;
	
	// Default saveToMP3 (create seperate class to handle prefs)
	boolean saveToMP3 = false;
	
	// The dir to store the MP3's (again, create seperate class to handle prefs)
	String mp3DIRString;
	
	// The time (in seconds)
	int time;
	
	// Est FileSize (Pass this to thread instead of global)
	int estFileSizeKB;
	
	// The estPercentDone calc and updated by the buffer thread.  As well as Song Length and Song position.
	int estPercentDone = 0;
	String currentSongLength;
	String currentSongPosition;
	
	// The playing thread (Global so we can use accessors to modify play status)
	SongPlayer liveSong;
	
	// --------------------------------------------------------------------------------------\\
	// ------------------------------------ CONSTRUCTOR ------------------------------------ \\
	// --------------------------------------------------------------------------------------\\
	
	public QueueManager(MainPandora tempPandoraBackEnd){
		pandoraBackEnd = tempPandoraBackEnd;
	}

	// --------------------------------------------------------------------------------------\\
	// ---------------------------------- CONTROL METHODS ---------------------------------- \\
	// --------------------------------------------------------------------------------------\\
	
	public void pause(){
		synchronized(threadLock){
			currentSong.setSongStatus(PAUSED);
			liveSong.pause();
		}
	}
	
	public void resume(){
		synchronized(threadLock){
			currentSong.setSongStatus(PLAYING);
			liveSong.resume();
			threadLock.notifyAll();
		}
	}
	
	public void nextSong(){
		synchronized(threadLock){
			currentSong.setSongStatus(FINISHED);
			liveSong.stop();
		}
	}
	
	public void stop(){
		synchronized(threadLock){
			currentSong.setSongStatus(STOPPED);
			liveSong.stop();
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
		return estPercentDone;
	}
	
	public String getCurrentSongLength(){
		return currentSongLength;
	}
	
	public String getCurrentSongPosition(){
		return currentSongPosition;
	}
	
	public PandoraSong getCurrentSong(){
		synchronized(threadLock){
			return currentSong;
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
					synchronized(threadLock){
						if(currentSong.getSongStatus() == STOPPED){
							songPlaylist.removeAll(songPlaylist);
							
							// We set this so we no longer get stuck in this loop if we choose another station
							currentSong.setSongStatus(FINISHED);
							break;
						}
						
						currentSong = songPlaylist.get(0);
					}
					
					String streamURL = currentSong.getAudioUrl();

					// Play file
					try{
						URL url = new URL(streamURL);
						HttpURLConnection urlConnect = (HttpURLConnection)url.openConnection();
						int size = urlConnect.getContentLength();
						urlConnect.disconnect();

						is = url.openStream();
						
						Bitstream stream = new Bitstream(is);
						Header header = stream.readFrame();
						stream.unreadFrame();
						
						time = (int)(header.total_ms(size) / 1000);
						
						estFileSizeKB = (time * 128) / 8;
						
						if(time == 42){
							System.out.print("Skipping song...");
						}else{

							currentFile = new byte[size];
							
							ByteArrayInputStream readFromArray = new ByteArrayInputStream(currentFile);
				
							BufferManager liveBuffer = new BufferManager(); 
							ProgressManager liveProgress = new ProgressManager();
							liveSong = new SongPlayer(readFromArray);
							
							Thread bufferThread = new Thread(liveBuffer);
							Thread progressThread = new Thread(liveProgress);
							Thread songThread = new Thread(liveSong);
							
							// If skipped previously, we need to switch from "FINISHED" to "PLAYING" before we start any threads
							// to make sure they don't prematurely end
							synchronized(threadLock){
								currentSong.setSongStatus(PLAYING);
							}
							
							progressThread.start();
							songThread.start();
							bufferThread.start();

							bufferThread.join();
							progressThread.join();
							songThread.join();
							
							currentFile = null;
							System.gc();
						}
						
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
		
		public void run() {
		
				try{
						int byteLength = 0;
						int byteRead = 0;
						byte[] byteArray = new byte[512];
						
						// Loops while there is still input stream data
						while((byteLength = is.read(byteArray)) != -1){
							
							// Appends bytes to currentFile index 0 byte array
							System.arraycopy(byteArray, 0,  currentFile, byteRead, byteLength);
							byteRead += byteLength;
							
							estPercentDone = (int)((((double)byteRead / 1024.0) / (double)estFileSizeKB) * 100.0);
							
							// If estimation is above 100, set to 100% (We'll be done in no time at this point)
							if(estPercentDone > 100){
								estPercentDone = 100;
							}
							
							synchronized(threadLock){
								if(currentSong.getSongStatus() == FINISHED || currentSong.getSongStatus() == STOPPED){
									break;
								}
								
								// Notifies the play thread that we're ready to go (This is only needed to have a timer and play locally if enabled)
								if(byteRead > 49152){
									threadLock.notifyAll();
								}
							}
						}
						
						synchronized(threadLock){
							if(saveToMP3 && currentSong.getSongStatus() != FINISHED){
								OutputStream writeStream = new FileOutputStream(new File(mp3DIRString + currentSong.getArtistName() + " - " + currentSong.getSongName() + ".mp3"));
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
					currentSong.setSongStatus(PLAYING);
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
				currentSongLength = formatTime.format(time * 1000);
				
				for(int i = 0; i <= time; i++){
					currentSongPosition = formatTime.format(i * 1000);
					
					synchronized (threadLock) {
						while (currentSong.getSongStatus() == PAUSED) {
							try {
								threadLock.wait();
							}catch(Exception e){}
						}
						
						if(currentSong.getSongStatus() == FINISHED || currentSong.getSongStatus() == STOPPED){
							break;
						}
					}
					
					System.out.print("\r" + getBufferPercentage() + "% : " + currentSong.getArtistName() + " - " + currentSong.getSongName() + " (" + currentSongPosition + " / " + currentSongLength + ")");
					
					try{
						Thread.sleep(1000);
					}catch(Exception e){}

				}
				
				System.out.println("\r" + "Playing: " + currentSong.getArtistName() + " - " + currentSong.getSongName() + "                 ");
				
			}catch(Exception e){}
			
		}
	}
	
}