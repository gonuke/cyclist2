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
 *     Kristin Potter
 *******************************************************************************/
package edu.utah.sci.cyclist.core.ui.wizards;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLSyntaxErrorException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.apache.log4j.Logger;

import javafx.animation.RotateTransition;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
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
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;
import edu.utah.sci.cyclist.Cyclist;
import edu.utah.sci.cyclist.core.model.Blob;
import edu.utah.sci.cyclist.core.model.CyclistDatasource;
import edu.utah.sci.cyclist.core.model.Simulation;
import edu.utah.sci.cyclist.core.ui.components.SQLitePage;
import edu.utah.sci.cyclist.core.ui.components.UpdateDbDialog;
import edu.utah.sci.cyclist.core.util.SimulationTablesPostProcessor;

public class SqliteLoaderWizard extends VBox {
	
	private Stage _dialog;
	private String _fileName = "";
	private VBox _errorMessageBox = null;
	private VBox _vbox = null;
	private TextArea _statusText;
	private RotateTransition _animation;
	private UpdateDbDialog _updateDialog;
	private ObservableList<Simulation> _selection =  FXCollections.observableArrayList();
	private ObjectProperty<Boolean> _dsIsValid  = new SimpleObjectProperty<>();
	private List<CyclistDatasource> _sources = null;
	TextField _path;
	double _windowX=0, _windowWidth = 0, _windowY=0, _windowHeight=0;
	
	
	private static final String SIMULATION_ID_FIELD_NAME = "SimID";
	private static final String SIMULATION_ID_QUERY = "SELECT DISTINCT " + SIMULATION_ID_FIELD_NAME  +" FROM Info order by SimID";
	private static String SIMULATION_INFO_QUERY = "select initialYear, initialMonth, Duration from Info where SimID=?";
	
	/**
	 * Shows the dialog.
	 * @param Window window - the owner.
	 * @return  ObservableList<Simulation> - list of the simulations in the sqlite database.
	 */
	public ObservableList<Simulation> show(Window window) {
		_dialog.initOwner(window);
//		_dialog.show();
		_windowX=window.getX();
		_windowWidth=window.getWidth();
		_windowY=window.getY();
		_windowHeight=window.getHeight();
//		_dialog.setX(window.getX() + (window.getWidth() - _dialog.getWidth())*0.5);
//		_dialog.setY(window.getY() + (window.getHeight() - _dialog.getHeight())*0.5);
		_dialog.setWidth(350);
		_dialog.setHeight(120);
		return _selection;
	}
	
	public SqliteLoaderWizard(List<CyclistDatasource> sources) {	
		createDialog();
		_sources = new ArrayList<>(sources);
	}
	
	public void runOperations(){
		showFileChoser();
	}
	
	private void createDialog(){
		_dialog = new Stage();
		_dialog.setTitle("Load Sqlite");
		_dialog.initModality(Modality.WINDOW_MODAL);
		_dialog.setScene( createScene(_dialog) );	
		_dialog.centerOnScreen();
	}
	
	private Scene createScene(final Stage dialog) {
		HBox pane = new HBox();  //set spacing;
		pane.setAlignment(Pos.CENTER);
		pane.setPadding(new Insets(5));
		pane.setSpacing(10);
		pane.setMinWidth(250);
		
		_path = new TextField();
		_path.setPrefWidth(250);
		
//		Button button = new Button("...");
//		button.setFont(new Font(15));
//		button.getStyleClass().add("flat-button");
//		button.setOnAction(new EventHandler<ActionEvent>() {
//				 @Override
//				 public void handle(ActionEvent event) {
//					 showFileChoser();
//				 }
//		});
		
		
		pane.getChildren().addAll(/*new Text("Path:"),*/_path/*,button*/);
	
//		HBox.setHgrow(_path, Priority.ALWAYS);
		
		HBox buttons = new HBox();
//		buttons.setSpacing(10);
//		buttons.setPadding(new Insets(5));
		buttons.setAlignment(Pos.CENTER_RIGHT);
		
		Button cancel = new Button("Cancel");
		cancel.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent event) {
				dialog.close();
			}
		});
		
		Button ok = new Button("Ok");
		ok.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent event) {
				CyclistDatasource ds = getDataSource(_path.getText());
				if(ds != null){
					Boolean updateReuired = SimulationTablesPostProcessor.isDbUpdateRequired(ds);
					if(updateReuired){
						setDbUpdate(true,ds);
		 				_dsIsValid.addListener(new ChangeListener<Boolean>(){
		 					@Override
		 					public void changed(ObservableValue<? extends Boolean> arg0, Boolean oldVal, Boolean newVal) {
		 						_dsIsValid.setValue(newVal);
		 						setSimulations(ds);
		 					}
		 				});
					}
					else{
						_dsIsValid.set(true);
						setSimulations(ds);
					}
				}
			}
		});
		
		buttons.getChildren().addAll(cancel,ok);
			
		HBox.setHgrow(buttons,  Priority.ALWAYS);
		
		// Controls for the update database dialog part
		_statusText = new TextArea();
		_animation = new RotateTransition();
		_updateDialog = new UpdateDbDialog(_statusText, _animation);
		
		//The error message VBox, to be displayed when more than one simulation.
		_errorMessageBox = new VBox();
		_errorMessageBox.setSpacing(15);
		_errorMessageBox.setPadding(new Insets(10));
		_errorMessageBox.setAlignment(Pos.CENTER);
		
		Button dismiss = new Button("Dismiss");
		dismiss.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent event) {
				_selection.clear();
				dialog.close();
			}
		});
		
		HBox btn = new HBox();
		btn.setSpacing(10);
		btn.setPadding(new Insets(5));
		btn.setAlignment(Pos.CENTER_RIGHT);
		
		btn.getChildren().add(dismiss);
		
		Text errorText = new Text("Load simulations from the current database failed");
		
		_errorMessageBox.getChildren().addAll(errorText, btn);
		
		_vbox = new VBox();
		_vbox.setSpacing(5);
		_vbox.setPadding(new Insets(5));
		_vbox.getChildren().addAll(pane/*,buttons*/);
		
		Scene scene = new Scene(_vbox);
		
        scene.getStylesheets().add(Cyclist.class.getResource("assets/Cyclist.css").toExternalForm());
		return scene;
	}

	private void setSimulations(CyclistDatasource ds){
		List<Simulation> simulations = null;
		if(_dsIsValid.getValue()){
			//Return an existing data source with the same path, if already exists.
			simulations = new ArrayList<>();
			CyclistDatasource dataSource = getExistingDs(ds);
			simulations= getSimulations(dataSource);
		}else{
			setErrorDisplay();
		}
		
		if(simulations != null && simulations.size() >0){
			_selection.addAll(simulations);
			_dialog.close();
		}else if(simulations != null){   //No simulations in list.
			_vbox.getChildren().clear();
			_vbox.getChildren().add(_errorMessageBox);
			_dialog.close();
		}
	}
	
	/*
	 * Displays the file choser tool to choose a sqlite file.
	 */
	private void showFileChoser(){
		 FileChooser chooser = new FileChooser();
		 chooser.getExtensionFilters().add( new FileChooser.ExtensionFilter("SQLite files (*.sqlite)", "*.sqlite") );
		 File file = chooser.showOpenDialog(null);
		 if (file != null){
			 _path.setText(file.getPath());
			 _fileName = file.getName();
			 setChosenDatabaseFile();
		 }else{
			 _dialog.close();
		 }
	}
	
	/*
	 * Sets the datasource as the sqlite file and tries to fetch the simulations from this data source.
	 */
	private void setChosenDatabaseFile(){
		CyclistDatasource ds = getDataSource(_path.getText());
		if(ds != null){
			Boolean updateReuired = SimulationTablesPostProcessor.isDbUpdateRequired(ds);
			if(updateReuired){
				setDbUpdate(true,ds);
 				_dsIsValid.addListener(new ChangeListener<Boolean>(){
 					@Override
 					public void changed(ObservableValue<? extends Boolean> arg0, Boolean oldVal, Boolean newVal) {
 						_dsIsValid.setValue(newVal);
 						setSimulations(ds);
 					}
 				});
			}
			else{
				_dsIsValid.set(true);
				setSimulations(ds);
			}
		}else{
			_dialog.close();
		}
	}
	
	/*
	 * Checks if the simulation data source already exists in the model sources list.
	 * If yes - use it as the simulation datasource. (to make sure the datasource uid is the same).
	 * This is important since if the data source alre
	 */
	private CyclistDatasource getExistingDs(CyclistDatasource ds){
		CyclistDatasource dataSource = ds;
		for(CyclistDatasource source : _sources){
			if(source.getURL().equals(ds.getURL())){
				dataSource = source;
				break;
			}
		}
		return dataSource;
	}
	
	/*
	 * Get the url from the jdbc and sqlite file path,
	 * for the creation of data source.
	 * @return String = the generated url.
	 */
	private  String getURL(String path) {
		if(!path.isEmpty()){
			return "jdbc:sqlite:/"+path;
		}else{
			return "";
		}
	}
	
	/*
	 * Gets the path of a Sqlite file and creates its Cyclist data source.
	 * @param path - the path of the Sqlite file
	 * @return CyclistDatasource = the datasource created from the given path.
	 */
	private  CyclistDatasource getDataSource(String path) {
		Logger log = Logger.getLogger(SQLitePage.class);
		CyclistDatasource ds = null;
		
		try {
			Class.forName("org.sqlite.JDBC");
		} catch (ClassNotFoundException e) {
			log.warn("Can not load sqlite driver", e);
		}

		String url = getURL(path);
		if(!url.isEmpty()){
			ds = new CyclistDatasource();
			ds.setURL(getURL(path));
			Properties p = ds.getProperties();
			p.setProperty("driver", "sqlite");
			p.setProperty("type", "SQLite");
			p.setProperty("path", path);
			String name = _fileName;
			p.setProperty("name", name);
		}
		return ds;	
	}
	
	/*
	 * Retrieves a simulation from the specified data source.
	 * If there is more than one simulation - displays an error message and returns null.
	 * @param CyclistDatasource ds - the data source to look for the simulation.
	 * @return Simulation = the simulation found in the specified data source.
	 */
	private List<Simulation> getSimulations(CyclistDatasource ds){
		List<Simulation> simulations = new ArrayList<>();
		try (Connection conn = ds.getConnection()) {
			Blob simulationId = null;
			Simulation simulation = null;
			Statement stmt = conn.createStatement();
			ResultSet rs = null;
			rs = stmt.executeQuery(SIMULATION_ID_QUERY);
			while (rs.next()) {
				 simulationId = new Blob(rs.getBytes(SIMULATION_ID_FIELD_NAME));
				if(simulationId != null ){
					simulation = new Simulation(simulationId);
					simulation.setDataSource(ds);
					String alias = _fileName.substring(0,_fileName.indexOf(".sqlite"));
					simulation.setAlias(alias);
					fetchSimulationInfo(simulation, conn);
					simulations.add(simulation);
				}
			}
			if(simulations.size()>1){
				for(int i=0;i<simulations.size();i++){
					Simulation sim = simulations.get(i);
					sim.setAlias(sim.getAlias()+"-"+(i+1));
				}
			}
			return simulations;
			
		}catch(SQLSyntaxErrorException e){
			System.out.println("Table for SimId doesn't exist");
			setErrorDisplay();
			return null;
		}
		catch (Exception e) {
			System.out.println("Get simulation failed");
			setErrorDisplay();
			return null;
		}finally{
			ds.releaseConnection();
		}
	}
	
	/* 
	 * shows the dialog, and displays an error message with a "dismiss" button.
	 */
	private void setErrorDisplay(){
		_dialog.show();
		_dialog.setX(_windowX + (_windowWidth - _dialog.getWidth())*0.5);
		_dialog.setY(_windowY + (_windowHeight - _dialog.getHeight())*0.5);
		_vbox.getChildren().clear();
		_vbox.getChildren().add(_errorMessageBox);
	}
	
	/*
	 * Gets additional simulation details.
	 * @param Simulation sim - the simulation to it's details.
	 * @param Connection conn - the connection to the data source of the simulation.
	 */
	private void fetchSimulationInfo(Simulation sim, Connection conn) {
			PreparedStatement stmt;
			try {
				stmt = conn.prepareStatement(SIMULATION_INFO_QUERY);
				stmt.setBytes(1, sim.getSimulationId().getData());
				ResultSet rs = stmt.executeQuery();
				if (rs.next()) {
					sim.setStartYear(rs.getInt(1));
					sim.setStartMonth(rs.getInt(2));
					sim.setDuration(rs.getInt(3));
				}
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	}
	
	/*
	 * Checks the argument "isStart":
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
	private void setDbUpdate(Boolean isStart, CyclistDatasource ds){
		if(isStart){
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
			_updateDialog.hide();
			_statusText.textProperty().unbind();
		}

	}
	
	/*
	 * Calls the post processing utility to perform a database update.
	 * Updates the animation and the status text to display the database update status to the user.
	 * @param CyclistDatasource ds - the data source to update.
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
}
