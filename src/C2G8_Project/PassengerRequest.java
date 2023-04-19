/**
 * 
 */
package C2G8_Project;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Objects;
import java.util.Random;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Class represents a record for a passenger request within the scenario.
 * 
 * Class static methods contain factory to create scenario with given duration and number of people.
 * @author Jayson Mendoza
 *
 */
final class PassengerRequest implements Serializable, Comparable<PassengerRequest>{
	/**
	 * 
	 */
	private static final int NUM_INPUT_FILE_COLUMNS = 5;
	private final static Logger LOG =LogManager.getFormatterLogger(PassengerRequest.class);
	private static final long serialVersionUID = 3210607718970269460L;
	private final long scenarioDurationMills;
	private final int currentFloor;

	private final Direction direction;
	private final int targetFloor;
	private final ScenarioFaults faultType;
    
    private PassengerRequest(long scenarioDurationMills, int currentFloor, Direction direction, int targetFloor,final ScenarioFaults faultType) throws IncompatableInputFile {
		super();
		if(targetFloor > CONFIG.FLOORS || currentFloor > CONFIG.FLOORS) {
			String errMsg = String.format("The scenario input file \"%s\" is incompatable with the current scenario configuration with %d floors.", CONFIG.REQUEST_FILE,CONFIG.FLOORS);
			LOG.error(errMsg);
			throw new IncompatableInputFile(errMsg);
		}
		this.scenarioDurationMills = scenarioDurationMills;
		this.currentFloor = currentFloor;
		this.direction = direction;
		this.targetFloor = targetFloor;
		this.faultType = faultType;
		
	}
    private PassengerRequest(long scenarioDurationMills, int currentFloor, Direction direction, int targetFloor) throws IncompatableInputFile {
    	this(scenarioDurationMills,currentFloor,direction,targetFloor,null);
    }
    /**
     * Load a scenario by first fetching the default file. If the file is not present
     * this will print warnings in the log and then save then load a newly generated scenario.
     * If the file existed but was corrupted a warning will print advising the deletion of the file
     * along with possible causes and then a new scenario will be generated and loaded but the file
     * will not be replaced. 
     * @return
     * @throws IncompatableInputFile 
     */
	public static ArrayList<PassengerRequest> loadScenario() throws IncompatableInputFile {
		ArrayList<PassengerRequest> scenario = null;
		boolean isCorrputed = false;
		try {
			scenario = readScenarioFile();
		} catch (FileNotFoundException e) {
			
			LOG.warn("Scenario File %s not found. Generating new scenario file.",CONFIG.REQUEST_FILE);
		}
		catch (ScenarioFileCorrupted e) {
			LOG.warn("Scenario File %s was corrputed. Please either fix or delete the file. Deleting the file will cause a new one to be generated next time the program is run.",CONFIG.REQUEST_FILE);
			isCorrputed = true;
		}
		
		if(scenario == null) {
			if(isCorrputed) {
				scenario = generateScenario(CONFIG.MAX_PEOPLE,CONFIG.MAX_SCENARIO_DURATION_MILLS);
			}
			else {
				scenario = generateScenarioFile();
			}
		}
		return scenario;
	}
    
    /**
     * Generates a scenario that spans a given duration and includes a number of passengers
     * @param numPeople The number of passengers throughout the scenario
     * @param scenarioDurationMills The duration of the scenario in Milliseconds
     * @return A sorted list of PassengerRequests
     * @throws IncompatableInputFile 
     */
    public static ArrayList<PassengerRequest> generateScenario(final int numPeople,final long scenarioDurationMills) throws IncompatableInputFile {
    	final ArrayList<PassengerRequest> scenario  = new ArrayList<PassengerRequest>();
    	
    	for(int i=0;i<numPeople;++i) {
    		Random rnd = new Random();
    		long durationIndex = rnd.nextLong(0, scenarioDurationMills);
    		int currFloor = CONFIG.FLOORS == 1 ? 1 : rnd.nextInt(1,CONFIG.FLOORS+1);
    		int targetFloor=0;
    		Direction dir = null;
    		while(targetFloor<1 || targetFloor==currFloor) {
    			targetFloor= CONFIG.FLOORS == 1 ? 1 : rnd.nextInt(1,CONFIG.FLOORS+1);
    		}
    		if(targetFloor > currFloor) {
    			dir = Direction.UP;
    		}
    		else {
    			dir = Direction.DOWN;
    		}
    		scenario.add(new PassengerRequest(durationIndex,currFloor,dir,targetFloor,null));
    	}
    	LOG.info("Generating scenario with %d people and a duration of %d mills.",numPeople,scenarioDurationMills);
    	
    	try {
    		return enforceScenarioRequirements(scenario);
    	}
    	catch(Exception e) {
    		return scenario;
    	}
    }
    
    private static ArrayList<PassengerRequest> enforceScenarioRequirements(final ArrayList<PassengerRequest> inScenario) throws Exception {
    	if(inScenario.size() < CONFIG.MINIMUM_PEOPLE_FOR_SCENARIO) {
    		String  errMsg = String.format("Unable to enforce scenario requirements because there must be at least %d people but there are only %d", CONFIG.MINIMUM_PEOPLE_FOR_SCENARIO,inScenario.size());
    		LOG.error(errMsg);
    		throw new Exception(errMsg);
    	}
    	ArrayList<PassengerRequest> outScenario = new ArrayList<PassengerRequest>(inScenario);
    	Collections.sort(outScenario);
    	final ScenarioFaults stuckDoors = ScenarioFaults.DOOR_STUCK;
    	final ScenarioFaults stuckElevator = ScenarioFaults.ELEVATOR_STUCK;
    	
    	//Depart from 1st and 2nd floor
    	PassengerRequest req = outScenario.remove(0);
    	outScenario.add(0,new PassengerRequest(req.getScenarioDurationMills(),1,Direction.UP,CONFIG.FLOORS,null));
    	req = outScenario.remove(1);
    	outScenario.add(1,new PassengerRequest(req.getScenarioDurationMills(),2,Direction.UP,CONFIG.FLOORS,null));
    	
    	//Return trip to 1st and 2nd floor from top floor.
    	req = outScenario.remove(5);
    	outScenario.add(5,new PassengerRequest(req.getScenarioDurationMills(),CONFIG.FLOORS,Direction.DOWN,1,null));
    	req = outScenario.remove(6);
    	outScenario.add(6,new PassengerRequest(req.getScenarioDurationMills(),CONFIG.FLOORS,Direction.DOWN,2,null));
    	
    	//Stuck Floor on 5
    	req = outScenario.remove(4);
    	outScenario.add(4,new PassengerRequest(req.getScenarioDurationMills(),req.currentFloor,req.getDirection(),req.targetFloor,stuckDoors));
    	//Stuck Elevator on 10
    	req = outScenario.remove(9);
    	outScenario.add(9,new PassengerRequest(req.getScenarioDurationMills(),req.currentFloor,req.getDirection(),req.targetFloor,stuckElevator));
    	return outScenario;
    }
    
    private static ArrayList<PassengerRequest> generateScenarioFile() throws IncompatableInputFile {
    	ArrayList<PassengerRequest> scenario = generateScenario(CONFIG.MAX_PEOPLE,CONFIG.MAX_SCENARIO_DURATION_MILLS);
    	generateScenarioFile(scenario);
    	return scenario;
    }
    
    private static void generateScenarioFile(ArrayList<PassengerRequest> scenario) {
    	File file = new File(CONFIG.REQUEST_FILE);
		StringBuffer fileData = new StringBuffer();
		for(PassengerRequest req : scenario) {
			ScenarioFaults faultType = ScenarioFaults.NONE;
			if(req.getFaultType()!=null) {
				faultType = req.getFaultType();
			}
			fileData.append(String.format("%d %d %s %d %s\n",req.scenarioDurationMills,req.currentFloor,req.getDirection(),req.targetFloor,faultType));
		}
		
    	try (FileWriter fileWriter = new FileWriter(CONFIG.REQUEST_FILE)) {
    		fileWriter.write(fileData.toString());
    		
    	} catch (IOException e) {
			LOG.error("An unexpected error occured while generating scenario file.\n%s\n",e.getMessage());
			e.printStackTrace();
			System.exit(2);
		}
    	LOG.info("New Scenario File Generated at %s with %d requests with a scenario duration of %d mills.",file.getAbsolutePath(),CONFIG.MAX_PEOPLE,CONFIG.MAX_SCENARIO_DURATION_MILLS);    	
    }
    
    public static ArrayList<PassengerRequest> readScenarioFile() throws ScenarioFileCorrupted,FileNotFoundException, IncompatableInputFile {
    	ArrayList<PassengerRequest> scenario = new ArrayList<PassengerRequest>();
    	File file = new File(CONFIG.REQUEST_FILE);
    	int max = 0;
    	try (BufferedReader fileReader = new BufferedReader(new FileReader(file))) {
    		String line = "";
    		while( (line = fileReader.readLine()) != null) {
    			String[] lineData = line.split(" ");
    			if(lineData.length!=NUM_INPUT_FILE_COLUMNS) {
    				throw new ScenarioFileCorrupted(line);
    			}
    			int durationStamp = Integer.valueOf(lineData[0]);
    			max = max < durationStamp ? durationStamp : max;
    			int currentFloor = Integer.valueOf(lineData[1]);
    			Direction dir = Direction.valueOf(lineData[2]);
    			int targetFloor = Integer.valueOf(lineData[3]);
//    			ElevatorFault fault = null;
//    			ScenarioFaults faultType = ScenarioFaults.valueOf(lineData[4]);
    			ScenarioFaults fault = ScenarioFaults.valueOf(lineData[4]);
    			
//    			if(faultType!=ScenarioFaults.NONE) {
//    				fault = new ElevatorFault(faultType,false);    				
//    			}
    			
    			PassengerRequest req = new PassengerRequest(durationStamp,currentFloor,dir,targetFloor,fault);
    			scenario.add(req);
    		}
    		
    	} catch (FileNotFoundException e) {
    		throw e;
		}
    	catch (IllegalArgumentException  | NullPointerException e) {
    		throw new ScenarioFileCorrupted(String.format("Unable to load scenario file %s because information format is not recognized. Perhaps this is an older version?",file.getAbsolutePath()));
    	} catch (IOException e) {
    		LOG.error("Unable to load file at %s for an unknown reason.\n%s\n",file.getAbsolutePath(),e.getMessage());
			throw new ScenarioFileCorrupted(e.getMessage());
		}
    	LOG.info("Successfully loaded scenario file at %s with %d requests and a duration of %d mills",file.getAbsolutePath(),scenario.size(),max);
    	return scenario;
    }


	public long getScenarioDurationMills() {
		return scenarioDurationMills;
	}


	public int getCurrentFloor() {
		return currentFloor;
	}


	public Direction getDirection() {
		return direction;
	}


	public int getTargetFloor() {
		return targetFloor;
	}


	public ScenarioFaults getFaultType() {
		return faultType;
	}
	
	@Override
	public int hashCode() {
		return Objects.hash(currentFloor, direction, scenarioDurationMills, targetFloor);
	}

	/**
	 * Sets equality based on scenario duration timestamp being the same
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		PassengerRequest other = (PassengerRequest) obj;
		return scenarioDurationMills == other.scenarioDurationMills;
	}
	
	/**
	 * Sets the order of PassengerRequests based on the scenario duration for sorting
	 */
	@Override
	public int compareTo(PassengerRequest o) {
		if(scenarioDurationMills > o.scenarioDurationMills) {
			return 1;
		}
		else if(scenarioDurationMills < o.scenarioDurationMills) {
			return -1;
		}
		else {
			return 0;
		}
	}
	@Override
	public String toString() {
		String strFaultType = "NULL";
		if(faultType!=null) {
			strFaultType = faultType.toString();
		}
		return "PassengerRequest [TimeStamp:" + scenarioDurationMills + ", Floor " + currentFloor
				+ "->" + targetFloor+ ", Direction: " + direction + ",  faultType: " + strFaultType+"]";
	}
	
	
   
}