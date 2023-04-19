package C2G8_Project;

/**
 * This class is designed as a central configuration and takes the place of a config file.
 * 
 * Place all system wide configuration settings in this file and it should be referenced
 * throughout the program.
 * 
 * All data members should be public static final and have names that clearly indicate the concern
 * @author Jayson Mendoza
 *
 */
public final class CONFIG {
	public static final int MAX_PEOPLE = 15;	// Recommend: 15-25
	public static final int FLOORS = 22;
	public static final int ELEVATORS = 4;
	public static final long MAX_SCENARIO_DURATION_MILLS = 60 * 1000;	// Recommend: 15 * 1000, up to 60 * 1000
	public static final String REQUEST_FILE = "InputFile.txt";
	public static final int MAX_MESSAGE_BYTES = 65507;
	public static final int SCHEDULER_PORT = 20000;
	public static final int FLOOR_SYSTEM_PORT = 20001;
	public static final int ELEVATOR_SYSTEM_PORT = 20002;
	public static final int SCENARIO_ACCELERATION_MULTIPLIER = 1; // Default is 1, Recommended no higher than 50
	public static final int MINIMUM_PEOPLE_FOR_SCENARIO = 10; // Door stuck at 5, Floor Stuck at 10
	public static final String ICON_FOLDER_URI = "/C2G8_Project/MonitorSystem/icons";
	public static final String FXML_FOLDER_URI = "/C2G8_Project/MonitorSystem";
}

/*** NOTE: Must delete "InputFile.txt" located at /SYSC3303_Project for any new file to be generated with the modified settings. ELEVATOR_SPEED_MULTIPLIER is an exception and will work regardless ***/
