/**
 * 
 */
package C2G8_Project;

import java.util.ArrayList;

/**
 * @author Jordan
 *
 * ElevatorData that can be properly sent to the view/monitor
 */
public record ViewElevatorData(
		ElevatorSignal signal,
		ArrayList<Integer> targetLocations
) {}
