/**
 * Evan Reichard
 * https://github.com/evreichard
 * evan@evanreichard.com
 * 2013 - 2014
 **/

public class Pandorica{
	public static void main(String args[]){
		if(args.length == 0){
			javafx.application.Application.launch(PandoricaGUI.class);
		}else if(args.length == 1 && args[0].equals("-nogui")){
			// Execute no gui
			new PandoricaTerminal();
		}else{
			System.out.println("Invalid Command Line Arguments.\n\t-nogui\n\t\tAppend this flag to execute the program with no gui.");
		}
	}
}