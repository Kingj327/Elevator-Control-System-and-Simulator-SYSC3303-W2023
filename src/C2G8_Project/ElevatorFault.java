package C2G8_Project;

/*
 * The error package/data structure for the simulated elevator errors
 */
public class ElevatorFault {
	private ScenarioFaults faultType;	// Shouldn't be changed outside of creation.
	private boolean isFaultHandled = false;
	
	ElevatorFault() {};
	
	ElevatorFault(ScenarioFaults faultType, boolean isFaultHandled) {
		this.faultType = faultType;
		this.isFaultHandled = isFaultHandled;
	}
	
	ElevatorFault(ScenarioFaults faultType) {
		this(faultType, false);
	}
	
	public ScenarioFaults getFaultType() {
		return faultType;
	}

	/*** Getters ***/
	ScenarioFaults faultType() { return faultType; }
	boolean isFaultHandled() { return isFaultHandled; }
	
	/*** Setters ***/
	boolean setIsFaultHandled(boolean flag) { isFaultHandled = flag; return isFaultHandled;}

	@Override
	public String toString() {
		return "ElevatorFault [faultType=" + faultType + ", isFaultHandled=" + isFaultHandled + "]";
	}
	
	
}

