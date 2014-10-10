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
package edu.utah.sci.cyclist.core.ui.panels;

import java.util.ArrayList;
import java.util.List;

import javafx.beans.property.ListProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.geometry.Side;
import javafx.scene.Scene;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import org.mo.closure.v1.Closure;

import edu.utah.sci.cyclist.core.model.CyclusJob;
import edu.utah.sci.cyclist.core.model.CyclusJob.Status;
import edu.utah.sci.cyclist.core.model.Simulation;
import edu.utah.sci.cyclist.core.util.AwesomeIcon;
import edu.utah.sci.cyclist.core.util.GlyphRegistry;
import edu.utah.sci.cyclist.core.util.LoadSqlite;

public class JobsPanel extends TitledPanel  {
	public static final String ID 		= "jobs-panel";
	public static final String TITLE	= "Jobs";
	
	public static final String SELECTED_STYLE = "-fx-background-color: #99ccff";
	public static final String UNSELECTED_STYLE = "-fx-background-color: #f0f0f0";
	public static final long ALIAS_EDIT_WAIT_TIME = 1000;
	
	
	private ContextMenu _menu = new ContextMenu();
	private VBox _vbox = null;

	private Closure.V1<List<Simulation>> _onLoadSimulations = null;
	
	private ListProperty<CyclusJob> _jobs = new SimpleListProperty<>();
	private List<Entry> _entries = new ArrayList<>();
	private Entry _currentEntry = null;
	
	public JobsPanel() {
		super(TITLE, GlyphRegistry.get(AwesomeIcon.COGS));
		build();
	}
	
	public void setOnLoadSimulations(Closure.V1<List<Simulation>> action) {
		_onLoadSimulations = action;
	}
//	@Override
	public void setTitle(String title) {
		setTitle(title);
	}

	public ListProperty<CyclusJob> jobsProperty() {
		return _jobs;
	}
	
	private void addJob(CyclusJob job) {
		Entry entry = createEntry(job);
		_entries.add(entry);
		_vbox.getChildren().add(entry.title);
	}
	
	private Label getStatusIcon(CyclusJob job) {
		AwesomeIcon icon = null;
		switch( job.getStatus()) {
		case COMPLETED:
			icon = AwesomeIcon.CLOCK_ALT;
			System.out.println("completed");
			break;
		case FAILED:
			icon = AwesomeIcon.TIMES;
			break;
		case LOADING:
			icon = AwesomeIcon.DOWNLOAD;
			System.out.println("dowload");
			break;
		case READY:
			icon = AwesomeIcon.CHECK;
			System.out.println("ready");
			break;
		case SUBMITTED:
		case INIT:
		default:
			icon = AwesomeIcon.CLOCK_ALT;
			break;
		}
		return GlyphRegistry.get(icon);
	}
	
	private Entry createEntry(CyclusJob job) {
		
		final Entry entry = new Entry();
		entry.job = job;
		entry.title = new Label(job.getAlias(), getStatusIcon(job));
		
		entry.title.setOnMouseClicked(new EventHandler<Event>() {

			@Override
			public void handle(Event event) {
			    
				MouseEvent mouseEvent = (MouseEvent)event;
				select(entry);
				//Right click loads the "delete" simulation dialog box.
				if( mouseEvent.getButton()   == MouseButton.SECONDARY){
						_menu.show(entry.title, Side.BOTTOM, 0, 0);
					
				}  
			}
		});
		
		job.statusProperty().addListener(new ChangeListener<CyclusJob.Status>() {

			@Override
			public void changed(ObservableValue<? extends Status> observable,
					Status oldValue, Status newValue) {
				entry.title.setGraphic(getStatusIcon(entry.job));
				
			}
		});
		
		return entry;
	}
		
		
	private void select(Entry entry) {
		_currentEntry = entry;
	}
	
	public void removeJob(Entry entry) {
		_jobs.remove(entry.job);
	}
	
	private Entry getEntry(CyclusJob job) {
		for (Entry e : _entries)
			if (e.job == job) return e;
		return null;
	}
	 	private void build() {
		setPrefSize(USE_COMPUTED_SIZE, USE_COMPUTED_SIZE);
		_vbox = getContent();
		
		createMenu();
		
		_jobs.addListener(new ListChangeListener<CyclusJob>() {

			@Override
			public void onChanged(ListChangeListener.Change<? extends CyclusJob> c) {
				while (c.next()) {
					if (!c.wasPermutated() &&  !c.wasUpdated()) {
						for (CyclusJob job: c.getRemoved()) {
							Entry e = getEntry(job);
							_entries.remove(e);
							_vbox.getChildren().remove(e.title);						
						}
						for (CyclusJob job: c.getAddedSubList()) {
							addJob(job);
						}
					}
				}
			}
			
		});
	}
	private void createMenu(){
		 MenuItem deleteJob = new MenuItem("Delete job");
		 deleteJob.setOnAction(new EventHandler<ActionEvent>() {
		 							public void handle(ActionEvent e) { 
		 								removeJob(_currentEntry);
		 								_currentEntry = null;
		 							}
		 });
		 
		 MenuItem loadData = new MenuItem("Load data");
		 loadData.setOnAction(new EventHandler<ActionEvent>() {
				public void handle(ActionEvent e) { 
					if (_onLoadSimulations != null) {
						final Entry entry = _currentEntry;
						_currentEntry = null;
						final ListProperty<Simulation> list = LoadSqlite.load(_currentEntry.job.getDatafilePath(), new Stage()/*getScene().getWindow()*/);
						list.addListener(new ChangeListener<ObservableList<Simulation>>() {
							@Override
					        public void changed(
					                ObservableValue<? extends ObservableList<Simulation>> observable,
					                ObservableList<Simulation> oldValue,
					                ObservableList<Simulation> newValue) 
							{
								_onLoadSimulations.call(list);
								list.removeListener(this);
								removeJob(entry);
					        }
						});
					}
				}
		 });
		 
		 _menu.getItems().addAll(loadData, new SeparatorMenuItem(), deleteJob);
	}
	
	class Entry {
		Label title;
		CyclusJob job;
	}
}