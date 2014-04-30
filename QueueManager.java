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
	private final static int NOTSTARTED = 0;
	private final static int PLAYING = 1;
    private final static int PAUSED = 2;
	private final static int FINISHED = 3;
	private final static int STOPPED = 4;
	
	private int playerStatus = NOTSTARTED;
	
	// To access the backend, this object must be passed through in the constructor
	MainPandora pandoraBackEnd;
	
	// The current stationId (So we can process calls to the backend) 
	// Might want to set this in the constructor as well?
	String stationId;
	
	// The object to notify threads
	private final Object threadLock = new Object();
	
	// The current songPlaylist
	ArrayList<PandoraSong> songPlaylist = new ArrayList<PandoraSong>();
	
	// The writeStream to write to file (This shouldn't be global, set this in the buffer thread in the if save as mp3 statement)
	OutputStream writeStream;
	
	// InputStream for the current song, accessable via the threads
	InputStream is;
	
	String fileName;
	
	// The actual song itself, accesses by the buffer and play thread
	byte[] currentFile;
	
	// Default saveToMP3 (create seperate class to handle prefs)
	boolean saveToMP3 = false;
	
	// The dir to store the MP3's (again, create seperate class to handle prefs)
	String mp3DIRString;
	
	// The time (in seconds), seconds and minutes of the current song (Do we need this?)
	int time;
	int seconds;
	int minutes;
	
	// Est FileSize (Pass this to thread instead of global)
	double estFileSizeKB;
	
	// The estPercentDone calc and updated by the buffer thread.  As well as Song Length and Song position.
	int estPercentDone = 0;
	String currentSongLength;
	String currentSongPosition;
	
	// The playing thread (Global so we can use accessors to modify play status)
	SongPlayer liveSong;
	
	public QueueManager(MainPandora tempPandoraBackEnd){
		pandoraBackEnd = tempPandoraBackEnd;
	}

	public void addSong(PandoraSong _playObject){
		songPlaylist.add(_playObject);
	}
	
	public void pause(){
		synchronized(threadLock){
			playerStatus = PAUSED;
			liveSong.pause();
		}
	}
	
	public void resume(){
		synchronized(threadLock){
			playerStatus = PLAYING;
			liveSong.resume();
			threadLock.notifyAll();
		}
	}
	
	public void nextSong(){
		synchronized(threadLock){
			playerStatus = FINISHED;
			liveSong.stop();
		}
	}
	
	public void stop(){
		synchronized(threadLock){
			playerStatus = STOPPED;
			liveSong.stop();
		}
	}
	
	public int getBufferPercentage(){
		return estPercentDone;
	}
	
	public String getCurrentSongLength(){
		return currentSongLength;
	}
	
	public String getCurrentSongPosition(){
		return currentSongPosition;
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
	
	// Thow song not in queue
	public void playQueue() throws NoSongInQueueException{
		
		if(songPlaylist.isEmpty()){
			throw new NoSongInQueueException("No songs in queue");
		}else{
		
			PandoraSong currentSong = songPlaylist.get(0);
		
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
				seconds = time % 60;
				minutes = (time - seconds) / 60;
				
				
				estFileSizeKB = (double)((time * 128) / 8);
				
				String duration = minutes + ":" + seconds + ")";
				
				// 332362 (If we're using bytes instead of time - probably better to do)
				if(time == 42){
					// Skip song
					// This happens if we skip too much.  Pandora returns an empty 42 seconds long stream (CHANGE STATIONS AND WAIT, need to figure out how to get around)
					// Possible get around: Create Quicklist with only one stationID (Try it!)
					System.out.print("Skipping song...");
				}else{

					currentFile = new byte[size];
					
					fileName = "Playing: " + currentSong.getArtistName() + " - " + currentSong.getSongName();
					
					writeStream = new FileOutputStream(new File(mp3DIRString + currentSong.getArtistName() + " - " + currentSong.getSongName() + ".mp3"));
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
						playerStatus = PLAYING;
					}
					
					progressThread.start();
					songThread.start();
					bufferThread.start();

					bufferThread.join();
					progressThread.join();
					songThread.join();
					
					writeStream.close();
					currentFile = null;
					System.gc();
				}
				
				stream.close();
				is.close();
				
				songPlaylist.remove(0);
				
			}catch(Exception e){
				e.printStackTrace();
			}
			
		}
	}

	public void playStation(String tempStationId){
		stationId = tempStationId;
		
		synchronized(threadLock){
			playerStatus = NOTSTARTED;
		}
		
		ThreadedQueue playQueue = new ThreadedQueue();
		playQueue.start();
	}
	
	class ThreadedQueue extends Thread{
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
					// Plays song on top
					
					synchronized(threadLock){
						if(playerStatus == STOPPED){
							songPlaylist.removeAll(songPlaylist);
							break;
						}
					}
					
					try{
						playQueue();
					}catch(NoSongInQueueException nsiqe){
						nsiqe.printStackTrace();
					}
					
					songPlaylist.remove(0);
				}
			}
		}
	}
	
	class BufferManager implements Runnable{
		
		public void run() {
		
				try{
						// int counter = 0;
						int length = 0;
						int read = 0;
						byte[] bytes = new byte[512];
						
						// Loops while there is still input stream data
						while((length = is.read(bytes)) != -1){
							
							// Appends bytes to currentFile index 0 byte array
							System.arraycopy(bytes, 0,  currentFile, read, length);
							//writeStream.write(bytes, 0, length);
							read += length;
							
							if(estPercentDone > 100){
								estPercentDone = 100;
							}
							
							synchronized(threadLock){
								if(playerStatus == FINISHED || playerStatus == STOPPED){
									break;
								}
								
								// Notifies the play thread that we're ready to go (This is only needed to have a timer and play locally if enabled)
								if(read > 49152){
									threadLock.notifyAll();
								}
							}
						}
						
						synchronized(threadLock){
							// If saveToMP3, write out to file.  Otherwise, skip.
							if(saveToMP3 && playerStatus != FINISHED){
								writeStream.write(currentFile);
							}
						}
						
						bytes = null;
						
						writeStream.flush();
						writeStream.close();
					
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
					playerStatus = PLAYING;
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
					
					// ---------------
					synchronized (threadLock) {
						while (playerStatus == PAUSED) {
							try {
								threadLock.wait();
							}catch(Exception e){}
						}
						
						if(playerStatus == FINISHED || playerStatus == STOPPED){
							break;
						}
					}
					
				
					// ---------------
					
					System.out.print("\r" + fileName + " (" + currentSongPosition + " / " + currentSongLength + ")");
					
					try{
						Thread.sleep(1000);
					}catch(Exception e){}

				}
			

				System.out.println("\r" + fileName + "                 ");
			}catch(Exception e){}
			
		}
	}
	
}