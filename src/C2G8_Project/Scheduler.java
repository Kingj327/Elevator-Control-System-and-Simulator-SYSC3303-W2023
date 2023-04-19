/**
 * 
 */
package C2G8_Project;

import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Queue;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.time.StopWatch;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import C2G8_Project.MonitorSystem.MonitorTopics;

/**
 * @author Jordan
 * 
 * Overview:
 *  One of 3 main systems. The scheduler handles all communication between the FloorSystem and ElevatorSystem (and subsequently, the
 *  individual floors and elevators). It is also the brains behind determining what floor an elevator should go to, and when.
 * 
 * More details:
 *  The scheduler uses a UDP dispatcher that subscribes it to certain subjects for network communication. It coordinates between the
 *  floor and elevator systems with this dispatcher. When receiving messages, they go into specific queues. Those queues, as well as
 *  other variables (such as the "MASTER SHEET" variables), are used when in different states to maintain knowledge of the system, and
 *  pass around messages as required. The scheduler itself doesn't know how to do all of this. The scheduler only knows how to send and
 *  receive messages, or how to tell a state to "do an action" (it doesn't know what the action is). The states in SchedulerState.java
 *  hold all the logic for how the scheduler works. The scheduler just holds the variables required for those states to work effectively.
 */
class Scheduler implements Runnable, DispatchConsumer { 
	
	/*** Variables ***/
	private volatile SchedulerState state;	// For state machine
	final static Logger LOG =LogManager.getFormatterLogger(Scheduler.class);
	protected volatile StopWatch timer;
	
	/*** Variables used in UDP networking (With dispatcher) ***/
	public static final String SCHEDULER_NAME = "Scheduler";
	protected String name = "Scheduler";
	private Dispatcher dispatcher;
	private Thread dispatcherThread;
	private ObjectMapper objMap;
	protected volatile boolean isRunning = false;	// is this scheduler running
	protected volatile boolean tryingToStartAllSystems = false;	// trying to start communication between all systems
	protected volatile boolean allSystemsStarted = false;	// has communication started between all systems yet
	protected volatile boolean allScenariosReceivedFromFloor = false;	// When the floor has sent all the scenarios and is waiting to shutdown, this is true
	
	
	/*** Variables dealing with sending/receiving requests from floor and elevator systems, as well as the GUI (View)***/
	protected volatile ArrayList<RequestData> signalsToSendToView;
	protected volatile Queue<FloorSignal> signalsToSendToFloor;
	protected volatile Queue<ElevatorSignal> signalsToSendToElevator;
	protected volatile Queue<FloorRequest> floorRequestQueue;	// Incoming floor request end up here
	protected volatile boolean isFloorRequestQueueAvailable;
	protected volatile Queue<ElevatorSignal> elevatorSignalQueue;	// Incoming elevator signals end up here
	protected volatile boolean isElevatorSignalQueueAvailable;
	
	/*** Scheduler "MASTER SHEET" Variables ***/
	protected volatile int requestIDCounter;
	protected volatile ArrayList<RequestData>                       requestsAwaitingElevatorAssignment;
	protected volatile HashMap<Integer, ArrayList<RequestData>>     requestsActive;
	protected volatile ArrayList<RequestData>                       requestsComplete;
	protected volatile HashMap<Integer, ElevatorData>               elevatorLatestData;
	protected volatile HashMap<Integer, HashMap<Direction,Boolean>> floorButtonLamp;	// Changes when button pressed by passengers
	protected volatile HashMap<Integer, HashMap<Direction,Boolean>> floorDirectionLamp;	// Changes on arrival/departure of elevator
	
	/*** Variables used to measure performance of the Scheduler ***/
	protected final boolean trackPerformance = false;	// Only set to true to see the scheduler state performance data.
	protected volatile StopWatch performanceTimer;
	protected volatile HashMap<String, ArrayList<Long>> performanceTracker;
	

	/*** Constructor 
	 * @throws SocketException ***/
	public Scheduler(final String floorSystemAddress, final int floorSystemPort, final String elevatorSystemAddress, final int elevatorSystemPort, final int schedulerListenPort) throws SocketException {
		state = null;
		timer = new StopWatch();
		timer = StopWatch.create();
		timer.reset();
		dispatcher = new Dispatcher(Destinations.SCHEDULER,schedulerListenPort);
		dispatcherThread = new Thread(dispatcher);
		dispatcherThread.setName(name+" Dispatcher");
		dispatcherThread.start();
		while (!dispatcher.isRunning()) {}
		try {	// setting up dispatcher for UDP communication
//			dispatcher.registerDestination(Destinations.FLOOR_SYSTEM, floorSystemAddress, floorSystemPort);
//			dispatcher.registerDestination(Destinations.ELEVATOR_SYSTEM, elevatorSystemAddress, elevatorSystemPort);
			dispatcher.connectNewDestination(Destinations.ELEVATOR_SYSTEM, elevatorSystemAddress, elevatorSystemPort);
			dispatcher.connectNewDestination(Destinations.FLOOR_SYSTEM, floorSystemAddress, floorSystemPort);
			System.out.println(String.format("\n%s: IP Communication between all systems is complete. Moving on...\n", name));
			dispatcher.subscribe(SchedulerTopics.SCENARIO_STARTED.toString(), this);
			dispatcher.subscribe(SchedulerTopics.FLOOR_REQUEST.toString(), this);
			dispatcher.subscribe(SchedulerTopics.ELEVATOR_SIGNAL.toString(), this);
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			LOG.error("Invalid Port.");
			e.printStackTrace();
			System.exit(1);
		}
		objMap = new ObjectMapper();
		signalsToSendToView = new ArrayList<RequestData>();
		signalsToSendToFloor = new ArrayDeque<FloorSignal>();
		signalsToSendToElevator = new ArrayDeque<ElevatorSignal>();
		floorRequestQueue = new ArrayDeque<FloorRequest>();
		isFloorRequestQueueAvailable = true;
		elevatorSignalQueue = new ArrayDeque<ElevatorSignal>();
		isElevatorSignalQueueAvailable = true;
		requestIDCounter = 0;
		requestsAwaitingElevatorAssignment = new ArrayList<RequestData>();
		requestsActive = new HashMap<Integer, ArrayList<RequestData>>();
		requestsComplete = new ArrayList<RequestData>();
		elevatorLatestData = new HashMap<Integer, ElevatorData>();
		floorButtonLamp = new HashMap<Integer, HashMap<Direction, Boolean>>();
		floorDirectionLamp = new HashMap<Integer, HashMap<Direction, Boolean>>();
		for (int floorNumber = 1; floorNumber <= CONFIG.FLOORS; floorNumber++) {	// Initializing floor maps
			HashMap<Direction, Boolean> tempMap1 = new HashMap<Direction, Boolean>();	// Used to initialize floor maps
			HashMap<Direction, Boolean> tempMap2 = new HashMap<Direction, Boolean>();	// Used to initialize floor maps
			for (Direction direction : Direction.values()) {
				tempMap1.put(direction, false);
				tempMap2.put(direction, false);
			}
			floorButtonLamp.put(floorNumber, tempMap1);
			floorDirectionLamp.put(floorNumber, tempMap2);
		}
		if (trackPerformance) {	// Performance tracking variables
			performanceTimer = new StopWatch();
			performanceTimer = StopWatch.create();
			performanceTimer.reset();
			performanceTracker = new HashMap<String, ArrayList<Long>>();
		}
	}
	
	/*
	 * Runs the main loop of the program.
	 */
	@Override
	public void run() {
		
		System.out.println("Running Scheduler");
		setState(new Listening());
		if (trackPerformance) { performanceTrackerHandleEnterState(); }	// For the performance tracker (starting timer)
		
		// Main loop
		isRunning = true;
		while(isRunning) {
			doAction();
		}
	}
	
	
	
	/******************************************************************/
	/*********************** STATE MACHINE CODE ***********************/
	/******************************************************************/
	
	/*** Sets the state ***/
	public void setState(SchedulerState _state) {
		state = _state;
		if (state != null) { LOG.info("STATE = '%s'", state.toString()); }
		else { LOG.info("STATE = 'null'"); }
	}
	
	/*** Gets the state ***/
	public SchedulerState getState() {
		return state;
	}
	
	/*** Allows the scheduler to call one function and operate all states without knowing what each specific state does ***/
	public void doAction() { 
		SchedulerState _state = state.doAction(this); 
		if (_state != null) {
			if (trackPerformance) { performanceTrackerHandleExitState(state.toString()); }	// For the performance tracker
			setState(_state);	/*** IMPORTANT STATE TRANSITION ***/
			if (trackPerformance) { performanceTrackerHandleEnterState(); }					// For the performance tracker
			// Can add more logic here if/when required. 
		}
	}
	
	
	/******************************************************************/
	/********************* PERFORMANCE TRACKER CODE *******************/
	/******************************************************************/
	
	
	
	/*** When exiting a state, stop the timer and save the elapsed time ***/
	private void performanceTrackerHandleExitState(String stateName) {
		performanceTrackerHandleExitState(stateName, -1);
	}
	
	
	
	/*** When exiting a state, stop the timer and save the elapsed time (if not using the performanceTimer, the timerValue inputed should be -1 ***/
	protected synchronized void performanceTrackerHandleExitState(String stateName, long timerValue) {
		if (timerValue == -1) {	// Handle the case where a time isn't supplied
			performanceTimer.stop();
			timerValue = performanceTimer.getNanoTime(); 
		}	
		
		if (state != null) {
			if (!performanceTracker.containsKey(stateName)) {
				performanceTracker.put(stateName, new ArrayList<Long>());
			}
			ArrayList<Long> elapsedTimeInThisState = performanceTracker.get(stateName);
			if (elapsedTimeInThisState == null) { elapsedTimeInThisState = new ArrayList<Long>(); }
			elapsedTimeInThisState.add(timerValue);
			performanceTracker.put(stateName, elapsedTimeInThisState);
		}
	}
	
	
	
	/*** When entering a new state, restart the timer ***/
	private void performanceTrackerHandleEnterState() {
		performanceTimer.reset();
		performanceTimer.start();
	}
	
	
	
	/*** HELPER. Prints the performance data in a somewhat readable format in seconds ***/
	private void performanceTrackerPrintDataInSeconds() {
		performanceTrackerPrintDataWithNDecimalPlaces(9);
	}
	
	
	
	/*** HELPER. Prints the performance data in a somewhat readable format in milliseconds ***/
	private void performanceTrackerPrintDataInMilliSeconds() {
		performanceTrackerPrintDataWithNDecimalPlaces(6);
	}
	
	
	
	/*** HELPER. Prints the performance data in a somewhat readable format in microseconds ***/
	private void performanceTrackerPrintDataInMicroSeconds() {
		performanceTrackerPrintDataWithNDecimalPlaces(3);
	}
	
	
	
	/*** HELPER. Prints the performance data in a somewhat readable format in nanoseconds ***/
	private void performanceTrackerPrintDataInNanoSeconds() {
		performanceTrackerPrintData();
	}
	
	
	
	/*** DON'T CALL THIS, USE HELPERS. Prints the performance data in a somewhat readable format in whatever the default format is (likely nanoseconds) ***/
	private void performanceTrackerPrintData() {
		String performanceData = "\n\n\n     ~~~~~~PERFORMANCE TRACKER DATA~~~~~~ ";
		if (performanceTracker != null && !performanceTracker.isEmpty()) {
			for (String stateNameKey : performanceTracker.keySet()) {
				performanceData = performanceData + String.format("\n%s\n%s", stateNameKey, performanceTracker.get(stateNameKey).toString());
			}
		}
		System.out.println(performanceData);
	}
	
	
	
	/*** DON'T CALL THIS, USE HELPERS. Prints the performance data in a somewhat readable format, separating the time values by <unit_of_time>.<remainder_of_one_unit_of_time> ***/
	private void performanceTrackerPrintDataWithNDecimalPlaces(int nDecimalPlaces) {
		String performanceData = "\n\n\n     ~~~~~~PERFORMANCE TRACKER DATA~~~~~~ ";
		long UNIT_OF_TIME = (long)Math.pow(10, nDecimalPlaces);
		
		// Making sure nothing is null or empty,
		if (performanceTracker != null && !performanceTracker.isEmpty()) {
			
			// Get the keys of the HashMap
			for (String stateNameKey : performanceTracker.keySet()) {
				performanceData = performanceData + "\n" + stateNameKey;	// Save the key name as the title of the list section
				String listData = "";	// the data in the map at the given key (it's an ArrayList, hence the name)
				
				// Add a leading second value to the nanosecond times (ex: 12,345,678ns->0.012345678s, 1,234,567,898ns->1.234567898s)
				if (performanceTracker.get(stateNameKey) != null && !performanceTracker.get(stateNameKey).isEmpty()) {
					
					// Print the the format when there's only one element
					if (performanceTracker.get(stateNameKey).size() == 1) {
						listData = String.format("\n[0.%s]", performanceTracker.get(stateNameKey).get(0));
					
					// Print the format when there's at least 2 elements
					} else if (performanceTracker.get(stateNameKey).size() > 1){
						
						// First element
						long time = performanceTracker.get(stateNameKey).get(0);
						listData = String.format("\n[%d.%0"+nDecimalPlaces+"d", time/UNIT_OF_TIME, time%UNIT_OF_TIME);
						
						// All other elements [1,size-1]
						for (int i = 1; i < performanceTracker.get(stateNameKey).size(); i++) {
							time = performanceTracker.get(stateNameKey).get(i);
							listData = listData + String.format(", %d.%0"+nDecimalPlaces+"d", time/UNIT_OF_TIME, time%UNIT_OF_TIME);
						}
						
						// Last element
						listData = listData + "]";
					}
				}
				
				// Update the performanceData string
				performanceData = performanceData + listData;
			}
		}
		System.out.println(performanceData);
	}
	
	
	
	/******************************************************************/
	/*********************** UDP DISPATCHER CODE **********************/
	/******************************************************************/
	
	/*** Returns the state of isRunning (is this program running) ***/
	public boolean isRunning() {
		return isRunning;
	}
	
	/*** Returns the dispatchers port ***/
	public int getPort() {
		return dispatcher.getPort();
	}
	
	/*** Sends the start message to the appropriate systems to start communication (to whoever is subscribed to the topic FloorTopics.SCENARIO_START) ***/
	protected void sendStart() {
		try {
			dispatcher.sendData(Destinations.FLOOR_SYSTEM, FloorTopics.SCENARIO_START.toString());
			System.out.println("  Trying to start scenarios from floors.");
		} catch (UnregisteredDispatcherDestination e) {
			LOG.error("%s: Unable to dispatch request to destination %s, endpoint is not registered with dispatcher.Request skipped.",name,Destinations.SCHEDULER);
			e.printStackTrace();
			return;
		}
	}
	
	
	/*** Sends an ElevatorSignal message to the appropriate monitor (to whoever is subscribed to the topic MonitorTopics.SCHEDULER_UPDATE) ***/
	protected void sendViewElevatorSignal(ElevatorSignal signal) {
		try {
			dispatcher.sendData(Destinations.MONITOR_SYSTEMS, MonitorTopics.SCHEDULER_UPDATE.toString(), signal);
		} catch (UnregisteredDispatcherDestination e) {
			LOG.error("%s: Unable to dispatch request to destination %s, endpoint is not registered with dispatcher.Request skipped.",name,Destinations.MONITOR_SYSTEMS);
//			e.printStackTrace();
			return;
		}
	}
	
	/*** Sends a RequestData message to the appropriate monitor (to whoever is subscribed to the topic MonitorTopics.SCHEDULER_UPDATE) ***/
	protected void sendViewRequestData(RequestData request) {
		try {
			dispatcher.sendData(Destinations.MONITOR_SYSTEMS, MonitorTopics.SCHEDULER_UPDATE.toString(), request);
		} catch (UnregisteredDispatcherDestination e) {
			LOG.error("%s: Unable to dispatch request to destination %s, endpoint is not registered with dispatcher.Request skipped.",name,Destinations.MONITOR_SYSTEMS);
//			e.printStackTrace();
			return;
		}
	}
	
	/*** Sends a ViewData message to the appropriate monitor (to whoever is subscribed to the topic MonitorTopics.SCHEDULER_UPDATE) ***/
	protected void sendViewMasterSheetData(ViewData data) {
		try {
			dispatcher.sendData(Destinations.MONITOR_SYSTEMS, MonitorTopics.SCHEDULER_UPDATE.toString(), data);
		} catch (UnregisteredDispatcherDestination e) {
			LOG.error("%s: Unable to dispatch request to destination %s, endpoint is not registered with dispatcher.Request skipped.",name,Destinations.MONITOR_SYSTEMS);
//			e.printStackTrace();
			return;
		}
	}
	
	/*** Sends a FloorSignal message to the appropriate floor (to whoever is subscribed to the topic FloorTopics.FLOOR_SIGNAL_<number>) ***/
	protected void sendFloorSignal(FloorSignal signal) {
		try {
			dispatcher.sendData(Destinations.FLOOR_SYSTEM, String.format("%s_%d",FloorTopics.FLOOR_SIGNAL.toString(),signal.floor()), signal);
//			dispatcher.sendData(Destinations.MONITOR_SYSTEMS,MonitorTopics.FLOOR_SIGNAL_UPDATE.toString(), signal);
		} catch (UnregisteredDispatcherDestination e) {
			LOG.error("%s: Unable to dispatch request to destination %s, endpoint is not registered with dispatcher.Request skipped.",name,Destinations.SCHEDULER);
			e.printStackTrace();
			return;
		}
	}
	
	/*** Sends an ElevatorSignal message to the appropriate elevator (to whoever is subscribed to the topic ElevatorTopics.ELEVATOR_SIGNAL_<number>) ***/
	protected void sendElevatorSignal(ElevatorSignal signal) {
		try {
			dispatcher.sendData(Destinations.ELEVATOR_SYSTEM, String.format("%s_%d",ElevatorTopics.ELEVATOR_SIGNAL.toString(),signal.id()), signal);
		} catch (UnregisteredDispatcherDestination e) {
			LOG.error("%s: Unable to dispatch request to destination %s, endpoint is not registered with dispatcher.Request skipped.",name,Destinations.SCHEDULER);
			e.printStackTrace();
			return;
		}
	}
	
	/*** Receives a FloorRequest message from a floor and adds it to the appropriate queue ***/
	public synchronized void receiveFloorRequest(FloorRequest request) {
		// TODO Auto-generated method stub

		while(!isFloorRequestQueueAvailable) {
			try {
				wait();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		isFloorRequestQueueAvailable = false;
		floorRequestQueue.add(request);
		isFloorRequestQueueAvailable = true;
		notifyAll();
	}
	
	/*** Receives a ElevatorSignal message from an elevator and adds it to the appropriate queue ***/
	public synchronized void receiveElevatorSignal(ElevatorSignal signal) {
		// TODO Auto-generated method stub

		while(!isElevatorSignalQueueAvailable) {
			try {
				wait();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		isElevatorSignalQueueAvailable = false;
		elevatorSignalQueue.add(signal);
		isElevatorSignalQueueAvailable = true;
		notifyAll();
	}

	
	/**
	 * Receives a message form the dispatcher that needs to be dealt with. Depending on the topic, each request is dealt with differently.
	 *  - SCENARIO_STARTED Is the ACK message from the FloorSystem after the scheudler tells the FloorSystem to start sending passenger 
	 *                     requests because communication is now working between the systems. It's confirmation that the message was reecived.
	 *  - SCENARIO_COMPLETE Is when the FloorSystem has completed all the passenger requests for the given demo/scenario. In this case, once
	 *                      all requests have been service, the scheduler is free to shutdown all the systems.
	 *  - FLOOR_REQUEST Is when the scheduler receives a floor request from a given FloorSubsystem.
	 *  - ELEVATOR_SIGNAL Is when the scheduler receives a elevator "State" message from a givem ElevatorSubsystem.
	 */
	@Override
	public synchronized void receiveDispatch(String topic, String data) {
		// TODO Auto-generated method stub
		LOG.info("%s receives topic %s with %s JSON data.", name,topic,data);
		
		try {
			SchedulerTopics convTopic = SchedulerTopics.valueOf(topic);
			switch(convTopic) {
				case SCENARIO_STARTED:
					if (!allSystemsStarted) {	// So if any additional messages come in while starting, we only handle it once (don't reset variables)
						dispatcher.subscribe(SchedulerTopics.SCENARIO_COMPLETE.toString(), this);
						dispatcher.unSubscribe(SchedulerTopics.SCENARIO_STARTED.toString(), this);
						allSystemsStarted = true;
						tryingToStartAllSystems = false;
						System.out.println("  Scenarios started. All systems should be up and running.");
					}
					break;
					
				case FLOOR_REQUEST:
					RawFloorRequest rawRequest = objMap.readValue(data, RawFloorRequest.class);
					FloorRequest request = new FloorRequest(rawRequest.reqestFloor(), rawRequest.targetFloor(), rawRequest.direction(), new ElevatorFault(rawRequest.fault()));
					receiveFloorRequest(request);
					break;
				
				case ELEVATOR_SIGNAL:
					ElevatorSignal signal = objMap.readValue(data, ElevatorSignal.class);
					receiveElevatorSignal(signal);
					break;
				
				case SCENARIO_COMPLETE:
					LOG.info("Received SCENARIO_COMPLETE. Shutting down all systems once all requests are complete...");
					allScenariosReceivedFromFloor = true;
					dispatcher.unSubscribe(SchedulerTopics.SCENARIO_COMPLETE.toString(), this);
					break;
			}
		} catch (JsonMappingException e) {
			LOG.error("[%s]: Unable to convert data payload1."+data);
		} catch (JsonProcessingException e) {
			LOG.error("[%s]: Unable to convert data payload2.+data");
			e.printStackTrace();
		}
	}
	
	

	/*** Returns the name of the system ***/
	@Override
	public String getSubscriberNameIdentifier() {
		// TODO Auto-generated method stub
		return name;
	}
	
	/*** Shuts down all systems, waits a bit (to make sure messages got sent), and then shuts down itself and the dispatcher ***/
	protected void shutdown() {
		LOG.info("All requests have been completed. Attempting to shut down ALL systems.");
		sendShutdownToSystems();
//		if (!timer.isStarted()) { timer.reset(); }
//		while (timer.getTime(TimeUnit.SECONDS) <= 5) {}	// Wait some seconds to make sure the dispatcher isn't shut down before sending the shutdown messages.
//		timer.stop();
		dispatcher.shutdown();
		while(dispatcher.isRunning()) {}
		isRunning = false;
		LOG.info("Shutting down ALL systems...");
		if (trackPerformance) { performanceTrackerPrintDataInMilliSeconds(); }	// For performance tracker
	}
	
	/*** Sends a shutdown message to both the FloorSystem and the ElevatorSystem. ***/
	protected void sendShutdownToSystems() {
		try {
			dispatcher.sendData(Destinations.FLOOR_SYSTEM, FloorTopics.SCENARIO_END.toString());
			dispatcher.sendData(Destinations.ELEVATOR_SYSTEM, ElevatorTopics.SCENARIO_END.toString());
		} catch (UnregisteredDispatcherDestination e) {
			LOG.error("%s: Unable to dispatch request to destination %s, endpoint is not registered with dispatcher.Request skipped.",name,Destinations.SCHEDULER);
			e.printStackTrace();
			return;
		}
	}
	
	
	
	/******************************************************************/
	/*********************** SYSTEM HELPER CODE ***********************/
	/******************************************************************/
	
	
	
	/*
	 * Sets the direction floor lamp "Master Sheet" variable to either on or off.
	 */
	protected void setFloorButtonLamp(int floorNumber, Direction direction, boolean state) {
		HashMap<Direction, Boolean> buttonLamp = floorButtonLamp.get(floorNumber);
		buttonLamp.put(direction, state);	// FloorSignals.BTN_LAMP_ON or FloorSignals.BTN_LAMP_OFF
		floorButtonLamp.put(floorNumber, buttonLamp);
	}
	
	
	
	/*
	 * Sets the direction floor lamp "Master Sheet" variable to either on or off.
	 */
	protected void setFloorDirectionLamp(int floorNumber, Direction direction, boolean state) {
		HashMap<Direction, Boolean> directionLamp = floorDirectionLamp.get(floorNumber);
		directionLamp.put(direction, state);	// FloorSignals.DIR_LAMP_ON or FloorSignals.DIR_LAMP_OFF
		floorDirectionLamp.put(floorNumber, directionLamp);
	}
	
	
	
	/*
	 * Adds the request to a particular elevator, and updates the appropriate "MASTER SHEET" variables.
	 * NOTE: Does not remove the request from any lists that it may be in (like requestsAwaitingElevatorAssignment). The clean up has to be done manually.
	 */
	protected boolean assignRequestToAnActiveElevator(RequestData request, int elevatorID) {
		if (!requestsActive.isEmpty() && requestsActive.containsKey(elevatorID) && requestsActive.get(elevatorID) != null) {
			request.setElevatorID(elevatorID);
			requestsActive.get(elevatorID).add(request);
			signalsToSendToView.add(request);	// Sends change in request/passenger data to View/Monitor
			if (elevatorLatestData.containsKey(elevatorID) && !elevatorLatestData.get(elevatorID).targetLocations().contains( request.requestFloor() )) {
				elevatorLatestData.get(elevatorID).targetLocations().add( request.requestFloor() );
			}
			System.out.println(String.format("   *Adding job to elevator %s's requestsActive with requestID=%s...     ",elevatorID,request.requestID()));
			return true;
		}
		return false;
	}
	
	
	
	/*
	 * Adds the request to a particular elevator, and updates the appropriate "MASTER SHEET" variables, just like
	 * assignRequestToAnActiveElevator(). However, this also notifys the scheduler of a change in the system if
	 * the selected elevator was "IDLE", This is done by sending an elevator message to itself (the scheduler)
	 * containing the data from the last message received form that elevator.
	 * NOTE: Does not remove the request from any lists that it may be in (like requestsAwaitingElevatorAssignment). The clean up has to be done manually.
	 */
	protected boolean assignRequestToAnActiveElevatorAndNotify(RequestData request, int elevatorID) {
		boolean success = assignRequestToAnActiveElevator(request, elevatorID);
		if (success) {
			// If the found elevator is currently IDLE, put its last message in the received buffer to have a message prepared for it by another state.
			if (!elevatorLatestData.isEmpty() && elevatorLatestData.containsKey(elevatorID) && elevatorLatestData.get(elevatorID) != null) {
				ElevatorData elevator = elevatorLatestData.get(elevatorID);
				if (elevator.state() == ElevatorStates.IDLE) {
					ElevatorSignal tempSignal = new ElevatorSignal(elevator.state(), elevator.id(), elevator.location(), elevator.newCarBttns(), elevator.carBttns(), elevator.faultType());
					synchronized (elevatorSignalQueue) {
						if (!elevatorSignalQueue.contains(tempSignal)) { elevatorSignalQueue.add(tempSignal); }
					}
				}
			}
			return true;
		}
		return false;
	}
	
	
	
	/******************************************************************/
	/*********************** MAIN: STARTS PROGRAM**********************/
	/******************************************************************/

	
	/*** Starts the program by getting the required addresses for floor and elevator systems, and then starting the scheduler in a new thread ***/
	public static void main(String[] args) {
		String floorAddress = "";
		String elevatorAddress = "";
		if(args.length > 2) {
			LOG.error("%s: Unexpected number of arguments. There should be 1 argument.\n%s <FLOOR_SYSTEM_ADDRESS> <ELEVATOR_SYSTEM_ADDRESS>",args[0]);
			System.exit(1);
		}
		else if(args.length == 2) {
			floorAddress = args[0];
			elevatorAddress = args[1];
			LOG.info("[Addresses] FloorSystem=\"%s\", ElevatorSystem=\"%s\"", floorAddress,elevatorAddress);
		}
		else {
			floorAddress = "127.0.0.1";
			elevatorAddress = "127.0.0.1";
			LOG.warn("Insufficient address specified in program args. Defaulting to loopback address for both systems %s",floorAddress);
		}
	
		try {
			Scheduler scheduler = new Scheduler(floorAddress,CONFIG.FLOOR_SYSTEM_PORT,elevatorAddress,CONFIG.ELEVATOR_SYSTEM_PORT,CONFIG.SCHEDULER_PORT);
			Thread th = new Thread(scheduler);
			th.setName(SCHEDULER_NAME);
			th.start();
		} catch (NumberFormatException e) {
			LOG.error("Invalid scheduler address provided. Please provide a valid network address. GIVEN: %s", args[1]);
			e.printStackTrace();
			System.exit(1);
		} catch (SocketException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.exit(1);
		} 
	}
}
