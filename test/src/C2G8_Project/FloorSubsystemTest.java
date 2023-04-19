package C2G8_Project;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import C2G8_Project.Direction;
import C2G8_Project.FloorSignal;
import C2G8_Project.FloorSignals;
import C2G8_Project.FloorSubsystem;
import C2G8_Project.FloorSystem;

class FloorSubsystemTest {
	ArrayList<FloorSubsystem> floors;
	HashMap<FloorSubsystem,Thread> floorThreads;
	FloorSystem parent;

	@BeforeAll
	static void setUpBeforeClass() throws Exception {
	}

	@AfterAll
	static void tearDownAfterClass() throws Exception {
	}

	@BeforeEach
	void setUp() throws Exception {
		parent = new FloorSystem(CONFIG.FLOORS);
		floors = parent.getFloors();
		floorThreads = new HashMap<FloorSubsystem,Thread>();
		
		for(FloorSubsystem fl : floors) {
			Thread th = new Thread(fl);
			floorThreads.put(fl,th);
			th.setName(fl.getName());
			th.start();
			while(!fl.isRunning()) {};
		}
		
	}

	@AfterEach
	void tearDown() throws Exception {
	}
	
	@Test
	void testSigDirLampOn() {
		assertFalse(floors.get(0).getDirectionLampOn().get(Direction.UP));
		floors.get(0).receiveSignal(new FloorSignal(FloorSignals.DIR_LAMP_ON,1,Direction.UP));
		assertTimeoutPreemptively(Duration.ofSeconds(1), () -> {
			while(floors.get(0).getDirectionLampOn().get(Direction.UP)==false) {}
		},"The expected condition never occured");
		assertTrue(floors.get(0).getDirectionLampOn().get(Direction.UP));
	}

}
