package edu.utah.sci.cyclist.neup.ui.views.inventory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.function.Consumer;
import java.util.function.IntPredicate;
import java.util.function.Predicate;

import javafx.animation.Animation;
import javafx.animation.RotateTransition;
import javafx.beans.Observable;
import javafx.beans.property.ListProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.concurrent.Task;
import javafx.scene.Node;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.util.Duration;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.sun.xml.internal.bind.v2.runtime.output.ForkXmlOutput;

import edu.utah.sci.cyclist.core.event.Pair;
import edu.utah.sci.cyclist.core.event.dnd.DnD;
import edu.utah.sci.cyclist.core.model.Configuration;
import edu.utah.sci.cyclist.core.model.Field;
import edu.utah.sci.cyclist.core.model.Simulation;
import edu.utah.sci.cyclist.core.ui.components.CyclistViewBase;
import edu.utah.sci.cyclist.core.ui.components.Spring;
import edu.utah.sci.cyclist.core.ui.panels.TitledPanel;
import edu.utah.sci.cyclist.core.util.AwesomeIcon;
import edu.utah.sci.cyclist.core.util.ColorUtil;
import edu.utah.sci.cyclist.core.util.GlyphRegistry;
import edu.utah.sci.cyclist.neup.model.Inventory;
import edu.utah.sci.cyclist.neup.model.NuclideFiltersLibrary;
import edu.utah.sci.cyclist.neup.model.proxy.SimulationProxy;
import edu.utah.sci.cyclist.neup.ui.views.inventory.InventoryChart.ChartType;

public class InventoryView extends CyclistViewBase {
	public static final String ID = "inventory-view";
	public static final String TITLE = "Inventory";
	
	static final Logger log = LogManager.getLogger(InventoryView.class.getName());
	
	private ObservableList<AgentInfo> _agents = FXCollections.observableArrayList();
	private List<String> _acceptableFields = new ArrayList<>();
	
	private Map<String, IntPredicate> _nuclideFilters = new TreeMap<>();
	private ObservableList<String> _nuclideFilterNames = FXCollections.observableArrayList();
	private ObjectProperty<Predicate<Inventory>> _currentNuclideFilterProperty = new SimpleObjectProperty<>();
	
	private Simulation _currentSim = null;
	private SimulationProxy _simProxy = null;
	
	
	private InventoryChart _chart;
	
	class AgentInfo {
		public String field;
		public String value;
		public Color color;
		public ListProperty<Inventory> inventory = new SimpleListProperty<>();
		public FilteredList<Inventory> filteredInventory; 
		
		public List<Pair<Integer, Double>> series = null;
		public ObjectProperty<Task<?>> taskProperty = new SimpleObjectProperty<>();
		
		public AgentInfo(String field, String value) {
			this.field = field;
			this.value = value;
			color = Configuration.getInstance().getColor(getName());
			
			inventory.set(FXCollections.observableArrayList());
			filteredInventory= new FilteredList<>(inventory.get());
			filteredInventory.predicateProperty().bind(_currentNuclideFilterProperty);
			inventory.addListener((Observable o)->System.out.println("inventory changed"));
			filteredInventory.addListener((Observable o)->System.out.println("filtered inventory changed"));
		}
		
		public String getName() {
			return field+"="+value;
		}
		
		public void setTask(Task<?> task) {
			taskProperty.set(task);
		}
		
		public Task<?> getTask() {
			return taskProperty.get();
		}
	}
	
	public InventoryView() {
		super();
		init();
		build();
	}
	
	private void selectChartType(ChartType type) {
		_chart.selectChartType(type);
	}
	
	@Override
	public void selectSimulation(Simulation sim, boolean active) {
		super.selectSimulation(sim, active);
		
		if (!active && sim != _currentSim) {
			return; // ignore
		}
		
		_currentSim = active? sim : null;

		_simProxy = _currentSim == null ?  null : new SimulationProxy(_currentSim);
		
		for (AgentInfo info : _agents) {
			info.inventory.unbind();
			info.inventory.bind(fetchInventory(info));
		}
	}
	
	private void init() {
		_acceptableFields.add("Implementation");
		_acceptableFields.add("Prototype");
		_acceptableFields.add("AgentID");
		_acceptableFields.add("InstitutionID");
		
		// default no-op filter
		_currentNuclideFilterProperty.set(inventory->true);
		
		for (Entry<String, IntPredicate> entry :  NuclideFiltersLibrary.getInstance().getFilters().entrySet())  {
			_nuclideFilters.put(entry.getKey(), entry.getValue());
			_nuclideFilterNames.add(entry.getKey());
		}
	}
	
	private void selectNuclideFilter(String key) {
		System.out.println("select filter:"+key);
		IntPredicate p = _nuclideFilters.get(key);

		if (p == null) {
			p = createNuclideFilter(key);
			_nuclideFilters.put(key, p);
			_nuclideFilterNames.add(key);
		}
		final IntPredicate predicate = p;
		_currentNuclideFilterProperty.set(inventory->predicate.test(inventory.nucid));
	}
	
	private IntPredicate createNuclideFilter(String spec) {
		// TODO: parse spec and create a real filter
		return  i->i/100000 == 93;
	}
	
	
	private void build() {
		setTitle(TITLE);
		getStyleClass().add("inventory");
	
		BorderPane pane = new BorderPane();
		pane.setCenter(buildChart());
		pane.setLeft(buildCtrl());

		setContent(pane);
	}
	
	private Node buildCtrl() {
		VBox vbox = new VBox();
		vbox.getStyleClass().add("ctrl");
		
		vbox.getChildren().addAll(
			buildChartCtrl(),
			buildAgentCtrl(),
			buildNuclideCtrl()
		);
		
		return vbox;
	}
	
	private Node buildChartCtrl() {
		VBox vbox = new VBox();
		vbox.getStyleClass().add("ctrl");
		
		ChoiceBox<ChartType> type = new ChoiceBox<>();
		type.getStyleClass().add("choice");
		type.getItems().addAll(ChartType.values());
		type.valueProperty().addListener(e->{
			selectChartType(type.getValue());
		});
		
		type.setValue(ChartType.INVENTORY);
		
		vbox.getChildren().add(type);
		return vbox;
	}
	
	public Node buildAgentCtrl() {
		
		TitledPanel panel = new TitledPanel("Agents");
		panel.getStyleClass().add("agents-panel");
		
		Node pane = panel.getPane();
		panel.setFillWidth(true);
		pane.setOnDragOver(e->{
			DnD.LocalClipboard clipboard = getLocalClipboard();
			if (clipboard.hasContent(DnD.VALUE_FORMAT)) {
				Field field = clipboard.get(DnD.FIELD_FORMAT, Field.class);
				if (_acceptableFields.contains(field.getName())) {
					e.acceptTransferModes(TransferMode.COPY);
					e.consume();
				}
			}
		});
		
		pane.setOnDragDropped(e->{
			DnD.LocalClipboard clipboard = getLocalClipboard();
			
			String value = clipboard.get(DnD.VALUE_FORMAT, Object.class).toString();
			String field = clipboard.get(DnD.FIELD_FORMAT, Field.class).getName();
			
			// ensure we don't already have this field
			for (AgentInfo agent : _agents) {
				if (agent.field.equals(field) && agent.value.equals(value)) {
					e.consume();
					return;
				}	
			}
			AgentInfo info = new AgentInfo(field, value);	
			AgentEntry entry = new AgentEntry(info);
			panel.getContent().getChildren().add(entry);
			entry.setOnClose(item->{
				_agents.remove(item.info);
				panel.getContent().getChildren().remove(item);
				_chart.remove(item.info);
			});
			
			addAgent(info);	
			e.setDropCompleted(true);
			e.consume();
		});
		
		return panel;
	}

	public Node buildNuclideCtrl() {
		VBox vbox = new VBox();
		vbox.getStyleClass().add("infobar");

		Text title = new Text("Nuclide");
		title.getStyleClass().add("title");
		
		ComboBox<String> filters = new ComboBox<>();
		filters.getStyleClass().add("nuclide-filters");
		
		filters.setPromptText("filter");
		filters.setEditable(true);
		filters.getItems().addAll(_nuclideFilters.keySet());
		
		filters.valueProperty().addListener(o->selectNuclideFilter(filters.getValue()));

		vbox.getChildren().addAll(
			title,
			filters
		);
		;
		return vbox;
	}
	

	private void addAgent(final AgentInfo info) {
		_agents.add(info);
		info.inventory.addListener((Observable o)->addToChart(info));
//		info.inventory.bind(fetchInventory(info));
		fetchInventory(info.inventory, info);
	}
	
	private ReadOnlyObjectProperty<ObservableList<Inventory>>  fetchInventory(AgentInfo info) {
		final String field = info.field;
		final String value = info.value;
		
		Task<ObservableList<Inventory>> task = new Task<ObservableList<Inventory>>() {
			@Override
			protected ObservableList<Inventory> call() throws Exception {
				ObservableList<Inventory> list = FXCollections.observableArrayList();
				updateValue(list);
				list.setAll(_simProxy.getInventory2(field, value));
				return list;
			}	
		};
		
		info.setTask(task);
		
		Thread thread = new Thread(task);
		thread.setDaemon(true);
		thread.start();	
				
		return task.valueProperty();
	}
	
	 
	private void  fetchInventory(ObservableList<Inventory> list, AgentInfo info) {
		final String field = info.field;
		final String value = info.value;
		
		Task<Void> task = new Task<Void>() {
			@Override
			protected Void call() throws Exception {
				list.setAll(_simProxy.getInventory2(field, value));
				return null;
			}	
		};
		
		info.setTask(task);
		
		Thread thread = new Thread(task);
		thread.setDaemon(true);
		thread.start();	
	}
	
	private void addToChart(AgentInfo info) {
		List<Pair<Integer, Double>> series = new ArrayList<>();
		Pair<Integer, Double> current =  null;
		
		// collect data. TODO: apply filters
		for (Inventory i : info.inventory) {
			if (current == null || current.v1 != i.time) {
				if (current != null) {
					series.add(current);
				}
				current = new Pair<>();
				current.v1 = i.time;
				current.v2 = i.amount;
			} else {
				current.v2 += i.amount;
			}
		}
		if (current != null) {
			series.add(current);
		}
		info.series = series;
		_chart.add(info);
	}
	
	private Node buildChart() {
		_chart = new InventoryChart();	
		return _chart;
	}
	
	
	class AgentEntry extends HBox {
		public AgentInfo info;
		private Status _status;
		private Consumer<AgentEntry> _onClose = null;
		
		public AgentEntry(final AgentInfo info) {
			super();
			this.info = info;
			
			getStyleClass().add("agent");
			Label text = new Label(info.value);
			text.setStyle("-fx-background-color:"+ColorUtil.toString(info.color));
			
			Node button = GlyphRegistry.get(AwesomeIcon.TIMES, "10px");
			button.setVisible(false);
			
			_status = new Status();
			getChildren().addAll(text, new Spring(), _status, button);
			
			info.taskProperty.addListener(o->_status.setTask(info.getTask()));
			setOnMouseEntered(e->{
				button.setVisible(true);
				getStyleClass().add("hover");
			});
			
			setOnMouseExited(e->{
				button.setVisible(false);
				getStyleClass().remove("hover");
			});
			
			setOnMouseClicked(e->{
				text.setDisable(!text.isDisable());
				if (text.isDisable()) {
					_chart.remove(info);
				} else {
					_chart.add(info);
				}
				
			});
			
			button.setOnMouseClicked(e->{
				if (_onClose != null) {
					_onClose.accept(this);
				}
			});
			
			HBox.setHgrow(text, Priority.ALWAYS);
		}
		
		public void setTask(Task<?> task) {
			_status.setTask(task);
		}
		
		public void setOnClose(Consumer<AgentEntry> cb) {
			setTask(null);
			_onClose = cb;
		}
	}
	
	class Status extends Pane {
		private Task<?> _task = null;
		private Node _icon;
		private RotateTransition _animation; 
		
		
		public Status() {
			super();
			_icon = GlyphRegistry.get(AwesomeIcon.REFRESH, "10px");
			getChildren().add(_icon);
			
			_animation = new RotateTransition(Duration.millis(100000), _icon);
			_animation.setFromAngle(0);
			_animation.setByAngle(36000);
			_animation.setCycleCount(Animation.INDEFINITE);
			setVisible(false);
			setOnMouseClicked(e->_task.cancel());
		}
		
		public void setTask(Task<?> task) {
			if (_task != null) {
				_task.cancel();
				_animation.stop();
				visibleProperty().unbind();
				log.info("Task Canceled");
			}
			
			_task = task;
			if (_task != null) {
				visibleProperty().bind(task.runningProperty());
				_task.runningProperty().addListener(e->{
					if (_task.isRunning()) {
						_animation.play();
						log.info("Fetch invetory");
					} else {
						_animation.stop();
					}
				});

				task.setOnFailed(e->{
					log.error(_task.getMessage());
					setTask(null);
				});
			}
		}
		
	}
}