package C2G8_Project;

/**
 * @author JP
 * All Possible Elevator States
 */
public enum ElevatorStates {
    START,  //Used to inform scheduler that an elevator is setup
    IDLE,
    MOVING_UP,
    MOVING_DOWN,
    OPEN_DOORS,
    CLOSE_DOORS,
    STOP,
    ERROR_ELEVATOR_STUCK,
    ERROR_DOOR_STUCK,
    REBOOT,
    DEAD
}
