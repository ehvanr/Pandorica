/**
 * Evan Reichard
 * https://github.com/evreichard
 * evan@evanreichard.com
 * April 2013
 *
 * BETA 1
 **/

import javazoom.jl.player.Player;
import java.net.*;
import java.io.*;
import java.nio.*;

public class PandoraReceiver{

	private final int PORT = 12346;
	private final String GROUP = "225.0.50.0";
	MulticastSocket ms;
	
	byte buffer[];
	byte song[];
	private final Object playerLock = new Object();
	
	public PandoraReceiver(){
		try{
			ms = new MulticastSocket(PORT);
			ms.joinGroup(InetAddress.getByName(GROUP));
			
			// Allocates 1500 bytes to the buffer array.  This should NOT overflow (This is the typical MTU size so we should never get more than this)
			buffer = new byte[1500];
			
			// Allocates 10MB
			// This 10MB is arbitrarily chosen.  Should be optimized. (Get HTTP headers from PandoraPlayer class (aka the MulticastSocket server) and send to this class, then allocate byte array to the appropriate size)
			song = new byte[10485760];

			// Creates and starts threads
			ReceiveThread receive = new ReceiveThread();
			PlayThread play = new PlayThread();
			receive.start();
			play.start();
			// Finishing
			//ms.leaveGroup(InetAddress.getByName(GROUP));
			//ms.close();
		}catch(IOException ioe){
			ioe.printStackTrace();
		}
	}
	
	class PlayThread extends Thread{
		public void run(){
		
			// Creates a ByteArrayInputStream from song array
			ByteArrayInputStream readFromArray = new ByteArrayInputStream(song);
			
			try{
				// Waits until notified by ReveiveThread (3 second buffer)
				synchronized(playerLock){
					playerLock.wait();
				}
				
				// Creates player and begins playing from ByteArrayInputStream
				Player player = new Player(readFromArray);
				player.play();
				
				readFromArray.close();
			}catch(Exception e){
				e.printStackTrace();
			}
		}
	}
	
	class ReceiveThread extends Thread{
		public void run(){
			try{
					int read = 0;
					int previous = 0;
					
					while(true){
						
						// Creates datagram packet with length buffer
						DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
						
						// Received packet
						ms.receive(packet);
						
						// Creates 516 byte array (512 bytes for music data & 4 bytes for packet number)
						byte temp[] = new byte[516];
						
						// Sets the temp array to packet data
						// NOTE:  This does NOT keep temp array to 516, it re-allocates it to 1500 (the buffer length)
						temp = packet.getData();
						
						// Gets packet length so we can adjust for buffer ahead
						int length = packet.getLength();
						
						// Creates byteCount array (This is for the packet number)
						byte byteCount[] = new byte[4];
						
						// Copies from temp array position, (length - 4), to byteCount (starting at 0) for 4 bytes. 
						System.arraycopy(temp, (length - 4), byteCount, 0, 4);
						
						// Converts byteCount array to an integer
						int current = ByteBuffer.wrap(byteCount).getInt();
						
						// If the previous packet was a larger packet number, then discard this packet.  
						// Else, copy it into buffer array
						if(current < previous){
							// Discards packet (came out of order)
							System.out.println("Discarded packet number: " + current);
						}else{
							// Copies temp array starting at 0 to song array from last written position for (length - 4) bytes
							System.arraycopy(temp, 0, song, read, (length - 4));
							
							// Adds to read so we know what position to resume adding to the song array (see above line)
							read += (length - 4);
							
							// Sets previous line to current
							previous = current;
						}
						
						// Unlocks the playThread (makes sure we have 3 seconds of buffer at 128kbps)
						if(read > 49152){
							synchronized(playerLock){
								playerLock.notifyAll();
							}
						}
					}
				
			}catch(Exception e){
				e.printStackTrace();
			}
		}
	}

	public static void main(String args[]){
		new PandoraReceiver();
	}
}
