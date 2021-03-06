package edu.bsu.julia.output;

import java.awt.Color;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.SwingWorker;
import javax.swing.SwingWorker.StateValue;

import org.apache.commons.math.complex.Complex;

import edu.bsu.julia.ComplexNumberUtils;
import edu.bsu.julia.Julia;
import edu.bsu.julia.generators.OutputSetGenerator;
import edu.bsu.julia.input.InputFunction;
import edu.bsu.julia.session.Session;

public class OutputSet {
	public static enum Type {
		BASIC("Basic Output Set"),

		FULL_JULIA("Full Composite Julia Set"),

		RANDOM_JULIA("Random Composite Julia Set"),

		FULL_ATTR("Full Composite Attractor Set"),

		RANDOM_ATTR("Random Composite Attractor Set"),

		FORWARD_IMAGE("Forward Image"),

		RANDOM_INVERSE_IMAGE("Random Inverse Image"),

		FULL_INVERSE_IMAGE("Full Inverse Image"),

		IND_FULL_JULIA("Full Individual Julia Set"),

		IND_RANDOM_JULIA("Random Individual Julia Set"),

		IND_FULL_ATTR("Full Individual Attractor Set"),

		IND_RANDOM_ATTR("Random Individual Attractor Set"),

		POST_CRITICAL("Post Critical Set");

		private String description;

		private Type(String d) {
			description = d;
		}

		public String description() {
			return description;
		}
	}

	/**
	 * this class is designed to abstract the iteration, skips, and seed values
	 * out of the session so we don't have to create a dummy session every time
	 * we want to create an OutputSet
	 * 
	 * @author Ben Dean
	 */
	public static abstract class Info {
		abstract public Integer iterations();

		abstract public Integer skips();

		abstract public Complex seed();

		public static Info sessionToInfo(final Session s) {
			return new Info() {
				@Override
				public Integer iterations() {
					return s.getIterations();
				}

				@Override
				public Complex seed() {
					return s.getSeedValue();
				}

				@Override
				public Integer skips() {
					return s.getSkips();
				}
			};
		}
	}

	private int sub = 0;
	protected final Integer iterations;
	protected final Integer skips;
	protected final Complex seed;
	protected final Type functionType;
	protected final InputFunction[] inputFunctions;
	private Complex[] points;
	private OutputSetGenerator generator;
	protected File pointsFile;

	private Color c;
	private final static Color[] colorSet = { Color.BLACK, Color.BLUE,
			Color.RED, Color.DARK_GRAY, Color.GREEN, Color.ORANGE,
			Color.MAGENTA };
	private static int colorIndex = 0;
	private PropertyChangeSupport support = new PropertyChangeSupport(this);
	private final JProgressBar bar = new JProgressBar(0, 100);
	private SwingWorker<File, Void> tempFileWriter;
	private SwingWorker<Complex[], Void> tempFileReader;
	protected final long creationTime;

	public OutputSet(Info info, InputFunction[] i, Type type,
			OutputSetGenerator gen, final ActionListener listener) {
		iterations = info.iterations();
		functionType = type;
		skips = (functionType == Type.POST_CRITICAL) ? null : info.skips();
		seed = info.seed();

		inputFunctions = i;
		generator = gen;
		c = getNextColor();
		creationTime = Julia.nextTimestamp();

		// add a property change listener to detect when the generator is done
		generator.addPropertyChangeListener(new PropertyChangeListener() {
			@Override
			public void propertyChange(PropertyChangeEvent evt) {
				if ("progress".equals(evt.getPropertyName())) {
					bar.setValue((Integer) evt.getNewValue());
					support.firePropertyChange("repaint", null, null);
				} else if ("state".equals(evt.getPropertyName())
						&& (StateValue) evt.getNewValue() == StateValue.DONE) {
					// try to get the points from the generator
					try {
						points = generator.get();
					} catch (Exception e) {
					}

					// if the points are null then there was some sort of error
					if (points == null) {
						listener.actionPerformed(new ActionEvent(
								OutputSet.this, 0, "delete output set"));
					} else {
						writePointsTempFile();
						support.firePropertyChange("reselect", null, null);
					}
				}
			}
		});
		generator.execute();
	}

	public Type getType() {
		return functionType;
	}

	public String getSubscript() {
		return (sub == 0) ? "?" : sub + "";
	}

	public void setSubscript(int subscript) {
		sub = subscript;
	}

	public int getNumOfPoints() {
		if (points == null)
			return 0;
		else
			return points.length;
	}

	public Color getColor() {
		return c;
	}

	public static Color getNextColor() {
		Color color = colorSet[colorIndex];
		if (colorIndex < colorSet.length - 1)
			colorIndex++;
		else
			colorIndex = 0;
		return color;
	}

	public void setColor(Color newColor) {
		c = newColor;
		support.firePropertyChange("Color", null, newColor);
	}

	public InputFunction[] getInputFunctions() {
		return inputFunctions;
	}

	/**
	 * @return an array of {@link Complex}. will be empty if points ==
	 *         null
	 */
	public Complex[] getPoints() {
		return getPoints(false);
	}

	/**
	 * method to get the points that will block if the points aren't generated
	 * or need to be read from a file
	 * 
	 * @param shouldWait
	 *            boolean for whether or not to wait for points to be generated
	 *            or read from file
	 * @return an array of {@link Complex}. will be empty if points ==
	 *         null
	 */
	public Complex[] getPoints(boolean shouldWait) {
		// if we have the points, return them
		if (points != null)
			return points;

		// we don't have the points, should we wait for them or not
		if (shouldWait) {
			// if the generator isn't done, wait on it to finish
			if (!generator.isDone()) {
				try {
					Complex[] results = generator.get();
					if (results != null)
						return results;
				} catch (Exception e) {
				}
			} else if (pointsFile != null) {
				// the generator was done so try and read the points from the
				// file and wait for them to finish reading
				readPointsTempFile();
				try {
					Complex[] results = tempFileReader.get();
					if (results != null)
						return results;
				} catch (Exception e) {
				}
			}
		} else {
			// we're not waiting but we still need to start reading the points
			// from the temp file
			readPointsTempFile();
		}

		// if it gets this far either points null and we might be waiting for it
		// to be read. for now, return an empty array
		return new Complex[] {};
	}

	public void addListener(PropertyChangeListener list) {
		support.addPropertyChangeListener(list);
	}

	public void removeListener(PropertyChangeListener list) {
		support.removePropertyChangeListener(list);
	}

	public String toString() {
		String s = "o" + getSubscript() + " = " + functionType.description();
		if (functionType == Type.BASIC)
			return s;

		s += " of ";
		for (int x = 0; x < inputFunctions.length; x++) {
			s = s + "f" + inputFunctions[x].getSubscript();
			if (x != (inputFunctions.length - 1))
				s = s + ", ";
		}
		return s;
	}

	public boolean equals(Object obj) {
		try {
			OutputSet other = (OutputSet) obj;
			boolean result = true;

			// check that iterations are equal
			if (iterations == null || other.iterations == null) {
				result = result && other.iterations == null
						&& iterations == null;
			} else {
				result = iterations.equals(other.iterations);
			}

			// check that skips are equal
			if (skips == null || other.skips == null) {
				result = result && other.skips == null && skips == null;
			} else {
				result = result && skips.equals(other.skips);
			}

			// check that seed is equal
			if (seed == null || other.seed == null) {
				result = result && other.seed == null && seed == null;
			} else {
				result = result && seed.equals(other.seed);
			}

			// check that functionType is equal
			if (functionType == null || other.functionType == null) {
				result = result && other.functionType == null
						&& functionType == null;
			} else {
				result = result && functionType.equals(other.functionType);
			}

			// check that inputFunctions are equal
			if (inputFunctions == null || other.inputFunctions == null) {
				result = result && other.inputFunctions == null
						&& inputFunctions == null;
			} else {
				result = result
						&& inputFunctions.length == other.inputFunctions.length;

				for (int i = 0; result && i < inputFunctions.length; i++) {
					result = result
							&& inputFunctions[i]
									.equals(other.inputFunctions[i]);
				}
			}

			// check that points are equal
			if (points == null || other.points == null) {
				result = result && other.points == null && points == null;
			} else {
				result = result && points.length == other.points.length;
				for (int i = 0; result && i < points.length; i++) {
					result = result && points[i].equals(other.points[i]);
				}
			}

			return result;
		} catch (ClassCastException e) {
			return false;
		}
	}

	public boolean isLoaded() {
		return points != null;
	}

	public Component getLoadingComponent() {
		bar.setStringPainted(true);
		return bar;
	}

	public void unload() {
		if (pointsFile == null)
			return;
		points = null;
	}

	private void writePointsTempFile() {
		if (pointsFile != null || tempFileWriter != null || points == null)
			return;

		tempFileWriter = new SwingWorker<File, Void>() {
			public File doInBackground() {
				try {
					// create a new temp file and open it.
					File file = File.createTempFile("output", ".dat");
					file.deleteOnExit();

					PrintStream out = new PrintStream(
							new FileOutputStream(file));

					for (Complex p : points)
						out.println(ComplexNumberUtils.exportString(p));

					out.close();
					return file;
				} catch (IOException e) {
					e.printStackTrace();
					return null;
				}
			}
		};

		tempFileWriter.addPropertyChangeListener(new PropertyChangeListener() {
			@Override
			public void propertyChange(PropertyChangeEvent evt) {
				if ("state".equals(evt.getPropertyName())
						&& (StateValue) evt.getNewValue() == StateValue.DONE) {
					try {
						pointsFile = tempFileWriter.get();
					} catch (Exception e) {
					}
					tempFileWriter = null;
				}
			}
		});
		tempFileWriter.execute();
	}

	private void readPointsTempFile() {
		if (tempFileReader != null || points != null || pointsFile == null)
			return;

		tempFileReader = new SwingWorker<Complex[], Void>() {
			@Override
			protected Complex[] doInBackground() throws Exception {
				List<Complex> tempPoints = new ArrayList<Complex>();

				try {
					// open the temp file
					Scanner in = new Scanner(new FileInputStream(pointsFile));

					// try to read all the points
					while (in.hasNextLine()) {
						String line = in.nextLine();
						tempPoints.add(ComplexNumberUtils.parseComplexNumber(line));
					}
					in.close();

					// create an array
					return tempPoints.toArray(new Complex[] {});
				} catch (IOException e) {
					return null;
				}
			}
		};

		tempFileReader.addPropertyChangeListener(new PropertyChangeListener() {
			@Override
			public void propertyChange(PropertyChangeEvent evt) {
				if ("state".equals(evt.getPropertyName())
						&& (StateValue) evt.getNewValue() == StateValue.DONE) {
					try {
						points = tempFileReader.get();
					} catch (Exception e) {
						points = null;
					}

					if (points != null)
						support.firePropertyChange("reselect", null, null);
					tempFileReader = null;
				}
			}
		});

		tempFileReader.execute();
	}

	/**
	 * called when an {@link OutputSet} is deleted from the session
	 */
	public void delete() {
		if (!generator.isDone())
			generator.cancel(true);
		unload();
	}

	/**
	 * method to access the files this {@link OutputSet}
	 * 
	 * @return an array of {@link File} containing two entries, one for point
	 *         data and one for the other information about the
	 *         {@link OutputSet}
	 */
	public File[] getFiles() {
		if (pointsFile == null)
			try {
				pointsFile = tempFileWriter.get();
			} catch (Exception e1) {
				System.err.println("OutputSet.getFiles(): temp file error");
				return null;
			}

		try {
			File info = File.createTempFile("output", ".txt");
			info.deleteOnExit();
			PrintStream out = new PrintStream(new BufferedOutputStream(
					new FileOutputStream(info)));

			for (String s : historyInfo())
				out.println(s);

			out.close();

			return new File[] { info, pointsFile };
		} catch (IOException e) {
			System.err.println("OutputSet.getFiles(): IO exception");
			return null;
		}
	}

	/**
	 * method to uniquely identify each {@link OutputSet}
	 * 
	 * @return a long integer that uniquely identifies each {@link OutputSet}
	 */
	public long getOutputID() {
		return creationTime;
	}

	/**
	 * create a string to represent this function's history
	 * 
	 * @return a {@link String} containing the class, m value, and coefficients
	 *         of this {@link OutputSet}
	 */
	public List<String> historyInfo() {
		List<String> result = new ArrayList<String>();
		result.add("class: " + this.getClass().getName());
		result.add("type: " + functionType);
		if (iterations != null)
			result.add("min_points: " + iterations);
		if (skips != null)
			result.add("skips: " + skips);
		if (seed != null)
			result.add("seed: " + ComplexNumberUtils.exportString(seed));
		result.add("\r\n \n \n\r");

		for (InputFunction function : inputFunctions) {
			result.add("begin_input_function: " + function.getInputID());
			for (String s : function.historyInfo())
				result.add("\t" + s);
			result.add("end_input_function");
			result.add("\r\n \n \n\r");
		}

		return result;
	}

	public JComponent[] propertiesComponents() {
		Box panel = Box.createVerticalBox();
		panel.add(new JLabel(this.toString()));

		if (iterations != null)
			panel.add(new JLabel("Points to Plot:  " + iterations));
		if (skips != null)
			panel.add(new JLabel("Skips:  " + skips));
		if (seed != null)
			panel.add(new JLabel("Seed Value:  " + seed));

		if (points != null)
			panel.add(new JLabel("Actual number of points in the set:   "
					+ points.length));

		JList list = new JList(inputFunctions);
		list.setVisibleRowCount(6);
		JScrollPane scroller = new JScrollPane(list);
		scroller.setBorder(BorderFactory.createTitledBorder("Input Functions"));
		return new JComponent[] { panel, scroller };
	}
}
