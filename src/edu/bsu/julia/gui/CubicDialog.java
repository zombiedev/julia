package edu.bsu.julia.gui;

import java.awt.Checkbox;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.apache.commons.math.complex.Complex;
import org.apache.commons.math.complex.ComplexUtils;

import edu.bsu.julia.Julia;
import edu.bsu.julia.input.CubicInputFunction;
import edu.bsu.julia.session.Session;

/**
 * 
 * <h3>Description</h3>
 * <p>
 * Creates a dialog box for the function type: az^3 + b. This dialog is used
 * when creating a new cubic function, editing an existing function, or cloneing
 * an existing function.
 * 
 */
public class CubicDialog extends JDialog implements ActionListener {

	/**
	 * The main frame over which this dialog sets.
	 */
	private Julia parentFrame;

	/**
	 * One of three types defined in a utility class, GUIUtil:
	 * {@link GUIUtil#NEW_DIALOG}, {@link GUIUtil#EDIT_DIALOG}, and
	 * {@link GUIUtil#CLONE_DIALOG}. It indicates the use to which this dialog
	 * is being put.
	 */
	private int dialogType;

	/**
	 * If this dialog is being used to edit or clone an existing function, the
	 * function that is being modified. If the dialog is being used to create a
	 * new function, this field is <b>null</b>.
	 */
	private CubicInputFunction function;

	/**
	 * The text field in which the user enters the function's m value.
	 */
	private JTextField mField = new JTextField(5);

	/**
	 * The text field in which the user enters the real portion of the
	 * function's 'a' coefficient.
	 */
	private JTextField axField = new JTextField(3);

	/**
	 * The text field in which the user enters the number which, multiplied by
	 * i, is the imaginary portion of the function's 'a' coefficient.
	 */
	private JTextField ayField = new JTextField(3);

	/**
	 * The text field in which the user enters the real portion of the
	 * function's 'b' coefficient.
	 */
	private JTextField bxField = new JTextField(3);

	/**
	 * The text field in which the user enters the number which, multiplied by
	 * i, is the imaginary portion of the function's 'b' coefficient.
	 */
	private JTextField byField = new JTextField(3);

	// for serializable interface: do not use
	public static final long serialVersionUID = 0;

	private Checkbox polarCheckBox;

	/**
	 * Builds the dialog for a cubic input function and makes it visible on the
	 * screen. 'type' indicates whether the dialog will be used for editing or
	 * cloneing an existing function or creating a new function. If editing or
	 * cloneing an existing function, that function is passed to the constructor
	 * as 'fn'. When creating a new function, 'fn' can be <b>null</b>.
	 * 
	 * @param f
	 *            reference to the main program and its frame
	 * @param type
	 *            constant indicating whether the dialog will be used to edit,
	 *            clone, or create a new function
	 * @param fn
	 *            the function to be edited or cloned
	 */
	public CubicDialog(Julia f, int type, CubicInputFunction fn) {
		super(f, "Create a Cubic Function", false);
		parentFrame = f;
		dialogType = type;
		function = fn;

		Complex[] coefficients = new Complex[2];
		if (dialogType == GUIUtil.EDIT_DIALOG
				|| dialogType == GUIUtil.CLONE_DIALOG)
			coefficients = function.getCoefficients();

		setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		setLayout(new GridLayout(0, 1));

		JPanel mPanel = new JPanel();
		mPanel.add(new JLabel("Enter the m value:"));
		if (dialogType == GUIUtil.EDIT_DIALOG
				|| dialogType == GUIUtil.CLONE_DIALOG)
			mField.setText("" + function.getM());
		else
			mField.setText("1");
		mField
				.addFocusListener(new TextFieldFocusListener(mField,
						parentFrame));
		mPanel.add(mField);
		add(mPanel);

		add(new JLabel("Enter the function coefficients:"));
		JPanel functionPanel = new JPanel();
		// format all labels in HTML
		functionPanel.add(new JLabel("<html>(</html>"));
		if (dialogType == GUIUtil.EDIT_DIALOG
				|| dialogType == GUIUtil.CLONE_DIALOG) {
			String show = String.valueOf(coefficients[0].getReal());
			String showShort = show;
			if (show.length() > 5)
				showShort = show.substring(0, 5);
			axField.setText(showShort);
		} else
			axField.setText("1");
		axField.addFocusListener(new TextFieldFocusListener(axField,
				parentFrame));
		functionPanel.add(axField);
		functionPanel.add(new JLabel(", "));
		if (dialogType == GUIUtil.EDIT_DIALOG
				|| dialogType == GUIUtil.CLONE_DIALOG) {
			String show = String.valueOf(coefficients[0].getImaginary());
			String showShort = show;
			if (show.length() > 5)
				showShort = show.substring(0, 5);
			ayField.setText(showShort);
		} else
			ayField.setText("0");
		ayField.addFocusListener(new TextFieldFocusListener(ayField,
				parentFrame));
		functionPanel.add(ayField);
		functionPanel.add(new JLabel("<html>) z<sup>3</sup> + (</html>"));
		if (dialogType == GUIUtil.EDIT_DIALOG
				|| dialogType == GUIUtil.CLONE_DIALOG) {
			String show = String.valueOf(coefficients[1].getReal());
			String showShort = show;
			if (show.length() > 5)
				showShort = show.substring(0, 5);
			bxField.setText(showShort);
		} else
			bxField.setText("0");
		bxField.addFocusListener(new TextFieldFocusListener(bxField,
				parentFrame));
		functionPanel.add(bxField);
		functionPanel.add(new JLabel(", "));
		if (dialogType == GUIUtil.EDIT_DIALOG
				|| dialogType == GUIUtil.CLONE_DIALOG) {
			String show = String.valueOf(coefficients[1].getImaginary());
			String showShort = show;
			if (show.length() > 5)
				showShort = show.substring(0, 5);
			byField.setText(showShort);
		} else
			byField.setText("0");
		byField.addFocusListener(new TextFieldFocusListener(byField,
				parentFrame));
		functionPanel.add(byField);
		functionPanel.add(new JLabel("<html>)</html>"));
		add(functionPanel);

		polarCheckBox = new Checkbox("Coefficient Values Use Polar Coordinates");
		polarCheckBox.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 10));
		add(polarCheckBox);

		JPanel buttonPanel = new JPanel();
		JButton finishButton = new JButton("Finish");
		finishButton.addActionListener(this);
		buttonPanel.add(finishButton);

		JButton cancelButton = new JButton("Cancel");
		cancelButton.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent event) {
				setVisible(false);
				dispose();
			}
		});
		buttonPanel.add(cancelButton);
		add(buttonPanel);

		pack();
		setLocationRelativeTo(parentFrame);
		setVisible(true);
	}

	/**
	 * Triggered when the user hits "Finish" on the dialog box. Builds the
	 * function and then either adds it to the
	 * {@link edu.bsu.julia.session.Session}'s input function list or replaces
	 * the old function on the list (if it is being used to edit a function).
	 * Input functions are immutable objects.
	 */
	public void actionPerformed(ActionEvent arg0) {
		if (mField.getText().equals("")) {
			JuliaError.EMPTY_FIELD.showDialog(parentFrame);
			return;
		}
		int m = 0;
		try {
			m = Integer.parseInt(GUIUtil.removeCommas(mField.getText()));
		} catch (NumberFormatException e) {
			JuliaError.M_INTEGER_ERROR.showDialog(parentFrame);
			return;
		}
		double ax = 0;
		double ay = 0;
		double bx = 0;
		double by = 0;
		if (axField.getText().equals("") || ayField.getText().equals("")
				|| bxField.getText().equals("") || byField.getText().equals("")) {
			JuliaError.EMPTY_FIELD.showDialog(parentFrame);
			return;
		}
		String axString = GUIUtil.removeCommas(axField.getText());
		axString = GUIUtil.parsePI(axString);
		String ayString = GUIUtil.removeCommas(ayField.getText());
		ayString = GUIUtil.parsePI(ayString);
		String bxString = GUIUtil.removeCommas(bxField.getText());
		bxString = GUIUtil.parsePI(bxString);
		String byString = GUIUtil.removeCommas(byField.getText());
		byString = GUIUtil.parsePI(byString);
		try {
			ax = Double.parseDouble(axString);
			ay = Double.parseDouble(ayString);
			bx = Double.parseDouble(bxString);
			by = Double.parseDouble(byString);
		} catch (NumberFormatException e) {
			JuliaError.COEFFICIENT_FORMAT_ERROR.showDialog(parentFrame);
			return;
		}

		Complex a = new Complex(ax, ay);
		Complex b = new Complex(bx, by);
		if (polarCheckBox.getState()) {
			a = ComplexUtils.polar2Complex(ax, ay);
			b = ComplexUtils.polar2Complex(bx, by);
		}
		CubicInputFunction newFunction;
		try {
			newFunction = new CubicInputFunction(m, a, b);
		} catch (IllegalArgumentException e) {
			if (e.getMessage().equals("a zero"))
				JuliaError.CUBIC_ILLEGAL_ARGUMENT.showDialog(parentFrame);
			else
				JuliaError.M_NEG_ERROR.showDialog(parentFrame);
			return;
		}

		Session s = parentFrame.getCurrentSession();
		if (dialogType == GUIUtil.NEW_DIALOG
				|| dialogType == GUIUtil.CLONE_DIALOG)
			s.addInputFunction(newFunction);
		else
			s.replaceInputFunction(function, newFunction);

		setVisible(false);
		dispose();
	}
}
