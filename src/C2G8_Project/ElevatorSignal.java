package C2G8_Project;

import java.util.ArrayList;
import java.util.Set;

/**
 * @author JP
 * Elevator Information shared in packets to and for the scheduler
 */
public record ElevatorSignal(
    ElevatorStates state,
    int id,
    int location,
    ArrayList<Integer> newCarBttns,
    Set<Integer> carBttns,
    ScenarioFaults faultType
){}
