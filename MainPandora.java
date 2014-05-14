/**
 * Evan Reichard
 * https://github.com/evreichard
 * evan@evanreichard.com
 * 2013 - 2014
 **/

/**
 * Protocol specifications taken from here:
 * http://pan-do-ra-api.wikia.com/wiki/Json/5
 **/
 
import java.io.*;
import java.net.*;
import javax.net.ssl.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Scanner;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import javax.xml.bind.DatatypeConverter;
import com.google.gson.*;

/**
 * This class is the "client backend" to the Pandora API. It communicates with the API
 * to execute a variety of calls. Some of which include loging in, receiving the 
 * station list, getting playlists with song information, etc.
 **/
public class MainPandora{

	private static String BASE_HTTPS_URL = "https://tuner.pandora.com/services/json/?method=";
	private static String BASE_HTTP_URL = "http://tuner.pandora.com/services/json/?method=";
	private String partnerAuthToken;
	private String urlPAT;
	private long clientStartTime;
	private int partnerID;
	private int syncTime = 0;	

	private String userId;
	private String stationId;
	private String userAuthToken;
	private String urlUAT;
	
	// Cache credentials so it can autologin on timeout
	private String cachedPassword;
	private String cachedEmail;
	
	private static MainPandora INSTANCE;
	
	Gson gson = new Gson();
	
	// Can only be accessed from the getInstance() method (This prevents accidentally spawning another MainPandora object)
	private MainPandora(){}
	
	/**
	 * Returns the singleton MainPandora instance.  If it doesn't already exist
	 * it creates one. 
	 * 
	 * @return The new or existing MainPandora instance
	 **/
	public static synchronized MainPandora getInstance(){
		if(INSTANCE == null){
			// Initiate Object
			INSTANCE = new MainPandora();
		}
		
		return INSTANCE;
	}
	
	/**
	 * This function performs the partnerLogin as well as the userLogin 
	 * part of the Pandora API
	 *
	 * @param	username the email of the user.
	 * @param 	password the password of the user.
	 * @return 	Whether we were successfully authenticated.
	 **/
	public boolean pandoraLogin(String username, String password){
		if(partnerLogin() && userLogin(username, password)){
			cachedPassword = password;
			cachedEmail = username;
			return true;
		}else{
			// Shouldn't need to throw any error here because the error should have already been thrown.
			return false;
		}
	}
	
	/**
	 * Returns an ArrayList, each element containing an ArrayList of Strings. 
	 * The first of which is the station name and the second is the station ID.
	 *
	 * @return An ArrayList of station names and their respective ID's.
	 **/
	public ArrayList<ArrayList<String>> getStationList(){
		
		// Sets JSON playlist URL
		String stationListURLMethod = String.format("user.getStationList&auth_token=%s&partner_id=%d&user_id=%s", urlUAT, partnerID, userId);

		JsonObject getStationListJSON = new JsonObject();
		getStationListJSON.addProperty("userAuthToken", userAuthToken);
		getStationListJSON.addProperty("syncTime", getSyncTime());

		// Will not worth with HTTPS, set to false
		JsonObject incomingObj = sendObject(encrypt(getStationListJSON.toString()), stationListURLMethod, false);
		
		if(incomingObj.get("stat").getAsString().equals("ok")){
			JsonArray tempStations = incomingObj.getAsJsonObject("result").getAsJsonArray("stations");
			
			// Creates ArratList of each pandoraStation
			ArrayList<ArrayList<String>> pandoraStations = new ArrayList<ArrayList<String>>();

			// Takes each element from JsonArray, get it as a JsonObject, and inputs it into pandoraStation JsonObject ArrayList
			for(JsonElement element : tempStations){
				ArrayList<String> tempObj = new ArrayList<String>();
				
				tempObj.add(element.getAsJsonObject().get("stationName").getAsString());
				tempObj.add(element.getAsJsonObject().get("stationToken").getAsString());
				pandoraStations.add(tempObj);
			}
			
			return pandoraStations;
		}else{
			// Didn't get expected response. Thow error?
			String errorCode = incomingObj.get("code").getAsString();
			System.out.println("getStationList API call crashed with error: " + errorCode);
			System.exit(0);
			return null;
		}
	}
	
	/**
	 * This will receive and return all songs in a station that the user has 
	 * saved in their Pandora account.
	 * 
	 * @param stationToken		the station that we want to get a playlist for.
	 * @return 					The first three songs in the specified station.
	 **/
	public ArrayList<PandoraSong> getPlaylist(String stationToken){
		
		// Sets JSON playlist URL
		String playlistURLMethod = String.format("station.getPlaylist&auth_token=%s&partner_id=%d&user_id=%s", urlUAT, partnerID, userId);

		JsonObject getPlaylistJSON = new JsonObject();
		getPlaylistJSON.addProperty("userAuthToken", userAuthToken);
		getPlaylistJSON.addProperty("stationToken", stationToken);
		getPlaylistJSON.addProperty("additionalAudioUrl", "HTTP_128_MP3");
		getPlaylistJSON.addProperty("syncTime", getSyncTime());

		JsonObject incomingObj = sendObject(encrypt(getPlaylistJSON.toString()), playlistURLMethod, true);
		
		if(incomingObj.get("stat").getAsString().equals("ok")){
			
			// Grabs "items" element (Each are song attributes, we still need to parse song URL)
			JsonElement tempStreams = incomingObj.getAsJsonObject("result").get("items");

			// Deserialization of tempStreams JsonElement - inputs them into JsonArray
			JsonArray songListParsed = (new Gson()).fromJson(tempStreams, JsonArray.class);

			ArrayList<PandoraSong> songListArray = new ArrayList<PandoraSong>();

			// Takes each element from JsonArray, get it as a JsonObject, and inputs it into songListArray JsonObject ArrayList
			for(JsonElement element : songListParsed){

				JsonObject tempObj = element.getAsJsonObject();

				// Makes sure it's a song (contains "trackToken" field)
				if(tempObj.has("trackToken")){
					PandoraSong tempSong = new PandoraSong();
					
					tempSong.setAudioUrl(tempObj.get("additionalAudioUrl").getAsString());
					tempSong.setAlbumArtUrl(tempObj.get("albumArtUrl").getAsString());
					tempSong.setArtistName(tempObj.get("artistName").getAsString());
					tempSong.setAlbumName(tempObj.get("albumName").getAsString());
					tempSong.setSongName(tempObj.get("songName").getAsString());
					
					songListArray.add(tempSong);
				}
			}
			
			return songListArray;
		}else if(incomingObj.get("code").getAsString().equals("1001")){
			// Reauthenticate (This error is called when the users token expires)
			if(pandoraLogin(cachedEmail, cachedPassword)){
				return getPlaylist(stationToken);
			}else{
				// Throw an exception here saying we couldn't reauthenticate with the same credentials
				System.exit(0);
				return null;
			}
		}else{
			// Throw an exception here
			String errorCode = incomingObj.get("code").getAsString();
			System.out.println("getPlaylist API call crashed with error: " + errorCode);
			System.exit(0);
			return null;
		}
	}
		
	/**
	 * This function sends an object with the appropriate actionParam to the
	 * Pandora API.  It then returns a recieved JSONObject.
	 *
	 * @param toSend 		the object to send.
	 * @param actionParam	the actionParameter part of the Pandora API.
	 * @return 				The returned JsonObject.
	 **/
	private static JsonObject sendObject(String toSend, String actionParam, boolean HTTPS){

		String inputLine = null;

		try{
			if(HTTPS){
				URL url = new URL(BASE_HTTPS_URL + actionParam);
				HttpsURLConnection hc = (HttpsURLConnection) url.openConnection();

				// Set all HttpURLConnection settings
				hc.setRequestMethod("POST");
				hc.setDoOutput(true);
				hc.setDoInput(true);
				hc.setRequestProperty("Content-Type", "text/plain");
				hc.connect();

				
				// The output stream
				DataOutputStream out = new DataOutputStream(hc.getOutputStream());
				
				// Sends out the JSON request
				out.writeBytes(toSend);
				out.flush();
				out.close();

				// Receives the response
				BufferedReader br = new BufferedReader(new InputStreamReader(hc.getInputStream(), "UTF-8"));
				
				inputLine = br.readLine();
				br.close();
			}else{
				URL url = new URL(BASE_HTTP_URL + actionParam);
				HttpURLConnection hc = (HttpURLConnection) url.openConnection();

				// Set all HttpURLConnection settings
				hc.setRequestMethod("POST");
				hc.setDoOutput(true);
				hc.setDoInput(true);
				hc.setRequestProperty("Content-Type", "text/plain");
				hc.connect();
				
				// The output stream
				DataOutputStream out = new DataOutputStream(hc.getOutputStream());
								
				// Sends out the JSON request
				out.writeBytes(toSend);
				out.flush();
				out.close();

				// Receives the response
				BufferedReader br = new BufferedReader(new InputStreamReader(hc.getInputStream(), "UTF-8"));
				
				inputLine = br.readLine();
				br.close();
			}
			


		}catch(MalformedURLException murle){
			System.out.println("MalformedURLException - This shouldn't happen as the URL's are embedded");
		}catch(IOException ioe){
			System.out.println("IOException");
		}

		// Parses the response as a JsonObject
		JsonParser parser = new JsonParser();
		JsonObject receivedObj = (JsonObject)parser.parse(inputLine);

		return receivedObj;
	}

	/**
	 * Takes in an encrypted string and outputs the decrypted string.
	 *
	 * @param encrypted		the encrypted string.
	 * @return 				The decrypted string.
	 **/
	private String decrypt(String encrypted){

		byte[] decryptedBytes = null;

		try{
			byte[] encryptedBytes = DatatypeConverter.parseHexBinary(encrypted);
			
			Cipher blowfishECB = Cipher.getInstance("Blowfish/ECB/PKCS5Padding");
			SecretKeySpec blowfishKey = new SecretKeySpec("R=U!LH$O2B#".getBytes(), "Blowfish");
			blowfishECB.init(Cipher.DECRYPT_MODE, blowfishKey);
			decryptedBytes = blowfishECB.doFinal(encryptedBytes);
		}catch(Exception e){
			e.printStackTrace();
		}

		// First 4 bytes are garbage according to specification (deletes first 4 bytes)
		byte[] trimGarbage = Arrays.copyOfRange(decryptedBytes, 4, decryptedBytes.length);

		String decrypted = new String(trimGarbage);
		return decrypted;
	}

	/**
	 * Takes in a decrypted string and outputs the encrypted string.
	 *
	 * @param decrypted		the decrypted string.
	 * @return 				The encrypted string.
	 **/
	private String encrypt(String decrypted){

		byte[] encryptedBytes = null;

		try{
			Cipher blowfishECB = Cipher.getInstance("Blowfish/ECB/PKCS5Padding");
			SecretKeySpec blowfishKey = new SecretKeySpec("6#26FRL$ZWD".getBytes(), "Blowfish");
			blowfishECB.init(Cipher.ENCRYPT_MODE, blowfishKey);
			encryptedBytes = blowfishECB.doFinal(decrypted.getBytes());
		}catch(Exception e){
			e.printStackTrace();
		}
		
		// It will fail if in uppercase -__-
		String encrypted = DatatypeConverter.printHexBinary(encryptedBytes).toLowerCase();
		return encrypted;
	}

	/**
	 * This calculates syncTime (Required for all JSON requests to the server)
	 *
	 * @return The calculated current SyncTime vs the saved initial SyncTime.
	 **/
	private long getSyncTime(){
		long currentSyncTime = syncTime + ((System.currentTimeMillis() / 1000) - clientStartTime);
		return currentSyncTime;
	}
	
	/**
	 * This function performs the userLogin which is run after the partnerLogin. It also
	 * sets global vars userAuthToken, urlUAT, userId.  All of which are used by later 
	 * API calls.
	 *
	 * @param username			the users email
	 * @param password			the users password
	 * @return 					An ArrayList of the users Pandora Stations (Or null if error)
	 **/
	private boolean userLogin(String username, String password){

		// Commented out JSON elements are not needed for implementation... yet
		String loginURLMethod = String.format("auth.userLogin&auth_token=%s&partner_id=%d", urlPAT, partnerID);

		JsonObject userLoginJSON = new JsonObject();
		userLoginJSON.addProperty("loginType", "user");
		userLoginJSON.addProperty("username", username);
		userLoginJSON.addProperty("password", password);
		userLoginJSON.addProperty("partnerAuthToken", partnerAuthToken);
		//userLoginJSON.addProperty("includePandoraOneInfo", true);
		//userLoginJSON.addProperty("includeAdAttributes", true);
		//userLoginJSON.addProperty("includeSubscriptionExpiration", true);
		//userLoginJSON.addProperty("includeStationArtUrl", true);
		userLoginJSON.addProperty("returnStationList", false);
		//userLoginJSON.addProperty("returnGenreStations", true);
		userLoginJSON.addProperty("syncTime", getSyncTime());

		JsonObject incomingObj = sendObject(encrypt(userLoginJSON.toString()), loginURLMethod, true);

		String errorCode = null;
		
		try{
			errorCode = incomingObj.get("code").getAsString();
		}catch(Exception e){}
		
		// Determines if we received an "ok" response from the server
		if(incomingObj.get("stat").getAsString().equals("ok")){
			
			// Sets userAuthToken
			userAuthToken = incomingObj.getAsJsonObject("result").get("userAuthToken").getAsString();

			// URLEncodes userAuthToken (we're parsing a URL with this token)
			try{
				urlUAT = URLEncoder.encode(userAuthToken, "ISO-8859-1");
			}catch(UnsupportedEncodingException uee){}
			
			// Gets userId from login
			userId = incomingObj.getAsJsonObject("result").get("userId").getAsString();
			
			// Return successful
			return true;
		}else if(errorCode.equals("1002")){
			// Throw bare-bone exception with incomingObj.get("code").getAsString() as details
			// Have the programmer deal with restarting the login
			System.out.println("User Login Error");
			return false;
		}else{
			System.out.println("userLogin API call crashed with error: " + errorCode);
			System.exit(0);
			return false;
		}
	}
	
	/**
	 * This is the partnerLogin portion of the authentication.  It must be 
	 * performed prior to user authentication. We're authenticating as an
	 * Android device here.  This method sets global vars clientStartTime, 
	 * partnerID, and syncTime.  All of which are used by later API calls.
	 **/ 
	private boolean partnerLogin(){

		// Gets current system time for sync (difference between this and syncTime)
		clientStartTime = System.currentTimeMillis() / 1000L;
		
		JsonObject partnerLogin = new JsonObject();
		partnerLogin.addProperty("username", "android");
		partnerLogin.addProperty("password", "AC7IBG09A3DTSYM4R41UJWL07VLN8JI7");
		partnerLogin.addProperty("deviceModel", "android-generic");
		partnerLogin.addProperty("version", "5");

		JsonObject incomingObj = new JsonObject();
		incomingObj = sendObject(partnerLogin.toString(), "auth.partnerLogin", true);

		// Checks if our "stat" is ok
		if((incomingObj.get("stat")).getAsString().equals("ok")){
		
			JsonObject result = (JsonObject)incomingObj.getAsJsonObject("result");

			// Gets values as JsonElement, converts elements to string and integer
			String syncTimeEncoded = (result.get("syncTime")).getAsString();
			partnerAuthToken = (result.get("partnerAuthToken")).getAsString();
			partnerID = Integer.parseInt((result.get("partnerId")).getAsString());

			try{
				urlPAT = URLEncoder.encode(partnerAuthToken, "ISO-8859-1");
			}catch(UnsupportedEncodingException uee){}

			syncTime = Integer.parseInt(decrypt(syncTimeEncoded));

			return true;
		}else{
			String errorCode = incomingObj.get("code").getAsString();
			System.out.println("partnerLogin API call crashed with error: " + errorCode);
			System.exit(0);
			return false;
		}
	}
}
