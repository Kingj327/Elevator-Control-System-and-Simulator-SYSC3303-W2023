/**
 * 
 */
package C2G8_Project;

import java.io.Serializable;

/**
 * @author Jordan
 * The main data structure that's used in the scheduler to keep track of requests.
 */
public class RequestData implements Serializable {
	private final int requestID;
	private final FloorRequest request;
	private int elevatorID;
	private boolean pickedUpPassenger;	// Arrived at Source Floor
	private boolean requestComplete;	// Arrived at Destination Floor (after Source Floor)
	
	RequestData(FloorRequest _request, int _requestID) {
		this(_request, _requestID, -1);
	}
	
	RequestData(FloorRequest _request, int _requestID, int _elevatorID) {
		request = _request;
		requestID = _requestID;
		elevatorID = _elevatorID;
		pickedUpPassenger = false;
		requestComplete = false;
	}
	
	/*** Getters ***/
	int requestID()             { return requestID; }
	FloorRequest request()      { return request; }
	int requestFloor()          { return request.reqestFloor(); }	// Source Floor
	int targetFloor()           { return request.targetFloor(); }	// Destination Floor
	Direction direction()       { return request.direction(); }
	ElevatorFault fault()       { return request.fault(); }
	ScenarioFaults faultType()  { return request.fault().faultType(); } 
	boolean isFaultHandled()    { return request.fault().isFaultHandled(); }
	int elevatorID()            { return elevatorID; }
	boolean pickedUpPassenger() { return pickedUpPassenger; }
	boolean requestComplete()   { return requestComplete; }
	public String toString()    { return String.format("RequestData[requestID=%s, reqestFloor=%s, targetFloor=%s, direction=%s, elevatorID=%s, pickedUpPassenger=%s, requestComplete=%s]", requestID, request.reqestFloor(), request.targetFloor(), request.direction().toString(), elevatorID, pickedUpPassenger, requestComplete); }
	
	/*** Setters ***/
	void setElevatorID(int id)              { elevatorID = id; }
	void setPickedUpPassenger(boolean flag) { pickedUpPassenger = flag; }
	void setRequestComplete(boolean flag)   { requestComplete = flag; }
}
