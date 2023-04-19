/**
 * 
 */
package C2G8_Project;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * @author Jordan
 * 
 * A data structure that contains all of the important information that the scheduler
 * keeps track of in order to get a full picture of how the system is running. Is used
 * to send to the monitor (or view) so that it can represent the system appropriately.
 */
public record ViewData(
		ArrayList<ViewRequestData>                   requestsAwaitingElevatorAssignment,
		HashMap<Integer, ArrayList<ViewRequestData>> requestsActive,
		ArrayList<ViewRequestData>                   requestsComplete,
		HashMap<Integer, ViewElevatorData>           elevatorLatestData,
		HashMap<Integer, HashMap<Direction,Boolean>> floorButtonLamp,	// Changes when button pressed by passengers
		HashMap<Integer, HashMap<Direction,Boolean>> floorDirectionLamp	// Changes on arrival/departure of elevator
) {}
