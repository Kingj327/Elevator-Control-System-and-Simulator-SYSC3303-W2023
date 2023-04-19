package C2G8_Project;

import static org.junit.jupiter.api.Assertions.*;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.Duration;
import java.util.HashMap;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import C2G8_Project.Destinations;
import C2G8_Project.Direction;
import C2G8_Project.Dispatcher;
import C2G8_Project.DispatcherMessage;
import C2G8_Project.FloorSignal;
import C2G8_Project.FloorSignals;
import C2G8_Project.UnregisteredDispatcherDestination;

class DispatcherTest {
	
	Dispatcher schedulerDispatcher;
	Dispatcher floorDispatcher;
	Dispatcher elevatorDispatcher;
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
		schedulerDispatcher = new Dispatcher(Destinations.SCHEDULER);
		floorDispatcher = new Dispatcher(Destinations.FLOOR_SYSTEM);
		elevatorDispatcher = new Dispatcher(Destinations.ELEVATOR_SYSTEM);
		objMap = new ObjectMapper();
		threads = new HashMap<Dispatcher,Thread>();
		threads.put(schedulerDispatcher, new Thread(schedulerDispatcher));
		threads.put(floorDispatcher, new Thread(floorDispatcher));
		threads.put(elevatorDispatcher, new Thread(elevatorDispatcher));
		threads.forEach((k,v) -> v.start());
		while( !schedulerDispatcher.isRunning() || !floorDispatcher.isRunning() || !elevatorDispatcher.isRunning() ) {} //Wait for threads finish starting
	}

	@AfterEach
	void tearDown() throws Exception {
		threads.forEach((k,v) -> k.shutdown());
		threads.clear();
	}
	
	void registerDestinationsManually() throws UnknownHostException {
		schedulerDispatcher.registerDestination(Destinations.FLOOR_SYSTEM, "localhost",floorDispatcher.getPort());
		schedulerDispatcher.registerDestination(Destinations.ELEVATOR_SYSTEM, "localhost",elevatorDispatcher.getPort());
		floorDispatcher.registerDestination(Destinations.SCHEDULER, "localhost", schedulerDispatcher.getPort());
		elevatorDispatcher.registerDestination(Destinations.SCHEDULER,"localhost", schedulerDispatcher.getPort());
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

	/**
	 * Tests Message sending and receiving using Dispatcher infrastructure.
	 * 
	 * NOTE: This must be done in a single test because DatagramSocket's cannot set SO_REUSEADDR on the socket before binding.
	 * This will result in an error when a new socket attempts to re-bind the same port and address in the next test while the
	 * address is in the cooldown and is unable to be re-assigned without the flag.  
	 * @throws UnknownHostException 
	 */
	@Test
	void testStringData() throws UnregisteredDispatcherDestination, JsonMappingException, JsonProcessingException, UnknownHostException {
		registerDestinationsManually();
		String testTopic = "test";
		String testMessage = "MyMESSAGE";
		DispatchSubscriberTester schSub = DispatchSubscriberTester.createSubscriberTester(schedulerDispatcher);
		DispatchSubscriberTester floorSub = DispatchSubscriberTester.createSubscriberTester(floorDispatcher);
		DispatchSubscriberTester elevatorSub = DispatchSubscriberTester.createSubscriberTester(elevatorDispatcher);
		schedulerDispatcher.subscribe(testTopic, schSub);
		floorDispatcher.subscribe(testTopic, floorSub);
		elevatorDispatcher.subscribe(testTopic, elevatorSub);
		sendWithDispatcher(elevatorDispatcher,Destinations.SCHEDULER,testTopic,testMessage);
		assertTimeoutPreemptively(Duration.ofSeconds(10), () -> {
			DispatcherMessage msg = schSub.receiveData();
			assertEquals(testTopic,msg.topic());
			assertEquals(testMessage,msg.data());
		},"The expected condition never occured");
	}
	
	@Test
	void testBinaryData() throws UnregisteredDispatcherDestination, UnknownHostException {
		registerDestinationsManually();
		String testTopic2 = FloorSignals.BTN_LAMP_ON.toString();
		FloorSignal testMessage2 = new FloorSignal(FloorSignals.BTN_LAMP_ON,5,Direction.UP);
		DispatchSubscriberTester floorSub = DispatchSubscriberTester.createSubscriberTester(floorDispatcher);
		floorDispatcher.subscribe(testTopic2, floorSub);
		schedulerDispatcher.sendData(Destinations.FLOOR_SYSTEM, testTopic2, testMessage2);
		assertTimeoutPreemptively(Duration.ofSeconds(10), () -> {
			DispatcherMessage msg2 = floorSub.receiveData();
			FloorSignal recData = objMap.readValue(msg2.data(), FloorSignal.class);
			assertEquals(testTopic2,msg2.topic());
			assertEquals(testMessage2.signal(),recData.signal());
			assertEquals(testMessage2.floor(),recData.floor());
			assertEquals(testMessage2.direction(),recData.direction());
		},"The expected condition never occured");
	}
	
	@Test
	void testDispatcherConnect() throws UnknownHostException, UnregisteredDispatcherDestination {
		assertFalse(schedulerDispatcher.isDestinationRegistered(Destinations.FLOOR_SYSTEM));
		assertFalse(schedulerDispatcher.isDestinationRegistered(Destinations.ELEVATOR_SYSTEM));
		assertFalse(floorDispatcher.isDestinationRegistered(Destinations.SCHEDULER));
		assertFalse(elevatorDispatcher.isDestinationRegistered(Destinations.SCHEDULER));
		schedulerDispatcher.connectNewDestination(Destinations.FLOOR_SYSTEM, "localhost", floorDispatcher.getPort());
		assertTrue(schedulerDispatcher.isDestinationRegistered(Destinations.FLOOR_SYSTEM));
		assertTrue(floorDispatcher.isDestinationRegistered(Destinations.SCHEDULER));
		schedulerDispatcher.connectNewDestination(Destinations.ELEVATOR_SYSTEM, "localhost", elevatorDispatcher.getPort());
		assertTrue(schedulerDispatcher.isDestinationRegistered(Destinations.ELEVATOR_SYSTEM));
		assertTrue(elevatorDispatcher.isDestinationRegistered(Destinations.SCHEDULER));
	}
}
