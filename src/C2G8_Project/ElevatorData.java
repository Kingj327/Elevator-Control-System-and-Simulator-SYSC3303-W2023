/**
 * 
 */
package C2G8_Project;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

/**
 * @author Jordan
 *
 * The main data structure to keep track of changes in an elevator's states in the scheduler.
 */
public class ElevatorData {
	private ElevatorSignal signal;
	private ArrayList<Integer> targetLocations;
	
	ElevatorData(ElevatorSignal _signal) {
		signal = _signal;
		targetLocations = new ArrayList<Integer>();
	}
	
	ElevatorData(ElevatorSignal _signal, ArrayList<Integer> _targetLocations) {
		signal = _signal;
		targetLocations = _targetLocations;
	}
	
	/*** Getters ***/
	int id()                             { return signal.id(); }
	ElevatorStates state()               { return signal.state(); }
    int location()                       { return signal.location(); }
    ArrayList<Integer> newCarBttns()     { return signal.newCarBttns(); }
    Set<Integer> carBttns()              { return signal.carBttns(); }
    ScenarioFaults faultType()           { return signal.faultType(); }
    ElevatorSignal signal()              { return signal; }
    ArrayList<Integer> targetLocations() { return targetLocations; }
    public String toString()             { return String.format("ElevatorData[state=%s, id=%s, location=%s, newCarBttns=%s, carBttns=%s, targetLocations=%s]", signal.state(), signal.id(), signal.location(), signal.newCarBttns(), signal.carBttns(), targetLocations.toString()); }
	
}
