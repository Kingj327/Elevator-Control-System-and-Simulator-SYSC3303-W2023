package C2G8_Project;

public record RawFloorRequest(
		int reqestFloor,
		int targetFloor,
		Direction direction,
		ScenarioFaults fault
) {}