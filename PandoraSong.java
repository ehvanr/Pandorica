/**
 * Evan Reichard
 * https://github.com/evreichard
 * evan@evanreichard.com
 * 2013 - 2014
 **/

public class PandoraSong{

	public final static int NOTSTARTED = 0;
	public final static int PLAYING = 1;
    public final static int PAUSED = 2;
	public final static int FINISHED = 3;
	public final static int STOPPED = 4;
	
	public int songStatus = NOTSTARTED;

	public String songAlbumName;
	public String songAlbumArtUrl;
	public String songName;
	public String songArtistName;
	public String songAudioUrl;
	
	public PandoraSong(){
		
	}
	
	public PandoraSong(String tempSongName, String tempAlbumName, String tempArtistName, String tempAlbumArtUrl, String tempAudioUrl){
		songName = tempSongName;
		songAlbumName = tempAlbumName;
		songArtistName = tempArtistName;
		songAlbumArtUrl = tempAlbumArtUrl;
		songAudioUrl = tempAudioUrl;
	}
	
	public String getAlbumName(){
		return songAlbumName;
	}
	
	public String getSongName(){
		return songName;
	}
	
	public String getArtistName(){
		return songArtistName;
	}
	
	public String getAlbumArtUrl(){
		return songAlbumArtUrl;
	}
	
	public String getAudioUrl(){
		return songAudioUrl;
	}
	
	public int getSongStatus(){
		return songStatus;
	}
	
	public void setSongStatus(int VALUE){
		songStatus = VALUE;
	}
	
	public void setSongName(String tempSongName){
		songName = tempSongName;
	}
	
	public void setAlbumName(String tempAlbumName){
		songAlbumName = tempAlbumName;
	}
	
	public void setArtistName(String tempArtistName){
		songArtistName = tempArtistName;
	}
		
	public void setAlbumArtUrl(String tempAlbumArtUrl){
		// Convert to URL
		songAlbumArtUrl = tempAlbumArtUrl;
	}
	
	public void setAudioUrl(String tempAudioUrl){
		// Convert to URL
		songAudioUrl = tempAudioUrl;
	}
}