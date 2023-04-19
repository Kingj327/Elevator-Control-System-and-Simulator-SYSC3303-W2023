package C2G8_Project;

/**
 * A list of possible Floor Signals recognized
 * by FloorSubsystems
 * @author Jayson Mendoza
 *
 */
public enum FloorSignals {
	BTN_LAMP_ON, //Turn button lamps on. Used to acknowledge a passenger request was accepted by the Scheduler
	BTN_LAMP_OFF, //Turn off button lamps. This should be done when the elevator has arrived to service the associated direction
	DIR_LAMP_ON, //Turns a direction lamp on. This should be done when an elevator has arrived for intended travel in the associated direction and is loading passengers
	DIR_LAMP_OFF, //Turns off a direction lamp. This is generally done when an elevator has closed its doors.
	SIG_OFFLINE, //NOT CURRENTLY USED. Will be used to signal to a FloorSubsystem its out of order
	SIG_ONLINE // NOT CURRENTLY USED. Will be used to signal a floor that is out of order to resume service
}
