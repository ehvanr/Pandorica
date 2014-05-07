/**
 * Evan Reichard
 * https://github.com/evreichard
 * evan@evanreichard.com
 * 2013 - 2014
 **/

 /**
  * This class contains all the information of one individual song.  It contains 
  * information like the song play status, album name, album art URL, song name,
  * artist name, and song audio url.
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
	
	/**
	 * The default constructor.
	 **/
	public PandoraSong(){}
	
	/**
	 * A constructor to initialize all elements at once.
	 *
	 * @param tempSongName		the song name.
	 * @param tempAlbumName		the album name.
	 * @param tempArtistName	the artist name.
	 * @param tempAlbumArtUrl	the album art URL.
	 * @param tempAudioUrl		the song audio URL.
	 **/
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
	
	/**
	 * This will return the songs album name.
	 *
	 * @return The songs album name.
	 **/
	public String getAlbumName(){
		return songAlbumName;
	}
	
	/**
	 * This will return the songs name.
	 *
	 * @return the songs name.
	 **/
	public String getSongName(){
		return songName;
	}
	
	/**
	 * This will return the songs artists name.
	 *
	 * @return the songs artist name.
	 **/
	public String getArtistName(){
		return songArtistName;
	}
	
	/**
	 * This will return the songs album art URL.
	 *
	 * @return te songs album art URL.
	 **/
	public String getAlbumArtUrl(){
		return songAlbumArtUrl;
	}
	
	/**
	 * This will return the songs audio URL.
	 *
	 * @return the songs audio URL.
	 **/
	public String getAudioUrl(){
		return songAudioUrl;
	}
	
	/**
	 * This will return the songs current play status in form of the defined 
	 * PlayerStatus enum.
	 *
	 * @return the songs current play status.
	 **/
	public PlayerStatus getSongStatus(){
		return songStatus;
	}
	
	// --------------------------------------------------------------------------------------\\
	// ---------------------------------- MODIFIER METHODS ----------------------------------\\
	// --------------------------------------------------------------------------------------\\
	
	/**
	 * Allows you to set the song status of this object in form of 
	 * the defined PlayerStatus enum.
	 *
	 * @param VALUE the desired song status to set.
	 **/
	public void setSongStatus(PlayerStatus VALUE){
		songStatus = VALUE;
	}
	
	/**
	 * Allows you to set the song name.
	 *
	 * @param tempSongName the desired song name to set.
	 **/
	public void setSongName(String tempSongName){
		songName = tempSongName;
	}
	
	/**
	 * Allows you to set the songs album name.
	 *
	 * @param tempAlbumName the desired album name to set.
	 **/
	public void setAlbumName(String tempAlbumName){
		songAlbumName = tempAlbumName;
	}
	
	/**
	 * Allows you to set the songs artists name.
	 *
	 * @param tempArtistName the desired artist name to set.
	 **/
	public void setArtistName(String tempArtistName){
		songArtistName = tempArtistName;
	}
	
	/**
	 * Allows you to set the songs album art URL.
	 *
	 * @param tempAlbumArtUrl the desired album art URL to set.
	 **/	
	public void setAlbumArtUrl(String tempAlbumArtUrl){
		// Convert to URL
		songAlbumArtUrl = tempAlbumArtUrl;
	}
	
	/**
	 * Allows you to set the songs audio URL.
	 *
	 * @param tempAudioUrl the desired song audio URL to set.
	 **/
	public void setAudioUrl(String tempAudioUrl){
		// Convert to URL
		songAudioUrl = tempAudioUrl;
	}
}