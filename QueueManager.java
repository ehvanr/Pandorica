import java.util.ArrayList;
import java.io.*;
import java.net.*;
import javazoom.jl.player.Player;
import javazoom.jl.decoder.*;
import java.text.SimpleDateFormat;

public class QueueManager{
	MainPandora pandoraBackEnd;
	PandoraPlayer playTrack;
	String stationId;
	private final Object threadLock = new Object();
	ArrayList<PandoraSong> songPlaylist = new ArrayList<PandoraSong>();
	
	OutputStream writeStream;
	InputStream is;
	String fileName;
	byte[] currentFile;
	
	String mp3DIRString;
	int time;
	int seconds;
	int minutes;
	
	SongPlayer liveSong;
	
	public QueueManager(MainPandora tempPandoraBackEnd){
		pandoraBackEnd = tempPandoraBackEnd;
	}

	public void addSong(PandoraSong _playObject){
		songPlaylist.add(_playObject);
	}
	
	public void pause(){
		liveSong.pause();
	}
	
	public void resume(){
		liveSong.resume();
	}
	
	public void stop(){
		liveSong.stop();
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
		
					ProgressManager liveProgress = new ProgressManager();
					liveSong = new SongPlayer(readFromArray);
					BufferManager liveBuffer = new BufferManager(); 
					
					Thread progressThread = new Thread(liveProgress);
					Thread songThread = new Thread(liveSong);
					Thread bufferThread = new Thread(liveBuffer);

					progressThread.start();
					songThread.start();
					bufferThread.start();

					progressThread.join();
					songThread.join();
					bufferThread.join();

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
		ThreadedQueue playQueue = new ThreadedQueue();
		playQueue.start();
	}
	
	class ThreadedQueue extends Thread{
		public void run(){
			// Thread me
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
							writeStream.write(bytes, 0, length);
							read += length;
							
							// Notifies the play thread that we're ready to go (This is only needed to have a timer and play locally if enabled)
							if(read > 49152){
								synchronized(threadLock){
									threadLock.notifyAll();
								}
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
				}
				
				play();
				
			}catch(Exception e){
				e.printStackTrace();
			}
		}
	}
	
	class ProgressManager implements Runnable{
		
		String songLength;
		
		public void run(){
			
			try{
			
				SimpleDateFormat formatTime = new SimpleDateFormat("mm:ss");
				songLength = formatTime.format(time * 1000);
				
				synchronized(threadLock){
					threadLock.wait();
				}

				for(int i = 0; i <= time; i++){
					
					System.out.print("\r" + fileName + " (" + formatTime.format(i * 1000) + " / " + songLength + ")");
					
					try{
						Thread.sleep(1000);
					}catch(Exception e){}

				}

				System.out.println("\r" + fileName + "                 ");
			}catch(Exception e){}
			
		}
	}
	
}