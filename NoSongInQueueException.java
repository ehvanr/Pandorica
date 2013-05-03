/**
 * Evan Reichard
 * https://github.com/evreichard
 * evan@evanreichard.com
 * April 2013
 *
 * BETA 1
 **/

public class NoSongInQueueException extends Exception {
  public NoSongInQueueException(){ super(); }
  public NoSongInQueueException(String message) { super(message); }
  public NoSongInQueueException(String message, Throwable cause) { super(message, cause); }
  public NoSongInQueueException(Throwable cause) { super(cause); }
}