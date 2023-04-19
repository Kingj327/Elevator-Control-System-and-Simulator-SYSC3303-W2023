package C2G8_Project;

/**
 * A data packet representing a FloorSignal sent by the scheduler
 * to a FloorSubsystem
 * @author Jayson Mendoza
 *
 */
public record FloorSignal(
		FloorSignals signal,
		int floor,
		Direction direction
		)
{}
