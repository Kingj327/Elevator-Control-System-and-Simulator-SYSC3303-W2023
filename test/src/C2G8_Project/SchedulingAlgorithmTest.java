/**
 * 
 */
package C2G8_Project;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.HashMap;

import org.junit.jupiter.api.Test;

/**
 * @author Jordan
 *
 * JUnit5 Test Cases for SchedulingAlgorithm.java (The static helper class for assigning requests to elevators)
 * 
 * Overview:
 *  These test cases are intended to test the general functionality of the scheduling algorithm (calculatign trip times, sorting, etc),
 *  as well as assigning a request to an elevator. 
 */
class SchedulingAlgorithmTest {

	
	@Test
	/*
	 * NOTE: Requires atleast CONFIG.FLOORS = 22 or more.
	 * 
	 * Thoroughly tests the accuracy of assigning certain requests to an elevator.
	 */
	public void assignSingleRequestToAnElevatorTest() {
		RequestData request = null;
		HashMap<Integer, ArrayList<RequestData>> requestsActive = new HashMap<Integer, ArrayList<RequestData>>();
		HashMap<Integer, ElevatorData> elevatorLatestData = new HashMap<Integer, ElevatorData>();
		int requestID = 1;
		int elevatorID = 1;
		int results = -1;
		int expectedElevatorID = -1;

		
		
		// Test 1: Adding a request to a single, empty elevator
		// Checking for all states (since jobless elevators shouldnt care about states)
		for (ElevatorStates state : ElevatorStates.values()) {
			if (!SchedulingAlgorithm.isElevatorIneligibleDueToErrorHandling(state)) {
				// Setting up elevator data & active requests
				elevatorID = 1;
				elevatorLatestData.put(elevatorID, new ElevatorData(new ElevatorSignal(state, elevatorID, 5, null, null, null), null) );	// Elevators start at floor 5
				requestsActive.put(elevatorID, new ArrayList<RequestData>());
				
				// Create the request, and check that the results are what is expected
				request = new RequestData(new FloorRequest(1, 6, Direction.UP,null), requestID++);
				expectedElevatorID = 1;
				results = SchedulingAlgorithm.assignSingleRequestToAnElevator(request, requestsActive, elevatorLatestData);
				assertEquals(expectedElevatorID, results);
				
				// Change the request to be equal to the elevators floor
				request = new RequestData(new FloorRequest(5, 6, Direction.UP,null), requestID++);
				expectedElevatorID = 1;
				results = SchedulingAlgorithm.assignSingleRequestToAnElevator(request, requestsActive, elevatorLatestData);
				assertEquals(expectedElevatorID, results);
				
				// Change the request to be greater than the elevators floor
				request = new RequestData(new FloorRequest(10, 6, Direction.DOWN,null), requestID++);
				expectedElevatorID = 1;
				results = SchedulingAlgorithm.assignSingleRequestToAnElevator(request, requestsActive, elevatorLatestData);
				assertEquals(expectedElevatorID, results);
			}
		}
		
		
		
		// Test 2: Adding a request to multiple empty elevators
		// Clear previous test data
		request = null;
		requestsActive.clear();	elevatorLatestData.clear();
		requestID = 1; elevatorID = 1; 
		results = -1; expectedElevatorID = -1;
		
		// Checking for all states (since jobless elevators shouldnt care about states)
		for (ElevatorStates state : ElevatorStates.values()) {	
			if (!SchedulingAlgorithm.isElevatorIneligibleDueToErrorHandling(state)) {
				// Setting up elevator data & active requests
				elevatorID = 1;
				elevatorLatestData.put(elevatorID, new ElevatorData(new ElevatorSignal(state, elevatorID, 1, null, null, null), null) );	// Elevators start at floor 1
				requestsActive.put(elevatorID, new ArrayList<RequestData>());
				
				elevatorID = 2;
				elevatorLatestData.put(elevatorID, new ElevatorData(new ElevatorSignal(state, elevatorID, 4, null, null, null), null) );	// Elevators start at floor 4
				requestsActive.put(elevatorID, new ArrayList<RequestData>());
				
				elevatorID = 3;
				elevatorLatestData.put(elevatorID, new ElevatorData(new ElevatorSignal(state, elevatorID, 10, null, null, null), null) );	// Elevators start at floor 10
				requestsActive.put(elevatorID, new ArrayList<RequestData>());
				
				elevatorID = 4;
				elevatorLatestData.put(elevatorID, new ElevatorData(new ElevatorSignal(state, elevatorID, 16, null, null, null), null) );	// Elevators start at floor 16
				requestsActive.put(elevatorID, new ArrayList<RequestData>());
				
				// Create the request, and check that the results are what is expected
				request = new RequestData(new FloorRequest(1, 5, Direction.UP,null), requestID++);
				expectedElevatorID = 1;
				results = SchedulingAlgorithm.assignSingleRequestToAnElevator(request, requestsActive, elevatorLatestData);
				assertEquals(expectedElevatorID, results);
				
				// Change the request to be picked up by the 1st elevator, and check results
				request = new RequestData(new FloorRequest(2, 5, Direction.UP,null), requestID++);
				expectedElevatorID = 1;
				results = SchedulingAlgorithm.assignSingleRequestToAnElevator(request, requestsActive, elevatorLatestData);
				assertEquals(expectedElevatorID, results);
				
				// Change the request to be picked up by the 2nd elevator, and check results
				request = new RequestData(new FloorRequest(3, 5, Direction.UP,null), requestID++);
				expectedElevatorID = 2;
				results = SchedulingAlgorithm.assignSingleRequestToAnElevator(request, requestsActive, elevatorLatestData);
				assertEquals(expectedElevatorID, results);
				
				// Change the request to be picked up by the 3rd elevator, and check results
				request = new RequestData(new FloorRequest(8, 5, Direction.DOWN,null), requestID++);
				expectedElevatorID = 3;
				results = SchedulingAlgorithm.assignSingleRequestToAnElevator(request, requestsActive, elevatorLatestData);
				assertEquals(expectedElevatorID, results);
				
				// Change the request to be picked up by the 4th elevator, and check results
				request = new RequestData(new FloorRequest(14, 5, Direction.DOWN,null), requestID++);
				expectedElevatorID = 4;
				results = SchedulingAlgorithm.assignSingleRequestToAnElevator(request, requestsActive, elevatorLatestData);
				assertEquals(expectedElevatorID, results);
				
				// Change the request to be picked up by the 4th elevator, and check results
				request = new RequestData(new FloorRequest(20, 5, Direction.DOWN,null), requestID++);
				expectedElevatorID = 4;
				results = SchedulingAlgorithm.assignSingleRequestToAnElevator(request, requestsActive, elevatorLatestData);
				assertEquals(expectedElevatorID, results);
			}
		}
		
		 
		
		// Test 3: Adding a request to an elevator with one job, thats either MOVING_UP (requests direction=up), or MOVING_DOWN (requests direction=down)
		// Clear previous test data
		request = null;
		requestsActive.clear();	elevatorLatestData.clear();
		requestID = 1; elevatorID = 1; 
		results = -1; expectedElevatorID = -1;
		
		// Setting up elevator data & active requests
		elevatorID = 1;
		elevatorLatestData.put(elevatorID, new ElevatorData(new ElevatorSignal(ElevatorStates.MOVING_UP, elevatorID, 6, null, null, null), null) );	// Elevators start at floor 6
		requestsActive.put(elevatorID, new ArrayList<RequestData>());
		requestsActive.get(elevatorID).add(new RequestData(new FloorRequest(8, 13, Direction.UP,null), requestID++, elevatorID));
		
		elevatorID = 2;
		elevatorLatestData.put(elevatorID, new ElevatorData(new ElevatorSignal(ElevatorStates.MOVING_DOWN, elevatorID, 18, null, null, null), null) );	// Elevators start at floor 18
		requestsActive.put(elevatorID, new ArrayList<RequestData>());
		requestsActive.get(elevatorID).add(new RequestData(new FloorRequest(16, 5, Direction.DOWN,null), requestID++, elevatorID));
		
		// Create the request, and check that the results are what is expected
		request = new RequestData(new FloorRequest(1, 5, Direction.UP,null), requestID++);
		expectedElevatorID = -1;
		results = SchedulingAlgorithm.assignSingleRequestToAnElevator(request, requestsActive, elevatorLatestData);
		assertEquals(expectedElevatorID, results);
		
		// Change the request to be picked no elevator (cause the requestFloor == location, and we're moving, not stopping), and check results
		request = new RequestData(new FloorRequest(6, 8, Direction.UP,null), requestID++);
		expectedElevatorID = -1;	// This should work when the state isn't MOVING_UP (because if its MOVING and at the same floor, its not stopping at that floor)
		results = SchedulingAlgorithm.assignSingleRequestToAnElevator(request, requestsActive, elevatorLatestData);
		assertEquals(expectedElevatorID, results);
		
		// Change the request to be picked by the 1st elevator, and check results
		request = new RequestData(new FloorRequest(7, 8, Direction.UP,null), requestID++);
		expectedElevatorID = 1;
		results = SchedulingAlgorithm.assignSingleRequestToAnElevator(request, requestsActive, elevatorLatestData);
		assertEquals(expectedElevatorID, results);
		
		// Change the request to be picked by no elevator (cause elevator 1's highest point is 8), and check results
		request = new RequestData(new FloorRequest(9, 10, Direction.UP,null), requestID++);
		expectedElevatorID = -1;
		results = SchedulingAlgorithm.assignSingleRequestToAnElevator(request, requestsActive, elevatorLatestData);
		assertEquals(expectedElevatorID, results);
		
		// Change the request to be picked by the 1st elevator, and check results
		requestsActive.get(1).get(0).setPickedUpPassenger(true);
		request = new RequestData(new FloorRequest(9, 10, Direction.UP,null), requestID++);
		expectedElevatorID = 1;
		results = SchedulingAlgorithm.assignSingleRequestToAnElevator(request, requestsActive, elevatorLatestData);
		assertEquals(expectedElevatorID, results);
		
		// Change the request to be picked by no elevator (cause elevator 1's highest point is 8), and check results
		request = new RequestData(new FloorRequest(16, 17, Direction.UP,null), requestID++);
		expectedElevatorID = -1;
		results = SchedulingAlgorithm.assignSingleRequestToAnElevator(request, requestsActive, elevatorLatestData);
		assertEquals(expectedElevatorID, results);
		
		// Change the request to be picked by the 2nd elevator, and check results
		request = new RequestData(new FloorRequest(20, 19, Direction.DOWN,null), requestID++);
		expectedElevatorID = -1;
		results = SchedulingAlgorithm.assignSingleRequestToAnElevator(request, requestsActive, elevatorLatestData);
		assertEquals(expectedElevatorID, results);
		
		// Change the request to be picked by no elevator (cause the requestFloor == location, and we're moving, not stopping), and check results
		request = new RequestData(new FloorRequest(18, 16, Direction.DOWN,null), requestID++);
		expectedElevatorID = -1;	// This should work when the state isn't MOVING_DOWN (because if its MOVING and at the same floor, its not stopping at that floor)
		results = SchedulingAlgorithm.assignSingleRequestToAnElevator(request, requestsActive, elevatorLatestData);
		assertEquals(expectedElevatorID, results);
		
		// Change the request to be picked by the 2nd elevator, and check results
		request = new RequestData(new FloorRequest(17, 16, Direction.DOWN,null), requestID++);
		expectedElevatorID = 2;
		results = SchedulingAlgorithm.assignSingleRequestToAnElevator(request, requestsActive, elevatorLatestData);
		assertEquals(expectedElevatorID, results);
		
		// Change the request to be picked by no elevator (cause elevator 2's lowest point is 5), and check results
		request = new RequestData(new FloorRequest(10, 7, Direction.DOWN,null), requestID++);
		expectedElevatorID = -1;
		results = SchedulingAlgorithm.assignSingleRequestToAnElevator(request, requestsActive, elevatorLatestData);
		assertEquals(expectedElevatorID, results);
		
		// Change the request to be picked by the 2nd elevator, and check results
		requestsActive.get(2).get(0).setPickedUpPassenger(true);
		request = new RequestData(new FloorRequest(10, 7, Direction.DOWN,null), requestID++);
		expectedElevatorID = 2;
		results = SchedulingAlgorithm.assignSingleRequestToAnElevator(request, requestsActive, elevatorLatestData);
		assertEquals(expectedElevatorID, results);
		
		// Change the request to be picked by no elevator (cause elevator 2's lowest point is 5), and check results
		request = new RequestData(new FloorRequest(4, 2, Direction.DOWN,null), requestID++);
		expectedElevatorID = -1;
		results = SchedulingAlgorithm.assignSingleRequestToAnElevator(request, requestsActive, elevatorLatestData);
		assertEquals(expectedElevatorID, results);
		
		
		
		// Test 4: Adding a request to an elevator with one job, thats *NOT* MOVING_UP (requests direction=up), or MOVING_DOWN (requests direction=down)
		// Clear previous test data
		request = null;
		requestsActive.clear();	elevatorLatestData.clear();
		requestID = 1; elevatorID = 1; 
		results = -1; expectedElevatorID = -1;
		
		// Checking for all states (since jobless elevators shouldnt care about states)
		for (ElevatorStates state : ElevatorStates.values()) {
			if (!state.equals(ElevatorStates.MOVING_DOWN) && !state.equals(ElevatorStates.MOVING_UP)) {	// Don't check these states, because these were tested above 
				// boolean that is set if the elevator is in an error state.
				boolean isInErrorState = false;
				if (SchedulingAlgorithm.isElevatorIneligibleDueToErrorHandling(state) || state == ElevatorStates.START) {
					isInErrorState = true;
					
				}
				
				// Setting up elevator data & active requests
				elevatorID = 1;
				elevatorLatestData.put(elevatorID, new ElevatorData(new ElevatorSignal(state, elevatorID, 6, null, null, null), null) );	// Elevators start at floor 6
				requestsActive.put(elevatorID, new ArrayList<RequestData>());
				requestsActive.get(elevatorID).add(new RequestData(new FloorRequest(8, 13, Direction.UP,null), requestID++, elevatorID));
				
				elevatorID = 2;
				elevatorLatestData.put(elevatorID, new ElevatorData(new ElevatorSignal(state, elevatorID, 18, null, null, null), null) );	// Elevators start at floor 18
				requestsActive.put(elevatorID, new ArrayList<RequestData>());
				requestsActive.get(elevatorID).add(new RequestData(new FloorRequest(16, 5, Direction.DOWN,null), requestID++, elevatorID));
				
				// Create the request, and check that the results are what is expected
				//System.out.println("TEST 1");
				request = new RequestData(new FloorRequest(1, 5, Direction.UP,null), requestID++);
				expectedElevatorID = (isInErrorState) ? -1 : -1;
				results = SchedulingAlgorithm.assignSingleRequestToAnElevator(request, requestsActive, elevatorLatestData);
				assertEquals(expectedElevatorID, results);
				
				// Change the request to be picked by the 1st elevator (cause the requestFloor == location, and we're NOT moving), and check results
				//System.out.println("TEST 2");
				request = new RequestData(new FloorRequest(6, 8, Direction.UP,null), requestID++);
				expectedElevatorID = (isInErrorState) ? -1 : 1;	// This should work when the state isn't MOVING_UP (because if its MOVING and at the same floor, its not stopping at that floor)
				results = SchedulingAlgorithm.assignSingleRequestToAnElevator(request, requestsActive, elevatorLatestData);
				assertEquals(expectedElevatorID, results);
				
				// Change the request to be picked by the 1st elevator, and check results
				//System.out.println("TEST 3");
				request = new RequestData(new FloorRequest(7, 8, Direction.UP,null), requestID++);
				expectedElevatorID = (isInErrorState) ? -1 : 1;
				results = SchedulingAlgorithm.assignSingleRequestToAnElevator(request, requestsActive, elevatorLatestData);
				assertEquals(expectedElevatorID, results);
				
				// Change the request to be picked by no elevator (cause elevator 1's highest point is 8), and check results
				//System.out.println("TEST 4");
				request = new RequestData(new FloorRequest(9, 10, Direction.UP,null), requestID++);
				expectedElevatorID = (isInErrorState) ? -1 : -1;
				results = SchedulingAlgorithm.assignSingleRequestToAnElevator(request, requestsActive, elevatorLatestData);
				assertEquals(expectedElevatorID, results);
				
				// Change the request to be picked by the 1st elevator, and check results
				//System.out.println("TEST 5");
				requestsActive.get(1).get(0).setPickedUpPassenger(true);
				request = new RequestData(new FloorRequest(9, 10, Direction.UP,null), requestID++);
				expectedElevatorID = (isInErrorState) ? -1 : 1;
				results = SchedulingAlgorithm.assignSingleRequestToAnElevator(request, requestsActive, elevatorLatestData);
				assertEquals(expectedElevatorID, results);
				
				// Change the request to be picked by no elevator (cause elevator 1's highest point is 8), and check results
				//System.out.println("TEST 6");
				request = new RequestData(new FloorRequest(16, 17, Direction.UP,null), requestID++);
				expectedElevatorID = (isInErrorState) ? -1 : -1;
				results = SchedulingAlgorithm.assignSingleRequestToAnElevator(request, requestsActive, elevatorLatestData);
				assertEquals(expectedElevatorID, results);
				
				// Change the request to be picked by the 2nd elevator, and check results
				//System.out.println("TEST 7");
				request = new RequestData(new FloorRequest(20, 19, Direction.DOWN,null), requestID++);
				expectedElevatorID = (isInErrorState) ? -1 : -1;
				results = SchedulingAlgorithm.assignSingleRequestToAnElevator(request, requestsActive, elevatorLatestData);
				assertEquals(expectedElevatorID, results);
				
				// Change the request to be picked by the 2nd elevator (cause the requestFloor == location, and we're NOT moving), and check results
				//System.out.println("TEST 8");
				request = new RequestData(new FloorRequest(18, 16, Direction.DOWN,null), requestID++);
				expectedElevatorID = (isInErrorState) ? -1 : 2;	// This should work when the state isn't MOVING_DOWN (because if its MOVING and at the same floor, its not stopping at that floor)
				results = SchedulingAlgorithm.assignSingleRequestToAnElevator(request, requestsActive, elevatorLatestData);
				assertEquals(expectedElevatorID, results);
				
				// Change the request to be picked by the 2nd elevator, and check results
				//System.out.println("TEST 9");
				request = new RequestData(new FloorRequest(17, 16, Direction.DOWN,null), requestID++);
				expectedElevatorID = (isInErrorState) ? -1 : 2;
				results = SchedulingAlgorithm.assignSingleRequestToAnElevator(request, requestsActive, elevatorLatestData);
				assertEquals(expectedElevatorID, results);
				
				// Change the request to be picked by no elevator (cause elevator 2's lowest point is 5), and check results
				//System.out.println("TEST 10");
				request = new RequestData(new FloorRequest(10, 7, Direction.DOWN,null), requestID++);
				expectedElevatorID = (isInErrorState) ? -1 : -1;
				results = SchedulingAlgorithm.assignSingleRequestToAnElevator(request, requestsActive, elevatorLatestData);
				assertEquals(expectedElevatorID, results);
				
				// Change the request to be picked by the 2nd elevator, and check results
				//System.out.println("TEST 11");
				requestsActive.get(2).get(0).setPickedUpPassenger(true);
				request = new RequestData(new FloorRequest(10, 7, Direction.DOWN,null), requestID++);
				expectedElevatorID = (isInErrorState) ? -1 : 2;
				results = SchedulingAlgorithm.assignSingleRequestToAnElevator(request, requestsActive, elevatorLatestData);
				assertEquals(expectedElevatorID, results);
				
				// Change the request to be picked by no elevator (cause elevator 2's lowest point is 5), and check results
				//System.out.println("TEST 12");
				request = new RequestData(new FloorRequest(4, 2, Direction.DOWN,null), requestID++);
				expectedElevatorID = (isInErrorState) ? -1 : -1;
				results = SchedulingAlgorithm.assignSingleRequestToAnElevator(request, requestsActive, elevatorLatestData);
				assertEquals(expectedElevatorID, results);
			}
		}
	}
	
	
	
	@Test
	/*
	 * Test the sorting of an ArrayList of RequestData in the requestActive HashMap. Sorting is done in order of where the elevator
	 * is supposed to go next. For example, if the elevator is at floor 3 and is moving up, while having requests to pick up a 
	 * passenger on floors 9, 12, and 15, then the list would be ordered as 9, 12, 15. Once the passenger at floor 9 is picked up, 
	 * and say they want to be dropped of at floor 13, then the list becomes 12,13,15.
	 */
	public void sortSingleActiveRequestsListTest() {
		ArrayList<RequestData> requestsActive = new ArrayList<RequestData>();
		ElevatorData elevator;
		
		
		// Test 1: No passenger picked up, adding request to list (MOVING_UP)
		// Create Test Values
		RequestData request;
		int elevatorID = 1;
		int requestID = 1;
		elevator = new ElevatorData(new ElevatorSignal(ElevatorStates.MOVING_UP, elevatorID, 1, null, null, null), null);	// Elevators start at floor 1
		request = new RequestData(new FloorRequest(5, 1, Direction.DOWN,null), requestID++, elevatorID);
		requestsActive.add(request);
		request = new RequestData(new FloorRequest(7, 1, Direction.DOWN,null), requestID++, elevatorID);
		requestsActive.add(request);
		request = new RequestData(new FloorRequest(9, 1, Direction.DOWN,null), requestID++, elevatorID);
		requestsActive.add(request);
		request = new RequestData(new FloorRequest(6, 1, Direction.DOWN,null), requestID++, elevatorID);
		requestsActive.add(request);
		
		// Create Expected Values
		ArrayList<RequestData> expectedValues = new ArrayList<RequestData>();
		expectedValues.add( requestsActive.get(0) );
		expectedValues.add( requestsActive.get(3) );
		expectedValues.add( requestsActive.get(1) );
		expectedValues.add( requestsActive.get(2) );
		
		// Check that the results are what is expected
		ArrayList<RequestData> results = SchedulingAlgorithm.sortSingleActiveRequestsList(requestsActive, elevator);
		assertEquals(expectedValues, results);
		
		
		// Test 2: No passenger picked up, adding request to list (MOVING_DOWN)
		// Clean Variables from previous test
		requestsActive.clear();
		expectedValues.clear();
		results.clear();
		request = null;
		elevatorID = 1;
		requestID = 1;
		
		// Create Test Values
		elevator = new ElevatorData(new ElevatorSignal(ElevatorStates.MOVING_DOWN, elevatorID, 10, null, null, null), null);	// Elevators start at floor 10
		request = new RequestData(new FloorRequest(5, 1, Direction.DOWN,null), requestID++, elevatorID);
		requestsActive.add(request);
		request = new RequestData(new FloorRequest(7, 1, Direction.DOWN,null), requestID++, elevatorID);
		requestsActive.add(request);
		request = new RequestData(new FloorRequest(9, 1, Direction.DOWN,null), requestID++, elevatorID);
		requestsActive.add(request);
		request = new RequestData(new FloorRequest(6, 1, Direction.DOWN,null), requestID++, elevatorID);
		requestsActive.add(request);
		
		// Create Expected Values
		expectedValues.add( requestsActive.get(2) );
		expectedValues.add( requestsActive.get(1) );
		expectedValues.add( requestsActive.get(3) );
		expectedValues.add( requestsActive.get(0) );
		
		// Check that the results are what is expected
		results = SchedulingAlgorithm.sortSingleActiveRequestsList(requestsActive, elevator);
		assertEquals(expectedValues, results);
		
		
		// Test 3: One passenger picked up, adding request to list (MOVING_UP)
		// Clean Variables from previous test
		requestsActive.clear();
		expectedValues.clear();
		results.clear();
		request = null;
		elevatorID = 1;
		requestID = 1;
		
		// Create Test Values
		elevator = new ElevatorData(new ElevatorSignal(ElevatorStates.MOVING_UP, elevatorID, 3, null, null, null), null);	// Elevators start at floor 3
		request = new RequestData(new FloorRequest(3, 1, Direction.DOWN,null), requestID++, elevatorID);
		request.setPickedUpPassenger(true);
		requestsActive.add(request);
		request = new RequestData(new FloorRequest(7, 1, Direction.DOWN,null), requestID++, elevatorID);
		requestsActive.add(request);
		request = new RequestData(new FloorRequest(9, 1, Direction.DOWN,null), requestID++, elevatorID);
		requestsActive.add(request);
		request = new RequestData(new FloorRequest(6, 1, Direction.DOWN,null), requestID++, elevatorID);
		requestsActive.add(request);
		
		// Create Expected Values
		expectedValues.add( requestsActive.get(3) );
		expectedValues.add( requestsActive.get(1) );
		expectedValues.add( requestsActive.get(2) );
		expectedValues.add( requestsActive.get(0) );
		
		// Check that the results are what is expected
		results = SchedulingAlgorithm.sortSingleActiveRequestsList(requestsActive, elevator);
		assertEquals(expectedValues, results);
		
		
		// Test 4: One passenger picked up, adding request to list (MOVING_DOWN)
		// Clean Variables from previous test
		requestsActive.clear();
		expectedValues.clear();
		results.clear();
		request = null;
		elevatorID = 1;
		requestID = 1;
		
		// Create Test Values
		elevator = new ElevatorData(new ElevatorSignal(ElevatorStates.MOVING_DOWN, elevatorID, 8, null, null, null), null);	// Elevators start at floor 8
		request = new RequestData(new FloorRequest(3, 1, Direction.DOWN,null), requestID++, elevatorID);
		requestsActive.add(request);
		request = new RequestData(new FloorRequest(7, 1, Direction.DOWN,null), requestID++, elevatorID);
		requestsActive.add(request);
		request = new RequestData(new FloorRequest(9, 12, Direction.UP,null), requestID++, elevatorID);
		request.setPickedUpPassenger(true);
		requestsActive.add(request);
		request = new RequestData(new FloorRequest(6, 1, Direction.DOWN,null), requestID++, elevatorID);
		requestsActive.add(request);
		
		// Create Expected Values
		expectedValues.add( requestsActive.get(1) );
		expectedValues.add( requestsActive.get(3) );
		expectedValues.add( requestsActive.get(0) );
		expectedValues.add( requestsActive.get(2) );
		
		// Check that the results are what is expected
		results = SchedulingAlgorithm.sortSingleActiveRequestsList(requestsActive, elevator);
		assertEquals(expectedValues, results);
		
		
		// Test 5: Two passengers picked up, adding request to list (MOVING_UP)
		// Clean Variables from previous test
		requestsActive.clear();
		expectedValues.clear();
		results.clear();
		request = null;
		elevatorID = 1;
		requestID = 1;
		
		// Create Test Values
		elevator = new ElevatorData(new ElevatorSignal(ElevatorStates.MOVING_UP, elevatorID, 4, null, null, null), null);	// Elevators start at floor 4
		request = new RequestData(new FloorRequest(2, 2, Direction.DOWN,null), requestID++, elevatorID);
		request.setPickedUpPassenger(true);
		requestsActive.add(request);
		request = new RequestData(new FloorRequest(3, 4, Direction.DOWN,null), requestID++, elevatorID);
		request.setPickedUpPassenger(true);
		requestsActive.add(request);
		request = new RequestData(new FloorRequest(7, 1, Direction.DOWN,null), requestID++, elevatorID);
		requestsActive.add(request);
		request = new RequestData(new FloorRequest(9, 1, Direction.DOWN,null), requestID++, elevatorID);
		requestsActive.add(request);
		request = new RequestData(new FloorRequest(6, 1, Direction.DOWN,null), requestID++, elevatorID);
		requestsActive.add(request);
		
		// Create Expected Values
		expectedValues.add( requestsActive.get(4) );
		expectedValues.add( requestsActive.get(2) );
		expectedValues.add( requestsActive.get(3) );
		expectedValues.add( requestsActive.get(1) );
		expectedValues.add( requestsActive.get(0) );
		
		// Check that the results are what is expected
		results = SchedulingAlgorithm.sortSingleActiveRequestsList(requestsActive, elevator);
		assertEquals(expectedValues, results);
		
		
		// Test 6: Two passengers picked up, adding request to list (MOVING_DOWN)
		// Clean Variables from previous test
		requestsActive.clear();
		expectedValues.clear();
		results.clear();
		request = null;
		elevatorID = 1;
		requestID = 1;
		
		// Create Test Values
		elevator = new ElevatorData(new ElevatorSignal(ElevatorStates.MOVING_DOWN, elevatorID, 9, null, null, null), null);	// Elevators start at floor 9
		request = new RequestData(new FloorRequest(3, 1, Direction.DOWN,null), requestID++, elevatorID);
		requestsActive.add(request);
		request = new RequestData(new FloorRequest(7, 1, Direction.DOWN,null), requestID++, elevatorID);
		requestsActive.add(request);
		request = new RequestData(new FloorRequest(9, 14, Direction.UP,null), requestID++, elevatorID);
		request.setPickedUpPassenger(true);
		requestsActive.add(request);
		request = new RequestData(new FloorRequest(6, 1, Direction.DOWN,null), requestID++, elevatorID);
		requestsActive.add(request);
		request = new RequestData(new FloorRequest(11, 17, Direction.UP,null), requestID++, elevatorID);
		request.setPickedUpPassenger(true);
		requestsActive.add(request);
		
		// Create Expected Values
		expectedValues.add( requestsActive.get(1) );
		expectedValues.add( requestsActive.get(3) );
		expectedValues.add( requestsActive.get(0) );
		expectedValues.add( requestsActive.get(2) );
		expectedValues.add( requestsActive.get(4) );
		
		// Check that the results are what is expected
		results = SchedulingAlgorithm.sortSingleActiveRequestsList(requestsActive, elevator);
		assertEquals(expectedValues, results);
		
		
		// Test 7: All but 1 passengers picked up, adding request to list (MOVING_UP)
		// Clean Variables from previous test
		requestsActive.clear();
		expectedValues.clear();
		results.clear();
		request = null;
		elevatorID = 1;
		requestID = 1;
		
		// Create Test Values
		elevator = new ElevatorData(new ElevatorSignal(ElevatorStates.MOVING_UP, elevatorID, 7, null, null, null), null);	// Elevators start at floor 7
		request = new RequestData(new FloorRequest(2, 2, Direction.DOWN,null), requestID++, elevatorID);
		request.setPickedUpPassenger(true);
		requestsActive.add(request);
		request = new RequestData(new FloorRequest(3, 4, Direction.DOWN,null), requestID++, elevatorID);
		request.setPickedUpPassenger(true);
		requestsActive.add(request);
		request = new RequestData(new FloorRequest(7, 3, Direction.DOWN,null), requestID++, elevatorID);
		request.setPickedUpPassenger(true);
		requestsActive.add(request);
		request = new RequestData(new FloorRequest(9, 7, Direction.DOWN,null), requestID++, elevatorID);
		requestsActive.add(request);
		request = new RequestData(new FloorRequest(6, 1, Direction.DOWN,null), requestID++, elevatorID);
		request.setPickedUpPassenger(true);
		requestsActive.add(request);
		
		// Create Expected Values
		expectedValues.add( requestsActive.get(3) );
		expectedValues.add( requestsActive.get(1) );
		expectedValues.add( requestsActive.get(2) );
		expectedValues.add( requestsActive.get(0) );
		expectedValues.add( requestsActive.get(4) );
		
		// Check that the results are what is expected
		results = SchedulingAlgorithm.sortSingleActiveRequestsList(requestsActive, elevator);
		assertEquals(expectedValues, results);
		
		
		// Test 8: All but 1 passengers picked up, adding request to list (MOVING_DOWN)
		// Clean Variables from previous test
		requestsActive.clear();
		expectedValues.clear();
		results.clear();
		request = null;
		elevatorID = 1;
		requestID = 1;
		
		// Create Test Values
		elevator = new ElevatorData(new ElevatorSignal(ElevatorStates.MOVING_DOWN, elevatorID, 5, null, null, null), null);	// Elevators start at floor 5
		request = new RequestData(new FloorRequest(3, 1, Direction.DOWN,null), requestID++, elevatorID);
		requestsActive.add(request);
		request = new RequestData(new FloorRequest(7, 3, Direction.DOWN,null), requestID++, elevatorID);
		request.setPickedUpPassenger(true);
		requestsActive.add(request);
		request = new RequestData(new FloorRequest(9, 14, Direction.UP,null), requestID++, elevatorID);
		request.setPickedUpPassenger(true);
		requestsActive.add(request);
		request = new RequestData(new FloorRequest(6, 5, Direction.DOWN,null), requestID++, elevatorID);
		request.setPickedUpPassenger(true);
		requestsActive.add(request);
		request = new RequestData(new FloorRequest(11, 17, Direction.UP,null), requestID++, elevatorID);
		request.setPickedUpPassenger(true);
		requestsActive.add(request);
		
		// Create Expected Values
		expectedValues.add( requestsActive.get(0) );
		expectedValues.add( requestsActive.get(1) );
		expectedValues.add( requestsActive.get(3) );
		expectedValues.add( requestsActive.get(2) );
		expectedValues.add( requestsActive.get(4) );
		
		// Check that the results are what is expected
		results = SchedulingAlgorithm.sortSingleActiveRequestsList(requestsActive, elevator);
		assertEquals(expectedValues, results);
		
		
		// Test 9: All passengers picked up, adding request to list (MOVING_UP)
		// Clean Variables from previous test
		requestsActive.clear();
		expectedValues.clear();
		results.clear();
		request = null;
		elevatorID = 1;
		requestID = 1;
		
		// Create Test Values
		elevator = new ElevatorData(new ElevatorSignal(ElevatorStates.MOVING_UP, elevatorID, 1, null, null, null), null);	// Elevators start at floor 1
		request = new RequestData(new FloorRequest(2, 2, Direction.DOWN,null), requestID++, elevatorID);
		request.setPickedUpPassenger(true);
		requestsActive.add(request);
		request = new RequestData(new FloorRequest(3, 4, Direction.DOWN,null), requestID++, elevatorID);
		request.setPickedUpPassenger(true);
		requestsActive.add(request);
		request = new RequestData(new FloorRequest(7, 3, Direction.DOWN,null), requestID++, elevatorID);
		request.setPickedUpPassenger(true);
		requestsActive.add(request);
		request = new RequestData(new FloorRequest(9, 7, Direction.DOWN,null), requestID++, elevatorID);
		request.setPickedUpPassenger(true);
		requestsActive.add(request);
		request = new RequestData(new FloorRequest(6, 9, Direction.DOWN,null), requestID++, elevatorID);
		request.setPickedUpPassenger(true);
		requestsActive.add(request);
		
		// Create Expected Values
		expectedValues.add( requestsActive.get(0) );
		expectedValues.add( requestsActive.get(2) );
		expectedValues.add( requestsActive.get(1) );
		expectedValues.add( requestsActive.get(3) );
		expectedValues.add( requestsActive.get(4) );
		
		// Check that the results are what is expected
		results = SchedulingAlgorithm.sortSingleActiveRequestsList(requestsActive, elevator);
		assertEquals(expectedValues, results);
		
		
		// Test 10: All passengers picked up, adding request to list (MOVING_DOWN)
		// Clean Variables from previous test
		requestsActive.clear();
		expectedValues.clear();
		results.clear();
		request = null;
		elevatorID = 1;
		requestID = 1;
		
		// Create Test Values
		elevator = new ElevatorData(new ElevatorSignal(ElevatorStates.MOVING_DOWN, elevatorID, 18, null, null, null), null);	// Elevators start at floor 18
		request = new RequestData(new FloorRequest(3, 1, Direction.DOWN,null), requestID++, elevatorID);
		request.setPickedUpPassenger(true);
		requestsActive.add(request);
		request = new RequestData(new FloorRequest(7, 3, Direction.DOWN,null), requestID++, elevatorID);
		request.setPickedUpPassenger(true);
		requestsActive.add(request);
		request = new RequestData(new FloorRequest(9, 14, Direction.UP,null), requestID++, elevatorID);
		request.setPickedUpPassenger(true);
		requestsActive.add(request);
		request = new RequestData(new FloorRequest(6, 5, Direction.DOWN,null), requestID++, elevatorID);
		request.setPickedUpPassenger(true);
		requestsActive.add(request);
		request = new RequestData(new FloorRequest(11, 17, Direction.UP,null), requestID++, elevatorID);
		request.setPickedUpPassenger(true);
		requestsActive.add(request);
		
		// Create Expected Values
		expectedValues.add( requestsActive.get(4) );
		expectedValues.add( requestsActive.get(2) );
		expectedValues.add( requestsActive.get(3) );
		expectedValues.add( requestsActive.get(1) );
		expectedValues.add( requestsActive.get(0) );
		
		// Check that the results are what is expected
		results = SchedulingAlgorithm.sortSingleActiveRequestsList(requestsActive, elevator);
		assertEquals(expectedValues, results);
	}
	
	
	@Test
	/*
	 * Tests calculating the trip times of active requests for a given elevator.
	 */
	public void calculateTripTimeTest() {
		HashMap<Integer, ArrayList<RequestData>> requestsActive = new HashMap<Integer, ArrayList<RequestData>>();
		HashMap<Integer, ElevatorData> elevatorLatestData = new HashMap<Integer, ElevatorData>();
		HashMap<Integer, ArrayList<Integer>> expectedValues = new HashMap<Integer, ArrayList<Integer>>();
		int nElevators = 5;
		for (int i = 1; i <= nElevators; i++) {
			requestsActive.put(i, new ArrayList<RequestData>());
			elevatorLatestData.put(i, new ElevatorData(new ElevatorSignal(ElevatorStates.IDLE, i, 1, null, null, null), null) );	// Elevators start at floor 1
			expectedValues.put(i, new ArrayList<Integer>());
		}
		
		// Create Test Values (requestsActive)
		// NOTE: the syntax is as follows: new RequestData(new FloorRequest( REQUEST_FLOOR, TARGET_FLOOR, DIRECTION=null), REQUEST_ID, ELEVATOR_ID));
		
		// Elevator 1: Testing next Pick-up location at current drop-off locations (going up)
		int elevatorID = 1;
		elevatorLatestData.put(elevatorID, new ElevatorData(new ElevatorSignal(ElevatorStates.IDLE, elevatorID, 1, null, null, null), null) );	// Elevators start at floor 1
		requestsActive.get(elevatorID).add(new RequestData(new FloorRequest(1, 1, null,null),    1, elevatorID));	// 0 floors
		requestsActive.get(elevatorID).add(new RequestData(new FloorRequest(1, 2, null,null),    2, elevatorID));	// 1 floors
		requestsActive.get(elevatorID).add(new RequestData(new FloorRequest(2, 4, null,null),    3, elevatorID));	// 2 floors
		requestsActive.get(elevatorID).add(new RequestData(new FloorRequest(4, 7, null,null),    4, elevatorID));	// 3 floors
		requestsActive.get(elevatorID).add(new RequestData(new FloorRequest(7, 11, null,null),   5, elevatorID));	// 4 floors
		requestsActive.get(elevatorID).add(new RequestData(new FloorRequest(11, 21, null,null),  6, elevatorID));	// 10 floors
		requestsActive.get(elevatorID).add(new RequestData(new FloorRequest(21, 121, null,null), 7, elevatorID));	// 100 floors
		
		// Elevator 2: Testing next Pick-up location at current drop-off locations (going down)
		elevatorID = 2;
		elevatorLatestData.put(elevatorID, new ElevatorData(new ElevatorSignal(ElevatorStates.IDLE, elevatorID, 150, null, null, null), null) );	// Elevators start at floor 150
		requestsActive.get(elevatorID).add(new RequestData(new FloorRequest(150, 150, null,null), 8, elevatorID));	// 0 floors
		requestsActive.get(elevatorID).add(new RequestData(new FloorRequest(150, 149, null,null), 9, elevatorID));	// 1 floors
		requestsActive.get(elevatorID).add(new RequestData(new FloorRequest(149, 147, null,null), 10, elevatorID));	// 2 floors
		requestsActive.get(elevatorID).add(new RequestData(new FloorRequest(147, 144, null,null), 11, elevatorID));	// 3 floors
		requestsActive.get(elevatorID).add(new RequestData(new FloorRequest(144, 140, null,null), 12, elevatorID));	// 4 floors
		requestsActive.get(elevatorID).add(new RequestData(new FloorRequest(140, 100, null,null), 13, elevatorID));	// 40 floors
		requestsActive.get(elevatorID).add(new RequestData(new FloorRequest(100, 1, null,null),   14, elevatorID));	// 99 floors
		
		// Elevator 3: Testing normal movement (going up)
		elevatorID = 3;
		elevatorLatestData.put(elevatorID, new ElevatorData(new ElevatorSignal(ElevatorStates.IDLE, elevatorID, 1, null, null, null), null) );	// Elevators start at floor 1
		requestsActive.get(elevatorID).add(new RequestData(new FloorRequest(1, 1, null,null),    15, elevatorID));	// 0 floors to pickup, 0 floors to drop off
		requestsActive.get(elevatorID).add(new RequestData(new FloorRequest(2, 3, null,null),    16, elevatorID));	// 1 floors to pickup, 1 floors to drop off
		requestsActive.get(elevatorID).add(new RequestData(new FloorRequest(4, 6, null,null),    17, elevatorID));	// 1 floors to pickup, 2 floors to drop off
		requestsActive.get(elevatorID).add(new RequestData(new FloorRequest(7, 10, null,null),   18, elevatorID));	// 1 floors to pickup, 3 floors to drop off
		requestsActive.get(elevatorID).add(new RequestData(new FloorRequest(12, 13, null,null),  19, elevatorID));	// 2 floors to pickup, 1 floors to drop off
		requestsActive.get(elevatorID).add(new RequestData(new FloorRequest(15, 17, null,null),  20, elevatorID));	// 2 floors to pickup, 2 floors to drop off
		requestsActive.get(elevatorID).add(new RequestData(new FloorRequest(19, 22, null,null),  21, elevatorID));	// 2 floors to pickup, 3 floors to drop off
		requestsActive.get(elevatorID).add(new RequestData(new FloorRequest(25, 26, null,null),  22, elevatorID));	// 3 floors to pickup, 1 floors to drop off
		requestsActive.get(elevatorID).add(new RequestData(new FloorRequest(29, 31, null,null),  23, elevatorID));	// 3 floors to pickup, 2 floors to drop off
		requestsActive.get(elevatorID).add(new RequestData(new FloorRequest(34, 37, null,null),  24, elevatorID));	// 3 floors to pickup, 3 floors to drop off
		requestsActive.get(elevatorID).add(new RequestData(new FloorRequest(44, 49, null,null),  25, elevatorID));	// 7 floors to pickup, 5 floors to drop off
		requestsActive.get(elevatorID).add(new RequestData(new FloorRequest(69, 129, null,null), 26, elevatorID));	// 20 floors to pickup, 60 floors to drop off
		
		// Elevator 4: Testing normal movement (going down)
		elevatorID = 4;
		elevatorLatestData.put(elevatorID, new ElevatorData(new ElevatorSignal(ElevatorStates.IDLE, elevatorID, 150, null, null, null), null) );	// Elevators start at floor 150
		requestsActive.get(elevatorID).add(new RequestData(new FloorRequest(150, 150, null,null), 27, elevatorID));	// 0 floors to pickup, 0 floors to drop off
		requestsActive.get(elevatorID).add(new RequestData(new FloorRequest(149, 148, null,null), 28, elevatorID));	// 1 floors to pickup, 1 floors to drop off
		requestsActive.get(elevatorID).add(new RequestData(new FloorRequest(147, 145, null,null), 29, elevatorID));	// 1 floors to pickup, 2 floors to drop off
		requestsActive.get(elevatorID).add(new RequestData(new FloorRequest(144, 141, null,null), 30, elevatorID));	// 1 floors to pickup, 3 floors to drop off
		requestsActive.get(elevatorID).add(new RequestData(new FloorRequest(139, 138, null,null), 31, elevatorID));	// 2 floors to pickup, 1 floors to drop off
		requestsActive.get(elevatorID).add(new RequestData(new FloorRequest(136, 134, null,null), 32, elevatorID));	// 2 floors to pickup, 2 floors to drop off
		requestsActive.get(elevatorID).add(new RequestData(new FloorRequest(132, 129, null,null), 33, elevatorID));	// 2 floors to pickup, 3 floors to drop off
		requestsActive.get(elevatorID).add(new RequestData(new FloorRequest(126, 125, null,null), 34, elevatorID));	// 3 floors to pickup, 1 floors to drop off
		requestsActive.get(elevatorID).add(new RequestData(new FloorRequest(122, 120, null,null), 35, elevatorID));	// 3 floors to pickup, 2 floors to drop off
		requestsActive.get(elevatorID).add(new RequestData(new FloorRequest(117, 114, null,null), 36, elevatorID));	// 3 floors to pickup, 3 floors to drop off
		requestsActive.get(elevatorID).add(new RequestData(new FloorRequest(102, 96, null,null),  37, elevatorID));	// 12 floors to pickup, 6 floors to drop off
		requestsActive.get(elevatorID).add(new RequestData(new FloorRequest(78, 14, null,null),   38, elevatorID));	// 18 floors to pickup, 64 floors to drop off
		
		// Elevator 5: Testing random movement (going in random directions)
		elevatorID = 5;
		elevatorLatestData.put(elevatorID, new ElevatorData(new ElevatorSignal(ElevatorStates.IDLE, elevatorID, 1, null, null, null), null) );	// Elevators start at floor 1
		requestsActive.get(elevatorID).add(new RequestData(new FloorRequest(1,  5,  null,null), 39, elevatorID));	// 0 floors to pickup, 4 floors to drop off
		requestsActive.get(elevatorID).add(new RequestData(new FloorRequest(7,  18, null,null), 40, elevatorID));	// 2 floors to pickup, 11 floors to drop off
		requestsActive.get(elevatorID).add(new RequestData(new FloorRequest(3,  16, null,null), 41, elevatorID));	// 15 floors to pickup, 13 floors to drop off
		requestsActive.get(elevatorID).add(new RequestData(new FloorRequest(15, 17, null,null), 42, elevatorID));	// 1 floors to pickup, 2 floors to drop off
		requestsActive.get(elevatorID).add(new RequestData(new FloorRequest(16, 9,  null,null), 43, elevatorID));	// 1 floors to pickup, 7 floors to drop off
		requestsActive.get(elevatorID).add(new RequestData(new FloorRequest(2,  7,  null,null), 44, elevatorID));	// 7 floors to pickup, 5 floors to drop off
		requestsActive.get(elevatorID).add(new RequestData(new FloorRequest(4,  22, null,null), 45, elevatorID));	// 3 floors to pickup, 18 floors to drop off
		requestsActive.get(elevatorID).add(new RequestData(new FloorRequest(22, 5,  null,null), 46, elevatorID));	// 0 floors to pickup, 17 floors to drop off
		requestsActive.get(elevatorID).add(new RequestData(new FloorRequest(1,  6,  null,null), 47, elevatorID));	// 4 floors to pickup, 5 floors to drop off
		requestsActive.get(elevatorID).add(new RequestData(new FloorRequest(8,  9,  null,null), 48, elevatorID));	// 2 floors to pickup, 1 floors to drop off
		requestsActive.get(elevatorID).add(new RequestData(new FloorRequest(12, 18, null,null), 49, elevatorID));	// 3 floors to pickup, 6 floors to drop off
		requestsActive.get(elevatorID).add(new RequestData(new FloorRequest(18, 1,  null,null), 50, elevatorID));	// 0 floors to pickup, 17 floors to drop off
		
		
		// Create Expected Values (expectedValues)
		elevatorID = 1;
		expectedValues.get(elevatorID).add((ElevatorTimes.DOORS * 2) + (ElevatorTimes.DOORS * 2));	// Doors open and close when "picking up" and "Dropping off", even though its the same floor
		expectedValues.get(elevatorID).add((ElevatorTimes.DOORS * 2) + ElevatorTimes.STOP   + (ElevatorTimes.DOORS * 2));
		expectedValues.get(elevatorID).add((ElevatorTimes.DOORS * 2) + ElevatorTimes.MOVING + ElevatorTimes.STOP + (ElevatorTimes.DOORS * 2));
		expectedValues.get(elevatorID).add((ElevatorTimes.DOORS * 2) + ElevatorTimes.MOVING + (1 * ElevatorTimes.MOVING_MAX) + ElevatorTimes.STOP + (ElevatorTimes.DOORS * 2));
		expectedValues.get(elevatorID).add((ElevatorTimes.DOORS * 2) + ElevatorTimes.MOVING + (2 * ElevatorTimes.MOVING_MAX) + ElevatorTimes.STOP + (ElevatorTimes.DOORS * 2));
		expectedValues.get(elevatorID).add((ElevatorTimes.DOORS * 2) + ElevatorTimes.MOVING + (8 * ElevatorTimes.MOVING_MAX) + ElevatorTimes.STOP + (ElevatorTimes.DOORS * 2));
		expectedValues.get(elevatorID).add((ElevatorTimes.DOORS * 2) + ElevatorTimes.MOVING + (98 * ElevatorTimes.MOVING_MAX) + ElevatorTimes.STOP + (ElevatorTimes.DOORS * 2));
		
		elevatorID = 2;
		expectedValues.get(elevatorID).add((ElevatorTimes.DOORS * 2) + (ElevatorTimes.DOORS * 2));	// Doors open and close when "picking up" and "Dropping off", even though its the same floor
		expectedValues.get(elevatorID).add((ElevatorTimes.DOORS * 2) + ElevatorTimes.STOP   + (ElevatorTimes.DOORS * 2));
		expectedValues.get(elevatorID).add((ElevatorTimes.DOORS * 2) + ElevatorTimes.MOVING + ElevatorTimes.STOP + (ElevatorTimes.DOORS * 2));
		expectedValues.get(elevatorID).add((ElevatorTimes.DOORS * 2) + ElevatorTimes.MOVING + (1 * ElevatorTimes.MOVING_MAX) + ElevatorTimes.STOP + (ElevatorTimes.DOORS * 2));
		expectedValues.get(elevatorID).add((ElevatorTimes.DOORS * 2) + ElevatorTimes.MOVING + (2 * ElevatorTimes.MOVING_MAX) + ElevatorTimes.STOP + (ElevatorTimes.DOORS * 2));
		expectedValues.get(elevatorID).add((ElevatorTimes.DOORS * 2) + ElevatorTimes.MOVING + (38 * ElevatorTimes.MOVING_MAX) + ElevatorTimes.STOP + (ElevatorTimes.DOORS * 2));
		expectedValues.get(elevatorID).add((ElevatorTimes.DOORS * 2) + ElevatorTimes.MOVING + (97 * ElevatorTimes.MOVING_MAX) + ElevatorTimes.STOP + (ElevatorTimes.DOORS * 2));
		
		elevatorID = 3;
		expectedValues.get(elevatorID).add((ElevatorTimes.DOORS * 2) + (ElevatorTimes.DOORS * 2));	// Doors open and close when "picking up" and "Dropping off", even though its the same floor
		expectedValues.get(elevatorID).add(ElevatorTimes.STOP + (ElevatorTimes.DOORS * 2) + ElevatorTimes.STOP   + (ElevatorTimes.DOORS * 2));	// 1 floors to pickup, 1 floors to drop off
		expectedValues.get(elevatorID).add(ElevatorTimes.STOP + (ElevatorTimes.DOORS * 2) + ElevatorTimes.MOVING + ElevatorTimes.STOP + (ElevatorTimes.DOORS * 2));	// // 1 floors to pickup, 2 floors to drop off
		expectedValues.get(elevatorID).add(ElevatorTimes.STOP + (ElevatorTimes.DOORS * 2) + ElevatorTimes.MOVING + (1 * ElevatorTimes.MOVING_MAX) + ElevatorTimes.STOP + (ElevatorTimes.DOORS * 2));	// 1 floors to pickup, 3 floors to drop off
		expectedValues.get(elevatorID).add(ElevatorTimes.MOVING + ElevatorTimes.STOP + (ElevatorTimes.DOORS * 2) + ElevatorTimes.STOP   + (ElevatorTimes.DOORS * 2));	// 2 floors to pickup, 1 floors to drop off
		expectedValues.get(elevatorID).add(ElevatorTimes.MOVING + ElevatorTimes.STOP + (ElevatorTimes.DOORS * 2) + ElevatorTimes.MOVING + ElevatorTimes.STOP + (ElevatorTimes.DOORS * 2));	// 2 floors to pickup, 2 floors to drop off
		expectedValues.get(elevatorID).add(ElevatorTimes.MOVING + ElevatorTimes.STOP + (ElevatorTimes.DOORS * 2) + ElevatorTimes.MOVING + (1 * ElevatorTimes.MOVING_MAX) + ElevatorTimes.STOP + (ElevatorTimes.DOORS * 2));	// 2 floors to pickup, 3 floors to drop off
		expectedValues.get(elevatorID).add(ElevatorTimes.MOVING + (1 * ElevatorTimes.MOVING_MAX) + ElevatorTimes.STOP + (ElevatorTimes.DOORS * 2) + ElevatorTimes.STOP   + (ElevatorTimes.DOORS * 2));	// 3 floors to pickup, 1 floors to drop off
		expectedValues.get(elevatorID).add(ElevatorTimes.MOVING + (1 * ElevatorTimes.MOVING_MAX) + ElevatorTimes.STOP + (ElevatorTimes.DOORS * 2) + ElevatorTimes.MOVING + ElevatorTimes.STOP + (ElevatorTimes.DOORS * 2));	// 3 floors to pickup, 2 floors to drop off
		expectedValues.get(elevatorID).add(ElevatorTimes.MOVING + (1 * ElevatorTimes.MOVING_MAX) + ElevatorTimes.STOP + (ElevatorTimes.DOORS * 2) + ElevatorTimes.MOVING + (1 * ElevatorTimes.MOVING_MAX) + ElevatorTimes.STOP + (ElevatorTimes.DOORS * 2));	// 3 floors to pickup, 3 floors to drop off
		expectedValues.get(elevatorID).add(ElevatorTimes.MOVING + (5 * ElevatorTimes.MOVING_MAX)  + ElevatorTimes.STOP + (ElevatorTimes.DOORS * 2) + ElevatorTimes.MOVING + (3 * ElevatorTimes.MOVING_MAX)  + ElevatorTimes.STOP + (ElevatorTimes.DOORS * 2));	// 7 floors to pickup, 5 floors to drop off
		expectedValues.get(elevatorID).add(ElevatorTimes.MOVING + (18 * ElevatorTimes.MOVING_MAX) + ElevatorTimes.STOP + (ElevatorTimes.DOORS * 2) + ElevatorTimes.MOVING + (58 * ElevatorTimes.MOVING_MAX) + ElevatorTimes.STOP + (ElevatorTimes.DOORS * 2));	// 20 floors to pickup, 60 floors to drop off
		
		elevatorID = 4;
		expectedValues.get(elevatorID).add((ElevatorTimes.DOORS * 2) + (ElevatorTimes.DOORS * 2));	// Doors open and close when "picking up" and "Dropping off", even though its the same floor
		expectedValues.get(elevatorID).add(ElevatorTimes.STOP + (ElevatorTimes.DOORS * 2) + ElevatorTimes.STOP   + (ElevatorTimes.DOORS * 2));	// 1 floors to pickup, 1 floors to drop off
		expectedValues.get(elevatorID).add(ElevatorTimes.STOP + (ElevatorTimes.DOORS * 2) + ElevatorTimes.MOVING + ElevatorTimes.STOP + (ElevatorTimes.DOORS * 2));	// // 1 floors to pickup, 2 floors to drop off
		expectedValues.get(elevatorID).add(ElevatorTimes.STOP + (ElevatorTimes.DOORS * 2) + ElevatorTimes.MOVING + (1 * ElevatorTimes.MOVING_MAX) + ElevatorTimes.STOP + (ElevatorTimes.DOORS * 2));	// 1 floors to pickup, 3 floors to drop off
		expectedValues.get(elevatorID).add(ElevatorTimes.MOVING + ElevatorTimes.STOP + (ElevatorTimes.DOORS * 2) + ElevatorTimes.STOP   + (ElevatorTimes.DOORS * 2));	// 2 floors to pickup, 1 floors to drop off
		expectedValues.get(elevatorID).add(ElevatorTimes.MOVING + ElevatorTimes.STOP + (ElevatorTimes.DOORS * 2) + ElevatorTimes.MOVING + ElevatorTimes.STOP + (ElevatorTimes.DOORS * 2));	// 2 floors to pickup, 2 floors to drop off
		expectedValues.get(elevatorID).add(ElevatorTimes.MOVING + ElevatorTimes.STOP + (ElevatorTimes.DOORS * 2) + ElevatorTimes.MOVING + (1 * ElevatorTimes.MOVING_MAX) + ElevatorTimes.STOP + (ElevatorTimes.DOORS * 2));	// 2 floors to pickup, 3 floors to drop off
		expectedValues.get(elevatorID).add(ElevatorTimes.MOVING + (1 * ElevatorTimes.MOVING_MAX) + ElevatorTimes.STOP + (ElevatorTimes.DOORS * 2) + ElevatorTimes.STOP   + (ElevatorTimes.DOORS * 2));	// 3 floors to pickup, 1 floors to drop off
		expectedValues.get(elevatorID).add(ElevatorTimes.MOVING + (1 * ElevatorTimes.MOVING_MAX) + ElevatorTimes.STOP + (ElevatorTimes.DOORS * 2) + ElevatorTimes.MOVING + ElevatorTimes.STOP + (ElevatorTimes.DOORS * 2));	// 3 floors to pickup, 2 floors to drop off
		expectedValues.get(elevatorID).add(ElevatorTimes.MOVING + (1 * ElevatorTimes.MOVING_MAX) + ElevatorTimes.STOP + (ElevatorTimes.DOORS * 2) + ElevatorTimes.MOVING + (1 * ElevatorTimes.MOVING_MAX) + ElevatorTimes.STOP + (ElevatorTimes.DOORS * 2));	// 3 floors to pickup, 3 floors to drop off
		expectedValues.get(elevatorID).add(ElevatorTimes.MOVING + (10 * ElevatorTimes.MOVING_MAX)  + ElevatorTimes.STOP + (ElevatorTimes.DOORS * 2) + ElevatorTimes.MOVING + (4 * ElevatorTimes.MOVING_MAX)  + ElevatorTimes.STOP + (ElevatorTimes.DOORS * 2));	// 12 floors to pickup, 6 floors to drop off
		expectedValues.get(elevatorID).add(ElevatorTimes.MOVING + (16 * ElevatorTimes.MOVING_MAX) + ElevatorTimes.STOP + (ElevatorTimes.DOORS * 2) + ElevatorTimes.MOVING + (62 * ElevatorTimes.MOVING_MAX) + ElevatorTimes.STOP + (ElevatorTimes.DOORS * 2));	// 18 floors to pickup, 64 floors to drop off
		
		elevatorID = 5;
		expectedValues.get(elevatorID).add( (0*ElevatorTimes.MOVING) + (0*ElevatorTimes.MOVING_MAX) + (0*ElevatorTimes.STOP) + (ElevatorTimes.DOORS * 2) + (1*ElevatorTimes.MOVING) + (2*ElevatorTimes.MOVING_MAX) + (1*ElevatorTimes.STOP) + (ElevatorTimes.DOORS * 2) );	// 0 floors to pickup, 4 floors to drop off
		expectedValues.get(elevatorID).add( (1*ElevatorTimes.MOVING) + (0*ElevatorTimes.MOVING_MAX) + (1*ElevatorTimes.STOP) + (ElevatorTimes.DOORS * 2) + (1*ElevatorTimes.MOVING) + (9*ElevatorTimes.MOVING_MAX) + (1*ElevatorTimes.STOP) + (ElevatorTimes.DOORS * 2) );	// 2 floors to pickup, 11 floors to drop off
		expectedValues.get(elevatorID).add( (1*ElevatorTimes.MOVING) + (13*ElevatorTimes.MOVING_MAX)+ (1*ElevatorTimes.STOP) + (ElevatorTimes.DOORS * 2) + (1*ElevatorTimes.MOVING) + (11*ElevatorTimes.MOVING_MAX)+ (1*ElevatorTimes.STOP) + (ElevatorTimes.DOORS * 2) );	// 15 floors to pickup, 13 floors to drop off
		expectedValues.get(elevatorID).add( (0*ElevatorTimes.MOVING) + (0*ElevatorTimes.MOVING_MAX) + (1*ElevatorTimes.STOP) + (ElevatorTimes.DOORS * 2) + (1*ElevatorTimes.MOVING) + (0*ElevatorTimes.MOVING_MAX) + (1*ElevatorTimes.STOP) + (ElevatorTimes.DOORS * 2) );	// 1 floors to pickup, 2 floors to drop off
		expectedValues.get(elevatorID).add( (0*ElevatorTimes.MOVING) + (0*ElevatorTimes.MOVING_MAX) + (1*ElevatorTimes.STOP) + (ElevatorTimes.DOORS * 2) + (1*ElevatorTimes.MOVING) + (5*ElevatorTimes.MOVING_MAX) + (1*ElevatorTimes.STOP) + (ElevatorTimes.DOORS * 2) );	// 1 floors to pickup, 7 floors to drop off
		expectedValues.get(elevatorID).add( (1*ElevatorTimes.MOVING) + (5*ElevatorTimes.MOVING_MAX) + (1*ElevatorTimes.STOP) + (ElevatorTimes.DOORS * 2) + (1*ElevatorTimes.MOVING) + (3*ElevatorTimes.MOVING_MAX) + (1*ElevatorTimes.STOP) + (ElevatorTimes.DOORS * 2) );	// 7 floors to pickup, 5 floors to drop off
		expectedValues.get(elevatorID).add( (1*ElevatorTimes.MOVING) + (1*ElevatorTimes.MOVING_MAX) + (1*ElevatorTimes.STOP) + (ElevatorTimes.DOORS * 2) + (1*ElevatorTimes.MOVING) + (16*ElevatorTimes.MOVING_MAX)+ (1*ElevatorTimes.STOP) + (ElevatorTimes.DOORS * 2) );	// 3 floors to pickup, 18 floors to drop off
		expectedValues.get(elevatorID).add( (0*ElevatorTimes.MOVING) + (0*ElevatorTimes.MOVING_MAX) + (0*ElevatorTimes.STOP) + (ElevatorTimes.DOORS * 2) + (1*ElevatorTimes.MOVING) + (15*ElevatorTimes.MOVING_MAX)+ (1*ElevatorTimes.STOP) + (ElevatorTimes.DOORS * 2) );	// 0 floors to pickup, 17 floors to drop off
		expectedValues.get(elevatorID).add( (1*ElevatorTimes.MOVING) + (2*ElevatorTimes.MOVING_MAX) + (1*ElevatorTimes.STOP) + (ElevatorTimes.DOORS * 2) + (1*ElevatorTimes.MOVING) + (3*ElevatorTimes.MOVING_MAX) + (1*ElevatorTimes.STOP) + (ElevatorTimes.DOORS * 2) );	// 4 floors to pickup, 5 floors to drop off
		expectedValues.get(elevatorID).add( (1*ElevatorTimes.MOVING) + (0*ElevatorTimes.MOVING_MAX) + (1*ElevatorTimes.STOP) + (ElevatorTimes.DOORS * 2) + (0*ElevatorTimes.MOVING) + (0*ElevatorTimes.MOVING_MAX) + (1*ElevatorTimes.STOP) + (ElevatorTimes.DOORS * 2) );	// 2 floors to pickup, 1 floors to drop off
		expectedValues.get(elevatorID).add( (1*ElevatorTimes.MOVING) + (1*ElevatorTimes.MOVING_MAX) + (1*ElevatorTimes.STOP) + (ElevatorTimes.DOORS * 2) + (1*ElevatorTimes.MOVING) + (4*ElevatorTimes.MOVING_MAX) + (1*ElevatorTimes.STOP) + (ElevatorTimes.DOORS * 2) );	// 3 floors to pickup, 6 floors to drop off
		expectedValues.get(elevatorID).add( (0*ElevatorTimes.MOVING) + (0*ElevatorTimes.MOVING_MAX) + (0*ElevatorTimes.STOP) + (ElevatorTimes.DOORS * 2) + (1*ElevatorTimes.MOVING) + (15*ElevatorTimes.MOVING_MAX)+ (1*ElevatorTimes.STOP) + (ElevatorTimes.DOORS * 2) );	// 0 floors to pickup, 17 floors to drop off
		
		
		// Get the results
		HashMap<Integer, ArrayList<Integer>> results = SchedulingAlgorithm.calculateTripTimes(requestsActive, elevatorLatestData);
		
		// Check that the results are what is expected
		for (int i = 1; i <= requestsActive.size(); i++) {	// Elevator ID starts at 1
			for (int j = 0; j < requestsActive.get(i).size(); j++) {
				assertEquals(expectedValues.get(i).get(j), results.get(i).get(j));
			}
		}
	}
	
	
	
	
	@Test
	/*
	 * Tests calculating how long it takes to move between 2 floors (NOTE: Always assume that the elevator is started in a stationary state)
	 */
	public void calculateTimeTakenToMoveTest() {
		
		int[][] input = {
			{0,0},
			{1,1},
			{1,2},
			{3,5},
			{11,9},
			{2,12},
			{10,2},
			{15,6},
			{15,18},
		};
		
		int[] expectedTimes = {
			0,
			0,
			ElevatorTimes.STOP,
			ElevatorTimes.STOP + ElevatorTimes.MOVING,
			ElevatorTimes.STOP + ElevatorTimes.MOVING,
			ElevatorTimes.STOP + ElevatorTimes.MOVING + (8 * ElevatorTimes.MOVING_MAX),
			ElevatorTimes.STOP + ElevatorTimes.MOVING + (6 * ElevatorTimes.MOVING_MAX),
			ElevatorTimes.STOP + ElevatorTimes.MOVING + (7 * ElevatorTimes.MOVING_MAX),
			ElevatorTimes.STOP + ElevatorTimes.MOVING + (1 * ElevatorTimes.MOVING_MAX),
		};
		
		for (int i = 0; i < input.length; i++) {
			assertEquals(expectedTimes[i], SchedulingAlgorithm.calculateTimeTakenToMove(input[i][0], input[i][1]));
		}
	}

}
