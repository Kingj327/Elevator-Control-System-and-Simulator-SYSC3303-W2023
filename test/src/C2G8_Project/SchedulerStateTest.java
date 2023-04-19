package C2G8_Project;

import static org.junit.jupiter.api.Assertions.*;

import java.net.SocketException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import C2G8_Project.CONFIG;
import C2G8_Project.ElevatorSignal;
import C2G8_Project.ElevatorStates;
import C2G8_Project.ExampleState1;
import C2G8_Project.ExampleState2;
import C2G8_Project.ExampleState3;
import C2G8_Project.FloorRequest;
import C2G8_Project.FloorSignal;
import C2G8_Project.Listening;
import C2G8_Project.PrepareElevatorStateMessage;
import C2G8_Project.PrepareFloorMessage;
import C2G8_Project.ProcessMessage;
import C2G8_Project.Scheduler;
import C2G8_Project.SchedulerState;
import C2G8_Project.SendMessage;


/**
 * @author Jordan
 *
 * JUnit5 Test Cases for SchedulerState.java (The Scheduler State Machine States)
 * 
 * Overview:
 *  These test cases are intended to test the general functionality of the state machine (can states be set, changed, and can they all
 *  run their intended action based off of one method/command), as well as the logic of the specific state classes implemented for
 *  the scheduler to use (located in SchedulerStates.java). Example classes are used for the general functionality of the state machine,
 *  while the actual implemented classes are used for the logic testing.
 *  
 *  NOTE: The naming convention for each test is as follows: "<State>State(s)_<methodName>Test". Where <State> is the state class (or groups
 *        of classes in the case of Example States), and <methodName> is what the method does. For example, all example states used
 *        to test the functionality of the state machine start with "exampleStates_...", while actual implemented states in the
 *        scheduler (such as "Listening", "SendMessage", "PrepareFloorMessage", etc.) start with their name ("listeningState_...", 
 *        "sendMessageState_...", and "prepareFloorMessageState_...", respectively).
 *        
 *        Anything that doesn't follow that naming convention is a test that follows the flow of these States in the scheduler system.
 *        
 */
class SchedulerStateTest {
	Scheduler scheduler;
	Dispatcher elevatorDispatcher;
	Dispatcher floorDispatcher;
	Thread schedulerThread;
	HashMap<Dispatcher,Thread> threads;
	
	/*
	 * Initialize the scheduler before each test (since this is what we're using to hold our states, as well as to rig the tests).
	 */
	@BeforeEach
	void setUp() throws SocketException {
		threads = new HashMap<Dispatcher,Thread>();
		elevatorDispatcher = new Dispatcher(Destinations.ELEVATOR_SYSTEM);
		threads.put(elevatorDispatcher,new Thread(elevatorDispatcher));
		floorDispatcher = new Dispatcher(Destinations.FLOOR_SYSTEM);
		threads.put(floorDispatcher,new Thread(floorDispatcher));
		threads.forEach( (k,v) -> v.start());
		while(!elevatorDispatcher.isRunning() || !floorDispatcher.isRunning()) {};
		String floorAddress = "127.0.0.1";
		String elevatorAddress = "127.0.0.1";
		scheduler = new Scheduler(floorAddress, floorDispatcher.getPort(), elevatorAddress, elevatorDispatcher.getPort(), Dispatcher.NO_PORT);
//		schedulerThread = new Thread(scheduler);
//		schedulerThread.start();
//		while(!scheduler.isRunning()) {};
	}
	
	
	
	@Test
	/*
	 * This tests the scheduler's ability to change states. setState only accepts types SchedulerStates, so
	 * there's no need to test that, otherwise states are tested using example states that return their name
	 * in a toString() method.
	 */
	void exampleStates_ChangingStateTest() {
		
		// Scheduler Constructor sets state to "null", where as run() sets the state to a desired SchedulerState. This checks that.
		assertNull(scheduler.getState());
		
		// Changing state from "null" to some other state
		scheduler.setState(new ExampleState1());
		SchedulerState exampleState1 = scheduler.getState();
		assertNotNull(exampleState1);
		
		// Proving it with a toString() method that the state changed (since the state is no longer null, we can call this method)
		String expectedString = "ExampleState1";
		assertEquals(expectedString, exampleState1.toString());
		
		// Changing the state again to another example state, and asserting that the two states are not the same
		scheduler.setState(new ExampleState2());
		assertNotSame(exampleState1, scheduler.getState());
		
		// Again, checking that the new state is in fact what we expected with the toString() method.
		String expectedString2 = "ExampleState2";
		assertEquals(expectedString2, scheduler.getState().toString());
		
		// And finally, right back to testing if the scheduler can have a null state (which it can, however this means that it can't call doAction).
		scheduler.setState(null);
		assertNull(scheduler.getState());
	}
	
	
	
	@Test
	/*
	 * This test runs the doAction() method of the SchedulerStates using an Example State that increments a
	 * counter every time doAction() is run, and returns that value when you call its toString() method. This
	 * checks if the doAction runs the expected number of times, and if it resets when changing states (i.e. 
	 * creating "new" states).
	 */
	void exampleStates_DoActionTest() {
		// Setting the state to ExampleState3, which increments a counter (Starting from 0), every time doAction runs. The counter value is returned via toString()
		scheduler.setState(new ExampleState3());
		assertEquals("0", scheduler.getState().toString());
		
		// Running doAction a handful of times, and making sure the counter matched the expected value
		String expectedValue;
		for (int i = 1; i < 10; i++) {
			scheduler.doAction();
			expectedValue = "" + i;
			assertEquals(expectedValue, scheduler.getState().toString());
		}
		
		// Creating/Resetting a new state and trying again (counter should equal 0)
		scheduler.setState(new ExampleState3());
		assertEquals("0", scheduler.getState().toString());
		
		// Incrementing once just for the sake of it
		scheduler.doAction();
		assertEquals("1", scheduler.getState().toString());
		
		// Creating/Resetting a new state and trying again (counter should equal 0)
		scheduler.setState(new ExampleState3());
		
		// Running doAction "nOfCallsToDoAction" times, and checking one final time that the counter matches
		int nOfCallsToDoAction = 52;
		String expectedValue2 = "" + nOfCallsToDoAction;
		for (int i = 0; i < nOfCallsToDoAction; i++) {
			scheduler.doAction();
		}
		assertEquals(expectedValue2, scheduler.getState().toString());
	}
	
	
	
	@Test
	/*
	 * This test just runs through an arbitrary set of states and doAction calls just to see how that
	 * sequence would look. When it's done, 
	 */
	void exampleStates_RunningASequenceTest() {
		
		// Try to make it through all the states without an error. If there's an error, fail the test.
		try {
			
		scheduler.setState(new ExampleState1());
		scheduler.doAction();
		scheduler.doAction();
		
		scheduler.setState(new ExampleState2());
		scheduler.doAction();	// Randomizes variables
		scheduler.doAction();	// Reads the previously randomized variables, and randomizes again
		
		scheduler.setState(new ExampleState1());
		scheduler.doAction();
		scheduler.doAction();
		scheduler.doAction();
		scheduler.doAction();
		
		scheduler.setState(new ExampleState2());
		scheduler.doAction();	// Randomizes variables (new state, different from before, reset to 0).
		scheduler.doAction();	// Reads the previously randomized variables, and randomizes again
		
		} catch (Exception e) {
			fail();
		}
		
		assertTrue(true);
	}
	
	
	
	@Test
	/*
	 * Rigs a received floor message being added to the system and check if the Listening State can detect it.
	 */
	void listeningState_ReceivingFloorMessageTest() {
		// Set state to Listening
		scheduler.setState(new Listening());
		
		// Rig a floor message added to system via the dispatcher
		scheduler.floorRequestQueue.add(new FloorRequest(0, 0, null,null));
		
		// Run an iteration of doAction() for the state
		SchedulerState state = scheduler.getState().doAction(scheduler);
		
		// Check that the received state is what is expected
		SchedulerState expectedState = new ProcessMessage();
		assertEquals(expectedState.toString(), state.toString());
	}
	
	
	
	@Test
	/*
	 * Rigs a received elevator message being added to the system and check if the Listening State can detect it.
	 */
	void listeningState_ReceivingElevatorMessageTest() {
		// Set state to Listening
		scheduler.setState(new Listening());
		
		// Rig an elevator message added to system via the dispatcher
		scheduler.elevatorSignalQueue.add(new ElevatorSignal(null, 0, 0, null, null, null));
		
		// Run an iteration of doAction() for the state
		SchedulerState state = scheduler.getState().doAction(scheduler);
		
		// Check that the received state is what is expected
		SchedulerState expectedState = new ProcessMessage();
		assertEquals(expectedState.toString(), state.toString());
	}
	
	
	
	@Test
	/*
	 * Rigs a state in the scheduler where the Listening state should try to start communication between all systems
	 */
	void listeningState_TryToStartAllSystemsTest() {
		// Set state to Listening
		scheduler.setState(new Listening());
		
		// Rig variables so that the Listening state detects its time to try to start communication between all systems
		scheduler.elevatorLatestData.put(null, null);
		
		// Run an iteration of doAction() for the state
		SchedulerState state = scheduler.getState().doAction(scheduler);
		
		// Check that the received state is what is expected
		SchedulerState expectedState = null;	// Means don't change state
		assertEquals(expectedState, state);
		assertEquals(true, scheduler.tryingToStartAllSystems);
		assertEquals(true, scheduler.timer.isStarted());	// A timer starts after sending a "start" message, so it can be sent again after X time
	}
	
	
	
	@Test
	/*
	 * Rigs a state in the scheduler where the Listening state has started communication between all systems, and should no longer try to start communication.
	 */
	void listeningState_CommunicationHasStartedTest() {
		// Set state to Listening
		scheduler.setState(new Listening());
		
		// Rig variables so that the Listening state detects that the system has already started
		scheduler.elevatorLatestData.put(null, null);
		scheduler.tryingToStartAllSystems = false;
		scheduler.allSystemsStarted = true;
		
		// Run an iteration of doAction() for the state
		SchedulerState state = scheduler.getState().doAction(scheduler);
		
		// Check that the received state is what is expected
		SchedulerState expectedState = null;	// Means don't change state
		assertEquals(expectedState, state);
		assertEquals(false, scheduler.timer.isStarted());	// The timer wouldn't have been started because communication is already ongoing.
	}
	
	
	
	@Test
	/*
	 * Rigs a state in the scheduler where the Listening state receives a message about all scenarios being sent from the floors
	 * NOTE: There is a few second delay when shutting down to allow all dispatch messages to get sent out. This is reflected when
	 *       running this test. Usually its been set between 1-5 seconds of delay to let everything else shut down.
	 */
	void listeningState_ScenariosAreCompleteTest() {
		// Set state to Listening
		scheduler.setState(new Listening());
		
		// Rig variables so that the Listening state detects that all scenarios have been sent (but there are still requests that have not yet been completed)
		scheduler.isRunning = true;
		scheduler.tryingToStartAllSystems = false;
		scheduler.allSystemsStarted = true;
		scheduler.allScenariosReceivedFromFloor = true;
		for (int i = 0; i < CONFIG.MAX_PEOPLE-1; i++) {	// Adding CONFIG.MAX_PEOPLE amount of entries would mean everything is complete, so add 1 less.
			scheduler.requestsComplete.add(null);
		}
		
		// Run an iteration of doAction() for the state
		SchedulerState state = scheduler.getState().doAction(scheduler);
		
		// Check that the received state is what is expected
		SchedulerState expectedState = null;	// Means don't change state
		assertEquals(expectedState, state);
		assertEquals(true, scheduler.isRunning());
		
		// Now, Rig variables so that the Listening state detects that all scenarios have been sent AND all requests have been completed
		scheduler.requestsComplete.add(null);	// Add the last remaining request to complete all requests.
		scheduler.timer.start();	// At some point the timer would have been started for starting communication between systems. This ensures that.
		
		// Run an iteration of doAction() for the state
		SchedulerState state2 = scheduler.getState().doAction(scheduler);
		
		// Check that the received state is what is expected
		SchedulerState expectedState2 = null;	// Means don't change state
		assertEquals(expectedState2, state2);
		assertEquals(false, scheduler.isRunning());	// Shutdown has occurred as the system is done.
	}


	
	@Test
	/*
	 * Rigs a received floor message being added to the system and check if the "Process Message" State can detect it.
	 */
	void processMessageState_ProcessingFloorMessageTest() {
		// Set state to "Process Message"
		scheduler.setState(new ProcessMessage());
		
		// Rig a floor message added to system via the dispatcher
		scheduler.floorRequestQueue.add(new FloorRequest(0, 0, null,null));
		
		// Run an iteration of doAction() for the state
		SchedulerState state = scheduler.getState().doAction(scheduler);
		
		// Check that the received state is what is expected
		SchedulerState expectedState = new PrepareFloorMessage();
		assertEquals(expectedState.toString(), state.toString());
	}
	
	
	
	@Test
	/*
	 * Rigs a received elevator message being added to the system and check if the "Process Message" State can detect it.
	 */
	void processMessageState_ProcessingElevatorMessageTest() {
		// Set state to "Process Message"
		scheduler.setState(new ProcessMessage());
		
		// Rig an elevator message added to system via the dispatcher
		scheduler.elevatorSignalQueue.add(new ElevatorSignal(null, 0, 0, null, null, null));
		
		// Run an iteration of doAction() for the state
		SchedulerState state = scheduler.getState().doAction(scheduler);
		
		// Check that the received state is what is expected
		SchedulerState expectedState = new PrepareElevatorStateMessage();
		assertEquals(expectedState.toString(), state.toString());
	}
	
	
	
	@Test
	/*
	 * Rigs a received floor message being added to the system and check if the "Prepare Floor Message" State can handle it.
	 * Handling it involves removing it from the "received" queue, adding it to the "Awaiting Assignment" queue, and adding
	 * a message to the "Send Floor Signal" queue.
	 */
	void prepareFloorMessageState_HandleFloorMessageTest() {
		// Set state to "Prepare Floor Message"
		scheduler.setState(new PrepareFloorMessage());
		
		// Looping this a few times. The floor request queue should always be empty at the end, and the other queues should equal n
		int n = 10;
		for (int i = 1; i <= n; i++) {
			// Rig a floor message added to system via the dispatcher
			scheduler.floorRequestQueue.add(new FloorRequest(1, 1, null,null));
			
			// Run an iteration of doAction() for the state
			SchedulerState state = scheduler.getState().doAction(scheduler);
			
			// Check that the received state is what is expected
			SchedulerState expectedState = new SendMessage();
			assertEquals(expectedState.toString(), state.toString());
			assertEquals(0, scheduler.floorRequestQueue.size());	// Expect 0 requests to be in the received queue after each iteration
			assertEquals(i, scheduler.requestsAwaitingElevatorAssignment.size());	// Expect "i" request to be in the awaiting request queue
			assertEquals(i, scheduler.signalsToSendToFloor.size());	// Expect "i" request to be in the send queue
			
			// Bonus. every request saved to the scheduler will be given a request ID. this should also be equal to "i".
			if (scheduler.requestsAwaitingElevatorAssignment.size() == i) {
				assertEquals(i, scheduler.requestsAwaitingElevatorAssignment.get(i-1).requestID());
			}
		}
	}

	@Test
	/**
	 * Rigs several elevator messages to be handled by the Scheduler.
	 * Each State requires different handling and so multiple test may
	 * exist per state.
	 */
	void prepareElevatorStateMessage_HandleElevatorMessageTest() {

		// Set state to "Prepare Elevator State Message"
		scheduler.setState(new PrepareElevatorStateMessage());

		System.out.println("\nStart State Test");
		//"START" State Test
		// Rig a Elevator message added to system via the dispatcher
		scheduler.elevatorSignalQueue.add(new ElevatorSignal(ElevatorStates.START, 1,1, null, null, null));
		
		// Run an iteration of doAction() for the state
		scheduler.getState().doAction(scheduler);
		
		//Retrieve ElevatorData stored after doAction()
		ElevatorData data = scheduler.elevatorLatestData.get(1);
		assertEquals(ElevatorStates.IDLE, data.state());
		assertEquals(true, scheduler.elevatorSignalQueue.isEmpty());



		System.out.println("\nIdle State Test 1 (Move UP Output)");
		//"IDLE" State Test 1 (Move UP Output)
		// Rig a Elevator message added to system via the dispatcher
		scheduler.elevatorSignalQueue.add(new ElevatorSignal(ElevatorStates.IDLE, 1,1, null, null, null));
		
		// Rig the elevator to be in the IDLE state
		scheduler.elevatorLatestData.put(1, new ElevatorData(new ElevatorSignal(ElevatorStates.IDLE, 1,1, null, null, null)));
		
		//Prepare a Floor Request
		FloorRequest req = new FloorRequest(2, 4, Direction.UP,null);
		RequestData reqData = new RequestData(req, 123,1);
		scheduler.requestsActive.get(1).add(reqData);

		// Run an iteration of doAction() for the state
		scheduler.getState().doAction(scheduler);
		
		//Retrieve ElevatorData stored after doAction()
		data = scheduler.elevatorLatestData.get(1);
		assertEquals(ElevatorStates.MOVING_UP, data.state()); //Next Occuring State
		assertEquals(true, scheduler.elevatorSignalQueue.isEmpty()); //Proof that elevatorSignal above was processed



		System.out.println("\nIdle State Test 2 (Move Down Output)");
		//"IDLE" State Test 2 (Move Down Output)
		// Rig a Elevator message added to system via the dispatcher
		scheduler.elevatorSignalQueue.add(new ElevatorSignal(ElevatorStates.IDLE, 1,2, null, null, null));
		
		// Rig the elevator to be in the IDLE state
		scheduler.elevatorLatestData.put(1, new ElevatorData(new ElevatorSignal(ElevatorStates.IDLE, 1,1, null, null, null)));
		
		//Prepare a Floor Request
		req = new FloorRequest(1, 4, Direction.UP,null);
		reqData = new RequestData(req, 123,1);
		scheduler.requestsActive.get(1).clear();
		scheduler.requestsActive.get(1).add(reqData);

		// Run an iteration of doAction() for the state
		scheduler.getState().doAction(scheduler);
		
		//Retrieve ElevatorData stored after doAction()
		data = scheduler.elevatorLatestData.get(1);
		assertEquals(ElevatorStates.MOVING_DOWN, data.state());
		assertEquals(true, scheduler.elevatorSignalQueue.isEmpty()); //Proof that elevatorSignal above was processed



		System.out.println("\nIdle State Test 3 (Open Doors Output)");
		//"IDLE" State Test 3 (Open Doors Output)
		// Rig a Elevator message added to system via the dispatcher
		scheduler.elevatorSignalQueue.add(new ElevatorSignal(ElevatorStates.IDLE, 1,2, null, null, null));
		
		// Rig the elevator to be in the IDLE state
		scheduler.elevatorLatestData.put(1, new ElevatorData(new ElevatorSignal(ElevatorStates.IDLE, 1,1, null, null, null)));
		
		//Prepare a Floor Request
		req = new FloorRequest(1, 2, Direction.UP,null);
		reqData = new RequestData(req, 123,1);
		reqData.setPickedUpPassenger(true);
		scheduler.requestsActive.get(1).clear();
		scheduler.requestsActive.get(1).add(reqData);

		// Run an iteration of doAction() for the state
		scheduler.getState().doAction(scheduler);
		
		//Retrieve ElevatorData stored after doAction()
		data = scheduler.elevatorLatestData.get(1);
		assertEquals(ElevatorStates.OPEN_DOORS, data.state());
		assertEquals(true, scheduler.elevatorSignalQueue.isEmpty());



		System.out.println("\n Moving_Up State Test 1 (STOP: Elevator 1 Floor away from highest Floor)");
		//"MOVING_UP" State Test 1 (STOP: Elevator 1 Floor away from highest Floor)
		// Rig a Elevator message added to system via the dispatcher
		scheduler.elevatorSignalQueue.add(new ElevatorSignal(ElevatorStates.MOVING_UP, 1,CONFIG.FLOORS-1, null, null, null));
		ArrayList<Integer> target = new ArrayList<Integer>();
		target.add(CONFIG.FLOORS); //Location Doesn't matter in this case
		ElevatorData tempData = new ElevatorData(scheduler.elevatorSignalQueue.peek(),target);
		scheduler.elevatorLatestData.put(1, tempData);
		
		//Prepare a Floor Request (Not Used for this Case but hashmap can't be empty)
		req = new FloorRequest(target.get(0), 2, Direction.DOWN,null);
		reqData = new RequestData(req, 123,1);
		scheduler.requestsActive.get(1).clear();
		scheduler.requestsActive.get(1).add(reqData);

		// Run an iteration of doAction() for the state
		scheduler.getState().doAction(scheduler);
		
		//Retrieve ElevatorData stored after doAction()
		data = scheduler.elevatorLatestData.get(1);
		assertEquals(ElevatorStates.STOP, data.state());
		assertEquals(true, scheduler.elevatorSignalQueue.isEmpty());



		System.out.println("\n Moving_Up State Test 2 (STOP: No Target Floor Found Above Elevator)");
		//"MOVING_UP" State Test 2 (STOP: No Target Floor Found Above Elevator)
		// Rig a Elevator message added to system via the dispatcher
		scheduler.elevatorSignalQueue.add(new ElevatorSignal(ElevatorStates.MOVING_UP, 1,1, null, null, null));
		target = new ArrayList<Integer>();
		target.add(CONFIG.FLOORS+1); //Set to Config floors + 1 to launch No target floor case
		tempData = new ElevatorData(scheduler.elevatorSignalQueue.peek(),target);
		scheduler.elevatorLatestData.put(1, tempData);
		
		//Prepare a Floor Request (Not Used for this Case but hashmap can't be empty)
		req = new FloorRequest(target.get(0), 2, Direction.DOWN,null);
		reqData = new RequestData(req, 123,1);
		scheduler.requestsActive.get(1).clear();
		scheduler.requestsActive.get(1).add(reqData);

		// Run an iteration of doAction() for the state
		scheduler.getState().doAction(scheduler);
		
		//Retrieve ElevatorData stored after doAction()
		data = scheduler.elevatorLatestData.get(1);
		assertEquals(ElevatorStates.STOP, data.state());
		assertEquals(true, scheduler.elevatorSignalQueue.isEmpty());



		System.out.println("\n Moving_Up State Test 3 (STOP: 1 Floor away from Target Floor)");
		//"MOVING_UP" State Test 3 (STOP: 1 Floor away from Target Floor)
		// Rig a Elevator message added to system via the dispatcher
		scheduler.elevatorSignalQueue.add(new ElevatorSignal(ElevatorStates.MOVING_UP, 1,1, null, null, null));
		target = new ArrayList<Integer>();
		target.add(2); //Current Location + 1
		tempData = new ElevatorData(scheduler.elevatorSignalQueue.peek(),target);
		scheduler.elevatorLatestData.put(1, tempData);
		
		//Prepare a Floor Request (Not Used for this Case but hashmap can't be empty)
		req = new FloorRequest(target.get(0), 2, Direction.DOWN,null);
		reqData = new RequestData(req, 123,1);
		scheduler.requestsActive.get(1).clear();
		scheduler.requestsActive.get(1).add(reqData);

		// Run an iteration of doAction() for the state
		scheduler.getState().doAction(scheduler);
		
		//Retrieve ElevatorData stored after doAction()
		data = scheduler.elevatorLatestData.get(1);
		assertEquals(ElevatorStates.STOP, data.state());
		assertEquals(true, scheduler.elevatorSignalQueue.isEmpty());



		System.out.println("\n Moving_Up State Test 4 (Moving_Up: Elevator more than 1 floor away from target)");
		//"MOVING_UP" State Test 4 (MOVING_UP: Elevator more than 1 floor away from target)
		// Rig a Elevator message added to system via the dispatcher
		scheduler.elevatorSignalQueue.add(new ElevatorSignal(ElevatorStates.MOVING_UP, 1,1, null, null, null));
		target = new ArrayList<Integer>();
		target.add(4); //Target more than 1 floor away from current Location
		tempData = new ElevatorData(scheduler.elevatorSignalQueue.peek(),target);
		scheduler.elevatorLatestData.put(1, tempData);
		
		//Prepare a Floor Request (Not Used for this Case but hashmap can't be empty)
		req = new FloorRequest(target.get(0), 2, Direction.DOWN,null);
		reqData = new RequestData(req, 123,1);
		scheduler.requestsActive.get(1).clear();
		scheduler.requestsActive.get(1).add(reqData);

		// Run an iteration of doAction() for the state
		scheduler.getState().doAction(scheduler);
		
		//Retrieve ElevatorData stored after doAction()
		data = scheduler.elevatorLatestData.get(1);
		assertEquals(ElevatorStates.MOVING_UP, data.state());
		assertEquals(true, scheduler.elevatorSignalQueue.isEmpty());



		System.out.println("\n Moving_Down State Test 1 (STOP: Elevator 1 Floor away from lowest Floor)");
		//"MOVING_DOWN" State Test 1 (STOP: Elevator 1 Floor away from lowest Floor)
		// Rig a Elevator message added to system via the dispatcher
		scheduler.elevatorSignalQueue.add(new ElevatorSignal(ElevatorStates.MOVING_DOWN, 1,2, null, null, null));
		target = new ArrayList<Integer>();
		target.add(1); //Current Location - 1
		tempData = new ElevatorData(scheduler.elevatorSignalQueue.peek(),target);
		scheduler.elevatorLatestData.put(1, tempData);
		
		//Prepare a Floor Request (Not Used for this Case but hashmap can't be empty)
		req = new FloorRequest(target.get(0), 2, Direction.DOWN,null);
		reqData = new RequestData(req, 123,1);
		scheduler.requestsActive.get(1).clear();
		scheduler.requestsActive.get(1).add(reqData);

		// Run an iteration of doAction() for the state
		scheduler.getState().doAction(scheduler);
		
		//Retrieve ElevatorData stored after doAction()
		data = scheduler.elevatorLatestData.get(1);
		assertEquals(ElevatorStates.STOP, data.state());
		assertEquals(true, scheduler.elevatorSignalQueue.isEmpty());

		

		System.out.println("\n Moving_Down State Test 2 (STOP: No Target Floor Found Below Elevator)");
		//"MOVING_DOWN" State Test 2 (STOP: No Target Floor Found Below Elevator)
		// Rig a Elevator message added to system via the dispatcher
		scheduler.elevatorSignalQueue.add(new ElevatorSignal(ElevatorStates.MOVING_DOWN, 1,2, null, null, null));
		target = new ArrayList<Integer>();
		target.add(CONFIG.FLOORS-1); //Set to -1 to launch No target floor case
		tempData = new ElevatorData(scheduler.elevatorSignalQueue.peek(),target);
		scheduler.elevatorLatestData.put(1, tempData);
		
		//Prepare a Floor Request (Not Used for this Case but hashmap can't be empty)
		req = new FloorRequest(target.get(0), 2, Direction.DOWN,null);
		reqData = new RequestData(req, 123,1);
		scheduler.requestsActive.get(1).clear();
		scheduler.requestsActive.get(1).add(reqData);

		// Run an iteration of doAction() for the state
		scheduler.getState().doAction(scheduler);
		
		//Retrieve ElevatorData stored after doAction()
		data = scheduler.elevatorLatestData.get(1);
		assertEquals(ElevatorStates.STOP, data.state());
		assertEquals(true, scheduler.elevatorSignalQueue.isEmpty());



		System.out.println("\n Moving_Down State Test 3 (STOP: 1 Floor away frrom Target Floor)");
		//"MOVING_DOWN" State Test 3 (STOP: 1 Floor away from Target Floor)
		// Rig a Elevator message added to system via the dispatcher
		scheduler.elevatorSignalQueue.add(new ElevatorSignal(ElevatorStates.MOVING_DOWN, 1,5, null, null, null));
		target = new ArrayList<Integer>();
		target.add(4); //Current Location - 1
		tempData = new ElevatorData(scheduler.elevatorSignalQueue.peek(),target);
		scheduler.elevatorLatestData.put(1, tempData);
		
		//Prepare a Floor Request (Not Used for this Case but hashmap can't be empty)
		req = new FloorRequest(target.get(0), 2, Direction.DOWN,null);
		reqData = new RequestData(req, 123,1);
		scheduler.requestsActive.get(1).clear();
		scheduler.requestsActive.get(1).add(reqData);

		// Run an iteration of doAction() for the state
		scheduler.getState().doAction(scheduler);
		
		//Retrieve ElevatorData stored after doAction()
		data = scheduler.elevatorLatestData.get(1);
		assertEquals(ElevatorStates.STOP, data.state());
		assertEquals(true, scheduler.elevatorSignalQueue.isEmpty());



		System.out.println("\n Moving_Down State Test 4 (MOVING_DOWN: Elevator more than 1 floor away from target)");
		//"MOVING_DOWN" State Test 4 (MOVING_DOWN: Elevator more than 1 floor away from target)
		// Rig a Elevator message added to system via the dispatcher
		scheduler.elevatorSignalQueue.add(new ElevatorSignal(ElevatorStates.MOVING_DOWN, 1,5, null, null, null));
		target = new ArrayList<Integer>();
		target.add(2); //Target more than 1 floor away from current Location
		tempData = new ElevatorData(scheduler.elevatorSignalQueue.peek(),target);
		scheduler.elevatorLatestData.put(1, tempData);
		
		//Prepare a Floor Request (Not Used for this Case but hashmap can't be empty)
		req = new FloorRequest(target.get(0), 2, Direction.DOWN,null);
		reqData = new RequestData(req, 123,1);
		scheduler.requestsActive.get(1).clear();
		scheduler.requestsActive.get(1).add(reqData);

		// Run an iteration of doAction() for the state
		scheduler.getState().doAction(scheduler);
		
		//Retrieve ElevatorData stored after doAction()
		data = scheduler.elevatorLatestData.get(1);
		assertEquals(ElevatorStates.MOVING_DOWN, data.state());
		assertEquals(true, scheduler.elevatorSignalQueue.isEmpty());



		System.out.println("\n Stop State Test");
		//"STOP" State Test (Clears the CarBttns/targetLocations for the current floor and switches to OPEN_DOORS state and sets a direction)
		// Clean from previous states.
		scheduler.signalsToSendToFloor.clear();
		//Make the carBttns for the current location
		Set<Integer> carrBttns = new HashSet<Integer>();
		carrBttns.add(5);

		// Rig a Elevator message added to system via the dispatcher
		scheduler.elevatorSignalQueue.add(new ElevatorSignal(ElevatorStates.STOP, 1,5, null, carrBttns, null));

		target = new ArrayList<Integer>();
		target.add(5); //Target equal to current Location
		tempData = new ElevatorData(scheduler.elevatorSignalQueue.peek(),target);
		scheduler.elevatorLatestData.put(1, tempData);
		
		//Prepare a Floor Request
		req = new FloorRequest(target.get(0), 2, Direction.DOWN,null);
		reqData = new RequestData(req, 123,1);
		scheduler.requestsActive.get(1).clear();
		scheduler.requestsActive.get(1).add(reqData);

		// Run an iteration of doAction() for the state
		scheduler.getState().doAction(scheduler);
		
		//Retrieve ElevatorData stored after doAction()
		data = scheduler.elevatorLatestData.get(1);
		assertEquals(ElevatorStates.OPEN_DOORS, data.state()); //Next Occuring state
		assertEquals(2,scheduler.signalsToSendToFloor.size()); //Should be two from making 2 Lamp ON/OFF msgs
		assertEquals(Direction.DOWN,scheduler.signalsToSendToFloor.peek().direction()); //Both signals to Floor should have the same Direction which is Down as indicated in Floor Request above
		assertEquals(0,data.carBttns().size()); //Should be 0 from deleting current Location
		assertEquals(0,data.targetLocations().size()); //Should be 0 from deleting current Location


		
		System.out.println("\n Open_Doors State Test");
		//"OPEN_DOORS" State Test (Send to elevator newCarBttns once passenger enters)
		// Clean from previous states.
		scheduler.signalsToSendToFloor.clear();
		
		//Make a empty newCarBttns array for elevator
		ArrayList<Integer> newCarrBttns = new ArrayList<Integer>();

		// Rig a Elevator message added to system via the dispatcher
		scheduler.elevatorSignalQueue.add(new ElevatorSignal(ElevatorStates.OPEN_DOORS, 1,2, newCarrBttns, null, null));

		tempData = new ElevatorData(scheduler.elevatorSignalQueue.peek());
		scheduler.elevatorLatestData.put(1, tempData);
		
		//Prepare a Floor Request
		req = new FloorRequest(2, 5, Direction.UP,null);
		reqData = new RequestData(req, 123,1);
		scheduler.requestsActive.get(1).clear();
		scheduler.requestsActive.get(1).add(reqData);

		// Run an iteration of doAction() for the state
		scheduler.getState().doAction(scheduler);
		
		//Retrieve ElevatorData stored after doAction()
		data = scheduler.elevatorLatestData.get(1);
		assertEquals(ElevatorStates.CLOSE_DOORS, data.state()); //Next Occuring state
		assertEquals(5, data.newCarBttns().get(0)); //The newCarBttns array should contain the targetFloor from the floor request



		System.out.println("\n Close_Doors State Test 1 (Pick Up Passemger)");
		/* "CLOSE_DOORS" State Test 1 (Pick up a passenger (requestFloor = location), and take new pickup requests from elevator(newCarBttns)
			and put them into carBttns set for elevator for them to be handled. Also send a Lamp off cmd to floor)
		*/	
		//Make a the empty carBttn set and a newCarrBttns array with a single floor request for elevator
		newCarrBttns = new ArrayList<Integer>();
		newCarrBttns.add(5);
		carrBttns = new HashSet<Integer>();

		// Rig a Elevator message added to system via the dispatcher
		scheduler.elevatorSignalQueue.add(new ElevatorSignal(ElevatorStates.CLOSE_DOORS, 1,2, newCarrBttns, carrBttns, null));

		tempData = new ElevatorData(scheduler.elevatorSignalQueue.peek());
		scheduler.elevatorLatestData.put(1, tempData);
		
		//Prepare a Floor Request
		req = new FloorRequest(2, 5, Direction.UP,null);
		reqData = new RequestData(req, 123,1);
		scheduler.requestsActive.get(1).clear();
		scheduler.requestsActive.get(1).add(reqData);

		// Run an iteration of doAction() for the state
		scheduler.getState().doAction(scheduler);
		
		//Retrieve ElevatorData stored after doAction()
		data = scheduler.elevatorLatestData.get(1);
		assertEquals(ElevatorStates.IDLE, data.state()); //Next Occuring state
		assertEquals(Direction.UP,scheduler.signalsToSendToFloor.peek().direction()); //Both signals to Floor should have the same Direction which is UP as indicated in Floor Request above
		assertEquals(2,scheduler.signalsToSendToFloor.size()); //Should be two from making two Lamp OFF msgs
		assertEquals(true, scheduler.requestsActive.get(1).get(0).pickedUpPassenger()); //Should be true since passenger has been picked up
		assertEquals(0, data.newCarBttns().size()); //Should be 0 from scheduler transferring newCarBttns to carBttns set
		assertEquals(true, data.carBttns().contains(5)); //Should contain the floors that were in newCarrBttns up above (8)
		assertEquals(1, data.carBttns().size()); //The size should only be 1 due to only adding one newCarrBttns request



		System.out.println("\n Close_Doors State Test 2 (Drop off Passemger)");
		/* "CLOSE_DOORS" State Test 2 (Drop off passenger (targetFloor = location), and take new pickup requests from elevator(newCarBttns)
			and put them into carBttns set for elevator for them to be handled. Also send a Lamp off cmd to floor)
		*/		
		//Make a the empty carBttn set and a newCarrBttns array with a single floor request for elevator
		newCarrBttns = new ArrayList<Integer>();
		carrBttns = new HashSet<Integer>();

		// Rig a Elevator message added to system via the dispatcher
		scheduler.elevatorSignalQueue.add(new ElevatorSignal(ElevatorStates.CLOSE_DOORS, 1,5, newCarrBttns, carrBttns, null));

		tempData = new ElevatorData(scheduler.elevatorSignalQueue.peek());
		scheduler.elevatorLatestData.put(1, tempData);
		
		//Prepare a Floor Request
		req = new FloorRequest(2, 5, Direction.UP,null);
		reqData = new RequestData(req, 123,1);
		reqData.setPickedUpPassenger(true);
		scheduler.requestsActive.get(1).clear();
		scheduler.requestsActive.get(1).add(reqData);

		// Run an iteration of doAction() for the state
		scheduler.getState().doAction(scheduler);
		
		//Retrieve ElevatorData stored after doAction()
		data = scheduler.elevatorLatestData.get(1);
		assertEquals(ElevatorStates.IDLE, data.state()); //Next Occuring state
		assertEquals(Direction.UP,scheduler.signalsToSendToFloor.peek().direction()); //Both signals to Floor should have the same Direction which is UP as indicated in Floor Request above
		assertEquals(2,scheduler.signalsToSendToFloor.size()); //Should be two from making two Lamp OFF msgs
		assertEquals(1, scheduler.requestsComplete.get(0).elevatorID()); //Elevator ID is 1, if the request was complete, the ID's should match
		assertEquals(0, data.newCarBttns().size()); //Should be 0 from scheduler transferring newCarBttns to carBttns set
		assertEquals(0, data.carBttns().size()); //Should be 0 since newCarrBttns was empty
	}
	
	
	
	@Test
	/**
	 * Rigs situations in which the elevator transition involves the ElevatorStates OPEN_DOORS, or CLOSE_DOORS,
	 * since these are the only states in which a DOOR_STUCK error will occur. Also rigs messages that should
	 * be added to the system from the dispatcher in order to run through the state's doAction() method.
	 */
	void prepareElevatorStateMessage_HandleDoorStuckElevatorFaultMessageTest() {
		// Setup
		scheduler.requestsActive.put(1, new ArrayList<RequestData>());
		scheduler.elevatorLatestData.put(1, null);

		/*** TEST 1: Going from STOP -> OPEN_DOOR without picking up error'd passenger request (NO ERROR should exist) ***/
		// Set state to "Prepare Elevator State Message"
		scheduler.setState(new PrepareElevatorStateMessage());
		
		// Rig the passenger request into the requestsActive list (where we search for requests with faults)
		RequestData request = new RequestData(new FloorRequest(1, 2, Direction.UP, new ElevatorFault(ScenarioFaults.DOOR_STUCK, false)), 1);	// Request coming from floor 1 with DOOR_STUCK error
		scheduler.requestsActive.get(1).add(request);
		
		// Rig the elevator's previous state
		scheduler.elevatorLatestData.put(1, new ElevatorData(new ElevatorSignal(ElevatorStates.STOP, 1,1, new ArrayList<Integer>(), new HashSet<Integer>(), null)));

		// Rig an Elevator message added to system via the dispatcher
		scheduler.elevatorSignalQueue.add(new ElevatorSignal(ElevatorStates.STOP, 1,1, new ArrayList<Integer>(), new HashSet<Integer>(), null));	// Elevator starting at floor 1
		
		// Run an iteration of doAction() for the state
		scheduler.getState().doAction(scheduler);
		
		// Check that the received state is what is expected
		ElevatorData elevator = scheduler.elevatorLatestData.get(1);
		assertEquals(ElevatorStates.OPEN_DOORS, elevator.state());	// Moved to the correct state
		assertEquals(null, elevator.faultType());	// ** What we're looking for: NO ERROR **
		assertEquals(true, scheduler.elevatorSignalQueue.isEmpty());	// Request was handled and removed from queue
		
		
		
		/*** TEST 2: Going from STOP -> OPEN_DOOR WITH the error'd passenger request (ERROR should exist) ***/
		// Set state to "Prepare Elevator State Message"
		scheduler.setState(new PrepareElevatorStateMessage());
		
		// Rig the passenger request into the requestsActive list (where we search for requests with faults)
		request = new RequestData(new FloorRequest(1, 2, Direction.UP, new ElevatorFault(ScenarioFaults.DOOR_STUCK, false)), 1);	// Request coming from floor 1 with DOOR_STUCK error
		request.setPickedUpPassenger(true);		// Only difference between test 1 and 2.
		scheduler.requestsActive.get(1).clear();
		scheduler.requestsActive.get(1).add(request);
		
		// Rig the elevator's previous state
		scheduler.elevatorLatestData.clear();
		scheduler.elevatorLatestData.put(1, new ElevatorData(new ElevatorSignal(ElevatorStates.STOP, 1,1, new ArrayList<Integer>(), new HashSet<Integer>(), null)));

		// Rig an Elevator message added to system via the dispatcher
		scheduler.elevatorSignalQueue.clear();
		scheduler.elevatorSignalQueue.add(new ElevatorSignal(ElevatorStates.STOP, 1,1, new ArrayList<Integer>(), new HashSet<Integer>(), null));	// Elevator starting at floor 1
		
		// Run an iteration of doAction() for the state
		scheduler.getState().doAction(scheduler);
		
		// Check that the received state is what is expected
		elevator = scheduler.elevatorLatestData.get(1);
		assertEquals(ElevatorStates.OPEN_DOORS, elevator.state());	// Moved to the correct state
		assertEquals(ScenarioFaults.DOOR_STUCK, elevator.faultType());	// ** What we're looking for: ERROR **
		assertEquals(true, scheduler.elevatorSignalQueue.isEmpty());	// Request was handled and removed from queue
		
		
		
		/*** TEST 3: Going from OPEN_DOORS -> CLOSE_DOORS without picking up error'd passenger request (NO ERROR should exist) ***/
		// Set state to "Prepare Elevator State Message"
		scheduler.setState(new PrepareElevatorStateMessage());
		
		// Rig the passenger request into the requestsActive list (where we search for requests with faults)
		request = new RequestData(new FloorRequest(1, 2, Direction.UP, new ElevatorFault(ScenarioFaults.DOOR_STUCK, false)), 1);	// Request coming from floor 1 with DOOR_STUCK error
		scheduler.requestsActive.get(1).clear();
		scheduler.requestsActive.get(1).add(request);
		
		// Rig the elevator's previous state
		scheduler.elevatorLatestData.clear();
		scheduler.elevatorLatestData.put(1, new ElevatorData(new ElevatorSignal(ElevatorStates.OPEN_DOORS, 1,1, new ArrayList<Integer>(), new HashSet<Integer>(), null)));

		// Rig an Elevator message added to system via the dispatcher
		scheduler.elevatorSignalQueue.clear();
		scheduler.elevatorSignalQueue.add(new ElevatorSignal(ElevatorStates.OPEN_DOORS, 1,1, new ArrayList<Integer>(), new HashSet<Integer>(), null));	// Elevator starting at floor 1
		
		// Run an iteration of doAction() for the state
		scheduler.getState().doAction(scheduler);
		
		// Check that the received state is what is expected
		elevator = scheduler.elevatorLatestData.get(1);
		assertEquals(ElevatorStates.CLOSE_DOORS, elevator.state());	// Moved to the correct state
		assertEquals(null, elevator.faultType());	// ** What we're looking for: ERROR **
		assertEquals(true, scheduler.elevatorSignalQueue.isEmpty());	// Request was handled and removed from queue
		
		
		
		/*** TEST 4: Going from OPEN_DOORS -> CLOSE_DOORS WITH the error'd passenger request (ERROR should exist) ***/
		// Set state to "Prepare Elevator State Message"
		scheduler.setState(new PrepareElevatorStateMessage());
		
		// Rig the passenger request into the requestsActive list (where we search for requests with faults)
		request = new RequestData(new FloorRequest(1, 2, Direction.UP, new ElevatorFault(ScenarioFaults.DOOR_STUCK, false)), 1);	// Request coming from floor 1 with DOOR_STUCK error
		request.setPickedUpPassenger(true);		// Only difference between test 3 and 4.
		scheduler.requestsActive.get(1).clear();
		scheduler.requestsActive.get(1).add(request);
		
		// Rig the elevator's previous state
		scheduler.elevatorLatestData.clear();
		scheduler.elevatorLatestData.put(1, new ElevatorData(new ElevatorSignal(ElevatorStates.OPEN_DOORS, 1,1, new ArrayList<Integer>(), new HashSet<Integer>(), null)));

		// Rig an Elevator message added to system via the dispatcher
		scheduler.elevatorSignalQueue.clear();
		scheduler.elevatorSignalQueue.add(new ElevatorSignal(ElevatorStates.OPEN_DOORS, 1,1, new ArrayList<Integer>(), new HashSet<Integer>(), null));	// Elevator starting at floor 1
		
		// Run an iteration of doAction() for the state
		scheduler.getState().doAction(scheduler);
		
		// Check that the received state is what is expected
		elevator = scheduler.elevatorLatestData.get(1);
		assertEquals(ElevatorStates.CLOSE_DOORS, elevator.state());	// Moved to the correct state
		assertEquals(ScenarioFaults.DOOR_STUCK, elevator.faultType());	// ** What we're looking for: ERROR **
		assertEquals(true, scheduler.elevatorSignalQueue.isEmpty());	// Request was handled and removed from queue
		
		
		
		/*** TEST 5: Going from CLOSE_DOORS -> IDLE without picking up error'd passenger request (NO ERROR should exist) ***/
		// Set state to "Prepare Elevator State Message"
		scheduler.setState(new PrepareElevatorStateMessage());
		
		// Rig the passenger request into the requestsActive list (where we search for requests with faults)
		request = new RequestData(new FloorRequest(1, 2, Direction.UP, new ElevatorFault(ScenarioFaults.DOOR_STUCK, false)), 1);	// Request coming from floor 1 with DOOR_STUCK error
		scheduler.requestsActive.get(1).clear();
		scheduler.requestsActive.get(1).add(request);
		
		// Rig the elevator's previous state
		scheduler.elevatorLatestData.clear();
		scheduler.elevatorLatestData.put(1, new ElevatorData(new ElevatorSignal(ElevatorStates.CLOSE_DOORS, 1,1, new ArrayList<Integer>(), new HashSet<Integer>(), null)));

		// Rig an Elevator message added to system via the dispatcher
		scheduler.elevatorSignalQueue.clear();
		scheduler.elevatorSignalQueue.add(new ElevatorSignal(ElevatorStates.CLOSE_DOORS, 1,1, new ArrayList<Integer>(), new HashSet<Integer>(), null));	// Elevator starting at floor 1
		
		// Run an iteration of doAction() for the state
		scheduler.getState().doAction(scheduler);
		
		// Check that the received state is what is expected
		elevator = scheduler.elevatorLatestData.get(1);
		assertEquals(ElevatorStates.IDLE, elevator.state());	// Moved to the correct state
		assertEquals(null, elevator.faultType());	// ** What we're looking for: ERROR **
		assertEquals(true, scheduler.elevatorSignalQueue.isEmpty());	// Request was handled and removed from queue
		
		
		
		/*** TEST 6: Going from CLOSE_DOORS -> IDLE WITH the error'd passenger request (NO ERROR should exist) ***/
		// Set state to "Prepare Elevator State Message"
		scheduler.setState(new PrepareElevatorStateMessage());
		
		// Rig the passenger request into the requestsActive list (where we search for requests with faults)
		request = new RequestData(new FloorRequest(1, 2, Direction.UP, new ElevatorFault(ScenarioFaults.DOOR_STUCK, false)), 1);	// Request coming from floor 1 with DOOR_STUCK error
		request.setPickedUpPassenger(true);		// Only difference between test 5 and 6.
		scheduler.requestsActive.get(1).clear();
		scheduler.requestsActive.get(1).add(request);
		
		// Rig the elevator's previous state
		scheduler.elevatorLatestData.clear();
		scheduler.elevatorLatestData.put(1, new ElevatorData(new ElevatorSignal(ElevatorStates.CLOSE_DOORS, 1,1, new ArrayList<Integer>(), new HashSet<Integer>(), null)));

		// Rig an Elevator message added to system via the dispatcher
		scheduler.elevatorSignalQueue.clear();
		scheduler.elevatorSignalQueue.add(new ElevatorSignal(ElevatorStates.CLOSE_DOORS, 1,1, new ArrayList<Integer>(), new HashSet<Integer>(), null));	// Elevator starting at floor 1
		
		// Run an iteration of doAction() for the state
		scheduler.getState().doAction(scheduler);
		
		// Check that the received state is what is expected
		elevator = scheduler.elevatorLatestData.get(1);
		assertEquals(ElevatorStates.IDLE, elevator.state());	// Moved to the correct state
		assertEquals(null, elevator.faultType());	// ** What we're looking for: ERROR **
		assertEquals(true, scheduler.elevatorSignalQueue.isEmpty());	// Request was handled and removed from queue
		
		
		
		/*** TEST 7: Going from IDLE -> OPEN_DOORS without picking up error'd passenger request (NO ERROR should exist) ***/
		// Set state to "Prepare Elevator State Message"
		scheduler.setState(new PrepareElevatorStateMessage());
		
		// Rig the passenger request into the requestsActive list (where we search for requests with faults)
		request = new RequestData(new FloorRequest(1, 2, Direction.UP, new ElevatorFault(ScenarioFaults.DOOR_STUCK, false)), 1);	// Request coming from floor 1 with DOOR_STUCK error
		scheduler.requestsActive.get(1).clear();
		scheduler.requestsActive.get(1).add(request);
		
		// Rig the elevator's previous state
		scheduler.elevatorLatestData.clear();
		scheduler.elevatorLatestData.put(1, new ElevatorData(new ElevatorSignal(ElevatorStates.IDLE, 1,1, new ArrayList<Integer>(), new HashSet<Integer>(), null)));

		// Rig an Elevator message added to system via the dispatcher
		scheduler.elevatorSignalQueue.clear();
		scheduler.elevatorSignalQueue.add(new ElevatorSignal(ElevatorStates.IDLE, 1,1, new ArrayList<Integer>(), new HashSet<Integer>(), null));	// Elevator starting at floor 1
		
		// Run an iteration of doAction() for the state
		scheduler.getState().doAction(scheduler);
		
		// Check that the received state is what is expected
		elevator = scheduler.elevatorLatestData.get(1);
		assertEquals(ElevatorStates.OPEN_DOORS, elevator.state());	// Moved to the correct state
		assertEquals(null, elevator.faultType());	// ** What we're looking for: NO ERROR **
		assertEquals(true, scheduler.elevatorSignalQueue.isEmpty());	// Request was handled and removed from queue
		
		
		
		/*** TEST 8: Going from IDLE -> OPEN_DOORS WITH the error'd passenger request (ERROR should exist) ***/
		// Set state to "Prepare Elevator State Message"
		scheduler.setState(new PrepareElevatorStateMessage());
		
		// Rig the passenger request into the requestsActive list (where we search for requests with faults)
		request = new RequestData(new FloorRequest(1, 2, Direction.UP, new ElevatorFault(ScenarioFaults.DOOR_STUCK, false)), 1);	// Request coming from floor 1 with DOOR_STUCK error
		request.setPickedUpPassenger(true);		// Only difference between test 7 and 8.
		scheduler.requestsActive.get(1).clear();
		scheduler.requestsActive.get(1).add(request);
		
		// Rig a second passenger request that hasn't yet been picked up (otherwise, if everyone on the floor has been picked up, the elevator would leave. Now it will OPEN_DOORS)
		request = new RequestData(new FloorRequest(1, 3, Direction.UP, new ElevatorFault(ScenarioFaults.DOOR_STUCK, false)), 1);
		scheduler.requestsActive.get(1).add(request);
		
		// Rig the elevator's previous state
		scheduler.elevatorLatestData.clear();
		scheduler.elevatorLatestData.put(1, new ElevatorData(new ElevatorSignal(ElevatorStates.IDLE, 1,1, new ArrayList<Integer>(), new HashSet<Integer>(), null)));

		// Rig an Elevator message added to system via the dispatcher
		scheduler.elevatorSignalQueue.clear();
		scheduler.elevatorSignalQueue.add(new ElevatorSignal(ElevatorStates.IDLE, 1,1, new ArrayList<Integer>(), new HashSet<Integer>(), null));	// Elevator starting at floor 1
		
		// Run an iteration of doAction() for the state
		scheduler.getState().doAction(scheduler);
		
		// Check that the received state is what is expected
		elevator = scheduler.elevatorLatestData.get(1);
		assertEquals(ElevatorStates.OPEN_DOORS, elevator.state());	// Moved to the correct state
		assertEquals(ScenarioFaults.DOOR_STUCK, elevator.faultType());	// ** What we're looking for: ERROR **
		assertEquals(true, scheduler.elevatorSignalQueue.isEmpty());	// Request was handled and removed from queue
	}
	
	
	
	@Test
	/**
	 * Rigs situations in which the elevator transition involves the ElevatorStates MOVING_UP, MOVING_DOWN, or STOP,
	 * since these are the only states in which an ELEVATOR_STUCK error will occur. Also rigs messages that should
	 * be added to the system from the dispatcher in order to run through the state's doAction() method.
	 */
	void prepareElevatorStateMessage_HandleElevatorStuckElevatorFaultMessageTest() {
		// Setup
		scheduler.requestsActive.put(1, new ArrayList<RequestData>());
		scheduler.elevatorLatestData.put(1, null);

		/*** TEST 1: Going from IDLE -> MOVING_UP without picking up error'd passenger request (NO ERROR should exist) ***/
		// Set state to "Prepare Elevator State Message"
		scheduler.setState(new PrepareElevatorStateMessage());
		
		// Rig the passenger request into the requestsActive list (where we search for requests with faults)
		RequestData request = new RequestData(new FloorRequest(2, 3, Direction.UP, new ElevatorFault(ScenarioFaults.ELEVATOR_STUCK, false)), 1);	// Request coming from floor 2 with ELEVATOR_STUCK error
		scheduler.requestsActive.get(1).add(request);
		
		// Rig the elevator's previous state
		HashSet<Integer> carBttns = new HashSet<Integer>();
		carBttns.add(2);
		scheduler.elevatorLatestData.put(1, new ElevatorData(new ElevatorSignal(ElevatorStates.IDLE, 1,1, new ArrayList<Integer>(), carBttns, null)));	// Elevator starting at floor 1

		// Rig an Elevator message added to system via the dispatcher
		scheduler.elevatorSignalQueue.add(new ElevatorSignal(ElevatorStates.IDLE, 1,1, new ArrayList<Integer>(), carBttns, null));
		
		// Run an iteration of doAction() for the state
		scheduler.getState().doAction(scheduler);
		
		// Check that the received state is what is expected
		ElevatorData elevator = scheduler.elevatorLatestData.get(1);
		assertEquals(ElevatorStates.MOVING_UP, elevator.state());	// Moved to the correct state
		assertEquals(null, elevator.faultType());	// ** What we're looking for: NO ERROR **
		assertEquals(true, scheduler.elevatorSignalQueue.isEmpty());	// Request was handled and removed from queue
		
		
		
		/*** TEST 2: Going from IDLE -> MOVING_UP WITH the error'd passenger request (ERROR should exist) ***/
		// Set state to "Prepare Elevator State Message"
		scheduler.setState(new PrepareElevatorStateMessage());
		
		// Rig the passenger request into the requestsActive list (where we search for requests with faults)
		request = new RequestData(new FloorRequest(1, 3, Direction.UP, new ElevatorFault(ScenarioFaults.ELEVATOR_STUCK, false)), 1);	// Request coming from floor 1 with ELEVATOR_STUCK error
		request.setPickedUpPassenger(true);	// Only difference between test 1 and 2.
		scheduler.requestsActive.get(1).clear();
		scheduler.requestsActive.get(1).add(request);
		
		// Rig the elevator's previous state
		carBttns.clear();
		carBttns.add(3);
		scheduler.elevatorLatestData.clear();
		scheduler.elevatorLatestData.put(1, new ElevatorData(new ElevatorSignal(ElevatorStates.IDLE, 1,1, new ArrayList<Integer>(), carBttns, null)));	// Elevator starting at floor 1

		// Rig an Elevator message added to system via the dispatcher
		scheduler.elevatorSignalQueue.clear();
		scheduler.elevatorSignalQueue.add(new ElevatorSignal(ElevatorStates.IDLE, 1,1, new ArrayList<Integer>(), carBttns, null));
		
		// Run an iteration of doAction() for the state
		scheduler.getState().doAction(scheduler);
		
		// Check that the received state is what is expected
		elevator = scheduler.elevatorLatestData.get(1);
		assertEquals(ElevatorStates.MOVING_UP, elevator.state());	// Moved to the correct state
		assertEquals(ScenarioFaults.ELEVATOR_STUCK, elevator.faultType());	// ** What we're looking for: ERROR **
		assertEquals(true, scheduler.elevatorSignalQueue.isEmpty());	// Request was handled and removed from queue
		
		
		
		/*** TEST 3: Going from MOVING_UP -> MOVING_UP without picking up error'd passenger request (NO ERROR should exist) ***/
		// Set state to "Prepare Elevator State Message"
		scheduler.setState(new PrepareElevatorStateMessage());
		
		// Rig the passenger request into the requestsActive list (where we search for requests with faults)
		request = new RequestData(new FloorRequest(3, 4, Direction.UP, new ElevatorFault(ScenarioFaults.ELEVATOR_STUCK, false)), 1);	// Request coming from floor 3 with ELEVATOR_STUCK error
		scheduler.requestsActive.get(1).clear();
		scheduler.requestsActive.get(1).add(request);
		
		// Rig the elevator's previous state
		carBttns.clear();
		carBttns.add(3);
		scheduler.elevatorLatestData.clear();
		scheduler.elevatorLatestData.put(1, new ElevatorData(new ElevatorSignal(ElevatorStates.MOVING_UP, 1,1, new ArrayList<Integer>(), carBttns, null)));	// Elevator starting at floor 1

		// Rig an Elevator message added to system via the dispatcher
		scheduler.elevatorSignalQueue.clear();
		scheduler.elevatorSignalQueue.add(new ElevatorSignal(ElevatorStates.MOVING_UP, 2,1, new ArrayList<Integer>(), carBttns, null));
		
		// Run an iteration of doAction() for the state
		scheduler.getState().doAction(scheduler);
		
		// Check that the received state is what is expected
		elevator = scheduler.elevatorLatestData.get(1);
		assertEquals(ElevatorStates.MOVING_UP, elevator.state());	// Moved to the correct state
		assertEquals(null, elevator.faultType());	// ** What we're looking for: NO ERROR **
		assertEquals(true, scheduler.elevatorSignalQueue.isEmpty());	// Request was handled and removed from queue
		
		
		
		/*** TEST 4: Going from MOVING_UP -> MOVING_UP WITH the error'd passenger request, error returned/not handled (ERROR should exist) ***/
		// Set state to "Prepare Elevator State Message"
		scheduler.setState(new PrepareElevatorStateMessage());
		
		// Rig the passenger request into the requestsActive list (where we search for requests with faults)
		request = new RequestData(new FloorRequest(1, 4, Direction.UP, new ElevatorFault(ScenarioFaults.ELEVATOR_STUCK, false)), 1);	// Request coming from floor 1 with ELEVATOR_STUCK error
		request.setPickedUpPassenger(true);	// Only difference between test 3 and 4.
		scheduler.requestsActive.get(1).clear();
		scheduler.requestsActive.get(1).add(request);
		
		// Rig the elevator's previous state (If there's a fault, and its already moving, it'll contain the fault in its previous data)
		carBttns.clear();
		carBttns.add(4);
		scheduler.elevatorLatestData.clear();
		scheduler.elevatorLatestData.put(1, new ElevatorData(new ElevatorSignal(ElevatorStates.MOVING_UP, 1,1, new ArrayList<Integer>(), carBttns, ScenarioFaults.ELEVATOR_STUCK)));	// Elevator starting at floor 1

		// Rig an Elevator message added to system via the dispatcher  (If there's a fault, and its already moving, it'll contain the fault in its previous data)
		scheduler.elevatorSignalQueue.clear();
		scheduler.elevatorSignalQueue.add(new ElevatorSignal(ElevatorStates.MOVING_UP, 2,1, new ArrayList<Integer>(), carBttns, ScenarioFaults.ELEVATOR_STUCK));
		
		// Run an iteration of doAction() for the state
		scheduler.getState().doAction(scheduler);
		
		// Check that the received state is what is expected
		elevator = scheduler.elevatorLatestData.get(1);
		assertEquals(ElevatorStates.MOVING_UP, elevator.state());	// Moved to the correct state
		assertEquals(ScenarioFaults.ELEVATOR_STUCK, elevator.faultType());	// ** What we're looking for: ERROR **
		assertEquals(true, scheduler.elevatorSignalQueue.isEmpty());	// Request was handled and removed from queue
		
		
		
		/*** TEST 5: Going from MOVING_UP -> STOP without picking up error'd passenger request (NO ERROR should exist) ***/
		// Set state to "Prepare Elevator State Message"
		scheduler.setState(new PrepareElevatorStateMessage());
		
		// Rig the passenger request into the requestsActive list (where we search for requests with faults)
		request = new RequestData(new FloorRequest(2, 3, Direction.UP, new ElevatorFault(ScenarioFaults.ELEVATOR_STUCK, false)), 1);	// Request coming from floor 2 with ELEVATOR_STUCK error
		scheduler.requestsActive.get(1).clear();
		scheduler.requestsActive.get(1).add(request);
		
		// Rig the elevator's previous state
		carBttns.clear();
		carBttns.add(2);
		scheduler.elevatorLatestData.clear();
		scheduler.elevatorLatestData.put(1, new ElevatorData(new ElevatorSignal(ElevatorStates.MOVING_UP, 1,1, new ArrayList<Integer>(), carBttns, null)));	// Elevator starting at floor 1

		// Rig an Elevator message added to system via the dispatcher
		scheduler.elevatorSignalQueue.clear();
		scheduler.elevatorSignalQueue.add(new ElevatorSignal(ElevatorStates.MOVING_UP, 1,1, new ArrayList<Integer>(), carBttns, null));
		
		// Run an iteration of doAction() for the state
		scheduler.getState().doAction(scheduler);
		
		// Check that the received state is what is expected
		elevator = scheduler.elevatorLatestData.get(1);
		assertEquals(ElevatorStates.STOP, elevator.state());	// Moved to the correct state
		assertEquals(null, elevator.faultType());	// ** What we're looking for: NO ERROR **
		assertEquals(true, scheduler.elevatorSignalQueue.isEmpty());	// Request was handled and removed from queue
		
		
		
		/*** TEST 6: Going from MOVING_UP -> STOP WITH the error'd passenger request (ERROR should exist) ***/
		// Set state to "Prepare Elevator State Message"
		scheduler.setState(new PrepareElevatorStateMessage());
		
		// Rig the passenger request into the requestsActive list (where we search for requests with faults)
		request = new RequestData(new FloorRequest(1, 2, Direction.UP, new ElevatorFault(ScenarioFaults.ELEVATOR_STUCK, false)), 1);	// Request coming from floor 1 with ELEVATOR_STUCK error
		request.setPickedUpPassenger(true);	// Only difference between test 5 and 6.
		scheduler.requestsActive.get(1).clear();
		scheduler.requestsActive.get(1).add(request);
		
		// Rig the elevator's previous state
		carBttns.clear();
		carBttns.add(2);
		scheduler.elevatorLatestData.clear();
		scheduler.elevatorLatestData.put(1, new ElevatorData(new ElevatorSignal(ElevatorStates.MOVING_UP, 1,1, new ArrayList<Integer>(), carBttns, ScenarioFaults.ELEVATOR_STUCK)));	// Elevator starting at floor 1

		// Rig an Elevator message added to system via the dispatcher
		scheduler.elevatorSignalQueue.clear();
		scheduler.elevatorSignalQueue.add(new ElevatorSignal(ElevatorStates.MOVING_UP, 1,1, new ArrayList<Integer>(), carBttns, ScenarioFaults.ELEVATOR_STUCK));
		
		// Run an iteration of doAction() for the state
		scheduler.getState().doAction(scheduler);
		
		// Check that the received state is what is expected
		elevator = scheduler.elevatorLatestData.get(1);
		assertEquals(ElevatorStates.STOP, elevator.state());	// Moved to the correct state
		assertEquals(ScenarioFaults.ELEVATOR_STUCK, elevator.faultType());	// ** What we're looking for: ERROR **
		assertEquals(true, scheduler.elevatorSignalQueue.isEmpty());	// Request was handled and removed from queue
		
		
		
		/*** TEST 7: Going from STOP -> OPEN_DOORS without picking up error'd passenger request (NO ERROR should exist) ***/
		// Set state to "Prepare Elevator State Message"
		System.out.println("\n\nTEST7\n\n");
		scheduler.setState(new PrepareElevatorStateMessage());
		
		// Rig the passenger request into the requestsActive list (where we search for requests with faults)
		request = new RequestData(new FloorRequest(2, 3, Direction.UP, new ElevatorFault(ScenarioFaults.ELEVATOR_STUCK, false)), 1);	// Request coming from floor 2 with ELEVATOR_STUCK error
		scheduler.requestsActive.get(1).clear();
		scheduler.requestsActive.get(1).add(request);
		
		// Rig the elevator's previous state
		carBttns.clear();
		carBttns.add(2);
		scheduler.elevatorLatestData.clear();
		scheduler.elevatorLatestData.put(1, new ElevatorData(new ElevatorSignal(ElevatorStates.STOP, 1,1, new ArrayList<Integer>(), carBttns, null)));	// Elevator starting at floor 2

		// Rig an Elevator message added to system via the dispatcher
		scheduler.elevatorSignalQueue.clear();
		scheduler.elevatorSignalQueue.add(new ElevatorSignal(ElevatorStates.STOP, 1,2, new ArrayList<Integer>(), carBttns, null));
		
		// Run an iteration of doAction() for the state
		scheduler.getState().doAction(scheduler);
		
		// Check that the received state is what is expected
		elevator = scheduler.elevatorLatestData.get(1);
		System.out.println(elevator);
		assertEquals(ElevatorStates.OPEN_DOORS, elevator.state());	// Moved to the correct state
		assertEquals(null, elevator.faultType());	// ** What we're looking for: NO ERROR **
		assertEquals(true, scheduler.elevatorSignalQueue.isEmpty());	// Request was handled and removed from queue
		
		
		
		/*** TEST 8: Going from STOP -> ERROR_ELEVATOR_STUCK -> REBOOT WITH the error'd passenger request (ERROR should exist) ***/
		// Set state to "Prepare Elevator State Message"
		scheduler.setState(new PrepareElevatorStateMessage());
		
		// Rig the passenger request into the requestsActive list (where we search for requests with faults)
		request = new RequestData(new FloorRequest(1, 2, Direction.UP, new ElevatorFault(ScenarioFaults.ELEVATOR_STUCK, false)), 1);	// Request coming from floor 2 with ELEVATOR_STUCK error
		request.setPickedUpPassenger(true);	// Only difference between test 7 and 8.
		scheduler.requestsActive.get(1).clear();
		scheduler.requestsActive.get(1).add(request);
		
		// Rig the elevator's previous state
		carBttns.clear();
		carBttns.add(2);
		scheduler.elevatorLatestData.clear();
		scheduler.elevatorLatestData.put(1, new ElevatorData(new ElevatorSignal(ElevatorStates.STOP, 1,1, new ArrayList<Integer>(), carBttns, ScenarioFaults.ELEVATOR_STUCK)));	// Elevator starting at floor 1

		// Rig an Elevator message added to system via the dispatcher
		scheduler.elevatorSignalQueue.clear();
		scheduler.elevatorSignalQueue.add(new ElevatorSignal(ElevatorStates.ERROR_ELEVATOR_STUCK, 1,2, new ArrayList<Integer>(), carBttns, null));	// ** This is the expected error ** 
		
		// Run an iteration of doAction() for the state
		scheduler.getState().doAction(scheduler);
		
		// Check that the received state is what is expected
		elevator = scheduler.elevatorLatestData.get(1);
		assertEquals(ElevatorStates.DEAD, elevator.state());	// Moved to the correct state. NOTE: ** Being in DEAD tells us that the error was detected and handled **
		assertEquals(null, elevator.faultType());	// ** What we're looking for: NO ERROR **
		assertEquals(true, scheduler.elevatorSignalQueue.isEmpty());	// Request was handled and removed from queue
	}

	
	
	
	@Test
	/*
	 * Rigs a message that has been prepared to send to the floor system, and see if it sends.
	 */
	void sendMessageState_SendingFloorMessageTest() {
		// Set state to "Send Message"
		scheduler.setState(new SendMessage());
		
		// Rig a floor message created by the "Prepare Floor Message" State to be sent to the floor systems
		scheduler.signalsToSendToFloor.add(new FloorSignal(null, 0, null));
		
		// Run an iteration of doAction() for the state
		SchedulerState state = scheduler.getState().doAction(scheduler);
		
		// Check that the received state is what is expected
		SchedulerState expectedState = new UpdateView();
		assertEquals(expectedState.toString(), state.toString());
		assertEquals(true, scheduler.signalsToSendToFloor.isEmpty());	// All messages in this queue are sent
		
		// Now, Rig multiple floor messages, as they should all be sent at once if they are in the "send" queue
		for (int i = 0; i < 7; i++) { scheduler.signalsToSendToFloor.add(new FloorSignal(null, 0, null)); }
		
		// Run an iteration of doAction() for the state
		SchedulerState state2 = scheduler.getState().doAction(scheduler);
		
		// Check that the received state is what is expected
		SchedulerState expectedState2 = new UpdateView();
		assertEquals(expectedState2.toString(), state2.toString());
		assertEquals(true, scheduler.signalsToSendToFloor.isEmpty());	// All messages in this queue are sent
	}
	
	
	
	@Test
	/*
	 * Rigs a message that has been prepared to send to the elevator system, and see if it sends.
	 */
	void sendMessageState_SendingElevatorMessageTest() {
		// Set state to "Send Message"
		scheduler.setState(new SendMessage());
		
		// Rig an elevator message created by the "Prepare Elevator State Message" State to be sent to the elevator systems
		scheduler.signalsToSendToElevator.add(new ElevatorSignal(null, 0, 0, null, null, null));
		
		// Run an iteration of doAction() for the state
		SchedulerState state = scheduler.getState().doAction(scheduler);
		
		// Check that the received state is what is expected
		SchedulerState expectedState = new UpdateView();
		assertEquals(expectedState.toString(), state.toString());
		assertEquals(true, scheduler.signalsToSendToElevator.isEmpty());	// All messages in this queue are sent
		
		// Now, Rig multiple elevator messages, as they should all be sent at once if they are in the "send" queue
		for (int i = 0; i < 7; i++) { scheduler.signalsToSendToElevator.add(new ElevatorSignal(null, 0, 0, null, null, null)); }
		
		// Run an iteration of doAction() for the state
		SchedulerState state2 = scheduler.getState().doAction(scheduler);
		
		// Check that the received state is what is expected
		SchedulerState expectedState2 = new UpdateView();
		assertEquals(expectedState2.toString(), state2.toString());
		assertEquals(true, scheduler.signalsToSendToElevator.isEmpty());	// All messages in this queue are sent
	}
	
	
	
	@Test
	/*
	 * Rigs the scheduler starting in the UpdateView state and running through. There's not any real output to test since it's
	 * just calling a dispatcher method which is already tested, so this just tests that the state doesn't crash and burn.
	 */
	void updateViewState_SendingMessageTest() {
		// Set state to "Update View"
		scheduler.setState(new UpdateView());
		
		// Run an iteration of doAction() for the state
		SchedulerState state = scheduler.getState().doAction(scheduler);
		
		// Check that the received state is what is expected
		SchedulerState expectedState = new Listening();
		assertEquals(expectedState.toString(), state.toString());
	}
	
	
	
	@Test
	/*
	 * Tests the flow of the State Machine when a floor message is detected
	 * Listening -> ProcessMessage -> PrepareFloorMessage -> SendMessage -> UpdateView -> Listening
	 */
	void floorMessageStateMachineUseCaseTest() {
		// Set state to "Listening" (The starting state)
		scheduler.setState(new Listening());
		
		// Rig a floor message added to system via the dispatcher
		scheduler.floorRequestQueue.add(new FloorRequest(1, 2, null,null));
		
		// Create an ArrayList that keeps track of the states that we visited, in order.
		ArrayList<SchedulerState> statesVisitedSequence = new ArrayList<SchedulerState>();
		statesVisitedSequence.add(scheduler.getState());
		
		// Run through all the states in sequence. Once the state reaches the first state (=="LISTENING"), stop and test the sequence.
		while (scheduler.getState().toString() != statesVisitedSequence.get(0).toString() || (scheduler.getState().toString() == statesVisitedSequence.get(0).toString() && statesVisitedSequence.size() == 1)) {
			// Run an iteration of doAction() for the state
			SchedulerState state = scheduler.getState().doAction(scheduler);
			scheduler.setState(state);
			statesVisitedSequence.add(state);
		}
		
		// Check that the visited state sequence is what is expected for the use case
		ArrayList<SchedulerState> expectedStateSequence = new ArrayList<SchedulerState>();
		expectedStateSequence.add(new Listening());	// Start
		expectedStateSequence.add(new ProcessMessage());
		expectedStateSequence.add(new PrepareFloorMessage());
		expectedStateSequence.add(new SendMessage());
		expectedStateSequence.add(new UpdateView());
		expectedStateSequence.add(new Listening());	// End
		
		assertEquals(expectedStateSequence.size(), statesVisitedSequence.size());
		for (int i = 0; i < statesVisitedSequence.size(); i++) {
			assertEquals(expectedStateSequence.get(i).toString(), statesVisitedSequence.get(i).toString());
		}
	}
	
	
	
	@Test
	/*
	 * Tests the flow of the State Machine when a floor message is detected
	 * Listening -> ProcessMessage -> PrepareFloorMessage -> SendMessage -> UpdateView -> Listening
	 */
	void elevatorMessageStateMachineUseCaseTest() {
		// Set state to "Listening" (The starting state)
		scheduler.setState(new Listening());
		
		// Rig an elevator message added to system via the dispatcher. Since the elevator is not yet registered, the "START" state will allow us to do the same flow as every other ElevatorState
		scheduler.elevatorSignalQueue.add(new ElevatorSignal(ElevatorStates.START, 1, 0, null, null, null));	// ID has to be 0 < ID <= CONFIG.ELEVATORS. So 1 works. (anything else just does nothing and prints a warning. nothing to really test there)
		
		// Create an ArrayList that keeps track of the states that we visited, in order.
		ArrayList<SchedulerState> statesVisitedSequence = new ArrayList<SchedulerState>();
		statesVisitedSequence.add(scheduler.getState());
		
		// Run through all the states in sequence. Once the state reaches the first state (=="LISTENING"), stop and test the sequence.
		while (scheduler.getState().toString() != statesVisitedSequence.get(0).toString() || (scheduler.getState().toString() == statesVisitedSequence.get(0).toString() && statesVisitedSequence.size() == 1)) {
			// Run an iteration of doAction() for the state
			SchedulerState state = scheduler.getState().doAction(scheduler);
			scheduler.setState(state);
			statesVisitedSequence.add(state);
		}
		
		// Check that the visited state sequence is what is expected for the use case
		ArrayList<SchedulerState> expectedStateSequence = new ArrayList<SchedulerState>();
		expectedStateSequence.add(new Listening());	// Start
		expectedStateSequence.add(new ProcessMessage());
		expectedStateSequence.add(new PrepareElevatorStateMessage());
		expectedStateSequence.add(new SendMessage());
		expectedStateSequence.add(new UpdateView());
		expectedStateSequence.add(new Listening());	// End
		
		assertEquals(expectedStateSequence.size(), statesVisitedSequence.size());
		for (int i = 0; i < statesVisitedSequence.size(); i++) {
			assertEquals(expectedStateSequence.get(i).toString(), statesVisitedSequence.get(i).toString());
		}
	}
	
	
	
//	@Test
//	/*
//	 * Rigs a received elevator message being added to the system and check if the "Prepare Elevator State Message" State can handle it.
//	 * Handling it involves removing it from the "received" queue, adding it to the "Awaiting Assignment" queue, and adding
//	 * a message to the "Send Floor Signal" queue.
//	 */
//	void prepareElevatorStateMessageState_HandleElevatorMessageTest() {
//		// Set state to "Prepare Elevator State Message"
//		scheduler.setState(new PrepareElevatorStateMessage());
//		
//		// Create a list of all expected values for each input.
////		HashMap<ElevatorSignal, ArrayList<Object>> data = new HashMap<ElevatorSignal, ArrayList<Object>>();
////		data.put(new ElevatorSignal(null, 1, 1, null, null), );
//		
//		// Rig a floor message added to system via the dispatcher
//		scheduler.elevatorSignalQueue.add(new ElevatorSignal(null, 0, 0, null, null));
//		
//		// Run an iteration of doAction() for the state
//		SchedulerState state = scheduler.getState().doAction(scheduler);
//		
//		// Check that the received state is what is expected
//		SchedulerState expectedState = new SendMessage();
//		assertEquals(expectedState.toString(), state.toString());
//	}
	

}
