package C2G8_Project;

import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


/**
 * @author JP
 * Elevator System builds all the elevators and assigns a thread for each one.
 * When all the elevators are shutdown, the ElevatorSystem will then shutdown
 */
public class ElevatorSystem implements Runnable, DispatchConsumer{
    final private static Logger LOG =LogManager.getFormatterLogger(ElevatorSystem.class);
    final private Dispatcher dispatcher;
    final private Thread dispatchThread;
    private final int speedFactor; //timeAction divided by speedFactor, to speed up the system for testing/demos
    private final int POLL_INTERVAL_MICROSECONDS; //Time for thread to sleep in run() while loop to reduce load on cpu

    private ArrayList<Thread> elevSubThreads = new ArrayList<Thread>();
    private ArrayList<ElevatorSubsystem> elevators = new ArrayList<ElevatorSubsystem>();
    private int numElevators;

    private boolean isRunning = false; //Elevator System On/Off

    /**
     * 
     * @param numElevators Number of Elevators
     * @param schedulerAddr Schduler Address
     * @throws UnknownHostException If any issues with Scheduler Address
     * @throws SocketException If any issues with the Port
     */
    public ElevatorSystem(final int numElevators,final int elevatorSystemPort,final int speedFactor) throws UnknownHostException, SocketException{
        this.numElevators = numElevators;
        this.speedFactor = speedFactor;
        dispatcher = new Dispatcher(Destinations.ELEVATOR_SYSTEM,elevatorSystemPort);
        dispatchThread = new Thread(dispatcher);
        dispatchThread.setName(dispatcher.getName());
        POLL_INTERVAL_MICROSECONDS = 1000 / speedFactor;
        generateElevators();
    }
    
    public ElevatorSystem(final int numElevators,final int speedFactor) throws UnknownHostException, SocketException {
    	this(numElevators,Dispatcher.NO_PORT,speedFactor);
    }

    public ElevatorSubsystem getElevator(int index){
        return elevators.get(index);
    }

    public int getNumElevators(){
        return numElevators;
    }

    public int getPort(){
        return dispatcher.getPort();
    }
    
    public int getSpeedFactor() {
    	return  speedFactor;
    }

    /**
     * Makes numElevators amount of elevators each attached with a thread and added to a list
     * in order to start the threads later
     */
    private void generateElevators(){
        for(int i=0; i<numElevators; i++){
            ElevatorSubsystem e = new ElevatorSubsystem(i+1, 1, dispatcher,this);
            elevators.add(e);
            System.out.println("Created Elevator ID: " + (i+1) + " at floor 1");

            String elevName = String.format("ElevatorSubsystem %d", e.getId());
            
            Thread th = new Thread(e);
            th.setName(elevName);
            elevSubThreads.add(th);     
        }
    }

    /**
     * Starts the elevator threads and stops when shutdown() signal is sent
     */
    public void run(){
        dispatcher.subscribe(ElevatorTopics.SCENARIO_END.toString(), this);
        dispatchThread.start();
        
        for(Thread t: elevSubThreads){
            t.start();
        }
        
        LOG.info("%s is ready. Waiting for command from %s. Listening on port %d...",Destinations.ELEVATOR_SYSTEM,Destinations.SCHEDULER,dispatcher.getPort());	

        isRunning = true;
        while(isRunning && elevatorsAlive()){
            try{
                Thread.sleep(POLL_INTERVAL_MICROSECONDS);
            }catch(InterruptedException e){
                continue;
            }
        }
    }

    /**
     * Check if all elevators are alive
     * @return True if all elevators alive, false otherwise
     */
    public boolean elevatorsAlive(){
        for(Thread t: elevSubThreads){
            if(!t.isAlive()){
                return false;
            }
        }
        return true;
    }

    public boolean isRunning(){
        return isRunning;
    }

    /**
     * Sends the shutdown signal to all elevators and dispatcher
     */
    public void shutdown(){
        LOG.info("Elevator System Shutting Down");
        for(ElevatorSubsystem e: elevators){
            e.shutdown();
        }
        dispatcher.shutdown();
        isRunning = false;
    }

    /**
	 * Main takes a scheduler address from command line then setups up the Elevator System and Subsystems based on CONFIG file.
	 * It will connect to the scheduler at the provided address. If no address is provided it will use the localhost.
	 * 
	 * @param args Takes command line arguments. It is expecting at least one extra argument to be passed that has the scheduler's network address
	 */
    public static void main(String args[]){

        ElevatorSystem eSystem;
        try {
			eSystem = new ElevatorSystem(CONFIG.ELEVATORS,CONFIG.ELEVATOR_SYSTEM_PORT,CONFIG.SCENARIO_ACCELERATION_MULTIPLIER);
            Thread th = new Thread(eSystem);
            th.setName("Elevator System");
			th.start();
		} catch (NumberFormatException e) {
			LOG.error("Invalid scheduler address provided. Please provide a valid network address. GIVEN: %s", args[1]);
			e.printStackTrace();
			System.exit(1);
		} catch (UnknownHostException e) {
			LOG.error("Invalid port specified. Please provide an integer value as a port. GIVEN: %s", args[2]);
			e.printStackTrace();
			System.exit(1);
		} catch (SocketException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.exit(1);
		}
    }

    @Override
    public void receiveDispatch(String topic, String data) {
        ElevatorTopics signal = ElevatorTopics.valueOf(topic);
        if(signal == ElevatorTopics.SCENARIO_END){
        	this.shutdown();
        }
    }

    @Override
    public String getSubscriberNameIdentifier() {
        return "ElevatorSystem";
    }
}