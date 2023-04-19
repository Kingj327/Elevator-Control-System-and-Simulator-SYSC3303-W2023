package C2G8_Project;

import java.net.SocketException;
import java.net.UnknownHostException;

import org.junit.jupiter.api.Test;

import C2G8_Project.ElevatorSystem;

class ElevatorSystemTest {

	/**
	 * @author JP
	 * Test that the elevator creates a determined amount of elevator, and that they can
	 * all identify their current states
	 */
	@Test
	void test() throws SocketException {
		ElevatorSystem e = null;
		try {
			e = new ElevatorSystem(1,50);
			
			Thread th = new Thread(e);
			th.setName("Elevator System");
			th.start();
			while(!th.isAlive()); //Wait for threads to set up
		} catch (UnknownHostException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		System.out.println("1 Elevator Made vs " + numElevatorTest(e) + " ElevatorSystem contains");
		System.out.println("Retrieving the 1 Elevator, and printing states:");
		printElevators(e);
		
		System.out.println("Elevator System Shutdown Test");
		System.out.println("Elevator System Active?: " + e.isRunning());
		System.out.println("Elevator Subsystem Active: " + !e.getElevator(0).getShutdown());
		
		System.out.println("Sending shutdown signal!");
		e.shutdown();
		
		System.out.println("Elevator System Active?: " + e.isRunning());
		System.out.println("Elevator Subsystem Active: " + !e.getElevator(0).getShutdown());
		
		try {
			e = new ElevatorSystem(4,50);
			
			Thread th = new Thread(e);
			th.setName("Elevator System");
			th.start();
			while(!th.isAlive()); //Wait for threads to set up
		} catch (UnknownHostException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		System.out.println("4 Elevators Made vs " + numElevatorTest(e) + " ElevatorSystem contains");
		System.out.println("Retrieving the 4 Elevator, and printing states:");
		printElevators(e);
		
		System.out.println("Elevator System Shutdown Test");
		System.out.println("Elevator System Active?: " + e.isRunning());
		System.out.println("Elevator Subsystem Active? ");
		elevSubsystemActive(e);
		
		System.out.println("Sending shutdown signal!");
		e.shutdown();
		
		System.out.println("Elevator System Active?: " + e.isRunning());
		System.out.println("Elevator Subsystem Active? " );
		elevSubsystemActive(e);
	}

	int numElevatorTest(ElevatorSystem e){
		return e.getNumElevators();
	}

	void printElevators(ElevatorSystem e){
		for(int i=0; i<numElevatorTest(e); i++){
			e.getElevator(i).printElevator();
		}
	}

	void elevSubsystemActive(ElevatorSystem e){
		for(int i=0; i<numElevatorTest(e); i++){
			ElevatorSubsystem es = e.getElevator(i);
			System.out.println("Elevator ID: " + es.getId() + " Active?: " + !es.getShutdown());
		}
	}
}
