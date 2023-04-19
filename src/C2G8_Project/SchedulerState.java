/**
 * 
 */
package C2G8_Project;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.time.StopWatch;


/**
 * @author Jordan
 *
 *
 *                      SCHEDULER STATE MACHINE DIAGRAM
 *                      ═══════════════════════════════
 *                      
 *                      
 *                                     ┌───────────────────────────────────┐
 *                            _________V_________                          │
 *                           │                   │                         │
 *                           │ LISTENING / IDLE  │                         │
 *                           │___________________│                         │
 *                                   │    A                                │
 *                                   │    │                                │
 *                                   │    │                                │
 *                            _______V____│______                          │
 *                           │                   │                         │
 *                 ┌─────────│  PROCESS MESSAGE  │──────────┐              │
 *                 │         │___________________│          │              │
 *                 │                                        │              │
 *                 │                                        │              │
 *        _________V_________                      _________V_________     │
 *       │                   │                    │                   │    │
 *       │   PREPARE FLOOR   │                    │ PREPARE ELEVATOR  │    │
 *       │      MESSAGE      │                    │   STATE MESSAGE   │    │
 *       │___________________│                    │___________________│    │
 *                 │                                        │              │
 *                 │                                        │              │
 *                 │          ___________________           │              │
 *                 │         │                   │          │              │
 *                 └────────>│    SEND MESSAGE   │<─────────┘              │
 *                           │___________________│                         │
 *                                     │                                   │
 *                                     │                                   │
 *                                     │                                   │
 *                            _________V_________                          │
 *                           │                   │                         │
 *                           │    UPDATE VIEW    │                         │
 *                           │___________________│                         │
 *                                     │                                   │
 *                                     └───────────────────────────────────┘
 *
 * 
 * NOTE: The return of Process Message -> Listening was originally meant for launching new thread, but now is a fail-safe and 
 *       occurs only if we switch to ProcessingMessage with no message in the received queues.
 * 
 * Data being sent TO Elevator: "ElevatorSignal"
 * Data being received FROM Elevator: "ElevatorSignal"
 *
 * Data being sent TO Floor: "FloorSignal"
 * Data being received FROM Floor: "FloorRequest"
 * 
 * Data being sent TO View: "ViewData"
 * Data being received FROM View: N/A
 *
 */



/**
 * @author Jordan
 *
 * State Pattern Interface for the Scheduler class
 * 
 * Overview:
 *  The Scheduler is intended to only call doAction(), as well as hold any relevant information that it needs. The state classes below 
 *  are what drive the logic of the scheduler. This interface ensures every state "does an action" that the scheduler needs at that
 *  time, without the Scheduler having to know what that action is.
 */
public interface SchedulerState {
	public SchedulerState doAction(Scheduler context);
}




/**
 * @author Jordan
 *
 * A class intended as a State for the Scheduler (uses the SchedulerState Interface)
 * 
 * Overview:
 *  This class generally listens for changes in the system. Mainly, it repeatedly listens for incoming requests from either a Floor 
 *  or Elevator. When any changes in the system are heard/detected, or any type of message is received, it immediately switches 
 *  states to have the message dealt with, with the exception of starting communication between all systems. This just calls a 
 *  scheduler dispatcher method.
 * 
 * NOTE: Could name this class IdleListening because it handles all the IDLE functions of scheduler, which, aside from listening
 *       for incoming messages (such as for startup communication purposes, or receiving floor/elevator signals) is just listening 
 *       for changes in the system that need to be handled.
 */
class Listening implements SchedulerState {

	@Override
	public SchedulerState doAction(Scheduler context) {
		// TODO Auto-generated method stub
//		context.LOG.info("STATE = '%s'", toString());
		
		// Timer created to track one iteration's worth of work. Because this state is often idling, this is the only way to test one iterations worth of work.
		StopWatch timer = new StopWatch();
		timer = StopWatch.create();
		timer.reset();
		timer.start();
		
		SchedulerState state = null;
		
		// Listen for a message (Continuously)
//		while (context.floorRequestQueue.isEmpty() && context.elevatorSignalQueue.isEmpty()) {}
//		if (!context.floorRequestQueue.isEmpty() || !context.elevatorSignalQueue.isEmpty()) { state = new ProcessMessage(); }
		synchronized(context.elevatorSignalQueue) {
			if (!context.elevatorSignalQueue.isEmpty()) { state = new ProcessMessage(); }
		}
		synchronized(context.floorRequestQueue) {
			if (!context.floorRequestQueue.isEmpty()) { state = new ProcessMessage(); }
		}
		
//		System.out.println(context.requestsActive.toString()); 
//		System.out.println(context.requestsAwaitingElevatorAssignment.toString());
		// Listens for changes in the "Master Sheet" requests to signal a job should be initiated/assigned
		for (int i = 1; i < context.requestsActive.size()+1; i++) {
			if (context.requestsActive.containsKey(i) && context.requestsActive.get(i).isEmpty() && !context.requestsAwaitingElevatorAssignment.isEmpty() && context.elevatorLatestData.get(i).state() != ElevatorStates.DEAD) {
				ElevatorSignal tempSignal = new ElevatorSignal(ElevatorStates.IDLE, context.elevatorLatestData.get(i).id(), context.elevatorLatestData.get(i).location(), context.elevatorLatestData.get(i).newCarBttns(), context.elevatorLatestData.get(i).carBttns(), context.elevatorLatestData.get(i).faultType());
				synchronized(context.elevatorSignalQueue) {
					if (!context.elevatorSignalQueue.contains(tempSignal)) { context.elevatorSignalQueue.add(tempSignal); }
				}
				state = new ProcessMessage();
			}
		}
		
		
		
		// Listening for changes in the system that indicate it's time to start all of the systems's communications
		if (!context.allSystemsStarted && !context.tryingToStartAllSystems && !context.elevatorLatestData.isEmpty()) { context.tryingToStartAllSystems = true; }	// When an elevator is registered, try to tell the FloorSystem to start
		// If something happens and tryingToStartAllSystems didn't flip to false after the systems are started, flip it to false
		if (context.allSystemsStarted && context.tryingToStartAllSystems) { context.tryingToStartAllSystems = false; }
		// If the system hasn't started yet, try to start it
		if (context.tryingToStartAllSystems && !context.allScenariosReceivedFromFloor) {
			if (!context.timer.isStarted()) { context.timer.start(); }
			if (context.timer.getTime(TimeUnit.SECONDS) > 1) {	// Ping the floor every second until it responds (Scheduler sets tryingToStartAllSystems to false and allSystemsStarted to true when it responds)
				context.sendStart();
				context.timer.reset();
			}
		}
		
		// Listening for changes in the systems that indicate it's time to shutdown all of the system's communication
		if (context.allScenariosReceivedFromFloor && context.requestsAwaitingElevatorAssignment.isEmpty() && context.requestsComplete.size() == CONFIG.MAX_PEOPLE) {
			boolean isNotEmpty = false;
			for (ArrayList<RequestData> requestDataList : context.requestsActive.values()) {
				if (!requestDataList.isEmpty()) { isNotEmpty = true; }
			}
			if (!isNotEmpty) {
				context.shutdown();
//				return null;
				state = null;
			}
		}
		
		// Stop the timer and track the timer IFF we are about to transition states.
		timer.stop();
		if (state != null && context.trackPerformance) { context.performanceTrackerHandleExitState("LISTENING_ONE_ITERATION_WITH_WORK", timer.getNanoTime()); }
		
		return state;	// Don't change states.
		
		// When message is received, send to next state
//		return new ProcessMessage();
		
	}
	
	public String toString() { return "LISTENING"; }

}



/**
 * @author Jordan
 *
 * A class intended as a State for the Scheduler (uses the SchedulerState Interface)
 * 
 * Overview:
 *  This class handles processing/sorting received messages, and throwing the scheduler in the correct state to deal with the messages.
 * 
 * NOTE: With a few modifications, can be used as a launching point for a new thread starting from the LISTENING state, if required.
 */
class ProcessMessage implements SchedulerState {

	@Override
	public SchedulerState doAction(Scheduler context) {
		// TODO Auto-generated method stub
//		context.LOG.info("STATE = '%s'", toString());
		
		// If a floor message was received, transition to the appropriate state
		synchronized(context.floorRequestQueue) {
			if (!context.floorRequestQueue.isEmpty()) {
				return new PrepareFloorMessage();
			}
		}
		
		// If an elevator message was received, transition to the appropriate state
		synchronized(context.elevatorSignalQueue) {
			if (!context.elevatorSignalQueue.isEmpty()) {
				return new PrepareElevatorStateMessage();
			}
		}
		
		// In the off chance that something happened and both queues are empty, just go back to LISTENING
		return new Listening();
	}
	
	public String toString() { return "PROCESS_MESSAGE"; }

}



/**
 * @author Jordan
 *
 * A class intended as a State for the Scheduler (uses the SchedulerState Interface)
 * 
 * Overview:
 *  This class handles the logic pertaining to received floor messages about new passenger requests, including adding the request to one
 *  of the Scheduler's "Master Sheet" for requests, and preparing an ACK message for the floor to light up its lamp.
 */
class PrepareFloorMessage implements SchedulerState {

	@Override
	public SchedulerState doAction(Scheduler context) {
		// TODO Auto-generated method stub
//		context.LOG.info("STATE = '%s'", toString());
		
		// Only handle the request if the queue isn't empty (We know it shouldn't be empty if the code got to this state, but just in case)
		RequestData request = null;
		synchronized(context.floorRequestQueue) {
			if (!context.floorRequestQueue.isEmpty()) {
				request = new RequestData(context.floorRequestQueue.remove(), ++context.requestIDCounter);
			}
		}
		
		if (request != null) {
			boolean requestHandled = false;
			
			// Determine which elevator is best suited for the request, if any 
			int selectedElevatorID = SchedulingAlgorithm.assignSingleRequestToAnElevator(request, context.requestsActive, context.elevatorLatestData);
			
			// If an elevator was found, add the request to this elevator's active list
			if (selectedElevatorID != -1) {
				requestHandled = context.assignRequestToAnActiveElevatorAndNotify(request, selectedElevatorID);
				
//				// If the found elevator is currently IDLE, put its last message in the received buffer to have a message prepared for it by another state.
//				if (!context.elevatorLatestData.isEmpty() && context.elevatorLatestData.containsKey(selectedElevatorID) && context.elevatorLatestData.get(selectedElevatorID) != null) {
//					ElevatorData elevator = context.elevatorLatestData.get(selectedElevatorID);
//					if (elevator.state() == ElevatorStates.IDLE) {
//						ElevatorSignal tempSignal = new ElevatorSignal(elevator.state(), elevator.id(), elevator.location(), elevator.newCarBttns(), elevator.carBttns());
//						if (!context.elevatorSignalQueue.contains(tempSignal)) { context.elevatorSignalQueue.add(tempSignal); }
//					}
//				}
			}
			
			// If an elevator wasn't found, then add this request to the "Awaiting Assignment" list.
			if (!requestHandled || selectedElevatorID == -1) { 
				context.LOG.info("STATE = '%s'. Floor request did not find a suitable elevator. Putting in \"Awaiting\" queue. REQUEST = %s", toString(), request.request().toString());
				requestHandled = context.requestsAwaitingElevatorAssignment.add(request);
				context.signalsToSendToView.add(request);	// Sends change in request/passenger data to View/Monitor
			}
//			System.out.println("\n\n\n  ====================================== " + selectedElevatorID+"\n\n");
//			boolean requestHandled = context.requestsAwaitingElevatorAssignment.add(request);
			
			// If the request hasn't been handled properly, then re-add it to the original queue.
			if (!requestHandled) { 
				context.LOG.warn("STATE = '%s'. Floor request not handled. Re-adding to 'received' queue. REQUEST = %s", toString(), request.request().toString()); 
				synchronized(context.floorRequestQueue) {
					context.floorRequestQueue.add(request.request());
				}
			
			// Otherwise, create a Floor Signal message to turn on the appropriate "Button Lamp"
			} else { 
				context.signalsToSendToFloor.add(new FloorSignal(FloorSignals.BTN_LAMP_ON, request.requestFloor(), request.direction()));
				context.setFloorButtonLamp(request.requestFloor(), request.direction(), true);	// Setting "Master Sheet" variables
				if (context.elevatorLatestData != null && context.elevatorLatestData.containsKey(selectedElevatorID) && context.elevatorLatestData.get(selectedElevatorID) != null && context.elevatorLatestData.get(selectedElevatorID).location() == request.requestFloor()) {
					context.signalsToSendToFloor.add(new FloorSignal(FloorSignals.DIR_LAMP_ON, request.requestFloor(), request.direction()));
					context.setFloorDirectionLamp(request.requestFloor(), request.direction(), true);	// Setting "Master Sheet" variables
				}
			}
		}
		
		// Transition to the SEND MESSAGE state (even if no message was created, which is fine).
		return new SendMessage();
	}

	public String toString() { return "PREPARE_FLOOR_MESSAGE"; }
	
}



/**
 * @author Jordan
 *
 * A class intended as a State for the Scheduler (uses the SchedulerState Interface)
 * 
 * Overview:
 *  This class handles the logic pertaining to Elevator State changes, including packaging messages for elevators (and sometimes floors 
 *  that the elevator is currently at), and determining if states need to be changed. It does not send messages.
 * 
 * More details:
 *  If a request comes in from an elevator that has not been seen before by the scheduler, and it's likely that it's a valid elevator,
 *  then this class will register that elevators existence, and move on without changing states. If a request comes in from an elevator
 *  that has been seen before, then this class determines if a state change is required, and if so, creates a message to send to the
 *  elevator. This class also updates the schedulers "Master Sheet" of elevator's latest stored data (including target locations and 
 *  carBttns), as well as the schedulers "Master Sheet" of request data. Namely, moving requests from requesrsAwaitingElevatorAssignment
 *  to requestsActive, updating related variables in the data structure, and moving the request to requestComplete when complete. This
 *  is all done while updating the information and logic required when updating the elevator state.
 * 
 * NOTE: The stored "elevatorLatestData" is the data that has been sent to that specific elevator previously. The signal is what is
 *       being received. The elevator ONLY sends a signal when it has completed its state functions (i.e. The scheduler sent "OPEN_DOOR"
 *       and stored that signal, and the elevator responds with "OPEN_DOOR" once it has completed opening the door). That being said,
 *       the cases below for signal.state() mean "deal with potentially transitioning out of this case/state that was sent", and not 
 *       "deal with transitioning into this case/sate".
 */
class PrepareElevatorStateMessage implements SchedulerState {

	@Override
	public SchedulerState doAction(Scheduler context) {
		// TODO Auto-generated method stub
//		context.LOG.info("STATE = '%s'", toString());
		
		// Only handle the request if the queue isn't empty (We know it shouldn't be empty if the code got to this state, but just in case)
		ElevatorSignal signal = null;
		synchronized(context.elevatorSignalQueue) {
			if (!context.elevatorSignalQueue.isEmpty()) {
				signal = context.elevatorSignalQueue.remove();
			}
		}
		
		if (signal != null) {
			// If the scheduler has never seen this elevator before, check if the signal is VALID (1 <= id <= CONFIG.ELEVATORS; State == "START")...
			if (!context.elevatorLatestData.containsKey(signal.id())) {
				
				// If the elevator signal is VALID: save the elevators information, package an ACK message (change elevator state to "IDLE"), and transition to SEND MESSAGE
				if ( (1 <= signal.id() && signal.id() <= CONFIG.ELEVATORS) && signal.state() == ElevatorStates.START ) {
					ElevatorSignal startSignal = new ElevatorSignal(ElevatorStates.IDLE, signal.id(), signal.location(), signal.newCarBttns(), signal.carBttns(), signal.faultType());
					context.elevatorLatestData.put(startSignal.id(), new ElevatorData(startSignal));
					context.requestsActive.put(startSignal.id(), new ArrayList<RequestData>());
					context.signalsToSendToElevator.add(startSignal);
					return new SendMessage();
					
				// If this elevator is unknown and invalid, log a warning
				} else { context.LOG.warn("STATE = '%s'. Received a signal from an unknown, invalid elevator. SIGNAL = %s", toString(), signal); }
				
				
			// If the scheduler has seen this elevator before, check if the state needs to be changed
			} else if (context.elevatorLatestData.containsKey(signal.id())) {
				ElevatorData elevatorLatestData = context.elevatorLatestData.get(signal.id());
				
				// Variables used to build the new message
				int id = signal.id();
				ElevatorStates state = null;
				int location = signal.location();
				ArrayList<Integer> newCarBttns = signal.newCarBttns();
				Set<Integer> carBttns = signal.carBttns();
				ScenarioFaults fault = null;
				ArrayList<Integer> targetLocations = elevatorLatestData.targetLocations();
				
				// ***********************************************************************************************************************************************************
				// NOTE: After the algorithm is done, revisit this switch statement and see if the targetFloor is at index 0 (it should be) instead of this min/max stuff.
				// NOTE from after Iteration 2: The algorithm does not sort the request in order of which is first. So keep the min/max stuff for now (its more dynamic).
				// ***********************************************************************************************************************************************************
//				List<Object> result = determineNewElevatorStateMessageValues(context, elevatorLatestData, signal, id, state, location, carBttns, targetLocations);
//				
//				// Reading in results for the new variables (and some others)
//				context = (Scheduler) result.get(0);
//				elevatorLatestData = (ElevatorData) result.get(1);
//				signal = (ElevatorSignal) result.get(2);
//				id = (int) result.get(3);
//				state = (ElevatorStates) result.get(4);
//				location = (int) result.get(5);
//				carBttns = (Set<Integer>) result.get(6);
//				targetLocations = (ArrayList<Integer>) result.get(7);
				
				// NOTE: Every case shouls be setting the "state" variable to the next state. state = null means do nothing (don't save data), and send nothing.
				switch (signal.state()) {
				

				
					case START:
						// If the elevator has been put in the "START" state after the scheduler has already marked the elevator as existing, something went wrong. 
						context.LOG.warn("STATE = '%s'. Elevator sent 'START' state, but the scheduler already knows that it has started. Either the message is being spammed, or something went wrong. Setting state to 'IDLE' and re-adding it to system. SIGNAL = %s", toString(), signal);
						
						// If the elevator signal is VALID: change the elevator state to IDLE, and add a spot in requestsActive "MASTER SHEET" list if it doesn't already exist for this elevator
						if ((1 <= signal.id() && signal.id() <= CONFIG.ELEVATORS)) {
							state = ElevatorStates.IDLE;
							if (!context.requestsActive.containsKey(signal.id())) { context.requestsActive.put(signal.id(), new ArrayList<RequestData>()); }
						}
						break;
				
						
						
					case IDLE:
						
						// If "IDLE" and there are no jobs lined up in the "requestsActive" list
						if (context.requestsActive.get(signal.id()).isEmpty() && elevatorLatestData.targetLocations().isEmpty()) {
							// Run Algorithm to see if a job is available, otherwise go to LISTENING
							if (!context.requestsAwaitingElevatorAssignment.isEmpty()) {
								ArrayList<Integer> toBeRemoved = new ArrayList<Integer>();
								
								// Check ALL requests in Awaiting Assignment list
								for (int i = 0; i < context.requestsAwaitingElevatorAssignment.size() ; i++) {
									RequestData request = context.requestsAwaitingElevatorAssignment.get(i);
									boolean requestHandled = false;
									
									// Determine which elevator is best suited for the request, if any 
									int selectedElevatorID = SchedulingAlgorithm.assignSingleRequestToAnElevator(request, context.requestsActive, context.elevatorLatestData);
									
									// If an elevator was found, add the request to this elevator's active list
									if (selectedElevatorID != -1) {
										requestHandled = context.assignRequestToAnActiveElevatorAndNotify(request, selectedElevatorID);
//										System.out.println("\n\n\n  ====================================== " + selectedElevatorID+" in PrepareElevatorStateMessage() IDLE Case\n\n");
									}
									
									// If the request was handled properly, then get ready to remove this element from the list
									if (requestHandled) { toBeRemoved.add(i); }
								}
//								System.out.println("       toBeRemoved size="+toBeRemoved.size()+", "+toBeRemoved.toString());
								
								// Remove any handled requests from the list
								for (int i = toBeRemoved.size()-1; i >= 0 ; i--) {
									context.requestsAwaitingElevatorAssignment.remove( toBeRemoved.get(i).intValue() );
//									System.out.println("   Removing..."+i+", "+toBeRemoved.get(i)+", "+context.requestsAwaitingElevatorAssignment.remove( toBeRemoved.get(i).intValue() ));
								}
							}
//							// If no job was found from the algorithm, return to LISTENING (no response required) (currently handled below with requestsActive being empty)
//							} else { state = null; }	// Returns to LISTENING by not packaging a message and going to SEND_MESSAGE.
						}
						
						
						
						// If the algorithm found a job (or there is a job lined up coming from CLOSE DOOR), transition to either MOVING_UP, MOVING_DOWN, or OPEN_DOOR
						if (!context.requestsActive.get(signal.id()).isEmpty()) {
							
							// If we already had a job, update the targetlocation
							if (!context.requestsActive.get(signal.id()).isEmpty() && elevatorLatestData.targetLocations().isEmpty()) {
								for (int i = 0; i < context.requestsActive.get(signal.id()).size(); i++) {
									RequestData request = context.requestsActive.get(signal.id()).get(i);
									int targetFloor = (!request.pickedUpPassenger()) ? request.requestFloor() : request.targetFloor();
									if (!targetLocations.contains(targetFloor)) { targetLocations.add(targetFloor);	}
									if (!elevatorLatestData.targetLocations().contains(targetFloor)) { elevatorLatestData.targetLocations().add(targetFloor); }
								}
							}
							
							/********** Simple code for Iteration 2 (No algorithm yet) (NOTE: Include check for invalid floors, i.e. -1 and CONFIG.FLOORS +1) **********/
							/********** Note from after iteration 2: The current code works with the algorithm and no changes are needed. Leaving comments here for future debugging (just incase) **********/
							int desiredSourceFloor = -1;
							int desiredSourceFloorIndex = 0;	// Remove first object
							
							// If the passenger hasn't been picked up yet, go to request floor
							if (!context.requestsActive.get(signal.id()).get(desiredSourceFloorIndex).pickedUpPassenger()) {
								desiredSourceFloor = context.requestsActive.get(signal.id()).get(desiredSourceFloorIndex).requestFloor();
							
							// If the passenger has been picked up, but not yet dropped off, go to target floor
							} else if (context.requestsActive.get(signal.id()).get(desiredSourceFloorIndex).pickedUpPassenger() && !context.requestsActive.get(signal.id()).get(desiredSourceFloorIndex).requestComplete()) {
								desiredSourceFloor = context.requestsActive.get(signal.id()).get(desiredSourceFloorIndex).targetFloor();
							}
							
							// Determine the state to transition to (direction to move)
							if (desiredSourceFloor > signal.location()) {
								state = ElevatorStates.MOVING_UP;
							} else if (desiredSourceFloor < signal.location()) {
								state = ElevatorStates.MOVING_DOWN;
							}
							if (desiredSourceFloor == signal.location() || targetLocations.contains(signal.location())){	// desiredSourceFloor == signal.location
								state = ElevatorStates.OPEN_DOORS;
								if (context.requestsActive.get(signal.id()).get(desiredSourceFloorIndex).pickedUpPassenger()) {	// Update floor lamps on pickup
									int requestFloor = context.requestsActive.get(signal.id()).get(desiredSourceFloorIndex).requestFloor();
									Direction direction = context.requestsActive.get(signal.id()).get(desiredSourceFloorIndex).direction();
									context.signalsToSendToFloor.add(new FloorSignal(FloorSignals.BTN_LAMP_OFF, requestFloor, direction));
									context.signalsToSendToFloor.add(new FloorSignal(FloorSignals.DIR_LAMP_ON, requestFloor, direction));
									// Setting "Master Sheet" variables
									context.setFloorButtonLamp(requestFloor, direction, false);
									context.setFloorDirectionLamp(requestFloor, direction, true);
								}
								// Remove this floor from targetLocations, regardless of whether its a pickup or drop-off location (NOTE: This is for the case where IDLE goes straight to OPEN_DOOR. the targetLocation needs to be removed here in that instance)
								while (targetLocations.contains(signal.location())) {	// while instead of if just in case of duplicates.
									targetLocations.remove(targetLocations.indexOf(signal.location()));
								}
							}
							
						// If the algorithm didn't find a job, return to LISTENING (no response required)
//						} else { return new Listening(); }
						} else { state = null; }	// Returns to LISTENING by not packaging a message and going to SEND_MESSAGE.
						
						break;
						
						
						
					case MOVING_UP:
						
						// Incase something happened and the targetLocations becomes empty during a trip, refill it
						if (!context.requestsActive.get(signal.id()).isEmpty() && elevatorLatestData.targetLocations().isEmpty()) {
							for (int i = 0; i < context.requestsActive.get(signal.id()).size(); i++) {
								RequestData request = context.requestsActive.get(signal.id()).get(i);
								int targetFloor = -1;
								if (!request.pickedUpPassenger()) {
									targetFloor = request.requestFloor();
								} else {
									targetFloor = request.targetFloor();
								}
								if (!targetLocations.contains(targetFloor)) {
									targetLocations.add(targetFloor);
								}
								if (!elevatorLatestData.targetLocations().contains(targetFloor)) {
									elevatorLatestData.targetLocations().add(targetFloor);
								}
							}
						}
						
						// If the elevator has a request and a target destination (It's moving up, so it should always have both)
						if (!context.requestsActive.get(signal.id()).isEmpty() && !elevatorLatestData.targetLocations().isEmpty()) {
							
							// Determine direction of requests
							Direction directionOfRequests = null;
							for (int i = 0; i < context.requestsActive.get(signal.id()).size(); i++) {
								// If we have a passenger, go in the appropriate direction to drop them off.
								if (context.requestsActive.get(signal.id()).get(i).pickedUpPassenger()) {
									directionOfRequests = context.requestsActive.get(signal.id()).get(i).direction();
									break;
								}
							}
							// If we do not have a passenger, go in the direction of the first request
							if (directionOfRequests == null) { directionOfRequests = context.requestsActive.get(signal.id()).get(0).direction(); }
							
							
							int targetFloor = CONFIG.FLOORS + 1;
							int targetFloorIndex = -1;
							
							if (directionOfRequests == Direction.UP) {
								// Find the lowest target floor that's **greater** than the elevator.
								targetFloor = CONFIG.FLOORS + 1;
								targetFloorIndex = -1;
								for (int i = 0; i < elevatorLatestData.targetLocations().size(); i++) {
									if (elevatorLatestData.targetLocations().get(i) < targetFloor && elevatorLatestData.targetLocations().get(i) > signal.location()) {
										targetFloor = elevatorLatestData.targetLocations().get(i);
										targetFloorIndex = i;
									}
								}
							} else if (directionOfRequests == Direction.DOWN) {
								// Find the highest target floor that's **greater** than the elevator.
								targetFloor = -1;
								targetFloorIndex = -1;
								for (int i = 0; i < elevatorLatestData.targetLocations().size(); i++) {
									if (elevatorLatestData.targetLocations().get(i) > targetFloor && elevatorLatestData.targetLocations().get(i) > signal.location()) {
										targetFloor = elevatorLatestData.targetLocations().get(i);
										targetFloorIndex = i;
									}
								}
							}
							
							// If the elevator is 1 floor away from the highest floor, transition to STOP
							if (CONFIG.FLOORS == signal.location() + 1){
								state = ElevatorStates.STOP;
								
							// If targetFloor wasn't found (= CONFIG.FLOORS+1), then there is no target floor above the elevator, and we should STOP.
							} else if (targetFloor == CONFIG.FLOORS + 1) {
								state = ElevatorStates.STOP;
							
							// If the elevator is 1 floor below its target floor (minimum value in targetLocations, could be source OR destination floor), transition to STOP
							} else if (targetFloor == signal.location() + 1) {
								state = ElevatorStates.STOP;
							
							// If the elevator still has more than 1 floor to go before reaching its destination, stay in the same state.
							} else { state = ElevatorStates.MOVING_UP; }
						
						// If something goes wrong, log the warning
						} else { 
							context.LOG.warn("STATE = '%s'. Elevator sent 'MOVING_UP' state, but has no target destinations. Maintaining state of 'MOVING_UP'. SIGNAL = %s", toString(), signal);
							state = ElevatorStates.MOVING_UP;	// Could also be safe and STOP.
						}
						
						break;
						
						
						
					case MOVING_DOWN:
						
						// Incase something happened and the targetLocations becomes empty during a trip, refill it
						if (!context.requestsActive.get(signal.id()).isEmpty() && elevatorLatestData.targetLocations().isEmpty()) {
							for (int i = 0; i < context.requestsActive.get(signal.id()).size(); i++) {
								RequestData request = context.requestsActive.get(signal.id()).get(i);
								int targetFloor = -1;
								if (!request.pickedUpPassenger()) {
									targetFloor = request.requestFloor();
								} else {
									targetFloor = request.targetFloor();
								}
								if (!targetLocations.contains(targetFloor)) {
									targetLocations.add(targetFloor);
								}
								if (!elevatorLatestData.targetLocations().contains(targetFloor)) {
									elevatorLatestData.targetLocations().add(targetFloor);
								}
							}
						}
						
						// If the elevator has a request and a target destination (It's moving down, so it should always have both)
						if (!context.requestsActive.get(signal.id()).isEmpty() && !elevatorLatestData.targetLocations().isEmpty()) {
							
							// Determine direction of requests
							Direction directionOfRequests = null;
							for (int i = 0; i < context.requestsActive.get(signal.id()).size(); i++) {
								// If we have a passenger, go in the appropriate direction to drop them off.
								if (context.requestsActive.get(signal.id()).get(i).pickedUpPassenger()) {
									directionOfRequests = context.requestsActive.get(signal.id()).get(i).direction();
									break;
								}
							}
							// If we do not have a passenger, go in the direction of the first request
							if (directionOfRequests == null) { directionOfRequests = context.requestsActive.get(signal.id()).get(0).direction(); }
							
							int targetFloor = -1;
							int targetFloorIndex = -1;
							if (directionOfRequests == Direction.DOWN) {
								// Find the highest target floor that's **less** than the elevator.
								targetFloor = -1;
								targetFloorIndex = -1;
								for (int i = 0; i < elevatorLatestData.targetLocations().size(); i++) {
									if (elevatorLatestData.targetLocations().get(i) > targetFloor && elevatorLatestData.targetLocations().get(i) < signal.location()) {
										targetFloor = elevatorLatestData.targetLocations().get(i);
										targetFloorIndex = i;
									}
								}
							} else if (directionOfRequests == Direction.UP) {
								// Find the lowest target floor that's **less** than the elevator.
								targetFloor = CONFIG.FLOORS + 1;
								targetFloorIndex = -1;
								for (int i = 0; i < elevatorLatestData.targetLocations().size(); i++) {
									if (elevatorLatestData.targetLocations().get(i) < targetFloor && elevatorLatestData.targetLocations().get(i) < signal.location()) {
										targetFloor = elevatorLatestData.targetLocations().get(i);
										targetFloorIndex = i;
									}
								}
							}
							
							// If the elevator is 1 floor away from the lowest floor (Which is assumed to be 1), transition to STOP
							if (1 == signal.location() - 1) {
								state = ElevatorStates.STOP;
							
							// If targetFloor wasn't found (= -1), then there is no target floor under the elevator, and we should STOP.
							}else if (targetFloor == -1) {
								state = ElevatorStates.STOP;
							
							// If the elevator is 1 floor above its target floor (maximum value in targetLocations, could be source OR destination floor), transition to STOP
							} else if (targetFloor == signal.location() - 1) {
								state = ElevatorStates.STOP;
			
							// If the elevator still has more than 1 floor to go before reaching its destination, stay in the same state.
							} else { state = ElevatorStates.MOVING_DOWN; }
						
						// If something goes wrong, log the warning
						} else { 
							context.LOG.warn("STATE = '%s'. Elevator sent 'MOVING_DOWN' state, but has no target destinations. Maintaining state of 'MOVING_DOWN'. SIGNAL = %s", toString(), signal);
							state = ElevatorStates.MOVING_DOWN;	// Could also be safe and STOP.
						}
						
						break;
						
						
						
					case STOP:
						
						// If the elevator has a request and a target destination (It should unless something went wrong)
						if (!context.requestsActive.get(signal.id()).isEmpty() && !elevatorLatestData.targetLocations().isEmpty()) {
							state = ElevatorStates.OPEN_DOORS;
							
							// Create a Floor Message turning off the button lamp and turning on the directional lamp
							Direction direction = null;
							for (int i = 0; i < context.requestsActive.get(signal.id()).size(); i++) {
								if (context.requestsActive.get(signal.id()).get(i).requestFloor() == signal.location()) {
									direction = context.requestsActive.get(signal.id()).get(i).direction();
								}
							}
							if (direction != null) {
								context.signalsToSendToFloor.add(new FloorSignal(FloorSignals.BTN_LAMP_OFF, signal.location(), direction));
								context.signalsToSendToFloor.add(new FloorSignal(FloorSignals.DIR_LAMP_ON, signal.location(), direction));
								// Setting "Master Sheet" variables
								context.setFloorButtonLamp(signal.location(), direction, false);
								context.setFloorDirectionLamp(signal.location(), direction, true);
							}
							
							// Remove this floor from carBttns (back-lit keys in elevator turn off), regardless of whether its a pickup (wouldn't be in list anyways) or drop-off location
							if (carBttns.contains(signal.location())) {
								carBttns.remove(signal.location());
							}
							
							// Remove this floor from targetLocations, regardless of whether its a pickup or drop-off location
							while (targetLocations.contains(signal.location())) {	// while instead of if just in case of duplicates.
								targetLocations.remove(targetLocations.indexOf(signal.location()));
								
							}
							
						} else {
							context.LOG.warn("STATE = '%s'. Elevator sent 'STOP' state, but has no target destinations. Continuing to next state, 'OPEN_DOOR'. SIGNAL = %s", toString(), signal);
							state = ElevatorStates.OPEN_DOORS;	// Could also be safe and IDLE. (Leaving this here for that reason)
						}
						
						break;
						
						
						
					case OPEN_DOORS:
						
						// // If the elevator has an active request (target destination was removed in STOP case and could be empty), send the newCarBttns (that we don't know about) to the elevator
						if (!context.requestsActive.get(signal.id()).isEmpty()) {
							// If an active request's request location (source/pickup floor) is on the current floor, send the "unknown-to-scheduler" new target floors (destination/drop-off floors)
							for (int i = context.requestsActive.get(signal.id()).size()-1; i >= 0; i--) {
								if (context.requestsActive.get(signal.id()).get(i).requestFloor() == signal.location() && !context.requestsActive.get(signal.id()).get(i).pickedUpPassenger()) {
									newCarBttns.add(context.requestsActive.get(signal.id()).get(i).targetFloor());
								}
							}
							
							
						// If the elevator doesn't have an active request (target destination was removed in STOP case and could be empty), something went wrong. However, we will always close doors anyways.
						} else {
							context.LOG.warn("STATE = '%s'. Elevator sent 'OPEN_DOORS' state, but has no requests active. Either way, MUST continue to next state, 'CLOSE_DOORS'. SIGNAL = %s", toString(), signal);
						}
						state = ElevatorStates.CLOSE_DOORS;
						
						break;
						
						
						
					case CLOSE_DOORS:
						
						// If the elevator has requests (which is should since requests are removed in this state transition), update requests in the "MASTER SHEET" (requestsActive -> requestComplete)
						if (!context.requestsActive.get(signal.id()).isEmpty()) {
							
							// Create a Floor Message turning off the direction. The button lamp should be off but sending one again doesn't hurt.
							Direction direction = null;
							for (int i = 0; i < context.requestsActive.get(signal.id()).size(); i++) {
								if (context.requestsActive.get(signal.id()).get(i).requestFloor() == signal.location()) {
									direction = context.requestsActive.get(signal.id()).get(i).direction();
								}
							}
							if (direction != null) {
								context.signalsToSendToFloor.add(new FloorSignal(FloorSignals.BTN_LAMP_OFF, signal.location(), direction));
								context.signalsToSendToFloor.add(new FloorSignal(FloorSignals.DIR_LAMP_OFF, signal.location(), direction));
								// Setting "Master Sheet" variables
								context.setFloorButtonLamp(signal.location(), direction, false);
								context.setFloorDirectionLamp(signal.location(), direction, false);
							}
							
							// Check every active request and update it accordingly
							for (int i = context.requestsActive.get(signal.id()).size()-1; i >= 0; i--) {
								
								// If an active request's request location (source/pickup floor) is on the current floor and it was likely there before door closed, mark the passenger as picked up (typically newCarBttns works, unless target is already in carBttns. Then we check if pickedUpPassenger is false)
								if (context.requestsActive.get(signal.id()).get(i).requestFloor() == signal.location() && (signal.newCarBttns().contains(context.requestsActive.get(signal.id()).get(i).targetFloor()) || !context.requestsActive.get(signal.id()).get(i).pickedUpPassenger()) ) {
									context.requestsActive.get(signal.id()).get(i).setPickedUpPassenger(true);
									context.signalsToSendToView.add( context.requestsActive.get(signal.id()).get(i) );	// Sends change in request/passenger data to View/Monitor
//									carBttns.add(context.requestsActive.get(signal.id()).get(i).targetFloor());
//									targetLocations.add(context.requestsActive.get(signal.id()).get(i).targetFloor());
									System.out.println("     ~~~~~~PICKED UP PASSENGER~~~~~~ "+context.requestsActive.get(signal.id()).get(i).toString());
								}
								
								// If an active request's target location (destination/drop-off floor) is on the current floor and they've been picked up, mark the request as completed
								if (context.requestsActive.get(signal.id()).get(i).targetFloor() == signal.location() && context.requestsActive.get(signal.id()).get(i).pickedUpPassenger()) {
									RequestData request = context.requestsActive.get(signal.id()).remove(i);
									request.setRequestComplete(true);
									int index = targetLocations.indexOf(request.targetFloor());
									if (index != -1) { targetLocations.remove(index); };
									context.requestsComplete.add(request);
									context.signalsToSendToView.add(request);	// Sends change in request/passenger data to View/Monitor
									System.out.println("     ~~~~~~DROPPED OFF PASSENGER~~~~~~ "+request.toString());
								}
								
							}
							
							// Using the "new" target/destination/drop-off floors received from the elevator, update where the elevator is going (carBttns & targetLocations) 
							if (!signal.newCarBttns().isEmpty()) {
								carBttns.addAll(signal.newCarBttns());
								for (int targetLocation: signal.newCarBttns()) {
									if (!targetLocations.contains(targetLocation)) { targetLocations.add(targetLocation); }
								}
							}
							
							// Clean the "new" received target locations (it was handled above, and it no longer useful to be passed around)
							newCarBttns = new ArrayList<Integer>();
							
							
							
						} else {
							context.LOG.warn("STATE = '%s'. Elevator sent 'CLOSE_DOORS' state, but has no requests active. Continuing to next state, 'IDLE'. SIGNAL = %s", toString(), signal);
//							context.signalsToSendToFloor.add(new FloorSignal(FloorSignals.BTN_LAMP_OFF, signal.location(), null));
//							context.signalsToSendToFloor.add(new FloorSignal(FloorSignals.DIR_LAMP_OFF, signal.location(), null));
//							// Setting "Master Sheet" variables
//							context.setFloorButtonLamp(signal.location(), direction, false);
//							context.setFloorDirectionLamp(signal.location(), direction, true);
						}
						
						state = ElevatorStates.IDLE;
						
						break;
					
						
						
					
					case ERROR_DOOR_STUCK:
						
						context.LOG.warn("STATE = '%s'. Elevator sent 'ERROR_DOOR_STUCK' state. SIGNAL = %s \n   Rebooting Elevator %s...", toString(), signal, signal.id());
						
						// Mark the active request that triggered the "fault" as handled, and set the elevator state to "REBOOT" (this is a soft error)
						if (!context.requestsActive.get(signal.id()).isEmpty()) {
							
							// Find active requests for this elevator with an DOOR_STUCK fault, that is likely the cause of the fault, and mark as "handled"
							for (int i = 0; i < context.requestsActive.get(signal.id()).size(); i++) {
								
								RequestData request = context.requestsActive.get(signal.id()).get(i);
								if (request.fault() != null && request.pickedUpPassenger() && request.faultType() == ScenarioFaults.DOOR_STUCK && !request.isFaultHandled()) {
									context.requestsActive.get(signal.id()).get(i).fault().setIsFaultHandled(true);
								}
							}
						} else {
							context.LOG.warn("STATE = '%s'. Elevator sent 'ERROR_DOOR_STUCK' state, but has no requests active. Continuing to next state, 'REBOOT'. SIGNAL = %s", toString(), signal);
						}
						
						state = ElevatorStates.REBOOT;
						
						break;
						
					
						
					case ERROR_ELEVATOR_STUCK:
						
						context.LOG.error("STATE = '%s'. Elevator sent 'ERROR_ELEVATOR_STUCK' state. SIGNAL = %s \n   Shutting Down Elevator %s...", toString(), signal, signal.id());
						
						// Mark the active request that triggered the "fault" as handled, and set the elevator state to "DEAD" (this is a hard error, the elevator is shutdown)
						if (!context.requestsActive.get(signal.id()).isEmpty()) {
							
							// Find active requests for this elevator with a ELEVATOR_STUCK fault, that is likely the cause of the fault, and mark as "handled"
							for (int i = 0; i < context.requestsActive.get(signal.id()).size(); i++) {
								
								RequestData request = context.requestsActive.get(signal.id()).get(i);
								if (request.fault() != null && request.pickedUpPassenger() && request.faultType() == ScenarioFaults.ELEVATOR_STUCK && !request.isFaultHandled()) {
									context.requestsActive.get(signal.id()).get(i).fault().setIsFaultHandled(true);
								}
							}
						} else {
							context.LOG.warn("STATE = '%s'. Elevator sent 'ERROR_ELEVATOR_STUCK' state, but has no requests active. Continuing to next state, 'DEAD'. SIGNAL = %s", toString(), signal);
						}
						
						state = ElevatorStates.DEAD;
						
						break;
						
						
						
					case REBOOT:
						
						// This case is when the elevator has already rebooted after a soft error. The elevator remembers its state after a reboot, so we're just waiting on confirmation of this.
						context.LOG.info("STATE = '%s'. Elevator %s Successfully Rebooted. Waiting for confirmation that the elevator has continued operations.", toString(), signal.id());
						
						break;
						
					
						
					case DEAD:
						
						// This case is when the elevator has acknowledged that it is no longer in operation (shutdown/dead). Any passengers on the elevator are stuck.
						// Per the shutdown instructions for the scheduler, the requests have to be moved to "requestsCompleted", however their requestComplete boolean will still be false.
						// Any additional requests that were assigned to this elevator that have not been picked up yet will go back into the "Awaiting Request" queue.
						context.LOG.warn("STATE = '%s'. Elevator %s successfully shutdown. Elevator will remain shutdown until maintenance team arrives. o7 passengers.", toString(), signal.id());
						
						if (!context.requestsActive.get(signal.id()).isEmpty()) {
							// Find active requests for this elevator with a ELEVATOR_STUCK fault, that is likely the cause of the fault, and mark as "handled"
							for (int i = context.requestsActive.get(signal.id()).size()-1; i >= 0; i--) {
								RequestData request = context.requestsActive.get(signal.id()).remove(i);
								
								// Move stuck passenger requests to "completed" (or archived) since we can't do anything about them
								if (request.pickedUpPassenger()) {
									context.requestsComplete.add(request);
									context.signalsToSendToView.add(request);	// Sends change in request/passenger data to View/Monitor
									
								// Move assigned passenger requests that have yet to pick up the passenger to the awaiting queue to be reassigned to another elevator.
								} else {
									/******************************* NOTE: Can run the algorithm here as well if we want *******************************/
									request.setElevatorID(-1);
									context.requestsAwaitingElevatorAssignment.add(request);
									context.signalsToSendToView.add(request);	// Sends change in request/passenger data to View/Monitor
								}
							}
						}
						
						break;
						
						
						
						
					default:
						context.LOG.warn("STATE = '%s'. Received Invalid ElevatorStates in signal. SIGNAL = %s", toString(), signal);
						break;						
				}
				
				
				
				// Determining if a fault should be sent or not.
				ArrayList<Integer> indices = new ArrayList<Integer>();
				if (!context.requestsActive.get(signal.id()).isEmpty()) {
					
					// Find active requests for this elevator with a fault, and save the index
					for (int i = 0; i < context.requestsActive.get(signal.id()).size(); i++) {
						if (context.requestsActive.get(signal.id()).get(i).fault() != null && context.requestsActive.get(signal.id()).get(i).faultType() != null) {
							indices.add(i);
						}
					}
					
					// Handle the faults found (prepare to send it if necessary)
					for (int i = 0; i < indices.size(); i++) {
						RequestData request = context.requestsActive.get(signal.id()).get(indices.get(i));
						
						// Only check for faults once the passenger is picked up, since we know this is when the request is truely ACTIVE.
						if (request.fault() != null && request.pickedUpPassenger()) {
							
							// ELEVATOR_STUCK only gets sent if the elevator is in one of the MOVING states (skips CLOSE_DOOR and IDLE, sent after trying to move) or STOP (because of how we handle moving 1 floor). 
							if (request.faultType() == ScenarioFaults.ELEVATOR_STUCK && !request.isFaultHandled() && (state == ElevatorStates.MOVING_UP || state == ElevatorStates.MOVING_DOWN || state == ElevatorStates.STOP) ) {
								fault = request.faultType();
								
							// DOOR_STUCK only gets sent if the elevator is in one of the DOOR states. (pickedUpPassenger gets set to true after the doors close, so likely doors won't open...)
							} else if (request.faultType() == ScenarioFaults.DOOR_STUCK && !request.isFaultHandled() && (state == ElevatorStates.CLOSE_DOORS || state == ElevatorStates.OPEN_DOORS) ) {
								fault = request.faultType();
							}
						}
					}
				}
				
				
				
				// If a state was determined, then package up a message to send to the elevator, and save the message as the "latest" elevator data.
				if (state != null) {
					ElevatorSignal signalToSendToElevator = new ElevatorSignal(state, id, location, newCarBttns, carBttns, fault);
					context.signalsToSendToElevator.add(signalToSendToElevator);
					context.elevatorLatestData.put(id, new ElevatorData(signalToSendToElevator, targetLocations));
				}
				
				
				// Printing the current state of the scheduler, just for visual sake.
				System.out.println("     ~~~~~~ELEVATOR LATEST DATA~~~~~~ ");
				for (int i = 0; i < context.elevatorLatestData.size(); i++) {
					if (context.elevatorLatestData != null && context.elevatorLatestData.get(i+1) != null) { 
						System.out.println("  "+context.elevatorLatestData.get(i+1).toString()); 
					}
				}
				
				System.out.println("     ~~~~~~CURRENT AWAITING REQUEST LIST~~~~~~ ");
				for (int i = 0; i < context.requestsAwaitingElevatorAssignment.size(); i++) {
					System.out.println("  "+context.requestsAwaitingElevatorAssignment.get(i).toString()+"");
					if (i >= 4) { System.out.println("  ... # Total Entries = "+context.requestsAwaitingElevatorAssignment.size()); break; }	// Print no more than 5
				}
				
				System.out.println("     ~~~~~~CURRENT ACTIVE REQUEST LIST~~~~~~ ");
				if (!context.requestsActive.isEmpty()) {
					for (int i = 1; i <= context.requestsActive.size(); i++) {
						if (context.requestsActive.containsKey(i) && !context.requestsActive.get(i).isEmpty()) {
							System.out.println("   Assigned to Elevator "+i+":");
							for (RequestData requestData: context.requestsActive.get(i)) {
								System.out.println("  "+requestData.toString());
							}
						}
					}
				}
				
				System.out.println("     ~~~~~~CURRENT COMPLETE REQUEST LIST~~~~~~ ");
				for (int i = context.requestsComplete.size()-1; i >= 0; i--) {
					System.out.println("  "+context.requestsComplete.get(i).toString()+"");
					if (i <= context.requestsComplete.size()-5) { System.out.println("  ... # Total Entries = "+context.requestsComplete.size()); break; }	// Print no more than 5
				}
				
				// System.out.println("     ~~~~~~FLOOR BUTTON LAMP LIST~~~~~~ ");
				// if (!context.floorButtonLamp.isEmpty()) {
				// 	for (int i = 1; i <= context.floorButtonLamp.size(); i++) {
				// 		if (context.floorButtonLamp.containsKey(i) && !context.floorButtonLamp.get(i).isEmpty()) {
				// 			HashMap<Direction, Boolean> floorData = context.floorButtonLamp.get(i);
							
				// 			for (Direction key : floorData.keySet()) {
				// 				System.out.println("  floor="+i+", key="+key+", value="+floorData.get(key));
				// 			}
				// 		}
				// 	}
				// }
				
				// System.out.println("     ~~~~~~FLOOR DIRECTION LAMP LIST~~~~~~ ");
				// if (!context.floorDirectionLamp.isEmpty()) {
				// 	for (int i = 1; i <= context.floorDirectionLamp.size(); i++) {
				// 		if (context.floorDirectionLamp.containsKey(i) && !context.floorDirectionLamp.get(i).isEmpty()) {
				// 			HashMap<Direction, Boolean> floorData = context.floorDirectionLamp.get(i);
							
				// 			for (Direction key : floorData.keySet()) {
				// 				System.out.println("  floor="+i+", key="+key+", value="+floorData.get(key));
				// 			}
				// 		}
				// 	}
				// }
			}
		}
		
		// Transition to the SEND MESSAGE state (even if no message was created, which is fine).
		return new SendMessage();
	}
	
	

	public String toString() { return "PREPARE_ELEVATOR_STATE_MESSAGE"; }
	
}



/**
 * @author Jordan
 *
 * A class intended as a State for the Scheduler (uses the SchedulerState Interface)
 * 
 * Overview:
 *  This class handles sending all the messages from the Scheduler to either a specific Floor or Elevator. It does not create the
 *  messages; only sending what has been created for it.
 */
class SendMessage implements SchedulerState {

	@Override
	public SchedulerState doAction(Scheduler context) {
		// TODO Auto-generated method stub
//		context.LOG.info("STATE = '%s'", toString());

		// Sends prepared messages to the Floor
		while (!context.signalsToSendToFloor.isEmpty()) {
			context.sendFloorSignal(context.signalsToSendToFloor.remove());
		}
		
		// Sends prepared messages to the Elevator
		while (!context.signalsToSendToElevator.isEmpty()) {
//			context.sendViewElevatorSignal(context.signalsToSendToElevator.peek());
			context.sendElevatorSignal(context.signalsToSendToElevator.remove());
		}
		
//		// Sends prepared messages to the View/Monitor (only the updates variables)
//		while (!context.signalsToSendToView.isEmpty()) {
//			context.sendViewRequestData(context.signalsToSendToView.remove(0));
//		}
		
//		System.out.println("\n");	// Just here to separate sent messages
		
//		// Transition back to LISTENING
//		return new Listening();
		
		// Transition to UPDATE_VIEW
		return new UpdateView();
	}
	
	public String toString() { return "SEND_MESSAGE"; }

}



/**
 * @author Jordan
 *
 * A class intended as a State for the Scheduler (uses the SchedulerState Interface)
 * 
 * Overview:
 *  This class handles updating the data structures meant for the view/monitor and sending it off the the view/monitor. If there
 *  is no view/monitor listening for a message, the dispatcher will just not send anything, so nothing needs to change in this state.
 */
class UpdateView implements SchedulerState {

	@Override
	public SchedulerState doAction(Scheduler context) {
		// TODO Auto-generated method stub
//		context.LOG.info("STATE = '%s'", toString());
		
		// Change the data type of some "Master Sheet" variables to make it able to be sent via the dispatcher
		// Converting the requestAwaitingElevatorAssignment list to something suitable to send to the view/monitor
		ArrayList<ViewRequestData> viewRequestsAwaitingElevatorAssignment = new ArrayList<ViewRequestData>();
		if (context.requestsAwaitingElevatorAssignment != null && !context.requestsAwaitingElevatorAssignment.isEmpty()) {
			for (RequestData data : context.requestsAwaitingElevatorAssignment) {
				viewRequestsAwaitingElevatorAssignment.add(new ViewRequestData(data.requestID(), data.request(), data.elevatorID(), data.pickedUpPassenger(), data.requestComplete()));
			}
		}
		
		// Converting the requestActive list to something suitable to send to the view/monitor
		HashMap<Integer, ArrayList<ViewRequestData>> viewRequestsActive = new HashMap<Integer, ArrayList<ViewRequestData>>();
		if (context.requestsActive != null && !context.requestsActive.isEmpty()) {
			for (int elevatorID = 1; elevatorID <= context.requestsActive.size(); elevatorID++) {
				ArrayList<ViewRequestData> list = new ArrayList<ViewRequestData>();
				if (context.requestsActive.get(elevatorID) != null && !context.requestsActive.get(elevatorID).isEmpty()) {
					for (RequestData data : context.requestsActive.get(elevatorID)) {
						list.add(new ViewRequestData(data.requestID(), data.request(), data.elevatorID(), data.pickedUpPassenger(), data.requestComplete()));
					}
				}
				viewRequestsActive.put(elevatorID, list);
			}
		}
		
		// Converting the requestComplete list to something suitable to send to the view/monitor
		ArrayList<ViewRequestData> viewRequestsComplete = new ArrayList<ViewRequestData>();
		if (context.requestsComplete != null && !context.requestsComplete.isEmpty()) {
			for (RequestData data : context.requestsComplete) {
				viewRequestsAwaitingElevatorAssignment.add(new ViewRequestData(data.requestID(), data.request(), data.elevatorID(), data.pickedUpPassenger(), data.requestComplete()));
			}
		}
		
		// Converting the elevatorLatestData list to something suitable to send to the view/monitor
		HashMap<Integer, ViewElevatorData> viewElevatorLatestData = new HashMap<Integer, ViewElevatorData>();
		if (context.elevatorLatestData != null && !context.elevatorLatestData.isEmpty()) {
			for (int elevatorID : context.elevatorLatestData.keySet()) {
				viewElevatorLatestData.put(elevatorID, new ViewElevatorData( context.elevatorLatestData.get(elevatorID).signal(), context.elevatorLatestData.get(elevatorID).targetLocations() ));
			}
		}
		
		
		// Sends the entire scheduler's "Master Sheet" to the view, with updates variables for the view
		context.sendViewMasterSheetData(
				new ViewData(
						viewRequestsAwaitingElevatorAssignment, 
						viewRequestsActive, 
						viewRequestsComplete, 
						viewElevatorLatestData,
						context.floorButtonLamp,
						context.floorDirectionLamp
						)
				);
		
		System.out.println("\n");	// Just here to separate sent messages
		
		// Transition back to LISTENING
		return new Listening();
	}
	
	public String toString() { return "UPDATE_VIEW"; }

}














// Everything below is currently used in testing... doesn't need to be though.


/**
 * @author Jordan
 *
 * Regular state, as bare bones as it gets. 
 * NOTE: Can be statically instantiated as it has no variables.
 */
class ExampleState1 implements SchedulerState {

	@Override
	public SchedulerState doAction(Scheduler context) {
		// TODO Auto-generated method stub
		System.out.println("doAction().  Current State: ExampleState1");
		return this;
	}
	
	public String toString() { return "ExampleState1"; }

}


/**
 * @author Jordan
 *
 * A state that contains variables. These variable's values will
 * not carry over from one state to the next, since a new state
 * is made every time this state is left behind.
 * 
 * The variables are randomized every doAction()
 */
class ExampleState2 implements SchedulerState {
	int x = 0;
	int y = 0;
	int z = 0;

	@Override
	public SchedulerState doAction(Scheduler context) {
		// TODO Auto-generated method stub
		System.out.println("doAction().  Current State: ExampleState2");
		printXYZ();
		randomizeVariablesXYZ();
		printXYZ();
		return this;
	}
	
	public void randomizeVariablesXYZ() {
		Random rand = new Random();
		x = rand.nextInt(100);
		y = rand.nextInt(100);
		z = rand.nextInt(100);
		System.out.println("  ExampleState2: Randomizing variables x, y, z");
	}
	
	public void printXYZ() {
		System.out.println("  ExampleState2: x = "+x+", y = "+y+", z = "+z);
	}
	
	public String toString() { return "ExampleState2"; }
	
}

/**
 * @author Jordan
 *
 * A state that contains variables. These variable's values will
 * not carry over from one state to the next, since a new state
 * is made every time this state is left behind.
 * 
 * The counter is increased by 1 every time doAction() runs.
 */
class ExampleState3 implements SchedulerState {
	int counter = 0;

	@Override
	public SchedulerState doAction(Scheduler context) {
		// TODO Auto-generated method stub
		counter++;
		System.out.println("doAction().  Current State: ExampleState3. Counter = "+counter);
		return this;
	}
	
	public String toString() { return ""+counter; }
	
}