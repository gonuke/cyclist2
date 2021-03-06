package edu.utexas.cycic.tools;

import edu.utah.sci.cyclist.core.ui.tools.Tool;
import edu.utah.sci.cyclist.core.ui.tools.ToolFactory;
import edu.utah.sci.cyclist.core.util.AwesomeIcon;

public class RegionCorralViewToolFactory implements ToolFactory {

	@Override
	public String getToolName() {
		return RegionCorralViewTool.TOOL_NAME;
	}
	
	@Override
	public Tool create() {
		return new RegionCorralViewTool();
	}

	@Override
	public AwesomeIcon getIcon() {
		return RegionCorralViewTool.ICON;
	}
}