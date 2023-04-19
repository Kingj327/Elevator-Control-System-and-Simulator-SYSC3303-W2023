package C2G8_Project;

public record FloorRequest(
		int reqestFloor,
		int targetFloor,
		Direction direction,
		ElevatorFault fault
) {}