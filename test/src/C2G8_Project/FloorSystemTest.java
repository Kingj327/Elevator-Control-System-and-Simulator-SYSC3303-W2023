package C2G8_Project;

import static org.junit.jupiter.api.Assertions.*;

import java.util.HashMap;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

import C2G8_Project.CONFIG;
import C2G8_Project.Destinations;
import C2G8_Project.Dispatcher;
import C2G8_Project.FloorSystem;
import C2G8_Project.FloorTopics;
import C2G8_Project.SchedulerTopics;
import C2G8_Project.UnregisteredDispatcherDestination;

class FloorSystemTest {
	
	Dispatcher schedulerDispatcher;
	DispatchSubscriberTester floorRequestSubscriber;
	FloorSystem floorSystem;
	Thread floorSystemThread;
	HashMap<Dispatcher,Thread> threads;
	ObjectMapper objMap;

	@BeforeAll
	static void setUpBeforeClass() throws Exception {
	}

	@AfterAll
	static void tearDownAfterClass() throws Exception {
	}
	
	@BeforeEach
	void setUp() throws Exception {
		threads = new HashMap<Dispatcher,Thread>();
		schedulerDispatcher = new Dispatcher(Destinations.SCHEDULER);
		threads.put(schedulerDispatcher, new Thread(schedulerDispatcher));
		threads.forEach((k,v) -> v.start());
		while( !schedulerDispatcher.isRunning()) {} //Wait for threads finish starting so we can get correct port readings
		floorRequestSubscriber = DispatchSubscriberTester.createSubscriberTester(schedulerDispatcher);
		schedulerDispatcher.subscribe(SchedulerTopics.FLOOR_REQUEST.toString(), floorRequestSubscriber);
		floorSystem = new FloorSystem(CONFIG.FLOORS,50); //FloorSystem created after mock scheduler chooses available port
		floorSystemThread = new Thread(floorSystem);
		floorSystemThread.setName(FloorSystem.FLOOR_SYSTEM_NAME);
		floorSystemThread.start();
		objMap = new ObjectMapper();
		while(!floorSystem.isRunning() ) {} //Wait for floorSystem to finish starting so we can get its port
		
		schedulerDispatcher.connectNewDestination(Destinations.FLOOR_SYSTEM, "localhost", floorSystem.getPort());
		
	}

	@AfterEach
	void tearDown() throws Exception {
		
		threads.forEach((k,v) -> k.shutdown());
		threads.clear();
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
	void scenarioTest() throws UnregisteredDispatcherDestination {
		DispatchSubscriberTester startScenarioSubscriber = DispatchSubscriberTester.createSubscriberTester(schedulerDispatcher);
		schedulerDispatcher.subscribe(SchedulerTopics.SCENARIO_STARTED.toString(), startScenarioSubscriber);
		sendWithDispatcher(schedulerDispatcher,Destinations.FLOOR_SYSTEM,FloorTopics.SCENARIO_START.toString(),null);
		while(startScenarioSubscriber.size() < 1) {};
		assertEquals(1,startScenarioSubscriber.size());
		DispatchSubscriberTester endScenarioSubcriber = DispatchSubscriberTester.createSubscriberTester(schedulerDispatcher);
		schedulerDispatcher.subscribe(SchedulerTopics.SCENARIO_COMPLETE.toString(), endScenarioSubcriber);
		while(!floorSystem.isScenarioRunning()) {}
		assertTrue(floorSystem.isScenarioRunning());
		while(endScenarioSubcriber.size() < 1) {};
		assertEquals(1,endScenarioSubcriber.size());
		assertFalse(floorSystem.isScenarioRunning());
		sendWithDispatcher(schedulerDispatcher,Destinations.FLOOR_SYSTEM,FloorTopics.SCENARIO_END.toString(),null);
		while(floorSystem.isRunning()) {}
		assertFalse(floorSystem.isRunning());
	}

}
