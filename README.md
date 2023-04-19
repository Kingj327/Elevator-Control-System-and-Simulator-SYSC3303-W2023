# SYSC3303_Project

## Documentation of Files
This iteration was prepared in javadoc format. Please visit ./doc/index.html to see a list of all java files and their included documentation.
Documentation includes the files purpose along with a breakdown of its methods

## UML Diagrams
These are located in the repository ./diagrams folder

## Setup Instructions
1) Unzip file to folder or you can ask us for access from GIT to clone the iteration.
2) Import the project into eclipse. 
3) Ensure Maven dependences have finished downloading and installing
4) Run FloorSystem.java. It will load and wait for a connection to scheduler
5) Run ElevatorSystem.java. It will load and start trying to connect to the scheduler
6) Run Scheduler.java. It will establish connections with all components and then start the scenario

***NOTE: This iteration takes a while to run. We will be accelerating this in the next iteration***

## Testing Instructions
1) Make Setup instructions are complete
2) Run ./test/src/C2G8_Project as a JUNIT test.

**NOTE: This works most of the time. However, we have had some issues in the past in coordinating thread timing for the setup. We believe they are all fixed now though**
