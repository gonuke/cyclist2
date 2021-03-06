package edu.utah.sci.cyclist.core.ui.wizards;
/*******************************************************************************
 * Copyright (c) 2013 SCI Institute, University of Utah.
 * All rights reserved.
 *
 * License for the specific language governing rights and limitations under Permission
 * is hereby granted, free of charge, to any person obtaining a copy of this software and
 * associated documentation files (the "Software"), to deal in the Software without restriction, 
 * including without limitation the rights to use, copy, modify, merge, publish, distribute, 
 * sublicense, and/or sell copies of the Software, and to permit persons to whom the 
 * Software is furnished to do so, subject to the following conditions: The above copyright notice 
 * and this permission notice shall be included in all copies  or substantial portions of the Software. 
 *  
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, 
 *  INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR 
 *  A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR 
 *  COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER 
 *  IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION 
 *  WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 * Contributors:
 *     Yarden Livnat  
 *     Kristi Potter
 *******************************************************************************/

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLSyntaxErrorException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import javafx.animation.RotateTransition;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableColumn.CellEditEvent;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;
import edu.utah.sci.cyclist.Cyclist;
import edu.utah.sci.cyclist.core.model.Blob;
import edu.utah.sci.cyclist.core.model.CyclistDatasource;
import edu.utah.sci.cyclist.core.model.Simulation;
import edu.utah.sci.cyclist.core.ui.components.UpdateDbDialog;
import edu.utah.sci.cyclist.core.util.AwesomeIcon;
import edu.utah.sci.cyclist.core.util.GlyphRegistry;
import edu.utah.sci.cyclist.core.util.SimulationTablesPostProcessor;


/*
 * Class to allow the user to create or edit a data table.
 * Also controls the creation/editing of data sources.
 */
public class SimulationWizard extends TilePane {

	private static String SIMULATION_INFO_QUERY = "select initialYear, initialMonth, Duration from Info where SimID=?";

	// GUI elements
	private Stage                       _dialog;
	private ListView<CyclistDatasource> _sourcesView;
//	private ImageView                   _statusDisplay;
	private Label						_status;
	private TableView<SimInfo>		    _simulationsTbl;
	private ObservableList<SimInfo> 	_simData =FXCollections.observableArrayList();
	private ObservableList<CyclistDatasource> _sources = FXCollections.observableArrayList();
	private UpdateDbDialog				_updateDialog;
	private ObjectProperty<Boolean> _dsIsValid  = new SimpleObjectProperty<>();
	private TextArea _statusText;
	private RotateTransition _animation;
	private List<Simulation> _aliases;
	
	   
	// DataType elements
	private CyclistDatasource     _current;
	private ObservableList<Simulation> _selection =  FXCollections.observableArrayList();
	private static final String SIMULATION_ID_FIELD_NAME = "SimID";
	private static final String SIMULATION_ID_QUERY = "SELECT DISTINCT " + SIMULATION_ID_FIELD_NAME  +" FROM Info order by SimID";
	// Gets the simulation in a string format, in case it is stored in a field of BLOB type.
	private static final String SIMULATION_ID_BLOB_QUERY = "SELECT DISTINCT quote(" + SIMULATION_ID_FIELD_NAME  +") AS SimID FROM Info order by SimID";
	//Gets the information of all the fields and their types in the Info table.
	private static final String INFO_TABLE_DATA = "PRAGMA table_info(Info)";
		
	// * * * Constructor creates a new stage * * * //
	public SimulationWizard() {
		createDialog();
	}
	
	// * * * Set the existing data sources in the combo box * * * //
	public void setItems(final ObservableList<CyclistDatasource> sources, Set<Simulation> aliases) {
		_sources = sources;
		_sourcesView.setItems(_sources);
		_sourcesView.getSelectionModel().selectFirst();
		_aliases = new ArrayList<>(aliases);
	}
			
	// * * * Show the dialog * * * //
	public  ObservableList<Simulation>  show(Window window) {

		 _dialog.initOwner(window);
		 _dialog.show();	
		 
		// TODO: hopefully in JAVA 8 moving this to be BEFORE the show() will make it not flash, but at the moment it doesn't work
		// Moves window to be in the middle of the main window 
		_dialog.setX(window.getX() + (window.getWidth() - _dialog.getWidth())*0.5);
		_dialog.setY(window.getY() + (window.getHeight() - _dialog.getHeight())*0.5);
		 	
		return _selection;
	}
	
	// * * * Create the dialog
	private void createDialog(){
		
		_dialog = new Stage();
		_dialog.setTitle("Simulations");
		
		
		_dialog.initModality(Modality.WINDOW_MODAL);
		_dialog.setScene( createScene(_dialog) );	
	
		System.out.println("changed" + _sourcesView.getSelectionModel().selectedItemProperty()) ;
	}
	
	// * * * Create scene creates the GUI * * * //
	private Scene createScene(final Stage dialog) {
			
		// * * * The connection settings group
		VBox connectionBox = new VBox();
		connectionBox.setSpacing(5);
		connectionBox.setAlignment(Pos.CENTER_LEFT);
		
		// Add, edit, or remove connection box
		HBox dataSrcHbox = new HBox();
		dataSrcHbox.setSpacing(10);
		dataSrcHbox.setAlignment(Pos.CENTER_LEFT);
		dataSrcHbox.setPadding(new Insets(5));
		
		HBox conHBox = new HBox();
		conHBox.setSpacing(10);
		conHBox.setPadding(new Insets(5));
		conHBox.setAlignment(Pos.CENTER_LEFT);
		
//		_statusDisplay = new ImageView();
		_status = new Label();
		
		connectionBox.getChildren().addAll(dataSrcHbox,conHBox,_status /*_statusDisplay*/);
		
		// Sources box
		VBox srcVbox = new VBox();
		srcVbox.setSpacing(5);
		srcVbox.setAlignment(Pos.CENTER_LEFT);
		
		// Add/Edit/Remove Buttons
		VBox buttonsVbox = new VBox();
		buttonsVbox.setSpacing(5);
		buttonsVbox.setAlignment(Pos.CENTER);
		
		dataSrcHbox.getChildren().addAll(srcVbox,buttonsVbox);
		
		Text txt = new Text("Data Sources");
		_sourcesView = new ListView<CyclistDatasource>();
		
		HBox.setHgrow(srcVbox, Priority.ALWAYS);
		HBox.setHgrow(_sourcesView, Priority.ALWAYS);
		
		
		srcVbox.getChildren().addAll(txt,_sourcesView);
		
		Button addButton = new Button("Add");
		addButton.setMinWidth(75);
//		addButton.getStyleClass().add("flat-button");
		
		Button editButton = new Button("Edit");
		editButton.setMinWidth(75);
		
		Button removeButton = new Button("Remove");
		removeButton.setMinWidth(75);
		
		buttonsVbox.getChildren().addAll(addButton,editButton,removeButton);
		
		Button selectionButton = new Button("Connect");
		selectionButton.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent arg0) {
				if(SimulationTablesPostProcessor.isDbUpdateRequired(_current)){
					setDbUpdate(true,_current);
	 				_dsIsValid.addListener(new ChangeListener<Boolean>(){
	 					@Override
	 					public void changed(ObservableValue<? extends Boolean> arg0, Boolean oldVal, Boolean newVal) {
	 						selectConnection(_current,newVal);
	 					}
	 				});
				}else{
					selectConnection(_current,true);
				}
			}			
		});
//		_statusDisplay = new ImageView();
		conHBox.getChildren().addAll(selectionButton, _status/*_statusDisplay*/);
		
		_sourcesView.setId("datasources-list");
		_sourcesView.setMinSize(100, 50);

		// Keep track of the currently selected data source
		_sourcesView.getSelectionModel().selectedItemProperty().addListener(
				new ChangeListener<CyclistDatasource>() {
					public void changed(ObservableValue<? extends CyclistDatasource> ov, 
							CyclistDatasource old_val, CyclistDatasource new_val) {
						
						_current = _sourcesView.getSelectionModel().getSelectedItem();
//						_statusDisplay.setImage(null);
						_status.setGraphic(null);	
					}
				});
		
		
	
		// Disable edit/remove until we have something selected
		editButton.disableProperty().bind( _sourcesView.getSelectionModel().selectedItemProperty().isNull());
		removeButton.disableProperty().bind( _sourcesView.getSelectionModel().selectedItemProperty().isNull());
		selectionButton.disableProperty().bind( _sourcesView.getSelectionModel().selectedItemProperty().isNull());
		
		// add button actions
		addButton.setOnAction(new EventHandler<ActionEvent>(){
			@Override
			public void handle(ActionEvent event) {
				selectDatasource(new CyclistDatasource());		
			}	
		});	

		editButton.setOnAction(new EventHandler<ActionEvent>(){
			@Override
			public void handle(ActionEvent event) {
				selectDatasource(_current);		
			}	
		});	
		
		removeButton.setOnAction(new EventHandler<ActionEvent>(){
			@Override
			public void handle(ActionEvent event) {			
				_sources.remove(_current);		
			}	
		});	
		
		VBox simulationBox = new VBox();
		simulationBox.setSpacing(5);
		simulationBox.setPadding(new Insets(5));
		simulationBox.setMaxHeight(Double.MAX_VALUE);
		
		Label simLbl =  new Label("Select Simulation:");
		simLbl.setAlignment(Pos.CENTER);
		simLbl.setFont(new Font(12));
		simLbl.setPadding(new Insets(3,0,0,0));
		
		_simulationsTbl = new TableView<SimInfo>();
		_simulationsTbl.setEditable(true);
		_simulationsTbl.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
		_simulationsTbl.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
		TableColumn<SimInfo, String> simIdCol = new TableColumn<SimInfo, String>("Simulation Id");
		simIdCol.setCellValueFactory(new PropertyValueFactory<SimInfo, String>("simId"));
		simIdCol.setEditable(false);
		
		TableColumn<SimInfo, String> simAliasCol = new TableColumn<SimInfo, String>("Alias");
		
//		simAliasCol.setCellFactory(CustomizedTableColumn(new DefaultStringConverter()));
		simAliasCol.setCellFactory(TextFieldTableCell.forTableColumn());
		
		simAliasCol.setCellValueFactory(new PropertyValueFactory<SimInfo, String>("alias"));
		simAliasCol.setEditable(true);
		_simulationsTbl.setItems(_simData);
		_simulationsTbl.getColumns().addAll(Arrays.asList(simIdCol, simAliasCol));
		
		simAliasCol.setOnEditCommit(
	            new EventHandler<CellEditEvent<SimInfo, String>>() {
	                @Override
	                public void handle(CellEditEvent<SimInfo, String> t) {
	                	((SimInfo) t.getTableView().getItems().get(
	                            t.getTablePosition().getRow())
	                            ).setAlias(t.getNewValue());
	                }
	            }
	        );
			
		simulationBox.getChildren().addAll(simLbl, _simulationsTbl);
		simulationBox.disableProperty().bind(_sourcesView.getSelectionModel().selectedItemProperty().isNull());
		VBox.setVgrow(srcVbox, Priority.ALWAYS);
		VBox.setVgrow(simulationBox,  Priority.ALWAYS);
		
		// The ok/cancel buttons
		HBox buttonsBox = new HBox();
		buttonsBox.setSpacing(10);
		buttonsBox.setAlignment(Pos.CENTER_RIGHT);
		buttonsBox.setPadding(new Insets(5));
		
		Button btnCancel = new Button("Cancel");
		btnCancel.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent arg0) {
				dialog.hide();
			};
		});
		Button btnOk = new Button("OK");
		btnOk.setOnAction(new EventHandler<ActionEvent>() {	
			@Override
			public void handle(ActionEvent arg0) {
				updateSimulation();
				dialog.hide();
			};
		});
		
		buttonsBox.getChildren().addAll(btnCancel,btnOk);	
		HBox.setHgrow(buttonsBox,  Priority.ALWAYS);
		
		btnOk.disableProperty().bind(_simulationsTbl.getSelectionModel().selectedItemProperty().isNull());
	
		
		// Create the scene
		VBox wizardVbox = new VBox();
		wizardVbox.setSpacing(5);
		wizardVbox.setPadding(new Insets(5));
		wizardVbox.setPrefHeight(400);
		wizardVbox.setId("simulation-wizard");
		Scene scene = new Scene(wizardVbox);
		wizardVbox.getChildren().addAll(connectionBox,simulationBox,buttonsBox);
		
		scene.getStylesheets().add(Cyclist.class.getResource("assets/Cyclist.css").toExternalForm());
	
		_sourcesView.getSelectionModel().selectFirst();
		
		_statusText = new TextArea();
		_animation = new RotateTransition();
		
		// Return the scene
		return scene;
	}
	
	/**
	 * selectDataSource
	 */
	private void selectDatasource(CyclistDatasource datasource){
		DatasourceWizard wizard = new DatasourceWizard(datasource);
		ObjectProperty<CyclistDatasource> selection = wizard.show(_dialog.getScene().getWindow());
		selection.addListener(new ChangeListener<CyclistDatasource>(){
			@Override
			public void changed(ObservableValue<? extends CyclistDatasource> arg0, CyclistDatasource oldVal, CyclistDatasource newVal) {
				if (!_sourcesView.getItems().contains(newVal)){
					_sourcesView.getItems().add(newVal);
				}else{
					//A ListView hack: in order to refresh the displayed items, Should change the number of items in the list.
					CyclistDatasource demoDs = new CyclistDatasource();
					demoDs.setName("demo");
					_sourcesView.getItems().add(demoDs);
					_sourcesView.getItems().remove(demoDs);
				}
					_sourcesView.getSelectionModel().select(newVal);
			}
		});
	}
	
	private void selectConnection(CyclistDatasource ds, Boolean dsIsValid) {
		
		_simData.clear();
	
		if(!dsIsValid){
			_status.setGraphic(GlyphRegistry.get(AwesomeIcon.WARNING));
		}else{
		
		
			try (Connection conn = ds.getConnection()) {
				_status.setGraphic(GlyphRegistry.get(AwesomeIcon.CHECK));//"FontAwesome|OK"));
				Statement stmt = conn.createStatement();
				ResultSet rs = null;
				rs = stmt.executeQuery(SIMULATION_ID_QUERY);
				while (rs.next()) {
					 Blob simulationId = new Blob(rs.getBytes(SIMULATION_ID_FIELD_NAME));
					 String alias = getAlias(simulationId.toString());
					 _simData.add(new SimInfo(simulationId, alias));
				}
				
				_simulationsTbl.setItems(_simData);
				
			}catch(SQLSyntaxErrorException e){
				System.out.println("Table for SimId doesn't exist");
			}
			catch (Exception e) {
				_status.setGraphic(GlyphRegistry.get(AwesomeIcon.WARNING));//"FontAwesome|WARNING"));
			}finally{
				ds.releaseConnection();
			}
		}
	}
	
	/*
	 * Checks the argument "isRunning":
	 * If true - 
	 * 	it's the beginning of the database update process. 
	 * 	Display the updateDb dialog and ask the user whether or not to update the database.
	 * 	If user approves - start the update process.
	 * 	If user cancels - hide the dialog and set the datasource validity to false.
	 * 
	 * If false - the data base update is done - close the update dialog.
	 * @param isStart - is it the start or the end of the process.
	 * @CyclistDatasource ds - the datasource to update.
	 * 
	 */
	private void setDbUpdate(Boolean isRunning, CyclistDatasource ds){
			if(isRunning){
					_updateDialog = new UpdateDbDialog(_statusText, _animation);
					_statusText.clear();  //in case it is not the first time it is called.
					ObjectProperty<Boolean> selection = _updateDialog.show(_dialog.getScene().getWindow());
					selection.addListener(new ChangeListener<Boolean>(){
						@Override
						public void changed(ObservableValue<? extends Boolean> arg0, Boolean oldVal,Boolean newVal) {
							if(newVal){
								runDbUpdate(ds);
							}else{
								_dsIsValid.set(false);
								_statusText.textProperty().unbind();
								_updateDialog.hide();
							}
						}
					});
			}else{
				_statusText.textProperty().unbind();
				_updateDialog.hide();
			}
	}
		
	/*
	 * Calls the post processing utility to perform a database update.
	 * Updates the animation and the status text to display the database update status to the user.
	 * @param CyclistDatasource ds - the data source to update 
	 */
	private void runDbUpdate(final CyclistDatasource ds){
			SimulationTablesPostProcessor postProcessor = new SimulationTablesPostProcessor();
			Task<Boolean> task = postProcessor.process(ds);
			if(task != null){	
				task.valueProperty().addListener(new ChangeListener<Boolean>() {
					 
			        @Override 
			        public void changed(ObservableValue<? extends Boolean> arg0,Boolean oldVal, Boolean newVal) {
			        	_animation.stop();
			        	_dsIsValid.set(newVal);
			        	setDbUpdate(false, ds);
			        }
			    });
			
				_statusText.textProperty().bind(task.messageProperty());
				_animation.play();
			}
	}

	private void updateSimulation() {
		List<SimInfo> simulations = _simulationsTbl.getSelectionModel().getSelectedItems();
		_selection.clear();
		if(simulations.size()>0){
			for(SimInfo simInfo:simulations){
				Blob simId = simInfo.getSimId();
				Simulation simulation = new Simulation(simId);
				simulation.setDataSource(_current);
				String alias = simInfo.getAlias();
				if(alias == null || alias.isEmpty()){
					alias = simId.toString();
				}
				simulation.setAlias(alias);
				fetchSimulationInfo(simulation);
				_selection.add(simulation);
			}
		}
	}
	
	private String getAlias(String simId){
		String alias = "";
		for(Simulation sim : _aliases){
			if(sim.getSimulationId().toString().equals(simId)){
				alias = sim.getAlias();
				break;
			}
		}
		return alias;
	}
	
	public CyclistDatasource getSelectedSource() {
		return _current;
	}

	public void setSelectedSource(CyclistDatasource source) {
		_current = source;	
		_sourcesView.getSelectionModel().select(_current);
	}
	
	public void setWorkDir(String workDir){
	}
		
	private void fetchSimulationInfo(Simulation sim) {
		try (Connection conn = sim.getDataSource().getConnection()) {
			PreparedStatement stmt = conn.prepareStatement(SIMULATION_INFO_QUERY);
			stmt.setBytes(1, sim.getSimulationId().getData());
			ResultSet rs = stmt.executeQuery();
			if (rs.next()) {
				sim.setStartYear(rs.getInt(1));
				sim.setStartMonth(rs.getInt(2));
				sim.setDuration(rs.getInt(3));
			}
		} catch (SQLException e) {
			System.out.println("Error fetching simulation information:"+e.getMessage());
		} finally {
			sim.getDataSource().releaseConnection();
		}
	}
	
	public static class SimInfo {
		private final SimpleObjectProperty<Blob> simId;
		private final SimpleStringProperty alias;
		
		private SimInfo(Blob simId, String alias){
			this.simId = new SimpleObjectProperty<Blob>(simId);
			this.alias = new SimpleStringProperty(alias);
		}
		
		public Blob getSimId() {
            return simId.get();
        }
        
		public void setSimId(Blob simId) {
        	this.simId.set(simId);
        }
		
		public String getAlias() {
            return alias.get();
        }
        public void setAlias(String alias) {
            this.alias.set(alias);
        }
	}
}
