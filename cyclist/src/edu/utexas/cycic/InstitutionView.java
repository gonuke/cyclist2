package edu.utexas.cycic;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;

import org.controlsfx.dialog.Dialog;

import edu.utah.sci.cyclist.core.event.notification.CyclistNotification;
import edu.utah.sci.cyclist.core.event.ui.CyclistEvent;
import edu.utah.sci.cyclist.core.ui.components.ViewBase;
import edu.utexas.cycic.presenter.InstitutionViewPresenter;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
/**
 * A view used to build and develop institutions for the simulation 
 * currently being built. 
 * @author Robert
 *
 */
public class InstitutionView extends ViewBase{
	/**
	 * Initiates a new window for building and modifying institutions. 
	 */
	public InstitutionView(){
		super();
		// Ensures the temporary institution is initiated only once. 
		if (CycicScenarios.workingCycicScenario.simInstitutions.size() < 1) {
			String string;
			for(int i = 0; i < XMLReader.institutionList.size(); i++){
				StringBuilder sb = new StringBuilder();
				StringBuilder sb1 = new StringBuilder();
				Process proc;
				try {
					proc = Runtime.getRuntime().exec("cyclus --agent-schema "+XMLReader.institutionList.get(i)); 
					BufferedReader read = new BufferedReader(new InputStreamReader(proc.getInputStream()));
					while((string = read.readLine()) != null){
						sb.append(string);
					}
					Process proc1 = Runtime.getRuntime().exec("cyclus --agent-annotations "+XMLReader.institutionList.get(i));
					BufferedReader read1 = new BufferedReader(new InputStreamReader(proc1.getInputStream()));
					while((string = read1.readLine()) != null){
						sb1.append(string);
					}
					institutionStructure test = new institutionStructure();
					test.institName = XMLReader.institutionList.get(i).replace(":", " ").trim();
					test.institStruct = XMLReader.annotationReader(sb1.toString(), XMLReader.readSchema(sb.toString()));
					DataArrays.simInstitutions.add(test);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		
		// ListView for initial facilities in the institution.
		final ListView<String> facilityList = new ListView<String>();
		facilityList.setOrientation(Orientation.VERTICAL);
		facilityList.setMinHeight(25);
		facilityList.setOnMousePressed(new EventHandler<MouseEvent>(){
			public void handle(MouseEvent event){
				if (event.isSecondaryButtonDown()){
					workingInstit.availFacilities.remove(facilityList.getSelectionModel().getSelectedIndex());
					facilityList.getItems().remove(facilityList.getSelectionModel().getSelectedItem());
				}
			}
		});
		
		// ListView for prototypes available it the institution.
		final ListView<String> prototypeList = new ListView<String>();
		prototypeList.setOrientation(Orientation.VERTICAL);
		prototypeList.setMinHeight(25);
		prototypeList.setOnMousePressed(new EventHandler<MouseEvent>(){
			public void handle(MouseEvent event){
				if (event.isSecondaryButtonDown()){
					workingInstit.availPrototypes.remove(prototypeList.getSelectionModel().getSelectedItem());
					prototypeList.getItems().remove(prototypeList.getSelectionModel().getSelectedItem());
				}
			}
		});
		structureCB.setPromptText("Select Institution");
		// Building the list of objects to be put in to the ComboBox.
		// Inputs all built institutions and adds a field for adding a new one. 
		structureCB.setOnMouseClicked(new EventHandler<MouseEvent>(){
			public void handle(MouseEvent e){
				structureCB.getItems().clear();
				for(int i = 0; i < DataArrays.institNodes.size(); i++){
					structureCB.getItems().add((String) DataArrays.institNodes.get(i).name);
				}
				structureCB.getItems().add("New Institution");
			}
		});
		//InstitutionViewPresenter.newInstitution(structureCB);
		// Change Listener for structureCB to indicate the selection of a new or saved institution to be loaded.
		structureCB.valueProperty().addListener(new ChangeListener<String>(){
			@SuppressWarnings("unchecked")
			public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue){
				if (newValue == null){
					
				} else if(newValue == "New Institution"){
					grid.getChildren().clear();
					rowNumber = 1;
					instituteNode tempInstit = new instituteNode();
					Dialog dlg = new Dialog(grid, "Institution Name");
					dlg.getActions().add(Dialog.Actions.OK);
					GridPane dlgGrid = new GridPane();
					TextField txtName = new TextField();
					txtName.textProperty().addListener(new ChangeListener<String>(){
						public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue){
							tempInstit.name = newValue;
						}
					});
					dlgGrid.add(new Label("Name"), 0, 0);
					dlgGrid.add(txtName, 1, 0);
					ComboBox cbType = new ComboBox();
					for(int i = 0; i < DataArrays.simInstitutions.size(); i++){
						cbType.getItems().add(DataArrays.simInstitutions.get(i).institName);
					}
					cbType.setOnAction(new EventHandler<Event>(){
						public void handle(Event event) {
							tempInstit.type = (String) cbType.getValue();
							for(int i = 0; i < DataArrays.simInstitutions.size(); i++){
								if (DataArrays.simInstitutions.get(i).institName.equalsIgnoreCase((String) cbType.getValue())){
									tempInstit.institStruct = DataArrays.simInstitutions.get(i).institStruct;
								}
							}			
						}
					});
					dlgGrid.add(new Label("Type"), 0, 1);
					dlgGrid.add(cbType, 1, 1);
					dlg.setContent(dlgGrid);
					dlg.show();
					workingInstit = tempInstit;
					for(int i = 0; i < DataArrays.simInstitutions.size(); i++){
						if(DataArrays.simInstitutions.get(i).institName.equalsIgnoreCase(structureCB.getValue())){
							tempInstit.institStruct = DataArrays.simInstitutions.get(i).institStruct;
						}
					}
					//System.out.println(workingInstit.institStruct);
					FormBuilderFunctions.formArrayBuilder(workingInstit.institStruct, workingInstit.institData);
					formBuilder(workingInstit.institStruct, workingInstit.institData);
					DataArrays.institNodes.add(tempInstit);
					facilityList.getItems().clear();
					prototypeList.getItems().clear();
					Label institutionName = new Label("Name");
					TextField nameTextField = FormBuilderFunctions.institNameBuilder(workingInstit);				
					grid.add(institutionName, 0, 0);
					grid.add(nameTextField, 1, 0);
					typeLabel.setText(workingInstit.type);
				} else {
					rowNumber = 1;
					grid.getChildren().clear();
					facilityList.getItems().clear();
					prototypeList.getItems().clear();
					workingInstit = CycicScenarios.workingCycicScenario.institNodes.get(structureCB.getItems().indexOf(newValue));
					for(String facility: workingInstit.availPrototypes) {
						prototypeList.getItems().add(facility);
					}
					for (facilityItem prototype: workingInstit.availFacilities) {
						facilityList.getItems().add(prototype.name + " - " + prototype.number);
					}
					formBuilder(workingInstit.institStruct, workingInstit.institData);
					Label institutionName = new Label("Name");
					TextField nameTextField = FormBuilderFunctions.institNameBuilder(workingInstit);
					grid.add(institutionName, 0, 0);
					grid.add(nameTextField, 1, 0);
					typeLabel.setText(workingInstit.type);
				}
			}
		});
		structureCB.autosize();
		topGrid.add(new Label("Choose Institution"), 0, 0);
		topGrid.add(structureCB, 1, 0);
		topGrid.add(typeLabel, 3, 0);
		
				
		// ComboBox to add facilities to the prototype ListView
		final ComboBox<String> addNewProtoBox = new ComboBox<String>();
		addNewProtoBox.setPromptText("Select Facility Type");
		addNewProtoBox.setOnMousePressed(new EventHandler<MouseEvent>(){
			public void handle(MouseEvent e){
				addNewProtoBox.getItems().clear();
				for (facilityNode node: CycicScenarios.workingCycicScenario.FacilityNodes){
					addNewProtoBox.getItems().add((String)node.name);
				}
			}
		});
		
		// Button to sumbit selected object in addNewPrototypeBox to Prototype ListView and Institution prototype array. 
		Button addAvailProto = new Button();
		addAvailProto.setText("Add Facility Type");
		addAvailProto.setOnAction(new EventHandler<ActionEvent>(){
			public void handle(ActionEvent e){
				prototypeList.getItems().clear();
				workingInstit.availPrototypes.add(addNewProtoBox.getValue());
				for (String facility: workingInstit.availPrototypes){
					prototypeList.getItems().add(facility);
				}
			}
		});
		
		//ComboBox for adding a new facility to the initial facility array of the institution.
		final ComboBox<String> addNewFacBox = new ComboBox<String>();
		addNewFacBox.setPromptText("Select Facility Type");
		addNewFacBox.setOnMousePressed(new EventHandler<MouseEvent>(){
			public void handle(MouseEvent e){
				addNewFacBox.getItems().clear();
				for (facilityNode node: CycicScenarios.workingCycicScenario.FacilityNodes){
						addNewFacBox.getItems().add((String)node.name);
				}
			}
		});
		
		// TextField to indicate the number of facilities to add at the start of the simulation.
		final TextField facilityNumber = new TextField(){
			// This bit of code prevents letters from being put into the TextField.
			@Override public void replaceText(int start, int end, String text) {
				if (!text.matches("[a-z]")){
					super.replaceText(start, end, text);
				}
			}
			
			public void replaceSelection(String text) {
				if (!text.matches("[a-z]")){
					super.replaceSelection(text);
				}
			}
		};
		facilityNumber.setPromptText("Integer");
		// Change Listener to update facilityItem with number of facilities to add at simulation start up
		
		// TODO Auto update list from TextField
		
		// ComboBox change listener to add new facility to the institutions initial facility list.
		addNewFacBox.valueProperty().addListener(new ChangeListener<String>(){
			public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue){
				for(facilityItem facility: workingInstit.availFacilities){
					if (facility.name == addNewFacBox.getValue()){
						facilityNumber.setText(facility.number);
					}
				}
			}
		});
		// Button to submit selection in addNewFacBox ComboBox.
		Button addAvailFac = new Button();
		addAvailFac.setText("Add Starting Facility");
		addAvailFac.setOnAction(new EventHandler<ActionEvent>(){
			public void handle(ActionEvent event){
				facilityList.getItems().clear();
				facilityItem facilityAdded = new facilityItem();
				facilityAdded.name = addNewFacBox.getValue();
				facilityAdded.number = facilityNumber.getText();
				workingInstit.availFacilities.add(facilityAdded);
				for (facilityItem facility: workingInstit.availFacilities){
					facilityList.getItems().add(facility.name + " - " + facility.number);
				}
			}
		});
		
		// Building the grids for the views.
		topGrid.add(new Label("Add Available Facility Type"), 0, 1);
		topGrid.add(addNewProtoBox, 1, 1);
		topGrid.add(addAvailProto, 2, 1);
		topGrid.add(new Label("Add Starting Facility"), 0, 2);
		topGrid.add(addNewFacBox, 1, 2);
		topGrid.add(new Label("Number: "), 2, 2);
		topGrid.add(facilityNumber, 3, 2);
		topGrid.add(addAvailFac, 4, 2);
		topGrid.setHgap(10);
		topGrid.setVgap(5);
		
		grid.autosize();
		grid.setAlignment(Pos.BASELINE_CENTER);
		grid.setVgap(10);
		grid.setHgap(5);
		grid.setPadding(new Insets(30, 30, 30, 30));
		grid.setStyle("-fx-background-color: silver;");
		
		// V and H boxes for the ListViews
		HBox institSideBar = new HBox();
		VBox facilitiesBox = new VBox();
		facilitiesBox.getChildren().addAll(new Label("Starting Facilities"), facilityList);
		//VBox prototypesBox = new VBox();
		//prototypesBox.getChildren().addAll(new Label("Prototypes"), prototypeList);
		institSideBar.setPadding(new Insets(0, 5, 0, 0));
		institSideBar.setMinWidth(200);
		institSideBar.setPrefWidth(200);
		institSideBar.getChildren().addAll(facilitiesBox);
		
		
		VBox institGridBox = new VBox();
		institGridBox.autosize();
		institGridBox.getChildren().addAll(topGrid, grid);		
		
		HBox institBox = new HBox();
		institBox.getChildren().addAll(institSideBar, institGridBox);
		setContent(institBox);

	}
	
	private ComboBox<String> structureCB = new ComboBox<String>();
	private GridPane grid = new GridPane();
	private GridPane topGrid = new GridPane(){
		{
			autosize();
		}
	};
	private Label typeLabel = new Label(){
		{
			autosize();
		}
	};
	private int rowNumber = 1;
	private int columnNumber = 0;
	private int columnEnd = 0;
	private int userLevel = 0;
	static instituteNode workingInstit;
	public static String TITLE;

	/**
	 * This function takes a constructed data array and it's corresponding 
	 * institution structure array and creates a form for the structure 
	 * and data arrays.
	 * @param facArray This is the structure of the data array. Included 
	 * in this array should be all of the information needed to fully 
	 * describe the data structure of a institution.
	 * @param dataArray The empty data array that is associated with 
	 * this institution. It should be built to match the structure
	 * of the institution structure passed to the form. 
	 */
	@SuppressWarnings("unchecked")
	public void formBuilder(ArrayList<Object> facArray, ArrayList<Object> dataArray){
		for (int i = 0; i < facArray.size(); i++){
			if (facArray.get(i) instanceof ArrayList && facArray.get(0) instanceof ArrayList) {
				formBuilder((ArrayList<Object>) facArray.get(i), (ArrayList<Object>) dataArray.get(i));
			} else if (i == 0){
				if (facArray.get(2) == "oneOrMore"){
					if ((int)facArray.get(6) <= userLevel && i == 0){
						Label name = new Label((String) facArray.get(0));
						grid.add(name, columnNumber, rowNumber);
						grid.add(orMoreAddButton(grid, (ArrayList<Object>) facArray, (ArrayList<Object>) dataArray), 1+columnNumber, rowNumber);
						rowNumber += 1;
						// Indenting a sub structure
						columnNumber += 1;
						for(int ii = 0; ii < dataArray.size(); ii ++){
							if ( ii > 0 ) {
								grid.add(arrayListRemove(dataArray, ii), columnNumber-1, rowNumber);
							}
							formBuilder((ArrayList<Object>)facArray.get(1), (ArrayList<Object>) dataArray.get(ii));	
							rowNumber += 1;
						}
						// resetting the indent
						columnNumber -= 1;
					}
				} else if (facArray.get(2) == "zeroOrMore") {
					if ((int)facArray.get(6) <= userLevel && i == 0){
						Label name = new Label((String) facArray.get(0));
						grid.add(name, columnNumber, rowNumber);
						grid.add(orMoreAddButton(grid, (ArrayList<Object>) facArray, (ArrayList<Object>) dataArray), 1+columnNumber, rowNumber);
						rowNumber += 1;
						// Indenting a sub structure
						columnNumber += 1;
						for(int ii = 0; ii < dataArray.size(); ii ++){
							grid.add(arrayListRemove(dataArray, ii), columnNumber-1, rowNumber);
							formBuilder((ArrayList<Object>)facArray.get(1), (ArrayList<Object>) dataArray.get(ii));	
							rowNumber += 1;
						}
						// resetting the indent
						columnNumber -= 1;
					}
				} else if (facArray.get(2) == "input" || facArray.get(2) == "output") {
					if ((int)facArray.get(6) <= userLevel){
						Label name = new Label((String) facArray.get(0));
						grid.add(name, columnNumber, rowNumber);
						rowNumber += 1;
						// Indenting a sub structure
						columnNumber += 1;
						for(int ii = 0; ii < dataArray.size(); ii ++){
							formBuilder((ArrayList<Object>)facArray.get(1), (ArrayList<Object>) dataArray.get(ii));						
						}
						// resetting the indent
						columnNumber -= 1;
					}
				} else {
					// Adding the label
					Label name = new Label((String) facArray.get(0));
					name.setTooltip(new Tooltip((String) facArray.get(7)));
					grid.add(name, columnNumber, rowNumber);
					// Setting up the input type for the label
					if (facArray.get(4) != null){
						// If statement to test for a continuous range for sliders.
						if (facArray.get(4).toString().split("[...]").length > 1){
							Slider slider = FormBuilderFunctions.sliderBuilder(facArray.get(4).toString(), dataArray.get(0).toString());
							TextField textField = FormBuilderFunctions.sliderTextFieldBuilder(slider, dataArray);
							grid.add(slider, 1+columnNumber, rowNumber);
							grid.add(textField, 2+columnNumber, rowNumber);
							columnEnd = 2+columnNumber+1;
						// Slider with discrete steps
						} else {
							ComboBox<String> cb = FormBuilderFunctions.comboBoxBuilder(facArray.get(4).toString(), dataArray);
							grid.add(cb, 1+columnNumber, rowNumber);
							columnEnd = 2 + columnNumber;
						}
					} else {
						// Switch that will contain current and future key words to indicate special form functions.
						switch ((String) facArray.get(0)) {
						/*case "Incommodity":
							grid.add(FormBuilderFunctions.comboBoxInCommod(formNode, dataArray), 1+columnNumber, rowNumber);
							break;
						case "Outcommodity":
							grid.add(FormBuilderFunctions.comboBoxOutCommod(formNode, dataArray), 1+columnNumber, rowNumber);
							break;
						case "Recipe":
							grid.add(FormBuilderFunctions.recipeComboBox(formNode, dataArray), 1+columnNumber, rowNumber);
							break;*/
						default:
							grid.add(FormBuilderFunctions.textFieldBuilder(facArray, (ArrayList<Object>)dataArray), 1+columnNumber, rowNumber);
							columnEnd = 2 + columnNumber;
							break;
						}
					}
					grid.add(FormBuilderFunctions.unitsBuilder((String)facArray.get(3)), columnEnd, rowNumber);
					columnEnd = 0;
					rowNumber += 1;
				}
			}
		}
	}
	
	/**
	 * Function to add an orMore button to the form. 
	 * This button allows the user to add additional 
	 * fields to zeroOrMore or oneOrMore form inputs.
	 * @param grid This is the grid of the current view. 
	 * @param facArray The ArrayList<Object> used to make 
	 * a copy of the one or more field. 
	 * @param dataArray The ArrayList<Object> the new 
	 * orMore field will be added to.
	 * @return Button that will add the orMore field 
	 * to the dataArray and reload the form.
	 */
	public Button orMoreAddButton(final GridPane grid, final ArrayList<Object> facArray,final ArrayList<Object> dataArray){
		Button button = new Button();
		button.setText("Add");
		
		button.setOnAction(new EventHandler<ActionEvent>(){
			public void handle(ActionEvent e){
 				FormBuilderFunctions.formArrayBuilder(facArray, (ArrayList<Object>) dataArray);
				grid.getChildren().clear();
				rowNumber = 1;
				Label institutionName = new Label("Name");
				TextField nameTextField = FormBuilderFunctions.institNameBuilder(workingInstit);
				grid.add(institutionName, 0, 0);
				grid.add(nameTextField, 1, 0);
				formBuilder(workingInstit.institStruct, workingInstit.institData);
			}
		});
		return button;
	}
	
	/**
	 * This function removes a orMore that has been 
	 * added to a particular field.
	 * @param dataArray The ArrayList<Object> containing the orMore field
	 * @param dataArrayNumber the index number of the orMore field 
	 * that is to be removed.
	 * @return Button for executing the commands in this function.
	 */
	public Button arrayListRemove(final ArrayList<Object> dataArray, final int dataArrayNumber){
		Button button = new Button();
		button.setText("Remove");
		
		button.setOnAction(new EventHandler<ActionEvent>(){
			public void handle(ActionEvent e) {
				dataArray.remove(dataArrayNumber);
				grid.getChildren().clear();
				rowNumber = 1;
				Label institutionName = new Label("Name");
				TextField nameTextField = FormBuilderFunctions.institNameBuilder(workingInstit);
				grid.add(institutionName, 0, 0);
				grid.add(nameTextField, 1, 0);
				formBuilder(workingInstit.institStruct, workingInstit.institData);
			}
		});
		return button;
	}
}
