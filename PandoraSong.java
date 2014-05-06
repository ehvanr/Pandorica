/**
 * Evan Reichard
 * https://github.com/evreichard
 * evan@evanreichard.com
 * 2013 - 2014
 **/

public class PandoraSong{
	
	private PlayerStatus songStatus = PlayerStatus.NOTSTARTED;
	private String songAlbumName;
	private String songAlbumArtUrl;
	private String songName;
	private String songArtistName;
	private String songAudioUrl;
	
	// --------------------------------------------------------------------------------------\\
	// ------------------------------------ CONSTRUCTORS ------------------------------------\\
	// --------------------------------------------------------------------------------------\\
	
	public PandoraSong(){}
	
	public PandoraSong(String tempSongName, String tempAlbumName, String tempArtistName, String tempAlbumArtUrl, String tempAudioUrl){
		songName = tempSongName;
		songAlbumName = tempAlbumName;
		songArtistName = tempArtistName;
		songAlbumArtUrl = tempAlbumArtUrl;
		songAudioUrl = tempAudioUrl;
	}
	
	// --------------------------------------------------------------------------------------\\
	// ---------------------------------- ACCESSOR METHODS ----------------------------------\\
	// --------------------------------------------------------------------------------------\\
	
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
	
	public PlayerStatus getSongStatus(){
		return songStatus;
	}
	
	// --------------------------------------------------------------------------------------\\
	// ---------------------------------- MODIFIER METHODS ----------------------------------\\
	// --------------------------------------------------------------------------------------\\
	
	public void setSongStatus(PlayerStatus VALUE){
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