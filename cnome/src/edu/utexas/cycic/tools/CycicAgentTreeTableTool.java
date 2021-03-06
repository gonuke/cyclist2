package edu.utexas.cycic.tools;

import edu.utah.sci.cyclist.core.event.notification.EventBus;
import edu.utah.sci.cyclist.core.presenter.ViewPresenter;
import edu.utah.sci.cyclist.core.ui.View;
import edu.utah.sci.cyclist.core.ui.tools.Tool;
import edu.utah.sci.cyclist.core.util.AwesomeIcon;
import edu.utexas.cycic.Cycic;
import edu.utexas.cycic.CycicAgentTreeTable;
import edu.utexas.cycic.presenter.CycicAgentTreeTablePresenter;
import edu.utexas.cycic.presenter.CycicPresenter;

public class CycicAgentTreeTableTool implements Tool{
	public static final String ID 			= "edu.utexas.cycic.CycicAgentTreeTableTool";
	public static final String TOOL_NAME 	= "TreeTable";
	public static final AwesomeIcon ICON 	= AwesomeIcon.QUESTION_CIRCLE;
	
	private View _view = null;
	private ViewPresenter _presenter = null;
	
	@Override
	public String getId() {
		return ID;
	}

	@Override
	public String getName() {
		return TOOL_NAME;
	}

	@Override
	public View getView() {
		if (_view == null) 
			_view = new CycicAgentTreeTable();
		return _view;
	}

	@Override
	public ViewPresenter getPresenter(EventBus bus) {
		if (_presenter == null)
			_presenter = new CycicAgentTreeTablePresenter(bus);
		return _presenter;
	}

}
