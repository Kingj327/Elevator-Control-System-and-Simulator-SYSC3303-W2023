package C2G8_Project;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

import C2G8_Project.CONFIG;
import C2G8_Project.Destinations;
import C2G8_Project.Dispatcher;
import C2G8_Project.ElevatorSignal;
import C2G8_Project.ElevatorStates;
import C2G8_Project.ElevatorSubsystem;
import C2G8_Project.ElevatorSystem;
import C2G8_Project.ElevatorTopics;
import C2G8_Project.SchedulerTopics;
import C2G8_Project.UnregisteredDispatcherDestination;

/**
 * 
 * @author JP
 * Each elevator is their own instance, hence testing the functionality of one elevator
 * tests the functionality for all elevators.
 */
class ElevatorSubsystemTest {

	Dispatcher schedulerDispatcher;
	DispatchSubscriberTester schedulerSignalSubscriber;
	ElevatorSystem elevatorSys;
	Thread elevatorSystemThread;
	ElevatorSubsystem elevator;
	ObjectMapper objMap;

	@BeforeAll
	static void setUpBeforeClass() throws Exception {
	}

	@AfterAll
	static void tearDownAfterClass() throws Exception {
	}
	
	@BeforeEach
	void setUp() throws Exception {
		
	
		//Prepare systems with random ports
		schedulerDispatcher = new Dispatcher(Destinations.SCHEDULER);
		Thread th = new Thread(schedulerDispatcher);
		th.setName("Scheduler System");
		th.start();
		while(!schedulerDispatcher.isRunning()) {} //Wait for threads finish starting so we can get correct port readings
		elevatorSys = new ElevatorSystem(1,Dispatcher.NO_PORT,50);
		elevatorSystemThread = new Thread(elevatorSys);
		elevatorSystemThread.setName("Elevator System");
		elevatorSystemThread.start();
		while(!elevatorSys.isRunning()) {} //Wait for threads finish starting so we can get correct port readings
		schedulerSignalSubscriber = DispatchSubscriberTester.createSubscriberTester(schedulerDispatcher);
		schedulerDispatcher.registerDestination(Destinations.ELEVATOR_SYSTEM, "localhost", elevatorSys.getPort());
		schedulerDispatcher.subscribe(SchedulerTopics.ELEVATOR_SIGNAL.toString(),schedulerSignalSubscriber);
		schedulerDispatcher.connectNewDestination(Destinations.SCHEDULER, "localhost", elevatorSys.getPort());

		objMap = new ObjectMapper();
		while(!elevatorSys.isRunning()); //Wait for Esystem to finish starting so we can get its port
		elevator = elevatorSys.getElevator(0);
	}

	@AfterEach
	void tearDown() throws Exception {
		schedulerDispatcher.shutdown();
	}
	
	<E extends Enum<?>,T> void sendWithDispatcher(Dispatcher dis,E destination,String topic,T data) throws UnregisteredDispatcherDestination {
		Runnable run = new Runnable() {

			@Override
			public void run() {
				try {
					System.out.println("Sending");
					dis.sendData(destination, topic,data);
				} catch (UnregisteredDispatcherDestination e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		};
		Thread td = new Thread(run);
		td.setName("Data Runner");
		td.start();
		while(td.isAlive()) {}
		System.out.println("Sending complete");
	}

	@Test
	void scenarioTest() throws Exception {
		while(!schedulerSignalSubscriber.isRunning()) {}

		//Test Initial Start State by receiving State packet
		while(schedulerSignalSubscriber.isEmpty()) {
			System.out.print(""); //Program won't work without this??
		}
		ElevatorSignal signal = objMap.readValue(schedulerSignalSubscriber.receiveData().data(), ElevatorSignal.class);
		System.out.println("Initial State START vs " + elevator.getSignal());
		
		
		//Send IDLE State Response and see if it switches		
		signal = new ElevatorSignal(ElevatorStates.IDLE, signal.id(), signal.location(), signal.newCarBttns(), signal.carBttns(), null);
		sendWithDispatcher(schedulerDispatcher, Destinations.ELEVATOR_SYSTEM,String.format("%s_%d",ElevatorTopics.ELEVATOR_SIGNAL.toString(),signal.id()),signal);
		
		//Get the Latest signal from message Queue (Should be Idle)
		while(schedulerSignalSubscriber.isEmpty()) {
			System.out.print(""); //Program won't work without this??
		}
		signal = objMap.readValue(schedulerSignalSubscriber.receiveData().data(), ElevatorSignal.class);
		System.out.println("IDLE State sent, elevator State: " + elevator.getSignal());
		
		//Send MOVING_UP State Response and receive first Move Message
		signal = new ElevatorSignal(ElevatorStates.MOVING_UP, signal.id(), signal.location(), signal.newCarBttns(), signal.carBttns(), null);
		sendWithDispatcher(schedulerDispatcher, Destinations.ELEVATOR_SYSTEM,String.format("%s_%d",ElevatorTopics.ELEVATOR_SIGNAL.toString(),signal.id()),signal);
		while(schedulerSignalSubscriber.isEmpty()) {
			System.out.print(""); //Program won't work without this??
		}
		signal = objMap.readValue(schedulerSignalSubscriber.receiveData().data(), ElevatorSignal.class);
		System.out.println("MOVE_UP State sent, elevator State: " + elevator.getSignal());
		
		//We want to get to floor 6, so send stop signal at floor 5
		while(signal.location() != 6){
			
			//Send STOP State Response
			if(signal.location() == 5){
				signal = new ElevatorSignal(ElevatorStates.STOP, signal.id(), signal.location(), signal.newCarBttns(), signal.carBttns(), null);
				sendWithDispatcher(schedulerDispatcher, Destinations.ELEVATOR_SYSTEM,String.format("%s_%d",ElevatorTopics.ELEVATOR_SIGNAL.toString(),signal.id()),signal);
			}
			while(schedulerSignalSubscriber.isEmpty()) {
				System.out.print(""); //Program won't work without this??
			}
			signal = objMap.readValue(schedulerSignalSubscriber.receiveData().data(), ElevatorSignal.class);
			System.out.println("SIGNAL in MOVE_UP State: " + signal);
		}

		//Print the Signal (Should be STOP at Floor 6)
		System.out.println("After Reaching Destination, elevator should be in STOP State (Floor 6) vs " + elevator.getSignal());

		//Send MOVING_DOWN State Response
		signal = new ElevatorSignal(ElevatorStates.MOVING_DOWN, signal.id(), signal.location(), signal.newCarBttns(), signal.carBttns(), null);
		sendWithDispatcher(schedulerDispatcher, Destinations.ELEVATOR_SYSTEM,String.format("%s_%d",ElevatorTopics.ELEVATOR_SIGNAL.toString(),signal.id()),signal);
		while(schedulerSignalSubscriber.isEmpty()) {
			System.out.print(""); //Program won't work without this??
		}
		signal = objMap.readValue(schedulerSignalSubscriber.receiveData().data(), ElevatorSignal.class);
		System.out.println("MOVE_DOWN State sent, elevator State: " + elevator.getSignal());
		
		//We want to get to floor 3, so send stop signal at floor 4
		while(signal.location() != 3){
			//Send STOP State Response
			if(signal.location() == 4){
				signal = new ElevatorSignal(ElevatorStates.STOP, signal.id(), signal.location(), signal.newCarBttns(), signal.carBttns(), null);
				sendWithDispatcher(schedulerDispatcher, Destinations.ELEVATOR_SYSTEM,String.format("%s_%d",ElevatorTopics.ELEVATOR_SIGNAL.toString(),signal.id()),signal);
			}
			while(schedulerSignalSubscriber.isEmpty()) {
				System.out.print(""); //Program won't work without this??
			}
			signal = objMap.readValue(schedulerSignalSubscriber.receiveData().data(), ElevatorSignal.class);
			System.out.println("SIGNAL in MOVE_DOWN State: " + signal);
		}

		//Print the Signal (Should be STOP at Floor 3)
		System.out.println("After Reaching Destination, elevator should be in STOP State (Floor 3) vs " + elevator.getSignal());

		//Send OPEN_DOORS State Response
		signal = new ElevatorSignal(ElevatorStates.OPEN_DOORS, signal.id(), signal.location(), signal.newCarBttns(), signal.carBttns(), null);
		sendWithDispatcher(schedulerDispatcher, Destinations.ELEVATOR_SYSTEM,String.format("%s_%d",ElevatorTopics.ELEVATOR_SIGNAL.toString(),signal.id()),signal);
		while(schedulerSignalSubscriber.isEmpty()) {
			System.out.print(""); //Program won't work without this??
		}
		signal = objMap.readValue(schedulerSignalSubscriber.receiveData().data(), ElevatorSignal.class);
		System.out.println("OPEN_DOORS State sent vs Elevator State: " + elevator.getSignal());

		//Send CLOSE_DOORS State Response + 2 new requests [1,2]
		ArrayList<Integer> newBttns = new ArrayList<Integer>();
		newBttns.add(1);
		newBttns.add(2);
		signal = new ElevatorSignal(ElevatorStates.CLOSE_DOORS, signal.id(), signal.location(), newBttns, signal.carBttns(), null);
		sendWithDispatcher(schedulerDispatcher, Destinations.ELEVATOR_SYSTEM,String.format("%s_%d",ElevatorTopics.ELEVATOR_SIGNAL.toString(),signal.id()),signal);
		while(schedulerSignalSubscriber.isEmpty()) {
			System.out.print(""); //Program won't work without this??
		}
		signal = objMap.readValue(schedulerSignalSubscriber.receiveData().data(), ElevatorSignal.class);
		System.out.println("CLOSE_DOORS State sent (new Floor requests [1,2] vs Elevator State): " + signal); //Must print signal since, newCarBttns list is cleared once message sent

		//Add newCarBttns received from elevator to carBttns (ElevatorSignal)
		//Then send 3 new requests [1,2,3], and we should receive [3] back as newCarBttns from Elevator
		Set<Integer> bttns = new HashSet<Integer>(signal.newCarBttns());
		newBttns.add(3);
		signal = new ElevatorSignal(ElevatorStates.CLOSE_DOORS, signal.id(), signal.location(), newBttns, bttns, null);
		sendWithDispatcher(schedulerDispatcher, Destinations.ELEVATOR_SYSTEM,String.format("%s_%d",ElevatorTopics.ELEVATOR_SIGNAL.toString(),signal.id()),signal);
		while(schedulerSignalSubscriber.isEmpty()) {
			System.out.print(""); //Program won't work without this??
		}
		signal = objMap.readValue(schedulerSignalSubscriber.receiveData().data(), ElevatorSignal.class);
		System.out.println("CLOSE_DOORS State sent (new Floor requests [1,2,3] -> [3] vs Elevator State): " + signal); //Must print signal since, newCarBttns list is cleared once message sent
		


		//Test Auto Stop (Moving UP)
		System.out.println("Testing Auto Stop (Moving UP) (AutoStop @ Floor 10)");
		signal = new ElevatorSignal(ElevatorStates.MOVING_UP, signal.id(), signal.location(), signal.newCarBttns(), signal.carBttns(), null);
		sendWithDispatcher(schedulerDispatcher, Destinations.ELEVATOR_SYSTEM,String.format("%s_%d",ElevatorTopics.ELEVATOR_SIGNAL.toString(),signal.id()),signal);
		
		while(signal.state() != ElevatorStates.STOP) {
			while(schedulerSignalSubscriber.isEmpty()) {
				System.out.print(""); //Program won't work without this??
			}
			signal = objMap.readValue(schedulerSignalSubscriber.receiveData().data(), ElevatorSignal.class);
			System.out.println("In MovingUP until Auto-Stop Loop with signal: " + signal);
		}
		System.out.println("Signal Should be STOP: " + signal);
		


		//Test Auto Stop (Moving Down)
		System.out.println("Testing Auto Stop (Moving DOWN) (AutoStop @ Floor 1)");
		signal = new ElevatorSignal(ElevatorStates.MOVING_DOWN, signal.id(), signal.location(), signal.newCarBttns(), signal.carBttns(), null);
		sendWithDispatcher(schedulerDispatcher, Destinations.ELEVATOR_SYSTEM,String.format("%s_%d",ElevatorTopics.ELEVATOR_SIGNAL.toString(),signal.id()),signal);

		while(signal.state() != ElevatorStates.STOP) {
			while(schedulerSignalSubscriber.isEmpty()) {
				System.out.print(""); //Program won't work without this??
			}
			signal = objMap.readValue(schedulerSignalSubscriber.receiveData().data(), ElevatorSignal.class);
			System.out.println("In Moving DOWN until Auto-Stop Loop with signal: " + signal);
		}
		System.out.println("Signal Should be STOP: " + signal);



		//Test Error Elevator Stuck (MOVING UP)
		System.out.println("Testing Elevator Stuck Error while moving up");
		signal = new ElevatorSignal(ElevatorStates.MOVING_UP, signal.id(),signal.location(),signal.newCarBttns(),signal.carBttns(),ScenarioFaults.ELEVATOR_STUCK);
		sendWithDispatcher(schedulerDispatcher, Destinations.ELEVATOR_SYSTEM,String.format("%s_%d",ElevatorTopics.ELEVATOR_SIGNAL.toString(),signal.id()),signal);

		while(schedulerSignalSubscriber.isEmpty()) {
			System.out.print(""); //Program won't work without this??
		}
		signal = objMap.readValue(schedulerSignalSubscriber.receiveData().data(), ElevatorSignal.class);
		System.out.println("Initial Moving State should be received vs " + signal);
		
		while(schedulerSignalSubscriber.isEmpty()) {
			System.out.print(""); //Program won't work without this??
		}
		signal = objMap.readValue(schedulerSignalSubscriber.receiveData().data(), ElevatorSignal.class);
		System.out.println("Signal Received State should be ERROR_ELEVATOR_STUCK and Null fault vs " + signal);
		
		//Elevator Expects DEAD response State from Scheduler, and replies with DEAD subsequently
		System.out.println("Sending DEAD State to Elevator");
		signal = new ElevatorSignal(ElevatorStates.DEAD, signal.id(),signal.location(),signal.newCarBttns(),signal.carBttns(),null);
		sendWithDispatcher(schedulerDispatcher, Destinations.ELEVATOR_SYSTEM,String.format("%s_%d",ElevatorTopics.ELEVATOR_SIGNAL.toString(),signal.id()),signal);

		while(schedulerSignalSubscriber.isEmpty()) {
			System.out.print(""); //Program won't work without this??
		}
		signal = objMap.readValue(schedulerSignalSubscriber.receiveData().data(), ElevatorSignal.class);
		System.out.println("Signal Received State should be DEAD vs " + signal.state());
		
		//Move Elevator Up 2 for next Test
		signal = new ElevatorSignal(ElevatorStates.MOVING_UP, signal.id(),signal.location(),signal.newCarBttns(),signal.carBttns(),null);
		sendWithDispatcher(schedulerDispatcher, Destinations.ELEVATOR_SYSTEM,String.format("%s_%d",ElevatorTopics.ELEVATOR_SIGNAL.toString(),signal.id()),signal);
		//Floor 2
		while(schedulerSignalSubscriber.isEmpty()) {
			System.out.print(""); //Program won't work without this??
		}
		signal = objMap.readValue(schedulerSignalSubscriber.receiveData().data(), ElevatorSignal.class);
//		//Floor 3
//		while(schedulerSignalSubscriber.isEmpty()) {
//			System.out.print(""); //Program won't work without this??
//		}
//		signal = objMap.readValue(schedulerSignalSubscriber.receiveData().data(), ElevatorSignal.class);
		
		signal = new ElevatorSignal(ElevatorStates.STOP, signal.id(),signal.location(),signal.newCarBttns(),signal.carBttns(),null);
		sendWithDispatcher(schedulerDispatcher, Destinations.ELEVATOR_SYSTEM,String.format("%s_%d",ElevatorTopics.ELEVATOR_SIGNAL.toString(),signal.id()),signal);
		while(schedulerSignalSubscriber.isEmpty()) {
			System.out.print(""); //Program won't work without this??
		}
		signal = objMap.readValue(schedulerSignalSubscriber.receiveData().data(), ElevatorSignal.class);
		
		
		//Test Error Elevator Stuck (MOVING DOWN)
		System.out.println("Testing Elevator Stuck Error while moving down");
		signal = new ElevatorSignal(ElevatorStates.MOVING_DOWN, signal.id(),signal.location(),signal.newCarBttns(),signal.carBttns(),ScenarioFaults.ELEVATOR_STUCK);
		sendWithDispatcher(schedulerDispatcher, Destinations.ELEVATOR_SYSTEM,String.format("%s_%d",ElevatorTopics.ELEVATOR_SIGNAL.toString(),signal.id()),signal);

		while(schedulerSignalSubscriber.isEmpty()) {
			System.out.print(""); //Program won't work without this??
		}
		signal = objMap.readValue(schedulerSignalSubscriber.receiveData().data(), ElevatorSignal.class);
		System.out.println("Initial Moving State should be received vs " + signal);
		
		while(schedulerSignalSubscriber.isEmpty()) {
			System.out.print(""); //Program won't work without this??
		}
		signal = objMap.readValue(schedulerSignalSubscriber.receiveData().data(), ElevatorSignal.class);
		System.out.println("Signal Received State should be ERROR_ELEVATOR_STUCK and Null fault vs " + signal);
		
		//Elevator Expects DEAD response State from Scheduler, and replies with DEAD subsequently
		System.out.println("Sending DEAD State to Elevator");
		signal = new ElevatorSignal(ElevatorStates.DEAD, signal.id(),signal.location(),signal.newCarBttns(),signal.carBttns(),null);
		sendWithDispatcher(schedulerDispatcher, Destinations.ELEVATOR_SYSTEM,String.format("%s_%d",ElevatorTopics.ELEVATOR_SIGNAL.toString(),signal.id()),signal);

		while(schedulerSignalSubscriber.isEmpty()) {
			System.out.print(""); //Program won't work without this??
		}
		signal = objMap.readValue(schedulerSignalSubscriber.receiveData().data(), ElevatorSignal.class);
		System.out.println("Signal Received State should be DEAD vs " + signal.state());



		//Test Error Door Stuck (OPEN DOORS)
		System.out.println("Testing Door Stuck Error (Open Doors)");
		signal = new ElevatorSignal(ElevatorStates.OPEN_DOORS, signal.id(),signal.location(),signal.newCarBttns(),signal.carBttns(),ScenarioFaults.DOOR_STUCK);
		sendWithDispatcher(schedulerDispatcher, Destinations.ELEVATOR_SYSTEM,String.format("%s_%d",ElevatorTopics.ELEVATOR_SIGNAL.toString(),signal.id()),signal);

		while(schedulerSignalSubscriber.isEmpty()) {
			System.out.print(""); //Program won't work without this??
		}
		signal = objMap.readValue(schedulerSignalSubscriber.receiveData().data(), ElevatorSignal.class);
		System.out.println("Signal Received State should be ERROR_DOOR_STUCK and Null fault vs " + signal);
		
		//Elevator Expects REBOOT response State from Scheduler, and replies with previous state (OPEN_DOORS) subsequently
		System.out.println("Sending REBOOT State to Elevator");
		signal = new ElevatorSignal(ElevatorStates.REBOOT, signal.id(),signal.location(),signal.newCarBttns(),signal.carBttns(),null);
		sendWithDispatcher(schedulerDispatcher, Destinations.ELEVATOR_SYSTEM,String.format("%s_%d",ElevatorTopics.ELEVATOR_SIGNAL.toString(),signal.id()),signal);

		while(schedulerSignalSubscriber.isEmpty()) {
			System.out.print(""); //Program won't work without this??
		}
		signal = objMap.readValue(schedulerSignalSubscriber.receiveData().data(), ElevatorSignal.class);
		System.out.println("Signal Received State should be OPEN_DOORS vs " + signal.state());
		


		//Test Error Door Stuck (CLOSE DOORS)
		System.out.println("Testing Door Stuck Error (Close Doors)");
		signal = new ElevatorSignal(ElevatorStates.CLOSE_DOORS, signal.id(),signal.location(),signal.newCarBttns(),signal.carBttns(),ScenarioFaults.DOOR_STUCK);
		sendWithDispatcher(schedulerDispatcher, Destinations.ELEVATOR_SYSTEM,String.format("%s_%d",ElevatorTopics.ELEVATOR_SIGNAL.toString(),signal.id()),signal);

		while(schedulerSignalSubscriber.isEmpty()) {
			System.out.print(""); //Program won't work without this??
		}
		signal = objMap.readValue(schedulerSignalSubscriber.receiveData().data(), ElevatorSignal.class);
		System.out.println("Signal Received State should be ERROR_DOOR_STUCK and Null fault vs " + signal);
		
		//Elevator Expects REBOOT response State from Scheduler, and replies with previous state (OPEN_DOORS) subsequently
		System.out.println("Sending REBOOT State to Elevator");
		signal = new ElevatorSignal(ElevatorStates.REBOOT, signal.id(),signal.location(),signal.newCarBttns(),signal.carBttns(),null);
		sendWithDispatcher(schedulerDispatcher, Destinations.ELEVATOR_SYSTEM,String.format("%s_%d",ElevatorTopics.ELEVATOR_SIGNAL.toString(),signal.id()),signal);

		while(schedulerSignalSubscriber.isEmpty()) {
			System.out.print(""); //Program won't work without this??
		}
		signal = objMap.readValue(schedulerSignalSubscriber.receiveData().data(), ElevatorSignal.class);
		System.out.println("Signal Received State should be CLOSE_DOORS vs " + signal.state());
	}
}
