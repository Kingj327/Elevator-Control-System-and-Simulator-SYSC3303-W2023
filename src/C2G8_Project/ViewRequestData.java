/**
 * 
 */
package C2G8_Project;

/**
 * @author Jordan
 * 
 * RequestData that can be properly sent to the view/monitor
 */
public record ViewRequestData (
		int requestID,
		FloorRequest request,
		int elevatorID,
		boolean pickedUpPassenger,
		boolean requestComplete
) {}
