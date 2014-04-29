/**
 * Evan Reichard
 * https://github.com/evreichard
 * evan@evanreichard.com
 * 2013 - 2014
 **/

public class NoSongInQueueException extends Exception {
  public NoSongInQueueException(){ super(); }
  public NoSongInQueueException(String message) { super(message); }
  public NoSongInQueueException(String message, Throwable cause) { super(message, cause); }
  public NoSongInQueueException(Throwable cause) { super(cause); }
}