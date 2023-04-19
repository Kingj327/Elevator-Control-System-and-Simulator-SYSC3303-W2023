package C2G8_Project;

public enum ScenarioFaults {
	NONE,
	ELEVATOR_STUCK, //This should be fatal and shut down the elevator
	DOOR_STUCK //This should have an associated time and the elevator can recover
}
