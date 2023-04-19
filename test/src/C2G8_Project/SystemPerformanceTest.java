/**
 * 
 */
package C2G8_Project;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;

import org.apache.commons.lang3.time.StopWatch;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author Jordan
 *
 * A test that run the entire program 30 times and records its data to a file. Also created
 * a file called "InputFiole - DO NOT DELETE" or something along those lines. This can be 
 * deleted if the test isn't running.
 */
class SystemPerformanceTest {
	
	boolean runTest = false; /*** MAKE THIS TREU TO RUN THE TEST (false so it doesn't run while running the test suite) ***/
	
	File inputFile, savedInputFile, outputFile;
	boolean renamed;
	FileWriter fileWriter;
	
	private StopWatch timer;
	
	ArrayList<Thread> threads;
	String floorAddress = "127.0.0.1";
	String elevatorAddress = "127.0.0.1";
	String schedulerAddress = "127.0.0.1";
	
	/**
	 * @throws java.lang.Exception
	 */
	@BeforeEach
	void setUp() throws Exception {
		if (runTest) {
			inputFile = new File(String.format("%s\\%s", System.getProperty("user.dir"), CONFIG.REQUEST_FILE));
			savedInputFile = new File(String.format("%s\\%s - DO NOT DELETE - Renamed during SystemPerformanceTest Execution.txt", System.getProperty("user.dir"), CONFIG.REQUEST_FILE.replace(".txt", "")));
			
			renamed = renameFile(inputFile, savedInputFile);
	//		if (CONFIG.REQUEST_FILE.equalsIgnoreCase("InputFile.txt")) {
	//			System.out.println("Error in SystemPerformanceTest.java: This test method repeatedly deletes the input file in order to "
	//					+ "test different inputs. To ensure that a desired input file is not being deleted, please");
	//		}
			// Used to write to new files (Comment out if not needed)
	//	    String dateTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));
	//		file = new File(String.format("%s_-_SYSC3303C_System_Performance_Test_Data.txt", dateTime));
			
			// Used to write to the same file (Comment out if not needed)
			outputFile = new File(String.format("SYSC3303C_System_Performance_Test_Data.txt"));
			
			fileWriter = new FileWriter(outputFile);
		}
	}

	/**
	 * @throws java.lang.Exception
	 */
	@AfterEach
	void tearDown() throws Exception {
		if (runTest) {
			boolean result = false;
			if (renamed) {
				result = renameFile(savedInputFile, inputFile);
				if (!result) {
					System.out.println("Error in SystemPerformanceTest.txt: Original Input File could not be changed back to normal. \n"+savedInputFile.toPath()+"\n"+inputFile.toPath());
				}
			} else { System.out.println("NOT RENAMED"); }
			
			
			
			fileWriter.close();
		}
	}

	@Test
	void test() {
		if (runTest) {
			int portOffset = 0;
			
			// Create Timer
			timer = new StopWatch();
			timer = StopWatch.create();
			
			for (int i = 0; i < 30; i++) {
				timer.reset();
				
				// Create Floor and Elevator systems in new threads
				threads = new ArrayList<Thread>();
				try {
					threads.add(new Thread(new FloorSystem(CONFIG.FLOORS,CONFIG.FLOOR_SYSTEM_PORT+portOffset, CONFIG.SCENARIO_ACCELERATION_MULTIPLIER)));
					threads.add(new Thread(new ElevatorSystem(CONFIG.ELEVATORS,CONFIG.ELEVATOR_SYSTEM_PORT+portOffset,CONFIG.SCENARIO_ACCELERATION_MULTIPLIER)));
				} catch (Exception e) {
					System.out.println("Exception in SystemPerformanceTest: "+e);
				}
				// Start Floor and Elevator systems
				for (Thread t : threads) {
					t.start();
				}
				
				// Create Scheduler in new thread
				try {
					threads.add(new Thread(new Scheduler(floorAddress,CONFIG.FLOOR_SYSTEM_PORT+portOffset,elevatorAddress,CONFIG.ELEVATOR_SYSTEM_PORT+portOffset,CONFIG.SCHEDULER_PORT+portOffset)));
				} catch (Exception e) {
					e.printStackTrace();
				}
				
				
				// Start timer and run scheduler
				timer.start();
				threads.get(2).start();
				while(threads.get(0).isAlive() || threads.get(1).isAlive() || threads.get(2).isAlive()) {
					// Do nothing
				}
				timer.stop();
				
				// Write time to file
				try {
					fileWriter.write(timer.getTime()+"\n");
				} catch (IOException e) {
					e.printStackTrace();
				}
				
				// Prepare for the next test (clean up variables)
				threads.clear();
				portOffset += 3;
				try {
					Files.deleteIfExists(inputFile.toPath());	// Remove the newly created file for the test
				} catch (IOException e) {
					System.out.println("Error in SystemPerformanceTest: The input file could not be deleted after iteration "+(i+1));
				}	
			}
	//		System.out.println("\n\n\nGetting user directory..."+System.getProperty("user.dir"));
	//		System.out.println("Time: "+timer.getTime());
	//		System.out.println("Time: "+timer.getNanoTime());
	//		System.out.println("Time: "+timer.getSplitNanoTime());
		}
	}
	
	boolean renameAndDeleteOriginalFile(File originalFile, File newFile) {
		boolean renamedResult = renameFile(originalFile, newFile);
		if (renamedResult) {
			boolean fileDeletionResult = deleteFile(originalFile);
			return fileDeletionResult;
		}
		return false;
	}
	
	boolean renameFile(File originalFile, File newFile) {
		boolean renamedResult = originalFile.renameTo(newFile);
		if (renamedResult) {
			System.out.println(String.format("Original file successfully renamed from %s to %s.", originalFile.getName(), newFile.getName()));
			return true;
		} else {
			System.out.println(String.format("Original file NOT successfully renamed from %s to %s.", originalFile.getName(), newFile.getName()));
			return false;
		}
	}
	
	boolean deleteFile(File originalFile) {
		boolean fileDeletionResult = false;
		try {
			fileDeletionResult = Files.deleteIfExists(originalFile.toPath());
		} catch (IOException e) {
//			fileDeletionResult = false;
		}
		
		if (fileDeletionResult) {
			System.out.println(String.format("Original file successfully deleted at %s.", originalFile.toPath()));
			return true;
		} else {
			System.out.println(String.format("Original file NOT successfully deleted at %s.", originalFile.toPath()));
			return false;
		}
	}

}
