SYSC 3303: Real-Time Concurrent Systems
Project Iteration 5
By: Jayson Mendoza, Jean-Pierre Aupont, Jordan King

Lab Section: C2
Group Number: 8
Milestone: 2

Due: April 12, 2023 @ 11:59pm



Instructions to Run:
 - Unzip the Project Folder
 - Open Eclipse
 - Open the Project
 - To run the program normally start by running the ElevatorSystem, then the Monitor (when open, click "connect in 
   the top left corner and press "Okay" until the popups go away), then the Scheduler, followed by the FloorSystem.
   
   *Note that the program ends automatically once a shutdown signal has been sent to each system from the Scheduler.
 
 You can also change settings in the "CONFIG.java" file. Speeding up the system can be done by changing
 ELEVATOR_SPEED_MULTIPLIER from 1 to 50 (recommended no higher than 50). To generate a bigger scenario (input file),
 change MAX_PEOPLE to be the amount of people/requests the system gets, change MAX_SCENARIO_DURATION_MILLS to be
 the amount of time (in milliseconds) that you want the scenarios to be spread out over, and then delete the current
 "InputFile.txt" located in the root folder, "/SYSC3303_Project".
 
 Recommended settings:
                                 Normal      Fast
 ELEVATOR_SPEED_MULTIPLIER    |    1    | 25 to 50 |
 MAX_PEOPLE                   |    15   | 25 to 50 |
 MAX_SCENARIO_DURATION_MILLS  | 60*1000 |  60*1000 |
            DELETE "/SYSC3303_Project/InputFile.txt" to generate a new file.




Source Files:
 - FloorSubsystem.java
   - One of the 3 main systems. The FloorSubsystem reads the input file to create passenger requests, 
     and sends those requests to the Scheduler. For the simulation, it also waits for the requests 
     to return. Once all of the requests have returned, it sends a shutdown message to the other
     systems.
     
 - Scheduler.java
   - One of the 3 main systems. The Scheduler receives passenger requests from the FloorSubsystem
     and sends them to the ElevatorSubsystem. It also receives requests from the ElevatorSubsystem
     and sends them to the FloorSubsystem.
     
 - ElevatorSubsystem.java
   - One of the 3 main systems. The ElevatorSubsystem receives a passenger request from the Scheduler
     (originally from the FloorSubsystem), and then replies by sending a message to the Scheduler
     (for the FloorSubsystem).
 
 - ElevatorSystem.java
   - Organizes the threads and dispatcher for the ElevatorSubsystem, also handles the shutdown sequence
     to end the Elevator programs.
     
 - ElevatorSignal.java
   - Represents the information sent by the ElevatorSubsystem to the Scheduler to update its current condition.
     It contains the elevator ID,State,Location,Incoming Car Button Requests, and currently presed car Buttons.
     
 - ElevatorStates.java
   - Represents the different possible Elevator States. START is to synch the programs and commence the simulation.
     IDLE is when the elevator has no job to do. MOVING_UP/DOWN, OPEN/CLOSE_DOORS is self-explanatory, and STOP is sent
     when the Elevator must stop to handle some sort of request.
     
 - ElevatorTopics.java
   - As a pub/sub design, these contain the topics that the Elevator subscribe to in order to receive any packets
     directed towards it. 
     
 - ElevatorData.java
   - The main data structure used by the Scheduler to maintain elevator states throughout the systems execution.
 
 - ElevatorTimes.java
   - Keeps the times it takes for an elevator to execute its state.
 
 - ElevatorFault.java
   - Data Structure used to simulate elevator errors
   
 - RequestData.java
   - The main data structure used by the Scheduler to maintain floor request states throughout the systems execution.
   
 - FloorRequest.java
   - Data sent from the Floor. It is essentially a passenger pressing the up or down button on a floor.
   
 - FloorSignal.java
   - Data sent to the Floor to make floor buttons change.
 
 - FloorTopics.java
   - As a pub/sub design, these contain the topics that the Scheduler subscribe to in order to receive any packets
     directed towards it.
     
 - ScenarioFaults.java
   - Enumerated file used to send Errors to the ElevatorSubsystems
 
 - SchedulerState.java
   - A state interface used in the scheduler's state machine. Classes that implement this are a state of the Scheduler.
     NOTE: The classes/states below are in the same file, SchedulerState.java.
     - Class/State: Listening
       - Listens for changes in the system (Receiving a message, or changes to master variables that keep the system state.
     - Class/State: ProcessMessage
       - Processes/redirects floor and elevator messages to be handled appropriately.
     - Class/State: PrepareFloorMessage
       - Handles received floor messages: the logic of what needs to be packaged into a message.
     - Class/State: PrepareElevatorStateMessage
       - Handles received elevator messages: the logic of what needs to be packaged into a message, including any state changes.
     - Class/State: SendMessage
       - Sends pre-packaged messages to their required destination.
       
 - SchedulerTopics.java
   - As a pub/sub design, these contain the topics that the Scheduler subscribe to in order to receive any packets
     directed towards it. 
 
 - SchedulingAlgorithm.java
   - A class containing mainly static methods that acts as a helper for the Scheduler by handling all things related to
     assigning a Passenger Request to a specific Elevator (subsystem), minimizing wait times (pickup times) for passengers.
     
 - PassengerRequest.java
   - A data structure class that holds the passenger's request data.
     
 - InputFile.txt
   - Holds the passenger request information in a file, meant for the simulation.
   
 - TestInputFile.txt
   - Holds the passenger request information in a file, meant a the JUnit test case.
   
 - Destinations.java
 - Direction.java
 - DispatchConsumer.java
 - Dispatcher.java
 - DispatchMessage.java
 - FloorSystem.java
 - ScenarioFileCurrupted.java
 - UnregisteredDispatcherDestination.java



Test Files:
 - DisptcherTest.java
 
 - DispatchSubscriberTester.java
 
 - DummyFloorSystem.java
 
 - ElevatorSystemTest.java
   - This is a JUnit file to test the functionality of the ElevatorSystem in terms of creating elevators, and shutting down
   
 - ElevatorSubsystemTest.java
   - This is a JUnit file to test the functionality of the ElevatorSubsystem in terms of state transition and state functionality
   
 - FloorSubsystemTest.java
 
 - FloorSystemTest.java
 
 - SchedulerStateTest.java
   - Tests the functionality of the state machine using JUnit tests
 
 - SchedulingAlgorithmTest.java
   - Tests the functionality of the scheduling algorithm, and its components.



Breakdown - Iteration 5:

 - Jayson
  - GUI Research
  - GUI Implementation
  - GUI Visuals/Symbols
 	
 - Jean-Pierre
  - Organization and final cleanup of all UML diagrams (Class, Sequence, Timing)
  - GUI implementation
  - GUI Floor Request List

 - Jordan
  - All system measurements
  - Report
  - Sending data to GUI
