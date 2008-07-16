package edu.bsu.julia.gui.actions;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.ButtonGroup;

import edu.bsu.julia.Julia;
import edu.bsu.julia.generators.RecursiveOutputSetGenerator;
import edu.bsu.julia.generators.OutputSetGenerator;
import edu.bsu.julia.gui.InputPanel;
import edu.bsu.julia.input.InputFunction;
import edu.bsu.julia.output.RecursiveOutputSet;
import edu.bsu.julia.output.OutputSet;
import edu.bsu.julia.session.Session;

public class InverseAction extends AbstractAction {
	private final Julia parentFrame;
	private final ButtonGroup methodGroup;

	// for serializable interface: do not use
	public static final long serialVersionUID = 0;

	public InverseAction(Julia f) {
		super("Inverse Image (Random/Full)");
		parentFrame = f;
		InputPanel inputPanel = parentFrame.getInputPanel();
		methodGroup = inputPanel.getMethodGroup();
		putValue("SHORT_DESCRIPTION", "Process Inverse Image");
		putValue(
				"LONG_DESCRIPTION",
				"Create a (Random/Full) Inverse Image of"
						+ "the selected Output Set(s) using the selected Input Functions");
	}

	public void actionPerformed(ActionEvent arg0) {
		// build list of input functions
		InputFunction[] inFunc = parentFrame.getInputPanel()
				.getSelectedFunctions();

		// build list of output functions
		Object[] objArray = parentFrame.getOutputSetList()
				.getSelectedValues();
		if (objArray.length == 0)
			return;
		OutputSet[] outFunc = new OutputSet[objArray.length];
		for (int i = 0; i < objArray.length; i++)
			outFunc[i] = (OutputSet) objArray[i];
		parentFrame.getOutputSetList().clearSelection();

		// create and add the Output Functions
		String command = methodGroup.getSelection().getActionCommand().toUpperCase();
		Session session = parentFrame.getCurrentSession();
		for (InputFunction function : inFunc) {
			OutputSetGenerator generator = new RecursiveOutputSetGenerator(
					parentFrame, function, outFunc,
					RecursiveOutputSetGenerator.Type.valueOf(command));
			OutputSet outputSet = new RecursiveOutputSet(session,
					new InputFunction[] { function },
					RecursiveOutputSetGenerator.Type.valueOf(command)
							.outputType(), generator, outFunc);
			session.addOutputSet(outputSet);
		}
	}
}
