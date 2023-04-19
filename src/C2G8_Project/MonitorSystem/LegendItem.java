package C2G8_Project.MonitorSystem;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.scene.image.ImageView;

/**
 * Represents a List item for a table and helps it know which data to display from this list
 * @author Jayson Mendoza
 *
 */
public final class LegendItem {
	public final ObjectProperty<ImageView> icon = new SimpleObjectProperty<ImageView>();
	final public SimpleStringProperty description = new SimpleStringProperty();
	
	public ImageView getIcon() {
		return icon.getValue();
	}

	public String getDescription() {
		return description.getValue();
	}

	LegendItem(ImageView ico,String des) {
		icon.setValue(ico);
		description.setValue(des);	
	}
}
