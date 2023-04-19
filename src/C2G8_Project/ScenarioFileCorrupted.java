package C2G8_Project;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * 
 * @author Jayson Mendoza
 *
 */
public class ScenarioFileCorrupted extends Exception {
	private final static Logger LOG =LogManager.getFormatterLogger(ScenarioFileCorrupted.class);
	
	public ScenarioFileCorrupted(String corruptedLine) {
		super(String.format("The scenario file has been corrupted and contains lines in an unexpected format.\n Data: %s\n",corruptedLine));
		LOG.error(this.getMessage());
	}

}
