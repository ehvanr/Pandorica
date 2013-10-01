/**
 * Evan Reichard
 * https://github.com/evreichard
 * evan@evanreichard.com
 * April 2013
 *
 * BETA 1
 **/

/**
 * Protocol specifications taken from here:
 * http://pan-do-ra-api.wikia.com/wiki/Json/5
 **/
 
import javax.crypto.spec.SecretKeySpec;
import org.apache.commons.codec.binary.Hex;
import javax.crypto.Cipher;
import java.util.ArrayList;
import com.google.gson.*;
import java.util.Scanner;
import java.util.Arrays;
import java.net.*;
import java.io.*;

// WINDOWS:
// javac -cp .;gson-2.2.3.jar;commons-codec-1.7.jar;jl1.0.1.jar *.java

// LINUX:
// javac -cp .:gson-2.2.3.jar:commons-codec-1.7.jar:jl1.0.1.jar *.java

public class MainPandora{

	static String BASE_URL = "https://tuner.pandora.com/services/json/?method=";
	private String partnerAuthToken;
	private String urlPAT;
	private long clientStartTime;
	private int partnerID;
	private int syncTime = 0;	

	private String userId;
	private String stationId;
	private String userAuthToken;
	private String urlUAT;
	private String mp3DIRString;
	
	Gson gson = new Gson();

	/**
	 * @param JsonObject toSend
	 * @param String actionParam
	 * @return JsonObject receivedObj
	 **/
	public static JsonObject sendObject(String toSend, String actionParam){

		String inputLine = null;

		try{

			URL url = new URL(BASE_URL + actionParam);
			HttpURLConnection hc = (HttpURLConnection) url.openConnection();

			hc.setRequestMethod("POST");
			hc.setDoOutput(true);
			hc.setDoInput(true);
			hc.setRequestProperty("Content-Type", "text/plain");
			hc.connect();

			// WRITE
			DataOutputStream out = new DataOutputStream(hc.getOutputStream());

			out.writeBytes(toSend);
			out.flush();
			out.close();

			// RECEIVE
			BufferedReader br = new BufferedReader(new InputStreamReader(hc.getInputStream(), "UTF-8"));
			inputLine = br.readLine();
			br.close();

		}catch(MalformedURLException murle){
			System.out.println("MalformedURLException");
		}catch(IOException ioe){
			System.out.println("IOException");
		}

		// CREATE OBJECT FROM RECEIVED STRING
		JsonParser parser = new JsonParser();
		JsonObject receivedObj = (JsonObject)parser.parse(inputLine);

		return receivedObj;
	}

	public void partnerLogin(){

		// Gets current system time for sync (difference between this and syncTime)
		clientStartTime = System.currentTimeMillis() / 1000L;
		
		JsonObject partnerLogin = new JsonObject();
		partnerLogin.addProperty("username", "android");
		partnerLogin.addProperty("password", "AC7IBG09A3DTSYM4R41UJWL07VLN8JI7");
		partnerLogin.addProperty("deviceModel", "android-generic");
		partnerLogin.addProperty("version", "5");

		JsonObject receivedJSON = new JsonObject();
		receivedJSON = sendObject(partnerLogin.toString(), "auth.partnerLogin");

		// Checks if our "stat" is ok
		if((receivedJSON.get("stat")).getAsString().equals("ok")){
		
			JsonObject result = (JsonObject)receivedJSON.getAsJsonObject("result");

			// Gets values as JsonElement, converts elements to string and integer
			String syncTimeEncoded =(result.get("syncTime")).getAsString();
			partnerAuthToken = (result.get("partnerAuthToken")).getAsString();
			partnerID = Integer.parseInt((result.get("partnerId")).getAsString());

			try{
				urlPAT = URLEncoder.encode(partnerAuthToken, "ISO-8859-1");
			}catch(UnsupportedEncodingException uee){}

			syncTime = Integer.parseInt(decrypt(syncTimeEncoded));
			
		}else{
			System.out.println("Error with partnerLogin");
		}
	}

	/**
	 * Decrypts String
	 *
	 * @param String encrypted
	 * @return String decrypted
	 **/
	private String decrypt(String encrypted){

		byte[] decryptedBytes = null;

		try{
			byte[] encryptedBytes = Hex.decodeHex(encrypted.toCharArray());
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
	 * Encrypt String
	 *
	 * @param String decrypted
	 * @return String encrypted
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

		String encrypted = Hex.encodeHexString(encryptedBytes);
		return encrypted;
	}

	/**
	 * @param String username
	 * @param String password
	 **/
	private void userLogin(String username, String password){

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
		userLoginJSON.addProperty("returnStationList", true);
		//userLoginJSON.addProperty("returnGenreStations", true);
		userLoginJSON.addProperty("syncTime", getSyncTime());

		JsonObject incomingObj = sendObject(encrypt(userLoginJSON.toString()), loginURLMethod);

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
			
			// Gets station list as a JsonElement
			JsonElement stationList = incomingObj.getAsJsonObject("result").getAsJsonObject("stationListResult").get("stations");
			
			// Deserialization - Grabs array of JSON (JsonElement stationList object) and inputs each into a JsonArray
			JsonArray stationListParsed = gson.fromJson(stationList, JsonArray.class);
			
			// Creates ArratList of each pandoraStation
			ArrayList<JsonObject> pandoraStations = new ArrayList<JsonObject>();

			// Takes each element from JsonArray, get it as a JsonObject, and inputs it into pandoraStation JsonObject ArrayList
			for(JsonElement element : stationListParsed){
				JsonObject tempObj = element.getAsJsonObject();
				pandoraStations.add(tempObj);
			}
			
			// Calls listStations method
			listStations(pandoraStations);
			
		}else{
			
			// We did not get an "ok" from the server, figure out why and print appropriate error messages
			if(incomingObj.get("code").getAsString().equals("1002")){
				System.out.println("Incorrect Login Information");
			}else{
				System.out.println("Unknown Error Code: " + incomingObj.get("code").getAsString());
			}
		}
	}

	public void getPlaylist(String stationToken){
		
		// Sets JSON playlist URL
		String playlistURLMethod = String.format("station.getPlaylist&auth_token=%s&partner_id=%d&user_id=%s", urlUAT, partnerID, userId);

		JsonObject getPlaylistJSON = new JsonObject();
		getPlaylistJSON.addProperty("userAuthToken", userAuthToken);
		getPlaylistJSON.addProperty("stationToken", stationToken);
		getPlaylistJSON.addProperty("additionalAudioUrl", "HTTP_128_MP3");
		getPlaylistJSON.addProperty("syncTime", getSyncTime());

		JsonObject incomingObj = sendObject(encrypt(getPlaylistJSON.toString()), playlistURLMethod);

		// Grabs "items" element (Each are song attributes, we still need to parse song URL)
		JsonElement tempStreams = incomingObj.getAsJsonObject("result").get("items");

		// Deserialization of tempStreams JsonElement - inputs them into JsonArray
		JsonArray songListParsed = gson.fromJson(tempStreams, JsonArray.class);

		// Creates JsonObject ArrayList
		ArrayList<JsonObject> songListArray = new ArrayList<JsonObject>();

		// Takes each element from JsonArray, get it as a JsonObject, and inputs it into songListArray JsonObject ArrayList
		for(JsonElement element : songListParsed){

			JsonObject tempObj = element.getAsJsonObject();

			// Makes sure it's a song (contains "trackToken" field)
			if(tempObj.has("trackToken")){
				songListArray.add(tempObj);
			}
		}
		
		// Sends songListArray JsonObject ArrayList object to PandoraPlayer
		PandoraPlayer playTrack = new PandoraPlayer(songListArray);
		
		// Plays the player, it will cycle through until it completes the playlist
		try{
			playTrack.play();
		}catch(NoSongInQueueException nsiqe){
			nsiqe.printStackTrace();
		}
	}

	public void listStations(ArrayList<JsonObject> pandoraStations){
	
		System.out.println("\nStation List: \n");

		for(int i = 0; i < pandoraStations.size(); i++){
			JsonObject tempObj = pandoraStations.get(i);
			System.out.println("(" + (i + 1) + ") " + tempObj.get("stationName").getAsString());
		}

		Scanner in = new Scanner(System.in);
		System.out.print("\nPlease enter a station: ");
		int selection = in.nextInt() - 1;
		System.out.println("\nYou Selected: " + pandoraStations.get(selection).get("stationName").getAsString() + " (" + pandoraStations.get(selection).get("stationId").getAsString() + ")\n");

		String tempStationToken = pandoraStations.get(selection).get("stationToken").getAsString();

		// This will continuously loop to get more songs (Each playlist call only returns four songs, once done looping 4, we loop here to continue)
		while(true){
			getPlaylist(tempStationToken);
		}
	}

	/**
	 * This calculates syncTime (Required for all JSON requests to the server)
	 *
	 * @return current syncTime
	 **/
	public long getSyncTime(){
		return syncTime + ((System.currentTimeMillis() / 1000) - clientStartTime);
	}

	public MainPandora(){

	
		mp3DIRString = System.getProperty("user.home") + "//MP3//";

		File mp3DIR = new File(mp3DIRString);

		if (!mp3DIR.exists()){
			mp3DIR.mkdir();
		}
		
	
	
		Scanner in = new Scanner(System.in);
		System.out.print("Email: ");
		String username = in.nextLine();
		Console console = System.console();
		char passwordArray[] = console.readPassword("Password: ");
		
		partnerLogin();
		userLogin(username, new String(passwordArray));
	}

	public static void main(String args[]){
			new MainPandora();
	}
}
