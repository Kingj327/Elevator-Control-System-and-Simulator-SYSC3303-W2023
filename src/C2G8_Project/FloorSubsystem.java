/**
 * 
 */
package C2G8_Project;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Queue;
import java.util.Stack;

/**
 * @author Jayson Mendoza
 * FloorSubsystem: A class that reads the initial test script for the simulation, sends off the requests from the script to the Scheduler
 * in the order that they are read, and waits for all of the requests sent off to return before sending a quit / shutdown message.
 */
class FloorSubsystem implements Runnable, DispatchConsumer {
	private final static Logger LOG =LogManager.getFormatterLogger(FloorSubsystem.class);
	private ObjectMapper objMap;
	private String name;
	private final int floorNum;
	private final Dispatcher dispatcher;
	private final FloorSystem parent;
	private volatile boolean isRunning;
	private Queue<FloorSignal> sigQueue = new ArrayDeque<FloorSignal>();
	private volatile boolean isSigQueueAvailable = true;
	private final Direction[] supportedDirections;
	private HashMap<Direction,Boolean> directionLampOn;
	private HashMap<Direction,Boolean> directionBtnOn;
	private final Stack<PassengerRequest> requests = new Stack<PassengerRequest>();
	
	/**
	 * Sets up a new FloorSubsystem representing a floor and its components within the system.
	 * This is designed to be run within a thread.
	 * 
	 * @param FloorName The floor name used for identification purposes within the log
	 * @param floorNumber The floor number this FloorSystem will represent
	 * @param parent The FloorSystem that owns this FloorSubsystem. This used to coordinate the scenario time
	 * @param dispatcher The dispatcher used to communicate with the scheduler and receive messages from the Scheduler
	 * @param supportedDirections The directions this floor supports. For example the lowest floor doesn't necessary support down.
	 */
	public FloorSubsystem(final String FloorName,final int floorNumber,final FloorSystem parent,final Dispatcher dispatcher,final Direction[] supportedDirections) {
		this.name = FloorName;
		this.dispatcher = dispatcher;
		this.parent = parent;
		this.floorNum = floorNumber;
		directionBtnOn = new HashMap<Direction,Boolean>();
		directionLampOn = new HashMap<Direction,Boolean>();
		this.supportedDirections = supportedDirections;
		objMap = new ObjectMapper();
		LOG.info("[%s]: SETUP COMPLETE!", name);
	}
	
	public String getName() {
		return name;
	}

	public boolean isRunning() {
		return isRunning;
	}
	
	

	public HashMap<Direction, Boolean> getDirectionLampOn() {
		return directionLampOn;
	}

	public HashMap<Direction, Boolean> getDirectionBtnOn() {
		return directionBtnOn;
	}

	private void init() {
		for(Direction d : supportedDirections) {
			directionBtnOn.put(d,false);
			directionLampOn.put(d, false);
		}
		
		//Subscribe to all topics for Floor system
		for(FloorTopics topic : FloorTopics.values()) {
			dispatcher.subscribe(String.format("%s_%d",topic.toString(),floorNum), this);
		}
		
		Collections.sort(requests,Collections.reverseOrder());
		LOG.info("[%s]: IS ONLINE!", name);
	}

	/**
	 * Main run loop for Subsystem thread checks for a signal and processes it if found. It will then
	 * check scenario time and send requests to the scheduler for any new passengers that have arrived
	 * within the cycle.
	 */
	@Override
	public void run() {
		init();
		
		isRunning = true;
		while(isRunning) {
			
			//Process any new signals received by the Scheduler
			if(!sigQueue.isEmpty()) {
				processNextSignal();
			}
			
			//Check to see if any passenger requests should be triggered in this cycle based on scenario time.
			while(!requests.isEmpty() && requests.peek().getScenarioDurationMills() <= parent.getScenarioTimeMilliseconds()) {
				sendRequest(requests.pop());
				if(requests.isEmpty()) {
					LOG.info("%s has no more requests. Reporting back to FloorSystem.",name);
					parent.reportRequestsEmpty(this);
				}
			}
			try {
				Thread.sleep(parent.getPOLL_INTERVAL_MICROSECONDS());
			} catch (InterruptedException e) {
				continue;
			}
		}
	}
	
	/**
	 * Shuts down floorSubsystem on the next execution cycle
	 */
	public void shutdown() {
		isRunning = false;
		LOG.info("[%s]: IS SHUTTING DOWN!", name);
	}
	
	/**
	 * Adds a passenger request to the FloorSubsystems concern.
	 * This should only be called when the system is not running
	 * and will fail once the scenario has started.
	 * 
	 * These do not need to be ordered and will be sorted when
	 * the scenario starts by trigger time.
	 * 
	 * This is not thread safe.
	 * @param req The Passenger request to be added to the scenario.
	 */
	public void pushRequest(PassengerRequest req) {
		if(req.getCurrentFloor()!= floorNum) {
			LOG.error("Invalid Request: Cannot Add a request for floor %d to floor %d",req.getCurrentFloor(),floorNum);
			return;
		}
		else if(isRunning) {
			LOG.error("Cannot add request while floor simulation is running.");
			return;
		}
		
		requests.push(req);
		LOG.info("Request Added: %s",req.toString());
	}
	
	
	/**
	 * Sends a request to the scheduler for a new passenger arrival.
	 * 
	 * @param request The request informing the scheduler about the new passenger, its direction, and destination
	 */
	private void sendRequest(PassengerRequest request) {
		RawFloorRequest requestData = new RawFloorRequest(request.getCurrentFloor(),request.getTargetFloor(),request.getDirection(),request.getFaultType());
		try {
			dispatcher.sendData(Destinations.SCHEDULER, SchedulerTopics.FLOOR_REQUEST.toString(), requestData);
		} catch (UnregisteredDispatcherDestination e) {
			LOG.error("%s: Unable to dispatch request to destination %s, endpoint is not registered with dispatcher.Request skipped.",name,Destinations.SCHEDULER);
			e.printStackTrace();
			return;
		}
	}

	/**
	 * Adds a new signal to a Queue for asynchronous processing.
	 * This is done instead of directly executing the signal
	 * because the FloorSystem will call this and it's execution
	 * context will be tied up while it is processing making it 
	 * unable to process message until it concludes.
	 * 
	 * The signal queue passes responsibility of the signal processing
	 * to the thread.
	 * @param signal The signal to be processed.
	 */
	public synchronized void receiveSignal(FloorSignal signal) {

		while(!isSigQueueAvailable) {
			try {
				wait();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		isSigQueueAvailable = false;
		sigQueue.add(signal);
		isSigQueueAvailable = true;
		notifyAll();
	}
	
	/**
	 * Gets the next signal to be processed from the
	 * queue in a thread safe way. Isolated into its 
	 * own function for performance so lock is released
	 * as soon as data has been retrieved.
	 * @return The next Floor Signal to  be processed.
	 */
	private synchronized FloorSignal nextSignal() {
		while(!isSigQueueAvailable) {
			try {
				wait();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		isSigQueueAvailable = false;
		FloorSignal sig = sigQueue.remove();
		isSigQueueAvailable = true;
		notifyAll();
		return sig;
	}
	
	/**
	 * Process the next signal in the queue.
	 * Since this is a queue we can allow additions
	 * while we read since additions will occur on the
	 * other end of the queue.
	 */
	private void processNextSignal() {
		if(sigQueue.isEmpty()) {
			return;
		}
		
		FloorSignal sig = nextSignal();
		LOG.info("[%s] Processing sig: %s", name, sig.signal());
		
		switch(sig.signal()) {
			case BTN_LAMP_ON -> {
				directionBtnOn.put(sig.direction(),true);
				LOG.info("[%s]: %s Button is ON.", name,sig.direction());
			}
			case BTN_LAMP_OFF -> {
				directionBtnOn.put(sig.direction(),false);
				LOG.info("[%s]: %s Button is OFF.", name,sig.direction());
			}
			case DIR_LAMP_ON,DIR_LAMP_OFF -> activateLamp(sig.direction());
			case SIG_OFFLINE -> {
				LOG.info("[%s]: IS OFFLINE!", name);
				directionBtnOn.clear();
				directionLampOn.clear();
			}
			case SIG_ONLINE -> {
				init();
			}
		}
	}
	
	/**
	 * Activates a direction lamp to signal the arrival of the elevator.
	 * 
	 * Only one direction can be active at a time, and direction can be cleared.
	 * 
	 * @param dir The direction of the elevators travel
	 */
	private void activateLamp(Direction dir) {
		if(dir == null) {
			LOG.error("[%s] Direction Lamp received an incorrect direction (null)");
		}
		
		directionBtnOn.put(dir,false); //Turn off the button, request fulfilled
		
		for(Direction d : supportedDirections) {
			directionLampOn.put(d,dir==d);
			LOG.info("[%s]: %s Lamp is %s.", name,d,(dir==d ? "ON" : "OFF"));
		}
		
	}

	/**
	 * Convert data from dispatcher into the appropriate data type
	 * and then call a method that will add it to a queue for processing.
	 * 
	 * The queue is used to process asynchronously because the dispatcher
	 * thread will be calling and executing this method and would be tied up
	 * and unable to receive new messages while this runs. Thus we let the 
	 * floorSubsystem worker thread handle it.
	 * 
	 * @param topic The topic for the message received from the dispatcher
	 * @param data The data payload received from the dispatcher
	 */
	@Override
	public void receiveDispatch(String topic, String data) {
		
		
		try {
			FloorTopics convTopic = FloorTopics.valueOf(topic.substring(0, topic.lastIndexOf("_")));
			LOG.info("%s receives topic %s with %s JSON data.", name,convTopic,data);
			switch(convTopic) {
				case FLOOR_SIGNAL:
					FloorSignal signal = objMap.readValue(data, FloorSignal.class);
					receiveSignal(signal);
					break;
				default:
					LOG.warn("[%s] Subscribed topic %s has no conversion specified. Ignored.",name,convTopic);
					break;
			}
		} catch (JsonMappingException e) {
			LOG.error("[%s]: Unable to convert data payload.",name);
		} catch (JsonProcessingException e) {
			LOG.error("[%s]: Unable to convert data payload.",name);
			e.printStackTrace();
		}
	}

	@Override
	public String getSubscriberNameIdentifier() {
		return name;
	}
	

}