package C2G8_Project;

/**
 * Represents all floor topics available.
 * 
 * NOTE: The actual topics will be modified to include
 * the floor number.
 * Format: TOPIC_FLOORNUM
 * Ex. Floor 5 would subscribe with topic
 * FLOOR_SIGNAL_5
 * 
 * @author Jayson Mendoza
 *
 */
public enum FloorTopics {
	FLOOR_SIGNAL,
	SCENARIO_START,
	SCENARIO_END
}
