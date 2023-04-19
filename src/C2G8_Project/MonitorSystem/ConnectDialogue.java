package C2G8_Project.MonitorSystem;

import C2G8_Project.CONFIG;
import javafx.scene.control.TextInputDialog;

/**
 * Dialogue box that is custom designed to handle the connection process
 * @author Jayson Mendoza
 *
 */
public class ConnectDialogue extends TextInputDialog {
	public static String DEFAULT_TITLE = "Connect to Scheduler";
	public static String DEFAULT_MESSAGE = String.format("Enter the IP4 address for the scheduler. Do not include port. Scheduler should host on port %d", CONFIG.SCHEDULER_PORT);
	public static String DEFAULT_INPUT_MSG = "IP4 Address:";
	public static String DEFAULT_VALUE = "127.0.0.1";
	
	ConnectDialogue() {
		super(DEFAULT_VALUE);
		this.setTitle(DEFAULT_TITLE);
		this.setHeaderText(DEFAULT_MESSAGE);
		this.setContentText(DEFAULT_INPUT_MSG);
	}
}
