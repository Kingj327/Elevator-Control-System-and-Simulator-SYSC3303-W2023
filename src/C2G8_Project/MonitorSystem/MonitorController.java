package C2G8_Project.MonitorSystem;

import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import C2G8_Project.CONFIG;
import C2G8_Project.Destinations;
import C2G8_Project.DispatchConsumer;
import C2G8_Project.Dispatcher;
import C2G8_Project.FloorSignal;
import C2G8_Project.FloorTopics;
import C2G8_Project.UnregisteredDispatcherDestination;
import C2G8_Project.ViewData;
import C2G8_Project.ViewRequestData;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

/**
 * The main controller used to give the main window functionality and also responsible for handling dispatcher updates to the system state
 * @author Jayson Mendoza
 *
 */
public class MonitorController implements DispatchConsumer {
	private final static Logger LOG =LogManager.getFormatterLogger(MonitorController.class);
	private static MonitorController instance;
	
	private final Thread dispatcherThread;
	private final Dispatcher dispatch;
	private final int clientID;
	private final String name;
	private boolean isConnected;
	private static int nextClientID = 0;
	private MainWindow mainWindow;
	private Scene mainScene;
	private String connectionStatus;
	private ObjectMapper objMap;
	private ViewData lastState;
	
	/**
	 * Assigns the client ID so it is unique. There may be more than one MonitorSystem running
	 * @return
	 */
	protected static int assignClientID() {
		return ++nextClientID;
	}
	
	/**
	 * Implements a singleton so only one Monitor Controller exists.
	 * This will return the instance that exists or create a new
	 * one if none exists.
	 * @return Monitor Controller instance
	 * @throws SocketException
	 */
	public static MonitorController instance() throws SocketException {
		if(instance==null) {
			instance = new MonitorController();
		}
		return instance;
	}
	
	/**
	 * Creates a new MonitorController. Private because it's a singleton.
	 * @throws SocketException Disaptcher failure
	 */
	private MonitorController() throws SocketException {
		this.dispatch = new Dispatcher(Destinations.MONITOR_SYSTEMS);
		dispatcherThread = new Thread(dispatch);
		dispatcherThread.setName(String.format("%s_Thread", dispatch.getName()));
		dispatcherThread.start();
		this.clientID = assignClientID();
		name = String.format("%s_%d", Destinations.MONITOR_SYSTEMS,clientID);
//        Scene scene = new Scene(new StackPane(l), 640, 480);
        this.mainWindow = new MainWindow();
        this.mainScene = new Scene(this.mainWindow);
        isConnected = false;
        this.connectionStatus = "Not Connected";
        objMap = new ObjectMapper();
        lastState =  null;
        init();
	}
	
	/**
	 * Shuts down Monitor System
	 */
	public void shutdown() {
		dispatch.shutdown();
	}
	
	
	public Scene getMainScene() {
		return mainScene;
	}

	/**
	 * Handles updates from subscribed messages to dipatcher. This is generally
	 * new state updates from the scheduler.
	 */
	@Override
	public void receiveDispatch(String topic, String data) {
		try {
				MonitorTopics recTopic = MonitorTopics.valueOf(topic);
				if(recTopic == MonitorTopics.SCHEDULER_UPDATE){
					lastState = objMap.readValue(data, ViewData.class);
					mainWindow.updateState(lastState);
					mainWindow.updateFloor(lastState);
					LOG.debug("ViewData  : %s", data);
				}else{
					LOG.warn("Unexpected Message Topic Ignored: %s, Data: %s", topic, data);
				}
		} catch (JsonMappingException e) {
			LOG.error("[%s]: Unable to convert data payload. Data: %s",topic,data);
			e.printStackTrace();
		} catch (JsonProcessingException e) {
			LOG.error("[%s]: Unable to convert data payload. Data: %s",topic,data);
			e.printStackTrace();
		}
		catch (IllegalArgumentException | NullPointerException e) {
			LOG.warn("Unexpected Message Topic Ignored: %s, Data: %s", topic, data);
		}
		
	}
	
	
	@Override
	public String getSubscriberNameIdentifier() {
		// TODO Auto-generated method stub
		return name;
	}
	
	/**
	 * Init the handlers
	 */
	private void init() {
		mainWindow.getLblFooterLeftStatus().setText(connectionStatus);
		this.mainWindow.getMiConnect().setOnAction(new EventHandler<ActionEvent>() {

			@Override
			public void handle(ActionEvent event) {
				ConnectDialogue connectDia = new ConnectDialogue();
				Optional<String> addr = connectDia.showAndWait();
				LOG.debug("Connection Dialogue Result: %s",  addr);
				if(!addr.isPresent()) {			
					String errMsg = "No address was provided. Connection attempt cancelled.";
					LOG.warn(errMsg);
					Alert alert = new Alert(AlertType.INFORMATION,errMsg,ButtonType.OK);
					alert.showAndWait();
				}
				connectToScheduler(addr.get());
			}
			
		});
		
		this.mainWindow.getMiQuit().setOnAction(new EventHandler<ActionEvent>() {

			@Override
			public void handle(ActionEvent event) {
				LOG.info("Monitor System Exit Command received.");
				Platform.exit();
			}
			
		});
	}
	
	/**
	 * Function to handle the connection process to the scheduler
	 * @param address The address of the scheduler
	 */
	public void connectToScheduler(final String address) {
		int schedulerPort = CONFIG.SCHEDULER_PORT;
		if(isConnected) {
			LOG.warn("Connection to %s%d ignored. Already connected!",address,schedulerPort);
			return;
		}
		Alert progressAlert = new Alert(AlertType.INFORMATION,String.format("Attempting to connect %s:%d. This could take a minute. Press Ok to continue...",address,schedulerPort),ButtonType.OK);
		progressAlert.showAndWait();
		try {
			dispatch.subscribe(MonitorTopics.SCHEDULER_UPDATE.toString(), this);
			dispatch.connectNewDestination(Destinations.SCHEDULER, address, schedulerPort);
			isConnected = dispatch.isDestinationRegistered(Destinations.SCHEDULER);
			LOG.info("Connection to %s:%d successful? %s", address, schedulerPort,isConnected);
			connectionStatus = String.format("Connected: %s:%d", address, schedulerPort);
			mainWindow.getLblFooterLeftStatus().setText(connectionStatus);
		} catch (UnknownHostException e) {
			String errMsg = String.format("Unable to connect to scheduler at address %s:%d", address,schedulerPort);
			LOG.warn(errMsg);
			Alert alert = new Alert(AlertType.INFORMATION,errMsg,ButtonType.OK);
			e.printStackTrace();
			alert.showAndWait();
		} catch (UnregisteredDispatcherDestination e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
