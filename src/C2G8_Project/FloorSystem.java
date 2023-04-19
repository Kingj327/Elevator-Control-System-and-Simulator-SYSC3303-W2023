package C2G8_Project;

import java.net.InetAddress;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.time.StopWatch;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Floor System is the executable program that controls the Floor System and the simulation's timing. 
 * It will setup a number of floors based on the configuration settings and read a simulation scenario
 * file or generate a new one if its missing. The scenario file contains a list of timings regarding
 * when an arbitrary person arrives on a floor and makes an elevator request, and what their destination
 * floor will be.
 * 
 * The floor simulation takes all these passenger requests and delegates them to the appropriate
 * floor subsystem of origin. 
 * 
 * The floor system also setups up a dispatcher and registers a connection to the scheduler depending
 * on the arguments used when the program is started. If no arguments are provided then the FloorSystem
 * will assume the scheduler will use the local host. The ports used are predetermined by the program
 * configuration.
 * 
 * @author Jayson Mendoza
 *
 */
public class FloorSystem implements Runnable, DispatchConsumer {
	final private static Logger LOG =LogManager.getFormatterLogger(FloorSystem.class);
	final private static String FLOOR_THREAD_PREFIX = "FloorSubsystem";
	final public static String FLOOR_SYSTEM_NAME = "FloorSystem";
	final private HashSet<FloorSubsystem> hasRequests = new HashSet<FloorSubsystem>();
	final private ArrayList<FloorSubsystem> floors = new ArrayList<FloorSubsystem>();
	final private HashMap<FloorSubsystem,Thread> threads = new HashMap<FloorSubsystem,Thread>();
	final private Dispatcher dispatcher;
	final private Thread dispatchThread;
	private volatile boolean isRunning = false;
	private volatile boolean isScenarioRunning = false;
	private volatile boolean isHasRequestsLocked = false;
	private final int ACCELERATION_MULTIPLIER;
	public final int POLL_INTERVAL_MICROSECONDS;
	private StopWatch watch; 
	
	/**
	 * Creates a new FloorSystem and connects it to the scheduler specified. It will generate a number
	 * of floors depending on the given input. The request sequence initiated is specified by a file
	 * read with its filename specified in the CONFIG class.
	 * 
	 * @throws UnknownHostException When the provided scheduler address cannot be resolved 
	 * 
	 */
	/**
	 * Creates a new FloorSystem and connects it to the scheduler specified. It will generate a number
	 * of floors depending on the given input. The request sequence initiated is specified by a file
	 * read with its filename specified in the CONFIG class.
	 * 
	 * @param numFloors The number of floors to create for the scenario
	 * @param schedulerAddr A string for the host path or IP where the scheduler resides
	 * @param schedulerPort The port at which the Scheduler will reside
	 * @param floorSystemPort The port at which the FloorSystem will listen. If Dispatcher.NO_FLOOR is passed it will choose any available port.
	 * @throws UnknownHostException If the provided scheduler address cannot be resolved on the network
	 * @throws SocketException 
	 * @throws IncompatableInputFile 
	 */
	public FloorSystem(final int numFloors,final int floorSystemPort,final int acclerationMultiplier) throws UnknownHostException, SocketException, IncompatableInputFile {
		this.ACCELERATION_MULTIPLIER =  acclerationMultiplier;
		POLL_INTERVAL_MICROSECONDS = this.ACCELERATION_MULTIPLIER/CONFIG.SCENARIO_ACCELERATION_MULTIPLIER;
		dispatcher = new Dispatcher("FloorSystemDispatcher",floorSystemPort);
		dispatchThread = new Thread(dispatcher);
		dispatchThread.setName(dispatcher.getName());
		generateFloors(numFloors);
		watch = StopWatch.create();

		initRequestSequence();
	}
	
	/**
	 * Creates a new FloorSystem and connects it to the scheduler specified. It will generate a number
	 * of floors depending on the given input. The request sequence initiated is specified by a file
	 * read with its filename specified in the CONFIG class.
	 * 
	 * @param schedulerAddr A string for the host path or IP where the scheduler resides
	 * @throws UnknownHostException If the provided scheduler address cannot be resolved on the network
	 * @throws SocketException 
	 * @throws IncompatableInputFile 
	 */
	public FloorSystem(final int numFloors) throws UnknownHostException, SocketException, IncompatableInputFile {
		this(CONFIG.FLOORS,CONFIG.FLOOR_SYSTEM_PORT,CONFIG.SCENARIO_ACCELERATION_MULTIPLIER);
	}
	
	/**
	 * Creates a new FloorSystem and connects it to the scheduler specified. It will generate a number
	 * of floors depending on the given input. The request sequence initiated is specified by a file
	 * read with its filename specified in the CONFIG class.
	 * 
	 * @param numFloors The number of floors to create for the scenario
	 * @param schedulerPort The port at which the Scheduler will reside
	 * @throws UnknownHostException If the provided scheduler address cannot be resolved on the network
	 * @throws SocketException 
	 * @throws IncompatableInputFile 
	 */
	public FloorSystem(final int numFloors,final int acclerationMultiplier) throws UnknownHostException, SocketException, IncompatableInputFile {
		this(numFloors,Dispatcher.NO_PORT,acclerationMultiplier);
	}
	
	/*
	 * Dangerous function which exists only for testing.
	 * Do not use this function outside of testing since
	 * the internal elements of the array should not be 
	 * exposed
	 */
	public ArrayList<FloorSubsystem> getFloors() {
		return floors;
	}
	
	

	public int getPOLL_INTERVAL_MICROSECONDS() {
		return POLL_INTERVAL_MICROSECONDS;
	}

	/**
	 * Used to get a watch that provides scenario time
	 * in milliseconds. Timer starts only when scenario
	 * is running.
	 * @return
	 */
	public long getScenarioTimeMilliseconds() {
		return watch.getTime(TimeUnit.MILLISECONDS)*ACCELERATION_MULTIPLIER;
	}
	
	

	public boolean isRunning() {
		return isRunning;
	}
		
	public boolean isScenarioRunning() {
		return isScenarioRunning;
	}

	public int getPort() {
		return dispatcher.getPort();
	}

	/**
	 * Main takes a scheduler address from command line then setups up the FloorSystem and subsystems based on program CONFIG.
	 * It will connect to the scheduler at the provided adderss. If no address is provided it will use the localhost.
	 * 
	 * @param args Takes command line arguments. It is expecting at least one extra argument to be passed that has schedulers network address
	 */
	public static void main(String[] args) throws Exception{
		FloorSystem prog;
		try {
			prog = new FloorSystem(CONFIG.FLOORS);
			Thread th = new Thread(prog);
			th.setName(FLOOR_SYSTEM_NAME);
			th.start();
		} catch (NumberFormatException e) {
			LOG.error("Invalid scheduler address provided. Please provide a valid network address. GIVEN: %s", args[1]);
			e.printStackTrace();
			throw e;
		} catch (UnknownHostException e) {
			LOG.error("Invalid port specified. Please provide an integer value as a port. GIVEN: %s", args[2]);
			e.printStackTrace();
			throw e;
		}
	}

	/**
	 * Run loop starts simulation then continues running until a shutdown command is received.
	 */
	@Override
	public void run() {
		
		dispatchThread.start();
		dispatcher.subscribe(FloorTopics.SCENARIO_START.toString(), this);
		dispatcher.subscribe(FloorTopics.SCENARIO_END.toString(), this);

		while(!dispatcher.isRunning() && dispatchThread.isAlive()) {};

		isRunning = true;
		LOG.info("%s is ready. Waiting for %s command from %s. Listening on port%d...",FLOOR_SYSTEM_NAME,FloorTopics.SCENARIO_START,Destinations.SCHEDULER,dispatcher.getPort());			
		
		//This loop prevents the program from ending until the simulation is finished. 
		while(isRunning) {
			if(!dispatchThread.isAlive()) {
				shutdown();
			}
			try {
				Thread.sleep(POLL_INTERVAL_MICROSECONDS);
			} catch (InterruptedException e) {
				continue;
			}
		}
	}
	
	/**
	 * Start scenario once appropriate message received from Scheduler. This should happen only once.
	 * @throws UnregisteredDispatcherDestination 
	 */
	private void startScenario() {
		if(!isRunning) {
			LOG.warn("%s signal ignored. %s is still starting up.",FloorTopics.SCENARIO_START,FLOOR_SYSTEM_NAME);
			return;
		}
		else if(isScenarioRunning) {
			return;
		}
		
		LOG.info("%s command received from Scheduler. Starting %s...",FloorTopics.SCENARIO_START,FLOOR_SYSTEM_NAME);
		for(Thread th : threads.values()) {
			th.start();
		}
		watch.start();
		isScenarioRunning = true;
		try {
			String topic =  SchedulerTopics.SCENARIO_STARTED.toString();
			dispatcher.sendData(Destinations.SCHEDULER, topic);
			dispatcher.unSubscribe(FloorTopics.SCENARIO_START.toString(), this);
			LOG.info("Startup Complete. Signaling scheduler that scenario has started with topic %s", topic);
		} catch (UnregisteredDispatcherDestination e) {
			LOG.error("Unable to send Scenario started message. Destination for %s doesn't exist",Destinations.SCHEDULER);
			e.printStackTrace();
			throw e;
		}
	}
	
	/**
	 * Informs the scheduler that the scenario is complete and there are no more requests scheduled to be dispatched.
	 * @throws UnregisteredDispatcherDestination 
	 */
	private void scenarioComplete() {
		LOG.info("All FloorSubsystems report requests exausted. Signaling Scheduler %s",SchedulerTopics.SCENARIO_COMPLETE);
		try {
			dispatcher.sendData(Destinations.SCHEDULER, SchedulerTopics.SCENARIO_COMPLETE.toString());
		} catch (UnregisteredDispatcherDestination e) {
			LOG.error("Unable to send Scenario complete message. Destination for %s doesn't exist",Destinations.SCHEDULER);
			e.printStackTrace();
			throw e;
		}
		isScenarioRunning = false;
	}

	/**
	 * Shut down the program
	 */
	private void shutdown() {
		LOG.info("%s command recieved from scheduler. Ending shutting down %s",FloorTopics.SCENARIO_END,FLOOR_SYSTEM_NAME);
		if(watch.isStarted()) {
			watch.stop();
		}
		for(FloorSubsystem floor : floors) {
			floor.shutdown();
		}
		dispatcher.shutdown();
		isRunning=false;
	}
	
	
	/**
	 * Initializes a scenario but creating a request sequence from the file specified in CONFIG.
	 * If the file is missing a new scenario is generated and saved.
	 * @throws IncompatableInputFile 
	 */
	private void initRequestSequence() throws IncompatableInputFile {
		//TODO Switch to make input form file the default option
		ArrayList<PassengerRequest> reqList = PassengerRequest.loadScenario();
		LOG.info("Scenario Schedule (%d requests)",reqList.size());
		
		//Assumes data in descending order so oldest will be at the bottom of stack
		for(PassengerRequest req : reqList) {
			FloorSubsystem fs = floors.get(req.getCurrentFloor()-1);
			fs.pushRequest(req);
			hasRequests.add(fs);
		}
    }
	
	/**
	 * Generates all floors, but doesn't start them.
	 * @param numFloors
	 */
	private void generateFloors(final int numFloors) {
		for(int i=0;i<numFloors;++i) {
			Direction[] supportedDirections;
			if(i == 0) {
				supportedDirections  = new Direction[] {Direction.UP};
			}
			else if(i == numFloors-1) {
				supportedDirections  = new Direction[] {Direction.DOWN};
			}
			else {
				supportedDirections = Direction.values();
			}
			String floorName = String.format("%s %d", FLOOR_THREAD_PREFIX,i+1);
			
			FloorSubsystem fs = new FloorSubsystem(floorName,i+1,this,dispatcher,supportedDirections);
			Thread th = new Thread(fs);
			th.setName(floorName);
			floors.add(fs);
			threads.put(fs,th);
			
		}
	}

	@Override
	public void receiveDispatch(String topic, String data) {
		FloorTopics sig = FloorTopics.valueOf(topic);
		switch(sig) {
			case SCENARIO_START -> startScenario();
			case SCENARIO_END -> shutdown();
		default -> LOG.warn("%s received an unrecognized topic from scheduler",FLOOR_SYSTEM_NAME,topic);
		}
	}

	@Override
	public String getSubscriberNameIdentifier() {
		return FLOOR_SYSTEM_NAME;
	}
	
	/**
	 * This function is called by FloorSubysystems to indicate its exhausted its scenario requests
	 * @param fs The floor subsystem that is reporting in
	 */
	public synchronized void reportRequestsEmpty(FloorSubsystem fs) {
		
		while(isHasRequestsLocked) {
			try {
				wait();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		isHasRequestsLocked = true;
		hasRequests.remove(fs);
		if(hasRequests.isEmpty()) {
			scenarioComplete();
		}
		isHasRequestsLocked=false;
		notifyAll();
	}

}

