package C2G8_Project.MonitorSystem;

import java.net.SocketException;


import javafx.application.Application;
import javafx.stage.Stage;

/**
 * This is the main application for the Monitor System.
 * The Monitor System connects to a scheduler and provides
 * a view portal into the system's overall state. More
 * than one can be connected to a single scheduler at a time
 * @author Jayson Mendoza
 *
 */
public class MonitorSystem extends Application {
	private MonitorController control;
	private Stage mainStage;
	
	/**
	 * Application start procedure
	 */
    @Override
    public void start(Stage stage) throws SocketException {
    	this.mainStage = stage;
    	
    	control = MonitorController.instance();
        this.mainStage.setScene(control.getMainScene());
        this.mainStage.setMaximized(true);
        this.mainStage.setTitle("Monitor System");
        this.mainStage.show();
    }

    /**
     * Application exist procedure
     */
    @Override
	public void stop() throws Exception {
    	control.shutdown();
		super.stop();
	}

	public static void main(String[] args) {
        launch(args);
    }
}
