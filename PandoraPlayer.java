/**
 * Evan Reichard
 * https://github.com/evreichard
 * evan@evanreichard.com
 * April 2013
 *
 * BETA 1
 **/

import javazoom.jl.player.Player;
import javazoom.jl.decoder.*;
import com.google.gson.*;
import java.util.ArrayList;
import java.io.*;
import java.nio.*;
import java.net.*;
import java.text.SimpleDateFormat;

public class PandoraPlayer{
	
	//OutputStream writeStream;
	InputStream is;
	String fileName;
	
	private final int PORT = 12346;
	private final String GROUP = "225.0.50.0";

	ArrayList<JsonObject> songPlaylist = new ArrayList<JsonObject>();
	ArrayList<byte[]> songCache = new ArrayList<byte[]>();
	private final Object playerLock = new Object();
	
	String mp3DIRString;
	int time;
	int seconds;
	int minutes;
	
	public PandoraPlayer(JsonObject _playObject){
		songPlaylist.add(_playObject);
		mp3DIRString = System.getProperty("user.home") + "//MP3//";
	}
	
	public PandoraPlayer(ArrayList<JsonObject> _songPlaylist){
		songPlaylist = _songPlaylist;
		mp3DIRString = System.getProperty("user.home") + "//MP3//";
	}
	
	public void addSong(JsonObject _playObject){
		songPlaylist.add(_playObject);
	}
	
	// Throw song not playing
	public void pause(){
		
	}
	
	// Thow song not in queue
	public void play() throws NoSongInQueueException{
		
		if(songPlaylist.isEmpty()){
			throw new NoSongInQueueException("No songs in queue");
		}else{
		
			for(JsonObject currentSong : songPlaylist){
			
				String streamURL = currentSong.get("additionalAudioUrl").getAsString();

				// Play file
				try{
					URL url = new URL(streamURL);
					URLConnection urlConnect = url.openConnection();
					int size = urlConnect.getContentLength();
					urlConnect.getInputStream().close();

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

						byte[] currentFile = new byte[size];
						songCache.add(currentFile);
						
						fileName = "Playing: " + currentSong.get("artistName").getAsString() + " - " + currentSong.get("songName").getAsString();
						
						//writeStream = new FileOutputStream(new File(mp3DIRString + currentSong.get("artistName").getAsString() + " - " + currentSong.get("songName").getAsString() + ".mp3"));

						// NOTE: PlayThread commented out, we're passing play functionality to clients via MulticastSocket (toServer thread)
						
						//PlayThread MP3player = new PlayThread();
						ProgressThread timeCount = new ProgressThread();
						WriteThread toServer = new WriteThread();
	
						timeCount.start();
						//MP3player.start();
						toServer.start();

						timeCount.join();
						//MP3player.join();
						toServer.join();

						//writeStream.close();
						songCache.remove(currentFile);
					}
					
					stream.close();
					is.close();
					
				}catch(Exception e){
					e.printStackTrace();
				}
			}
		}
	}
	
	class WriteThread extends Thread{
	
		// NOTE: WriteThread has a few things commented out
		// Commented out portions include the writing to file in the users home directory in a directory called "MP3"
		
		public void run() {
		
				try{
						int counter = 0;
						int length = 0;
						int read = 0;
						byte[] bytes = new byte[512];
						
						// Creates MulticastSocket on PORT (12345)
						MulticastSocket mcs = new MulticastSocket(PORT);
						
						// Joins Multicast Group, GROUP (Class D Address 225.0.50.0)
						mcs.joinGroup(InetAddress.getByName(GROUP));
						DatagramPacket toClientBuffer;
						
						// Loops while there is still input stream data
						while((length = is.read(bytes)) != -1){
							
							// ------ LOCAL STORAGE ------- \\
							
							// Appends bytes to songCache index 0 byte array
							System.arraycopy(bytes, 0,  songCache.get(0), read, length);
							//writeStream.write(bytes, 0, length);
							read += length;
							
							// ----- MulticastSocket Preperation and Output ------ \\
							
							// Creates finalAr (The data portion of the packet getting ready to be sent out)
							byte[] finalAr = new byte[length + 4];
							System.arraycopy(bytes, 0, finalAr, 0, length);
							
							// Creates byteCount array (converts counter to byteArray)
							byte byteCount[] = ByteBuffer.allocate(4).putInt(counter).array();
							System.arraycopy(byteCount, 0, finalAr, length, 4);
							
							// Creates DatagramPacket reads to be sent
							toClientBuffer = new DatagramPacket(finalAr, finalAr.length, InetAddress.getByName(GROUP), PORT);
							
							// Sends packet and adds to counter (next packet!)
							mcs.send(toClientBuffer);
							counter++;
							
							// Notifies the play thread that we're ready to go (This is only needed to have a timer and play locally if enabled)
							if(read > 49152){
								synchronized(playerLock){
									playerLock.notifyAll();
								}
							}
							
						}
						
						//writeStream.flush();
						//writeStream.close();
					
				}catch(Exception e){
					e.printStackTrace();
				}
		}
	}
	
	class PlayThread extends Thread{
		public void run() {
			
			try{
			
				ByteArrayInputStream readFromArray = new ByteArrayInputStream(songCache.get(0));
			
				synchronized(playerLock){
					playerLock.wait();
				}
				
				Player player = new Player(readFromArray);
				player.play();
				
				readFromArray.close();
				
			}catch(Exception e){
				e.printStackTrace();
			}
		}
	}
	
	class ProgressThread extends Thread{
		
		String songLength;
		
		public void run(){
			
			try{
			
				SimpleDateFormat formatTime = new SimpleDateFormat("mm:ss");
				songLength = formatTime.format(time * 1000);
				
				synchronized(playerLock){
					playerLock.wait();
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