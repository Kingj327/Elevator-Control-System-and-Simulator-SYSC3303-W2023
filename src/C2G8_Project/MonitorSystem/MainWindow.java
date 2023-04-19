package C2G8_Project.MonitorSystem;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import C2G8_Project.CONFIG;
import C2G8_Project.Direction;
import C2G8_Project.ElevatorStates;
import C2G8_Project.ViewData;
import C2G8_Project.ViewElevatorData;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.RowConstraints;
import javafx.scene.layout.VBox;

/**
 * Main window of program to show all system state data that is important.
 * 
 * This is mostly passive but it does have a connect function. 
 * 
 * This can be connected at any time during the simulation.
 * 
 * @author Jayson Mendoza
 *
 */
public class MainWindow extends VBox {
	private final static Logger LOG =LogManager.getFormatterLogger(MainWindow.class);
	private static final int ICON_SIZE = 60;
	
	@FXML
	private MenuBar menuBar;
	
	@FXML
	private Menu menuFile;
	
	@FXML
	private MenuItem miQuit;
	
	@FXML
	private MenuItem miConnect;
	
	@FXML
	private SplitPane mainDivider;
	
	@FXML
	private GridPane gpLeftMenu;
	
	@FXML
	private ScrollPane sbMainView;
	
	@FXML
	private HBox hbxFooter;
	
	@FXML
	private Label lblFooterLeftStatus;
	
	@FXML
	private Pane middleFooterPane;
	
	@FXML
	private Label lblFooterRightStatus;
	
	@FXML
	private GridPane gpMainView;
	
	private final  ObservableList<LegendItem> legendItems;
	private final TableView<LegendItem> legend;
	
	ArrayList<ImageView> buttonPaneViews = new ArrayList<ImageView>();
	ArrayList<ImageView> directionPaneViews = new ArrayList<ImageView>();
	ArrayList<ImageView> elevatorPaneViews = new ArrayList<ImageView>();
	
	
	private HashMap<String,Image> buttonImages = new HashMap<String,Image>();
	private HashMap<String,Image> directionImages = new HashMap<String,Image>();
	private HashMap<ElevatorStates,Image> elevatorImages = new HashMap<ElevatorStates,Image>();
	private HashMap<Integer,ImageView> floorBtnLamps = new HashMap<Integer,ImageView>();
	private HashMap<Integer,ImageView> floorDirLamps = new HashMap<Integer,ImageView>();
	private HashMap<Integer,ImageView> elevators = new HashMap<Integer,ImageView>();
	private HashMap<Integer,Label> elevatorBtns = new HashMap<Integer,Label>();

	/**
	 * Creates a new main window from a FXML template
	 */
	public MainWindow() {
		FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("MainWindow.fxml"));
		fxmlLoader.setRoot(this);
		fxmlLoader.setController(MainWindow.this);
		legend = new TableView<LegendItem>();
		legendItems = FXCollections.observableArrayList( new ArrayList<LegendItem>());
		try {
			fxmlLoader.load();
			LOG.info("Load Successful");
			for(Node  n : this.getChildren())  {
				LOG.info("Node ID: %s, idProperty: %s", n.getId(),n.idProperty());
			}
		}
		catch (IOException e) {
			LOG.error("%s",e.getMessage());
			throw new RuntimeException(e);
		}
		initResources();
		initLegend();
		init();
		this.setVisible(true);
		
	}

	public MenuBar getMenuBar() {
		return menuBar;
	}

	public Menu getMenuFile() {
		return menuFile;
	}

	public MenuItem getMiQuit() {
		return miQuit;
	}

	public MenuItem getMiConnect() {
		return miConnect;
	}
	
	public SplitPane getMainDivider() {
		return mainDivider;
	}


	public GridPane getGpLeftMenu() {
		return gpLeftMenu;
	}

	public ScrollPane getSbMainView() {
		return sbMainView;
	}

	public HBox getHbxFooter() {
		return hbxFooter;
	}

	public Label getLblFooterLeftStatus() {
		return lblFooterLeftStatus;
	}

	public Pane getMiddleFooterPane() {
		return middleFooterPane;
	}

	public Label getLblFooterRightStatus() {
		return lblFooterRightStatus;
	}
	
	/**
	 * Loads resources from files once into memory for use throughout the simulation
	 */
	private void initResources() {
		for(ElevatorStates state : ElevatorStates.values())  {
			String path = String.format("icons/ELEVATOR_%s.png",state);
			LOG.debug("Loading %s", path);
			Image img = new Image(getClass().getResourceAsStream(path),ICON_SIZE,ICON_SIZE,true,false);
			elevatorImages.put(state,img);
			ImageView imgV = new ImageView(img);
			String description = String.format("%s", state);
			LegendItem lItem = new LegendItem(imgV,description);
			legendItems.add(lItem);
		}
		
		buttonImages.put("NONE", new Image(getClass().getResourceAsStream("icons/FLOOR_NONE.png"),ICON_SIZE,ICON_SIZE,true,false));
		buttonImages.put("BOTH", new Image(getClass().getResourceAsStream("icons/FLOOR_BOTH.png"),ICON_SIZE,ICON_SIZE,true,false));
		for(Direction dir : Direction.values()) {
			buttonImages.put(dir.toString(), new Image(getClass().getResourceAsStream(String.format("icons/FLOOR_%s.png",dir)),ICON_SIZE,ICON_SIZE,true,false));
		}
		
		directionImages.put("NONE", new Image(getClass().getResourceAsStream("icons/FLOOR_DIR_NONE.png"),ICON_SIZE,ICON_SIZE,true,false));
		directionImages.put("BOTH", new Image(getClass().getResourceAsStream("icons/FLOOR_DIR_BOTH.png"),ICON_SIZE,ICON_SIZE,true,false));
		for(Direction dir : Direction.values()) {
			directionImages.put(dir.toString(), new Image(getClass().getResourceAsStream(String.format("icons/FLOOR_DIR_%s.png",dir)),ICON_SIZE,ICON_SIZE,true,false));
		}
		
	}
	
	/**
	 * Creates the legend in the left view pane
	 */
	private void initLegend() {
		TableColumn<LegendItem,ImageView> iconCol = new TableColumn<LegendItem,ImageView>("Icon");
		iconCol.setCellValueFactory(new PropertyValueFactory<LegendItem,ImageView>("icon"));
		legend.getColumns().add(iconCol);
		TableColumn<LegendItem,String> descriptionCol = new TableColumn<LegendItem,String>("Description");
		descriptionCol.setCellValueFactory(new PropertyValueFactory<LegendItem,String>("description"));
		legend.getColumns().add(descriptionCol);
		legend.setItems(legendItems);
		gpLeftMenu.add(legend, 0, 0);
	}
	
	/**
	 * Initialize the GUI with Floor and Elevator Information
	 */
	private void init() {
		gpMainView.getChildren().clear();
		RowConstraints row = new RowConstraints(ICON_SIZE);
		gpMainView.getRowConstraints().add(row);
		for(int i = 0; i<=CONFIG.FLOORS;++i) {
			if(i >= gpMainView.getRowCount()) {
				gpMainView.addRow(i);
			}
			if(i == CONFIG.FLOORS){
				gpMainView.add(new Label("Floor"),0,i);
				gpMainView.add(new Label("Request"),1,i);
				break;
			}
			Integer floorNum = CONFIG.FLOORS-i;
			gpMainView.add(new Label(String.format("%d",floorNum)),0,i);
			ImageView floorDir = new ImageView(directionImages.get("NONE"));
			ImageView floorBtn = new ImageView(buttonImages.get("NONE"));
			floorBtnLamps.put(floorNum, floorBtn);
			floorDirLamps.put(floorNum, floorDir);
			directionPaneViews.add(floorDir);
			buttonPaneViews.add(floorBtn);
			gpMainView.add(floorDir,2,i);
			gpMainView.add(floorBtn,1,i);
		}
		
		for(int j = 0; j < CONFIG.ELEVATORS;++j) {
			if(j >= gpMainView.getColumnCount()) {
				gpMainView.addColumn(j);;
			}
			ImageView elevator = new ImageView(elevatorImages.get(ElevatorStates.DEAD));
			elevators.put(j+1, elevator);
			elevatorPaneViews.add(elevator);
			gpMainView.add(elevator, j+3,CONFIG.FLOORS-1);
			
			Label label = new Label("[]");
			elevatorBtns.put(j+1, label);
			gpMainView.add(label,j+3,CONFIG.FLOORS);
		}
	}
	
	/**
	 * Update the Icons for the Floors such as the Direction and Lamp Buttons
	 * @param newState Data to update the Floor View
	 */
	public void updateFloor(ViewData newState) {
		for(Map.Entry<Integer, HashMap<Direction,Boolean>> e : newState.floorButtonLamp().entrySet()) {
			
			HashMap<Direction,Boolean> dirStates = newState.floorDirectionLamp().get(e.getKey());
			boolean isUpBtnUpLampOn = e.getValue().getOrDefault(Direction.UP, false);
			boolean isUpBtnDownLampOn = e.getValue().getOrDefault(Direction.DOWN, false);
			boolean isUpDirUpLampOn = dirStates.getOrDefault(Direction.UP, false);
			boolean isUpDirDownLampOn = dirStates.getOrDefault(Direction.DOWN, false);
			LOG.debug("[%d] BtnLamps(UP: %s, DOWN: %s) DirLamps(UP: %s, DOWN: %s)", e.getKey(), isUpBtnUpLampOn, isUpBtnDownLampOn, isUpDirUpLampOn, isUpDirDownLampOn);
//			floorBtnLamps;
			if(isUpBtnUpLampOn && isUpBtnDownLampOn) {
				floorBtnLamps.get(e.getKey()).setImage(buttonImages.get("BOTH"));
			}
			else if(isUpBtnUpLampOn) {
				floorBtnLamps.get(e.getKey()).setImage(buttonImages.get(Direction.UP.toString()));
			}
			else if(isUpBtnDownLampOn) {
				floorBtnLamps.get(e.getKey()).setImage(buttonImages.get(Direction.DOWN.toString()));
			}
			else {
				floorBtnLamps.get(e.getKey()).setImage(buttonImages.get("NONE"));
			}
			
			
			if(isUpDirUpLampOn && isUpDirDownLampOn) {
				
				floorDirLamps.get(e.getKey()).setImage(directionImages.get("BOTH"));
			}
			else if(isUpDirUpLampOn) {
				floorDirLamps.get(e.getKey()).setImage(directionImages.get(Direction.UP.toString()));
			}
			else if(isUpDirDownLampOn) {
				floorDirLamps.get(e.getKey()).setImage(directionImages.get(Direction.DOWN.toString()));
			}
			else {
				floorDirLamps.get(e.getKey()).setImage(directionImages.get("NONE"));
			}
		}
	}
	
	/**
	 * Update the Icons for Elevators and their placement, also Update the CarBttns label for each elevator
	 * @param newState Data to update the Elevators Information
	 */
	public void updateState(ViewData newState) {
		for(Map.Entry<Integer, ViewElevatorData>  e : newState.elevatorLatestData().entrySet()) {
			ViewElevatorData data = e.getValue();
			ImageView elevatorIcon = elevators.get(data.signal().id());
			int rowIdx = CONFIG.FLOORS-data.signal().location();
			GridPane.setRowIndex(elevatorIcon, rowIdx);
			elevatorIcon.setImage(elevatorImages.get(data.signal().state()));

			Platform.runLater(new Runnable() {
				@Override
				public void run() {
					elevatorBtns.get(data.signal().id()).setText(String.format("%s",data.signal().carBttns().toString()));
				}
			});
		}
		
	}
	

}
