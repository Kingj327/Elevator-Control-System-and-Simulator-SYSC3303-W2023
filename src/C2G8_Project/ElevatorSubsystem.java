package C2G8_Project;

import java.util.*;
import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.apache.commons.lang3.time.StopWatch;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @author JP
 * ElevatorSubsystem is a state driven elevator, that sends its information to the scheduler
 * each time a state is changed or a new floor is reached. The states only change by the Scheduler's command
 * unless the Elevator is changing its state due to the Error States handling
 */
public class ElevatorSubsystem implements Runnable, DispatchConsumer {
	private final static Logger LOG = LogManager.getFormatterLogger(ElevatorSubsystem.class);
	final private Dispatcher dispatcher;

	private int eid;
	private int currFloor;

	private ElevatorStates state;
	private boolean stateSent;
	private ElevatorStates stateBeforeStop;

	private ElevatorSignal signal;
	private final ElevatorSystem parent;

	private ArrayList <Integer> newCarBttns = new ArrayList<Integer>(); //Inside Elevator new Floor requests
	private Set<Integer> carBttns = new HashSet<Integer>(); //Inside elevator Floor requests
	
	private ScenarioFaults fault = null;
	
	private int timeForAction;	//Time taken for a specific state to complete
	
	private StopWatch currTime; //Tracks time elapsed for current state

	private Queue<ElevatorSignal> signalQueue = new ArrayDeque<ElevatorSignal>(); //New messages from Scheduler stored in here
	private volatile boolean isSigQueueAvailable = true; //Assures only one thread accesses the signalQueue

	private ObjectMapper objMap;
	private volatile boolean shutdown = false; //Elevator on/off

	/**
	 * 
	 * @param id Unique Identifier for each elevator
	 * @param location Current Floor Location of elevator
	 * @param dispatch Dispatcher used to send/receive messages from scheduler
	 * @param boundSystem Parent ElevatorSubsystem Parent
	 */
	public ElevatorSubsystem(final int id, final int location, final Dispatcher dispatch,final ElevatorSystem boundSystem) {
		eid = id;
		currFloor = location;
		this.parent = boundSystem;
		state = ElevatorStates.START;
		stateBeforeStop = ElevatorStates.START;
		timeForAction = 0;
		signal = new ElevatorSignal(state, eid, currFloor, newCarBttns, carBttns, fault); // Changed carBttn to Set version
		stateSent = false; // Assures States are only sent once
		dispatcher = dispatch;
		currTime = StopWatch.create();
		objMap = new ObjectMapper();
	}
	
	public int getAdjustedTimeForActionMilliseconds() {
		return (timeForAction * 1000) / parent.getSpeedFactor();
	}

	public int getId() {
		return eid;
	}

	public ElevatorSignal getSignal(){
		return signal;
	}

	/**
	 * Waits for dispatcher to set up, and subscribe to Elevator signal topic
	 */
	public void init() {
		while (!dispatcher.isRunning()){}
		dispatcher.subscribe(String.format("%s_%d", ElevatorTopics.ELEVATOR_SIGNAL.toString(), eid), this);
	}

	/**
	 * Keep processing states and extracting messages received by dispatcher (if any)
	 * until and shutdown message is received
	 */
	@Override
	public void run() {
		init();
		while (!shutdown) {
			if (!signalQueue.isEmpty()) {
				signal = extractNextSignal();
			}
			processNextSignal();
		}
		LOG.info("SHUTTING DOWN");
	}

	public void shutdown() {
		LOG.info("SHUTDOWN SIGNAL RECEIVED");
		shutdown = true;
	}

	public boolean getShutdown(){
		return shutdown;
	}


	/**
	 * Handles the state transition of the elevator.
	 * All States except START an*d MOVING_UP/DOWN are only sent once with the help of stateSent boolean
	 * Once a state has been processed, the ElevatorSignal containing the state is sent to the scheduler
	 */
	public synchronized void processNextSignal() {

//		// Emergency Stop since next Floor is Limit
//		if ((state == ElevatorStates.MOVING_DOWN && currFloor <= 2)
//				|| (state == ElevatorStates.MOVING_UP && currFloor >= CONFIG.FLOORS - 1)) {
//			state = ElevatorStates.STOP;
//		}

		switch (state) {
			case START:
				if(timeForAction == 0){
					LOG.info("STATE = 'START'");
					timeForAction = ElevatorTimes.START;
					if (!currTime.isStarted()) { currTime.start(); }	// The program starts the timer upon initialization, so without checking if it's started, the timer will crash.
				}else if(currTime.getTime(TimeUnit.SECONDS) > timeForAction){
					currTime.reset();
					timeForAction = 0;
					sendMessage();
				}
				break;

			case IDLE:
				if (!stateSent) {
					LOG.info("STATE = 'IDLE'");
					currTime.reset();	// Because "START" state can exit at any time and may not reset the timer.
					timeForAction = ElevatorTimes.IDLE;
					stateSent = true;
					LOG.info("Elevator is Idle on Floor:" + currFloor);
					sendMessage();
				}
				break;

			case MOVING_UP:
				if (timeForAction == 0) {
					LOG.info("STATE = 'MOVING_UP'");
					if (stateBeforeStop != ElevatorStates.MOVING_UP) { 
						sendMessage(); // Send message as the elevator leaves the floor to allow it to receive "STOP" if its needed to ONLY move 1 floor
						timeForAction = ElevatorTimes.MOVING;
					}
					if (timeForAction == 0) { timeForAction = ElevatorTimes.MOVING_MAX; }
					currTime.start();
					stateBeforeStop = ElevatorStates.MOVING_UP;
				} else if (fault == null && currTime.getTime(TimeUnit.MILLISECONDS) > getAdjustedTimeForActionMilliseconds()) {
					currFloor++;
					currTime.reset();
					timeForAction = 0;
					LOG.info("Elevator Moving Up to Floor: " + currFloor);
					sendMessage();
				}else if(fault == ScenarioFaults.ELEVATOR_STUCK && currTime.getTime(TimeUnit.MILLISECONDS) > (getAdjustedTimeForActionMilliseconds() + 1)){ //+1 So we can actually simulate waiting a little longer than supposed to
					state = ElevatorStates.ERROR_ELEVATOR_STUCK;
				}
				break;

			case MOVING_DOWN:
				if (timeForAction == 0) {
					LOG.info("STATE = 'MOVING_DOWN'");
					if (stateBeforeStop != ElevatorStates.MOVING_DOWN) { 
						sendMessage(); // Send message as the elevator leaves the floor to allow it to receive "STOP" if its needed to ONLY move 1 floor
						timeForAction = ElevatorTimes.MOVING;
					}
					if (timeForAction == 0) { timeForAction = ElevatorTimes.MOVING_MAX; }
					currTime.start();
					stateBeforeStop = ElevatorStates.MOVING_DOWN;
				} else if (fault == null && currTime.getTime(TimeUnit.MILLISECONDS) > getAdjustedTimeForActionMilliseconds()) {
					currFloor--;
					currTime.reset();
					timeForAction = 0;
					LOG.info("Elevator Moving Down to Floor: " + currFloor);
					sendMessage();
				}else if(fault == ScenarioFaults.ELEVATOR_STUCK && currTime.getTime(TimeUnit.MILLISECONDS) > (getAdjustedTimeForActionMilliseconds() + 1)){ //+1 So we can actually simulate waiting a little longer than supposed to
					state = ElevatorStates.ERROR_ELEVATOR_STUCK;
				}
				break;

			case OPEN_DOORS:
				if (timeForAction == 0 && !stateSent) {
					LOG.info("STATE = 'OPEN_DOORS'");
					timeForAction = ElevatorTimes.DOORS;
					currTime.start();
				} else if (fault == null && currTime.getTime(TimeUnit.MILLISECONDS) > getAdjustedTimeForActionMilliseconds() && !stateSent) {
					currTime.reset();
					timeForAction = 0;
					stateSent = true;
					LOG.info("Elevator Opening Doors on Floor: " + currFloor);
					sendMessage();
				}else if(fault == ScenarioFaults.DOOR_STUCK && currTime.getTime(TimeUnit.MILLISECONDS) > (getAdjustedTimeForActionMilliseconds() + 1)){ //+1 So we can actually simulate waiting a little longer than supposed to
					stateBeforeStop = ElevatorStates.OPEN_DOORS;
					state = ElevatorStates.ERROR_DOOR_STUCK;
				}
				break;

			case CLOSE_DOORS:
				if (timeForAction == 0 && !stateSent) {
					LOG.info("STATE = 'CLOSE_DOORS'");
					timeForAction = ElevatorTimes.DOORS;
					currTime.start();
				} else if (fault == null && currTime.getTime(TimeUnit.MILLISECONDS) > getAdjustedTimeForActionMilliseconds() && !stateSent) {
					currTime.reset();
					timeForAction = 0;
					stateSent = true;
					LOG.info("Elevator Closing Doors on Floor: " + currFloor);
					handleNewFloorRequests();
					sendMessage();
				}else if(fault == ScenarioFaults.DOOR_STUCK && currTime.getTime(TimeUnit.MILLISECONDS) > (getAdjustedTimeForActionMilliseconds() + 1)){ //+1 So we can actually simulate waiting a little longer than supposed to
					stateBeforeStop = ElevatorStates.CLOSE_DOORS;
					state = ElevatorStates.ERROR_DOOR_STUCK;
				}
				break;

			case STOP:
				if ((timeForAction == 0 || timeForAction == ElevatorTimes.MOVING || timeForAction == ElevatorTimes.MOVING_MAX) && !stateSent) {	// The "timeForAction == MOVING..." means its an interrupt from a MOVING state
					LOG.info("STATE = 'STOP'");
					timeForAction = ElevatorTimes.STOP;
					if (!currTime.isStarted()) { currTime.start(); }	// "STOP" can interrupt the "MOVING" states, so need to check what the timer is. 
					else { 
						currTime.reset(); 
						currTime.start(); 
					}
				} else if (fault == null && currTime.getTime(TimeUnit.MILLISECONDS) > getAdjustedTimeForActionMilliseconds() && !stateSent) {
					currTime.reset();
					timeForAction = 0;
					if (stateBeforeStop == ElevatorStates.MOVING_UP) {
						currFloor++;
					} else if (stateBeforeStop == ElevatorStates.MOVING_DOWN) {
						currFloor--;
					}
					stateBeforeStop = null;	// reset for sending the initial message from MOVING_UP or MOVING_DOWN

					if (carBttns.contains(currFloor)) {
						carBttns.remove(currFloor);
					}
					stateSent = true;
					LOG.info("Elevator Stopping at Floor: " + currFloor);
					sendMessage();
				} else if (fault == ScenarioFaults.ELEVATOR_STUCK && currTime.getTime(TimeUnit.MILLISECONDS) > (getAdjustedTimeForActionMilliseconds() + 1)){ //+1 So we can actually simulate waiting a little longer than supposed to
					state = ElevatorStates.ERROR_ELEVATOR_STUCK;
				}
				break;

			//Elevator has not travelled to the next floor in regular amount of time hence we send the error to the scheduler
			case ERROR_ELEVATOR_STUCK:
				if(!stateSent){
					LOG.info("STATE = 'ERROR_ELEVATOR_STUCK'");
					fault = null;
					sendMessage();
					stateBeforeStop = null;
					currTime.reset();
					timeForAction = 0;
					stateSent = true;
				}
				break;

			//Elevator doors not opening/closing in regular amount of time hence we send the error to the scheduler
			case ERROR_DOOR_STUCK:
				if(!stateSent){
					LOG.info("STATE = 'ERROR_DOOR_STUCK'");
					fault = null;
					sendMessage();
					currTime.reset();
					timeForAction = 0;
					stateSent = true;
				}
				break;

			//Elevator Fixes the Issue and returns to previous state
			case REBOOT:
				//To simulate this error we wait for 10 seconds and return to previous state
				if(timeForAction == 0){
					LOG.info("STATE = 'REBOOT'");
					timeForAction = 10;
					currTime.start();
				}else if(currTime.getTime(TimeUnit.MILLISECONDS) > getAdjustedTimeForActionMilliseconds()){
					state = stateBeforeStop;
					stateBeforeStop = null;
					LOG.info("Rebooted to STATE ='" + state.name() + "'");
					currTime.reset();
					timeForAction = 0;
				}
				break;

			/**For UI purposes instead of shutting down, the elevator will remain in this DEAD state until
			 * all elevators are set to shutdown
			*/
			case DEAD:
				//To simulate this error we wait for 5 seconds and send in the DEAD state in which we remain
				// for an undetermined amount of time
				if(!stateSent && timeForAction == 0){
					LOG.info("STATE = 'DEAD'");
					timeForAction = 5;
					currTime.start();
				}else if(!stateSent && currTime.getTime(TimeUnit.MILLISECONDS) > getAdjustedTimeForActionMilliseconds()){
					LOG.info("Elevator Remains DEAD");
					currTime.reset();
					timeForAction = 0;
					stateSent = true;
					sendMessage();
				}
			break;

			default:
				try {
					if (!stateSent) {
						stateSent = true;
						throw new Exception("Elevator State Invalid");
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
		}
		
		// Set the Emergency Stop Time to at least 1/5th of the regular travel time of the MOVING (or MOVING_MAX) speed, and no more than 1 second. 
		// Gives the scheduler some time to respond before imemdiately shutting on the emergency brakes (ideally we don't want to use emergency brakes).
		float emergencyStopTime = (timeForAction > 0 && timeForAction/5 < 1) ? timeForAction/5 : 1;
		// Emergency Stop since next Floor is Limit
		if (currTime.getTime(TimeUnit.MILLISECONDS) > emergencyStopTime	 // Check that the scheduler has had time to respond before using emergency brakes
				&& ((state == ElevatorStates.MOVING_DOWN && currFloor <= 2)
				|| (state == ElevatorStates.MOVING_UP && currFloor >= CONFIG.FLOORS - 1)) ) {
			state = ElevatorStates.STOP;
			System.out.println(" ****** Emergency Brakes Engaged for "+getSubscriberNameIdentifier()+" ******");
		}
	}

	/**
	 * In state Close_Doors the elevator receives the In-Elevator buttons pressed
	 * This method removes any buttons currently already pressed and informs the
	 * Scheduleer of In-Elevator buttons pressed that must be registered
	 */
	public void handleNewFloorRequests(){
		if(newCarBttns.size() > 0){
			newCarBttns.removeAll(carBttns);	//Remove any duplicate requests to a floor
			
			System.out.print("New Floor Requests from elevator(" + eid + "): " );
			for(int i=0; i<newCarBttns.size(); i++){
				System.out.print(newCarBttns.get(i) + ", ");
			}
			System.out.println();
		}
	}

	/**
	 * Extracts Signal from Queue when available and updates elevator information
	 * @return ElevatorSignal
	 */
	public synchronized ElevatorSignal extractNextSignal() {
		while (!isSigQueueAvailable) {
			try {
				wait();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

		isSigQueueAvailable = false;
		ElevatorSignal newSignal = signalQueue.remove();
		isSigQueueAvailable = true;

		state = newSignal.state();
		newCarBttns = newSignal.newCarBttns();
		carBttns = newSignal.carBttns();
		fault = newSignal.faultType();
		stateSent = false;

		return newSignal;
	}

	/**
	 * Sends Elevator Signal to Scheduler and clears any In-Elevator buttons pressed
	 */
	public void sendMessage() {
		signal = new ElevatorSignal(state, eid, currFloor, newCarBttns, carBttns, fault);
		try {
			dispatcher.sendData(Destinations.SCHEDULER, SchedulerTopics.ELEVATOR_SIGNAL.toString(), signal);
		} catch (UnregisteredDispatcherDestination e) {
			LOG.error("[%s]: DispatcherUnable to convert data payload.");
		}
		newCarBttns.clear();
	}

	/**
	 * Receive data from dispatcher and add it to Queue if it's an elevator_signal topic,
	 * or send the shutdown signal if its a Scenario_end topic
	 */
	@Override
	public synchronized void receiveDispatch(String topic, String data) {
		LOG.info("%s receives topic %s with %s JSON data.", getSubscriberNameIdentifier(), topic, data);
		try {
			ElevatorSignal signal = objMap.readValue(data, ElevatorSignal.class);

			while (!isSigQueueAvailable) {
				try {
					wait();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}

			isSigQueueAvailable = false;
			signalQueue.add(signal);
			isSigQueueAvailable = true;

		} catch (JsonMappingException e) {
			LOG.error("[%s]: Unable to convert data payload.");
		} catch (JsonProcessingException e) {
			LOG.error("[%s]: Unable to convert data payload.");
		}
	}

	@Override
	public String getSubscriberNameIdentifier() {
		// TODO Auto-generated method stub
		return String.format("ElevatorSubsystem_%d", eid);
	}

	/**
	 * Prints the contents of the Elevator Signal
	 */
	public void printElevator() {
		System.out.println(String.format("Elevator ID:%d at floor %d, with State %s and %d new Car Buttons pressed, and %d buttons lit", eid, currFloor, state.toString(), newCarBttns.size(), carBttns.size()));
	}
}