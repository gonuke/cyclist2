package edu.utah.sci.cyclist.presenter;

import org.mo.closure.v1.Closure;

import edu.utah.sci.cyclist.event.notification.CyclistNotification;
import edu.utah.sci.cyclist.event.notification.CyclistNotificationHandler;
import edu.utah.sci.cyclist.event.notification.CyclistNotifications;
import edu.utah.sci.cyclist.event.notification.CyclistTableNotification;
import edu.utah.sci.cyclist.event.notification.EventBus;
import edu.utah.sci.cyclist.model.Table;
import edu.utah.sci.cyclist.ui.View;
import edu.utah.sci.cyclist.ui.views.ChartView;

public class ChartPresenter extends CyclistViewPresenter {

	public ChartPresenter(EventBus bus) {
		super(bus);
		SingleSelection selectionModel = new SingleSelection();
		selectionModel.setOnSelectTableAction(new Closure.V2<Table, Boolean>() {

			@Override
			public void call(Table table, Boolean value) {
				getView().selectTable(table, value);
				
			}
		
		});
		
		setSelectionModel(selectionModel);
		addNotificationHandlers();
	}
	
	
	@Override
	public ViewPresenter clone(View view) {
		ChartPresenter copy = new ChartPresenter(getEventBus());
		copy.setView(view);
		
		// copy tables
		for (SelectionModel.Entry entry : getSelectionModel().getEntries()) {
			copy.addTable(entry.table, entry.remote, entry.active, entry.remoteActive);
		}
		
		ChartView chartView = (ChartView) view;
		chartView.copy(getView());

		chartView.setActive(true);
		return copy;
	}
	
	@Override 
	public void setActive(boolean active) {
		getView().setActive(active);
	}
	
	@Override
	public ChartView getView() {
		return (ChartView) super.getView();
	}
	
	/**
	 * setView
	 * @param view
	 */
	
	public void setView(View view) {
		super.setView(view);
		
		getView().setOnTableDrop(new Closure.V1<Table>() {
			
			@Override
			public void call(Table table) {
				addTable(table, false /* remote */, true /* active */, false /* remoteActive */);
			}
		});
		
		getView().setOnTableRemoved(new Closure.V1<Table>() {

			@Override
			public void call(Table table) {
				removeTable(table);			
			}
		});
		
		getView().setOnDuplicate(new Closure.V0() {
			@Override
			public void call() {
				broadcast(new CyclistNotification(CyclistNotifications.DUPLICATE_VIEW));
			}
		});
	}
	
	/**
	 * addNotficicationHandlers
	 */
	
	public void addNotificationHandlers() {
		addNotificationHandler(CyclistNotifications.DATASOURCE_ADD, new CyclistNotificationHandler() {
			
			@Override
			public void handle(CyclistNotification event) {
				CyclistTableNotification notification = (CyclistTableNotification) event;
				
				addTable(notification.getTable(), true /*remote*/, false /* active */, false /* remoteActive */);			
			}
		});
		
		addNotificationHandler(CyclistNotifications.DATASOURCE_REMOVE, new CyclistNotificationHandler() {
			
			@Override
			public void handle(CyclistNotification event) {
				CyclistTableNotification notification = (CyclistTableNotification) event;
				removeTable(notification.getTable());			
			}
		});
		
		addNotificationHandler(CyclistNotifications.DATASOURCE_SELECTED, new CyclistNotificationHandler() {
			
			@Override
			public void handle(CyclistNotification event) {
				CyclistTableNotification notification = (CyclistTableNotification) event;
				getSelectionModel().selectTable(notification.getTable(), true);
			}
		});
		
		addNotificationHandler(CyclistNotifications.DATASOURCE_UNSELECTED, new CyclistNotificationHandler() {
			
			@Override
			public void handle(CyclistNotification event) {
				CyclistTableNotification notification = (CyclistTableNotification) event;
				getSelectionModel().selectTable(notification.getTable(), false);			
			}
		});
	}
	
}
