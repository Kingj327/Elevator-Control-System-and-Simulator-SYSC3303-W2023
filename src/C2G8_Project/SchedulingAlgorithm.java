/**
 * 
 */
package C2G8_Project;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;

import org.apache.commons.lang3.tuple.MutablePair;
import org.apache.commons.lang3.tuple.Pair;

/**
 * @author Jordan
 * 
 * Overview:
 *  The brains behind selecting a partiular passenger request for a particular elevator. Most, if not all methods in this class are
 *  static, and meant to be accessible by anyone, as this class is essentially a helper class for the scheduler.
 * 
 * More details:
 *  This class contains plenty of helper functions that are/can be used when deciding what elevator should take a certain passenger
 *  request, from sorting in increasing/decreasing order, calculating trip/movement times, summations of certain list (related to
 *  trip time), determining what direction the elevator SHOULD be moving in (even when it's in a non-moving state), and, the most 
 *  important, assigning a single request to an elevator. Assigning all the requests originally wasn't meant to be handled here, as
 *  that should be handled by the scheduler, whether it wants to assign all the requests or not.
 *  
 * The Algorithm:
 *  The algorithm revolve around 2 main ideas for determing eligible elevators: REQUEST DIRECTION, and whether the request is 
 *  ON THE WAY (between the elevator and the request that the elevator is going to). A few simple rules were created off of these ideas.
 *  
 *  The algorithm, after determing eligible elevators, then determines the time it will take for all eligible elevators to get to
 *  the request floor. The elevator with the shortest time will get the request. NOTE that we only care about how long it takes to pick
 *  up the passenger, because: A) we don't know it's drop off location, just their desired direction of travel, so we can't optimize 
 *  for that (and once we know the drop off location, it's to late to transfer the individual form one elevator to the next); B) Once 
 *  an individual is on the elevator, they're typically not worried about how long it will take to get to their drop off floor (as long
 *  as they are moving in the correct direction to their desired floor, which they are). The impatience comes form waiting for the 
 *  elevator to arrive.
 *  
 *  Note that, in order to ensure that requests are handled somewhat timely, requests are ordered in the requestsAwaitingElevatorAssignment
 *  list in the scheduler, and every assignment starts with the oldest requests, so that these requests dictate the rest of the 
 *  request assignments.
 *  
 *  Also note that if the elevator is in the process of handling an error of some kind (state = ERROR_ELEVATOR_STUCK or ELEVATOR_DOOR_STUCK),
 *  or is doing some action based off of the error (state = REBOOT or DEAD), then the elevator is NOT condisdered to be eligible.
 *  
 *  Rules: 
 *  1) If an elevator has to move in the same direction as the passenger REQUEST'S DIRECTION, then it can pick up any additional
 *     passenger requests ALONG THE WAY that is also going in the same REQUEST DIRECTION.
 *     
 *     	ex: An elevator is moving up from floor 1 to pick up a passenger request at floor 10, whose REQUEST DIRECTION is also UP.
 *          If a passenger request comes in on floor 5 with a REQUEST DIRECTION of UP, then the elevator is eligible to pick up that
 *          request. If the elevator can arrive within the shortest amount of time, then that request is given to the elevator.
 *          
 *          If this were an ordered pickup list, the list would transition as so:
 *          	Original Request:  [10_UP]
 *          	After New Request: [5_UP, 10_UP] 
 *  
 *  2) If an elevator has to move in the opposite direction of the REQUEST'S DIRECTION (in order to pick it up), then it can pick up
 *     additional passenger requests ALONG THE WAY in the same direction as the REQUEST DIRECTION only after the elevator has already 
 *     picked up the furthest (original) request. This is so no individual gets picked up and starts moving in the opposite direction
 *     of their request.
 *     
 *      ex: An elevator is moving up from floor 1 to pick up a passenger request at floor 10, whose REQUEST DIRECTION is also DOWN.
 *          If a passenger request comes in on floor 5 with a REQUEST DIRECTION of DOWN, then the elevator is NOT eligible to pick up
 *          that request until after it has picked up the original request. (This essentially takes us back to the first rule).
 *          
 *          If this were an ordered pickup list, the list would transition as so:
 *          	Original Request:  [10_DOWN]
 *          	After New Request: [10_DOWN,5_DOWN] ... not [5_DOWN,10_DOWN]
 * 
 */
public class SchedulingAlgorithm {
	
	/*
	 * The main portion of the algorithm. Essentially, given a single request and a list of elevator data and active requests, 
	 * it will determine which elevator should deal with the request. It will return the elevatorID of the chosen elevator, 
	 * and if nothing was chose, it will return -1.
	 */
	public static int assignSingleRequestToAnElevator(RequestData request, HashMap<Integer, ArrayList<RequestData>> requestsActive, HashMap<Integer, ElevatorData> elevatorLatestData) {
		int assignedElevatorID = -1;
		
		// If the request list and elevator list aren't the same size, something was added wrong. We won't proceed.
		if (requestsActive.size() != elevatorLatestData.size()) {
			System.out.println("   SCHEDULER ALGORITHM ERROR - assignSingleRequestToAnElevator(): Data passed in was invalid. The requests list is not the same size as the elevator list. Returning -1.");
			return assignedElevatorID;
		}
		
		// Create a list of all the potential elevators to take this request
		ArrayList<Integer> potentialElevators = new ArrayList<Integer>();
		for (int elevatorID : elevatorLatestData.keySet()) {
			potentialElevators.add(elevatorID);
		}
		
		// Check for any eligible elevators (This is when an elevator is going to a request, and this new request is on the way, and the request directions are the same)
		for (int elevatorID = elevatorLatestData.size(); elevatorID >= 1; elevatorID--) {
			// Checking elevators with active requests (If it has no active requests, its eligible, so no need to try to remove it here)
			if (!requestsActive.get(elevatorID).isEmpty() ) {
				
				ElevatorData elevator = elevatorLatestData.get(elevatorID);
				ArrayList<RequestData> sortedRequestsActive = sortSingleActiveRequestsList(requestsActive.get(elevatorID), elevatorLatestData.get(elevatorID));
				RequestData nextActiveRequest = sortedRequestsActive.get(0);
				
				
				boolean onTheWay = false;
				boolean sameDirection = true;	// Default true, changes to false when the direction isn't found
				
				// Variables used to see how the elevator should be moving when the state isnt MOVING_UP/DOWN
				boolean shouldElevatorBeMovingUp   = SchedulingAlgorithm.shouldElevatorBeMovingUp(elevator, sortedRequestsActive, nextActiveRequest);
				boolean shouldElevatorBeMovingDown = SchedulingAlgorithm.shouldElevatorBeMovingDown(elevator, sortedRequestsActive, nextActiveRequest);
				
				// If the elevator is ineligible due to being in the process of handling errors (either rebooting or dead/shutdown, remove it from the list of potential elevators 
				// (because we don't know what will happen). NOTE: settin geither one of the onTheWay or sameDirection variables "false" will make the elevator ineligible.
				if (isElevatorIneligibleDueToErrorHandling(elevator.state()) || elevator.state() == ElevatorStates.START) {
					onTheWay = false;
					sameDirection = false;
					
				// If the elevator is moving up, find the highest floor its going to.
				} else if (elevator.state() == ElevatorStates.MOVING_UP || shouldElevatorBeMovingUp) {
					
					int highestFloor = -1;
					int highestFloorIndex = -1;
					
					for (int i = 0; i < requestsActive.get(elevatorID).size(); i++) {
						RequestData activeRequest = requestsActive.get(elevatorID).get(i);
						// Handle pre-pickup (use requestFloor)
						if (!activeRequest.pickedUpPassenger() && activeRequest.requestFloor() > highestFloor) {
							highestFloor = activeRequest.requestFloor();
							highestFloorIndex = i;
							
						// Handle post-pickup, pre-drop off (use targetFloor)
						} else if (activeRequest.pickedUpPassenger() && activeRequest.targetFloor() > highestFloor) {
							highestFloor = activeRequest.targetFloor();
							highestFloorIndex = i;
						}
						
						// Figure out if the request is going in the same direction
//						System.out.println("SAME DIRECTION [UP] = "+sameDirection+", "+activeRequest.direction()+" != "+request.direction());
						if (sameDirection && activeRequest.direction() != request.direction()) {
							sameDirection = false;
//							System.out.println("SAME DIRECTION [UP] (INNER) = "+sameDirection+", "+activeRequest.direction()+" != "+request.direction());
						}
					}
					
//					System.out.println("ON THE WAY [UP] = "+elevator.location()+" < "+request.requestFloor()+", "+request.requestFloor()+" <= "+highestFloor);
					// Figure out if the request is on the way
					if (shouldElevatorBeMovingUp && elevator.location() <= request.requestFloor() && request.requestFloor() <= highestFloor) { // when state != MOVING_UP (STOP, OPEN, CLOSE, IDLE)
//						System.out.println("ON THE WAY [UP] (INNER1) = "+elevator.location()+" < "+request.requestFloor()+", "+request.requestFloor()+" <= "+highestFloor);
						onTheWay = true;
					} else if (elevator.location() < request.requestFloor() && request.requestFloor() <= highestFloor) {
//						System.out.println("ON THE WAY [UP] (INNER2) = "+elevator.location()+" < "+request.requestFloor()+", "+request.requestFloor()+" <= "+highestFloor);
						onTheWay = true;
					}
					
					
				// If the elevator is moving down, find the lowest floor its going to.
				} else if (elevator.state() == ElevatorStates.MOVING_DOWN || shouldElevatorBeMovingDown) {
					
					int lowestFloor = 100;
					int lowestFloorIndex = -1;
					
					for (int i = 0; i < requestsActive.get(elevatorID).size(); i++) {
						RequestData activeRequest = requestsActive.get(elevatorID).get(i);
//						System.out.println("THIS IS IT...: "+activeRequest.toString());
						// Handle pre-pickup (use requestFloor)
						if (!activeRequest.pickedUpPassenger() && activeRequest.requestFloor() < lowestFloor) {
							lowestFloor = activeRequest.requestFloor();
							lowestFloorIndex = i;
							
						// Handle post-pickup, pre-drop off (use targetFloor)
						} else if (activeRequest.pickedUpPassenger() && activeRequest.targetFloor() < lowestFloor) {
							lowestFloor = activeRequest.targetFloor();
							lowestFloorIndex = i;
						}
						
						// Figure out if the request is going in the same direction
//						System.out.println("SAME DIRECTION [DOWN] = "+sameDirection+", "+activeRequest.direction()+" != "+request.direction()+", "+requestsActive.get(elevatorID).get(i).toString());
						if (sameDirection && activeRequest.direction() != request.direction()) {
							sameDirection = false;
//							System.out.println("SAME DIRECTION [DOWN] (INNER) = "+sameDirection+", "+activeRequest.direction()+" != "+request.direction());
						}
					}
					
//					System.out.println("ON THE WAY [DOWN] = "+elevator.location()+" > "+request.requestFloor()+", "+request.requestFloor()+" >= "+lowestFloor);
					// Figure out if the request is on the way
					if (shouldElevatorBeMovingDown && elevator.location() >= request.requestFloor() && request.requestFloor() >= lowestFloor) {	// when state != MOVING_DOWN (STOP, OPEN, CLOSE, IDLE)
//						System.out.println("ON THE WAY [DOWN] (INNER1) = "+elevator.location()+" > "+request.requestFloor()+", "+request.requestFloor()+" >= "+lowestFloor);
						onTheWay = true;
					} else if (elevator.location() > request.requestFloor() && request.requestFloor() >= lowestFloor) {
//						System.out.println("ON THE WAY [DOWN] (INNER2) = "+elevator.location()+" > "+request.requestFloor()+", "+request.requestFloor()+" >= "+lowestFloor);
						onTheWay = true;
					}
				}
				
				// If the request is both on the way and going in the same direction as all other active requests for this elevator. It is eligible. If either isn't true, remove it from the potentialElevators list
				if (!onTheWay || !sameDirection) { potentialElevators.remove( potentialElevators.indexOf(elevatorID) ); }
//				System.out.println(String.format("OnTheWay %s sameDirection %s, potentialElevators %s", onTheWay, sameDirection, potentialElevators));
			}
			
			// If the elevator is ineligible due to its state, regardless of what it has in its queue, it is ineligible, so remove it.
			if (isElevatorIneligibleDueToErrorHandling(elevatorLatestData.get(elevatorID).state())) {
				if(potentialElevators.contains(elevatorID)) { 
					potentialElevators.remove( potentialElevators.indexOf(elevatorID) ); 
				}
			}
		}
		

		
		// From elegible elevators, calculate the distance/times to the new request floor (shortest times wins).
		ArrayList<Pair<Integer, Integer>> timeForElevatorToGetToRequest = new ArrayList<Pair<Integer, Integer>>();
		
		for (int potentialElevatorsIndex = 0; potentialElevatorsIndex < potentialElevators.size(); potentialElevatorsIndex++) {
			int elevatorID = potentialElevators.get(potentialElevatorsIndex); 
			
			ElevatorData elevator = elevatorLatestData.get(elevatorID);
			
			// If the elevator has no active requests, calculate the time it takes to get to the request floor directly ad save it
			if (requestsActive.get(elevatorID).isEmpty()) {
				int time = SchedulingAlgorithm.calculateTimeTakenToMove(elevator.location(), request.requestFloor());
				timeForElevatorToGetToRequest.add(new MutablePair<Integer, Integer>(elevatorID, time));
				
			// If the elevator has some active requests, calculate the time it takes to get to the request floor while stopping at the floors between, and save it
			} else {
				ArrayList<RequestData> sortedRequestsActive = sortSingleActiveRequestsList(requestsActive.get(elevatorID), elevator);
				int time = 0;
				int previousLocation = elevator.location();
				int desiredFloor = -1;
				
				// Find the index of the last floor inbetween the elevator and the request floor
				for (int i = 0; i < sortedRequestsActive.size(); i++) {
					RequestData currentRequest = sortedRequestsActive.get(i);
					desiredFloor = (!currentRequest.pickedUpPassenger()) ? currentRequest.requestFloor() : currentRequest.targetFloor();
					
					// If there is a stop between the elevator location and the request floor, calculate that time
					if ( Math.abs(elevator.location() - desiredFloor) < Math.abs(elevator.location() - request.requestFloor()) ) {
						time += SchedulingAlgorithm.calculateTimeTakenToMove(previousLocation, desiredFloor) + SchedulingAlgorithm.calculateTimeTakenToOpenAndCloseDoors();
						previousLocation = desiredFloor;
						
					// Otherwise, calculate the time between the previous location and the request floor
					} else { 
						time += SchedulingAlgorithm.calculateTimeTakenToMove(previousLocation, request.requestFloor());
						break; 
					}
				}
				
				// Save the calculated time
				timeForElevatorToGetToRequest.add(new MutablePair<Integer, Integer>(elevatorID, time));
			}
		}
		
		
		// Sort the list by shortest time to largest time, and take the first entry key to return
		SchedulingAlgorithm.sortCollectionsIncreasingOrder(timeForElevatorToGetToRequest);
		if (!timeForElevatorToGetToRequest.isEmpty()) {
			assignedElevatorID = timeForElevatorToGetToRequest.get(0).getKey();
		} 
		
//		System.out.println(String.format("   ***Assigning request %s to elevator %s. %s", request.requestID(), assignedElevatorID, elevatorLatestData.get((assignedElevatorID != -1) ? assignedElevatorID : 1)));
		return assignedElevatorID;
	}
	
	
	
	/*
	 * Using specific information, determines if the elevator should be moving up even if it's state isn't MOVING_UP. Helper function.
	 */
	public static boolean shouldElevatorBeMovingUp(ElevatorData elevator, ArrayList<RequestData> sortedRequestsActive, RequestData nextActiveRequest) {
		if (elevator.state() != ElevatorStates.MOVING_UP &&
			((!sortedRequestsActive.isEmpty() && !nextActiveRequest.pickedUpPassenger() && nextActiveRequest.requestFloor() >= elevator.location()) ||	// Elevator should be moving up, even if not in MOVING_UP state (Pre-pickup)
			(!sortedRequestsActive.isEmpty() && nextActiveRequest.pickedUpPassenger() && nextActiveRequest.targetFloor() > elevator.location())) ) {	// Elevator should be moving up, even if not in MOVING_UP state (Post-pickup)
			return true;
		}
		return false;
	}
	
	
	
	/*
	 * Using specific information, determines if the elevator should be moving down even if it's state isn't MOVING_DOWN. Helper function.
	 */
	public static boolean shouldElevatorBeMovingDown(ElevatorData elevator, ArrayList<RequestData> sortedRequestsActive, RequestData nextActiveRequest) {
		if (elevator.state() != ElevatorStates.MOVING_DOWN &&
			((!sortedRequestsActive.isEmpty() && !nextActiveRequest.pickedUpPassenger() && nextActiveRequest.requestFloor() <= elevator.location()) ||	// Elevator should be moving down, even if not in MOVING_DOWN state (Pre-pickup)
			(!sortedRequestsActive.isEmpty() && nextActiveRequest.pickedUpPassenger() && nextActiveRequest.targetFloor() < elevator.location()) ) ) {	// Elevator should be moving down, even if not in MOVING_DOWN state (Post-pickup)
			return true;
		}
		return false;
	}
	
	
	
	/*
	 * Check if an elevator is handling an error. If it is, we'll pretend like we don't know what the result will be, and we won't assign 
	 * any requests to that elevator just incase it doesn't recover.
	 * INPUT: ElevatorState
	 * OUTPUT: If handling an error, returns true, otherwise returns false.
	 */
	public static boolean isElevatorIneligibleDueToErrorHandling(ElevatorStates state) {
		if (state == ElevatorStates.DEAD || state == ElevatorStates.REBOOT || 
			state == ElevatorStates.ERROR_ELEVATOR_STUCK || state == ElevatorStates.ERROR_DOOR_STUCK) {
			return true;
		}
		return false;
	}
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
//	public static int assignSingleRequestToAnElevator(RequestData request, HashMap<Integer, ArrayList<RequestData>> requestsActive, HashMap<Integer, ElevatorData> elevatorLatestData) {
//		int assignedElevatorID = -1;
//		
//		// If the request list and elevator list aren't the same size, something was added wrong. We won't proceed.
//		if (requestsActive.size() != elevatorLatestData.size()) {
//			System.out.println("   SCHEDULER ALGORITHM ERROR - assignSingleRequestToAnElevator(): Data passed in was invalid. The requests list is not the same size as the elevator list. Returning -1.");
//			return assignedElevatorID;
//		}
//		
//		// Create a list of all the potential elevators to take this request
//		ArrayList<Integer> potentialElevators = new ArrayList<Integer>();
//		for (int elevatorID : elevatorLatestData.keySet()) {
//			potentialElevators.add(elevatorID);
//		}
//		
//		// Check if the request is on the path of any elevator that has an active request
//		for (int elevatorID = 1; elevatorID <= elevatorLatestData.size(); elevatorID++) {
//			
//			// Checking elevators with active requests
//			if (!requestsActive.get(elevatorID).isEmpty()) {
//				
//				System.out.println(request.toString()+", "+elevatorLatestData.get(elevatorID).location());
//				ElevatorData elevator = elevatorLatestData.get(elevatorID);
//				ArrayList<RequestData> sortedRequestsActive = sortSingleActiveRequestsList(requestsActive.get(elevatorID), elevatorLatestData.get(elevatorID));
//				RequestData nextActiveRequest = sortedRequestsActive.get(0);
//				
//				// If the elevator IS NOT on its way to the floor already (moving up/down and request floor is above/below it, respectively). (NOTE: Could be one if statement, but broke it up for readability)
//				// The request floor is the way/path, AND the elevator is MOVING_UP 
//				if (request.requestFloor() > elevator.location() && elevator.state() == ElevatorStates.MOVING_UP) { System.out.println("c1");/* Do Nothing */ }
//				
//				// The request floor is the way/path, AND the elevator is MOVING_DOWN 
//				else if (request.requestFloor() < elevator.location() && elevator.state() == ElevatorStates.MOVING_DOWN) { System.out.println("c2");/* Do Nothing */ }
//				
//				// The request floor is the way/path, AND the elevator is in some state that isn't MOVING_UP (like STOP, OPEN_DOOR, CLOSE_DOOR, IDLE), but it should be transitioning to MOVING_UP soon
//				else if (request.requestFloor() > elevator.location() && elevator.state() != ElevatorStates.MOVING_UP && !sortedRequestsActive.isEmpty() && 
//						( (!request.pickedUpPassenger() && nextActiveRequest.requestFloor() > elevator.location() && nextActiveRequest.requestFloor() < request.requestFloor()) || 
//						  (request.pickedUpPassenger()  && nextActiveRequest.targetFloor() > elevator.location() && nextActiveRequest.targetFloor() < request.requestFloor()) ) ) { System.out.println("c3");/* Do Nothing */ }
//				
//				// The request floor is the way/path, AND the elevator is in some state that isn't MOVING_DOWN (like STOP, OPEN_DOOR, CLOSE_DOOR, IDLE), but it should be transitioning to MOVING_DOWN soon
//				else if (request.requestFloor() < elevator.location() && elevator.state() != ElevatorStates.MOVING_DOWN && !sortedRequestsActive.isEmpty() && 
//						( (!request.pickedUpPassenger() && nextActiveRequest.requestFloor() < elevator.location() && nextActiveRequest.requestFloor() > request.requestFloor()) || 
//						  (request.pickedUpPassenger()  && nextActiveRequest.targetFloor() < elevator.location() && nextActiveRequest.targetFloor() > request.requestFloor()) ) ) { System.out.println("c4");/* Do Nothing */ }
//				
//				// If the request floor is the elevators location and the state is opening/closing doors, or idle
//				else if (request.requestFloor() == elevator.location() && (elevator.state() == ElevatorStates.OPEN_DOORS || elevator.state() == ElevatorStates.CLOSE_DOORS || elevator.state() == ElevatorStates.IDLE )) { System.out.println("c5");/* Do Nothing */ }
//				
//		//hfghdah		// If the request floor is 1 floor away, 
//				else if (request.requestFloor() == elevator.location() && (elevator.state() == ElevatorStates.OPEN_DOORS || elevator.state() == ElevatorStates.CLOSE_DOORS || elevator.state() == ElevatorStates.IDLE )) { System.out.println("c6");/* Do Nothing */ }
//				
//				// Otherwise, if the request floor is not on the path of the elevator, remove the elevator as a potential elevator for this request
//				else {
//					System.out.println("c7");
//					// Remove the elevator as a potential Elevator
//					if (potentialElevators.contains(elevatorID)) { potentialElevators.remove( potentialElevators.indexOf(elevatorID) ); }
//				}
//				
//			/*********************TODO: CHECK ELEVATORS WITHOUT ACTIVE REQUESTS*********************/
//			} else {
//				System.out.println("c8 "+elevatorID);
//			}
//		}
//		
//		System.out.println("POTENTIAL ELEVATORS: "+potentialElevators.toString());
//		
//		
//		return assignedElevatorID;
//	}
	
	
	
	/*
	 * This method takes a single requestsActive list for a single elevator, and sorts it according to what should be
	 * the order that the elevator will follow when stopping at floors.
	 */
	public static ArrayList<RequestData> sortSingleActiveRequestsList(ArrayList<RequestData> requestsActive, ElevatorData elevator) {
		ArrayList<RequestData> sortedRequests = new ArrayList<RequestData>();
		
		// If the request list is empty or the elevator is null, return null.
		if (requestsActive.isEmpty()) {
			System.out.println("   SCHEDULER ALGORITHM ERROR - sortSingleActiveRequestsList(): requestsActive list is empty. Returning null.");
			return null;
		} else if (elevator == null) {
			System.out.println("   SCHEDULER ALGORITHM ERROR - sortSingleActiveRequestsList(): ElevatorData is null. Returning null.");
			return null;
		}
		
		// For the pair: Pair<Index in requestActive list, next destination floor>. The floor is requestFloor is a passenger isn't picked up, and target floor if they are picked up. 
		ArrayList<Pair<Integer, Integer>> nextDestination = new ArrayList<Pair<Integer, Integer>>();
			
		for (int requestIndex = 0; requestIndex < requestsActive.size(); requestIndex++) {
			RequestData request = requestsActive.get(requestIndex);
			//Pair<Integer, Integer> pair = new MutablePair<>(request.requestID(), -1);
			Pair<Integer, Integer> pair = new MutablePair<>(requestIndex, -1);
			
			// If the request is complete, don't care about it because it's essentially useless (likely in STOP, OPEN_DOOR, or CLOSE_DOOR state).
			if (request.requestComplete()) {
				pair.setValue(-1);
				
			// If the request isn't complete, but a passenger has been picked up, then we want the target floor
			} else if (request.pickedUpPassenger()) {
				pair.setValue(request.targetFloor());
				
			// If the passenger hasn't been picked up, then we want the request floor
			} else {
				pair.setValue(request.requestFloor());
			}
			
			nextDestination.add(pair);
		}
		
//		System.out.println("BEFORE SORTING:\n"+nextDestination.toString());
		
		
		// Sort the list according to the elevators state.
		boolean sorted = false;
		Direction direction = null;
		if (elevator.state() == ElevatorStates.MOVING_UP) {
			sortCollectionsIncreasingOrder(nextDestination);
			sorted = true;
		} else if (elevator.state() == ElevatorStates.MOVING_DOWN) {
			sortCollectionsDecreasingOrder(nextDestination);
			sorted = true;
			
		// Otherwise, if the elevator is in a different state, find a request that doesn't have the passenger picked up, and use that direction (Assuming the direction isn't null).
		} else {
			for (int requestIndex = 0; requestIndex < requestsActive.size(); requestIndex++) {
				if (!requestsActive.get(requestIndex).pickedUpPassenger() && !requestsActive.get(requestIndex).requestComplete() && requestsActive.get(requestIndex).direction() != null) {
					direction = requestsActive.get(requestIndex).direction();
					break;
				}
			}
			
			if (direction == Direction.UP) {
				sortCollectionsIncreasingOrder(nextDestination);
				sorted = true;
			} else if (direction == Direction.DOWN) {
				sortCollectionsDecreasingOrder(nextDestination);
				sorted = true;
			}
//			System.out.println("DIRECTION = "+direction);
		}
		
//		System.out.println("AFTER SORTING:\n"+nextDestination.toString());
		
		
		if (sorted) {
			// If we were able to sort, then we will take all the requests that are currently past/behind (or in the opposite direction) of the elevator, and move them to the end of the list.
			int nFloorsInOppositeDirectionOfElevator = 0;
			if (elevator.state() == ElevatorStates.MOVING_UP || direction == Direction.UP) {
				for (int i = 0; i < nextDestination.size(); i++) {
					if (elevator.location() < nextDestination.get(i).getValue()) {
//						System.out.println(elevator.location() +" < "+ nextDestination.get(i).getValue()+", "+i);
						nFloorsInOppositeDirectionOfElevator = i;
						break;
					}
				}
			} else if (elevator.state() == ElevatorStates.MOVING_DOWN || direction == Direction.DOWN) {
				for (int i = 0; i < nextDestination.size(); i++) {
					if (elevator.location() > nextDestination.get(i).getValue()) {
//						System.out.println(elevator.location() +" > "+ nextDestination.get(i).getValue()+", "+i);
						nFloorsInOppositeDirectionOfElevator = i;
						break;
					}
				}
			}
			
			// Move elements to the end of the list.
			for (int i = nFloorsInOppositeDirectionOfElevator; i > 0; i--) {
//				System.out.println(i-1);
				nextDestination.add( nextDestination.remove(i-1) );
			}
			
//			System.out.println("AFTER SECOND SORTING:\n"+nextDestination.toString());
			
			
			// Add the RequestData to the sorted list
			for (int i = 0; i < nextDestination.size(); i++) { sortedRequests.add(requestsActive.get(nextDestination.get(i).getKey())); }
			
//			System.out.println("FINAL:");
//			for (int i = 0; i < sortedRequests.size(); i++) {
//				System.out.println("  "+sortedRequests.get(i).toString());
//			}
//			System.out.println("");
			return sortedRequests;
		
		}
		
		// If the list wasn't sorted, we'll be returning the same list that was given originally
		return requestsActive;
	}
	
	
	
	/*
	 * Helper for sortSingleActiveRequestsList(). Sorts the collection in increasing order (when state is MOVING_UP)
	 */
	private static void sortCollectionsIncreasingOrder(ArrayList<Pair<Integer, Integer>> nextDestination) {
		Collections.sort(nextDestination, new Comparator<Pair<Integer, Integer>>() {
		    @Override
		    public int compare(Pair<Integer, Integer> p1, Pair<Integer, Integer> p2) {
		        return p1.getValue() - p2.getValue();
		    }
		});
	}
	
	
	
	/*
	 * Helper for sortSingleActiveRequestsList(). Sorts the collection in decreasing order (when state is MOVING_DOWN)
	 */
	private static void sortCollectionsDecreasingOrder(ArrayList<Pair<Integer, Integer>> nextDestination) {
		Collections.sort(nextDestination, new Comparator<Pair<Integer, Integer>>() {
		    @Override
		    public int compare(Pair<Integer, Integer> p1, Pair<Integer, Integer> p2) {
		        return p2.getValue() - p1.getValue();
		    }
		});
	}
	
	
	
	/*
	 * For each elevator, calculates the amount of time each individual active request that is assigned to that elevator will take.
	 * Returns a HashMap in the format HashMap<Integer, ArrayList<Integer>>, or... HashMap<elevatorID, ArrayList<total time remaining per request>>
	 */
	public static HashMap<Integer, ArrayList<Integer>> calculateTripTimes(HashMap<Integer, ArrayList<RequestData>> requestsActive, HashMap<Integer, ElevatorData> elevatorLatestData) {
		HashMap<Integer, ArrayList<Integer>> requestsActiveTimes = new HashMap<Integer, ArrayList<Integer>>();
		
		// If the request list and elevator list aren't the same size, something was added wrong. We won't proceed.
		if (requestsActive.size() != elevatorLatestData.size()) {
			System.out.println("   SCHEDULER ALGORITHM ERROR - calculateTripTimes(): Data passed in was invalid. The requests list is not the same size as the elevator list. Returning null.");
			return null;
		}
		
		// Looping through every elevator in our system
		for (int elevatorID = 1; elevatorID <= requestsActive.size(); elevatorID++) {	// Start at i=1 because the key is the elevatorID, which starts at 1.
			
			ElevatorData elevator = elevatorLatestData.get(elevatorID);
			int updatedLocation = elevator.location();	// The elevator location that will be updated for this "simulation" to determine total movement time.
			requestsActiveTimes.put(elevatorID, new ArrayList<Integer>());
			
			
			// Looping through every request for a given elevator
			for (int requestIndex = 0; requestIndex < requestsActive.get(elevatorID).size(); requestIndex++) {
				
				RequestData request = requestsActive.get(elevatorID).get(requestIndex);
				int time = 0;
				
				// Handling when a passenger hasn't been picked up yet. Calculate time to destination floor, update location, and calculate time taken to open doors.
				if (!request.pickedUpPassenger()) {
					time += calculateTimeTakenToMove(request.requestFloor(),updatedLocation) + calculateTimeTakenToOpenAndCloseDoors();
					updatedLocation = request.requestFloor();
				}
				
				// Handling when a passenger hasn't been dropped off yet. Calculate time to destination floor, update location, and calculate time taken to open doors.
				if (!request.requestComplete()) {
					time += calculateTimeTakenToMove(request.targetFloor(),updatedLocation) + calculateTimeTakenToOpenAndCloseDoors();
					updatedLocation = request.targetFloor();
				}
				
				// Save the time for this particular trip
				requestsActiveTimes.get(elevatorID).add(time);
			}
		}
		return requestsActiveTimes;
	}
	
	
	
	/*
	 * Helper for calculateTripTimes(). Find the travel time to the next destination floor.
	 * 
	 * NOTE: Always assumes that the elevator is stationary (has to accelerate). Thus, may be 
	 * off by (ElevatorTimes.MOVING - ElevatorTimes.MOVING_MAX) seconds (which is 2 seconds currently).
	 */
	public static int calculateTimeTakenToMove(int location, int destination) {
		int time = 0;
		int floorsToMove = Math.abs(destination - location);
		
		// Calculating time when we moving at least 2 floors. This means add acceleration time for 1 floor (MOVING), stop time for another (STOP), and max speed movement time for any other floors (MOVING_MAX)
		if (floorsToMove >= 2) {
			time += ElevatorTimes.MOVING + ElevatorTimes.STOP;
			time += (floorsToMove - 2) * ElevatorTimes.MOVING_MAX;
		
		// If there's only 1 floor to move, then the time is only the stop time (STOP)
		} else if (floorsToMove == 1) {
			time += ElevatorTimes.STOP;
		}
		
		return time;
	}
	
	
	
	/*
	 * Helper for calculateTripTimes(). Find the amount of time it takes to get the doors to open and close.
	 */
	public static int calculateTimeTakenToOpenAndCloseDoors() {
		return 2 * ElevatorTimes.DOORS;
	}
	
	
	
	/*
	 * Sums all the entries in a HashMap, similar to the one returned from calculateTripTimes(). 
	 * Returns a HashMap<Integer, Integer>, or in other words, HashMap<elevatorID, sum of ArrayList<Integer>>.
	 */
	public static HashMap<Integer, Integer> sumOfRequestsHashMap(HashMap<Integer, ArrayList<Integer>> map) {
		HashMap<Integer, Integer> sum = new HashMap<Integer, Integer>();
		for (int elevatorID = 1; elevatorID <= map.size(); elevatorID++) {
			sum.put(elevatorID, sumOfList(map.get(elevatorID)) );
		}
		return sum;
	}
	
	
	
	/*
	 * Sums all the entries in the list.
	 */
	public static int sumOfList(ArrayList<Integer> list) {
		return sumOfList(list, list.size());
	}
	
	
	
	/*
	 * Sums the first "nEntriesToSum" entries in the list.
	 */
	public static int sumOfList(ArrayList<Integer> list, int nEntriesToSum) {
		int sum = 0;
		int nElementsToSum = (nEntriesToSum-1 < list.size() && nEntriesToSum > 1) ? nEntriesToSum-1 : list.size();
		for (int i = 0; i < nElementsToSum; i++) {
			sum += list.get(i);
		}
		return sum;
	}
}
