package org.tzi.use.kodkod.plugin.gui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.LinkedHashMap;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.ToolTipManager;
import javax.swing.border.BevelBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.TableColumn;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.HierarchicalINIConfiguration;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.tzi.kodkod.model.config.impl.PropertyEntry;
import org.tzi.kodkod.model.iface.IClass;
import org.tzi.kodkod.model.iface.IModel;
import org.tzi.kodkod.model.type.TypeConstants;
import org.tzi.use.gui.util.ExtFileFilter;
import org.tzi.use.kodkod.plugin.PropertiesWriter;
import org.tzi.use.kodkod.plugin.gui.model.TableModelAssociation;
import org.tzi.use.kodkod.plugin.gui.model.TableModelAttribute;
import org.tzi.use.kodkod.plugin.gui.model.data.SettingsConfiguration;
import org.tzi.use.kodkod.plugin.gui.util.ChangeConfiguration;
import org.tzi.use.util.StringUtil;

import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;

/**
 *  A GUI for adjusting the configurations before giving them to the model validator
 *
 * @author Subi Aili
 * @author Frank Hilken
 */
public class ModelValidatorConfigurationWindow extends JDialog {

	private static final long serialVersionUID = 1L;

	private static final String DEFAULT_CONFIG_PREFIX = "config";

	private int defaultNameCount = 0;
	private String selectedSection;
	private JLabel currentFileLabel;
	private JLabel attributesLabel;
	private JLabel associationsLabel;
	private JTextArea statusArea;

	private JTable invariantsTable;
	private JTable optionsTable;
	private JTable associationTable;
	private JTable attributeTable;
	private JTable classTable;
	private JTable integerTable;
	private JTable realTable;
	private JTable stringTable;
	private JCheckBox attributeCheckBox;
	private JCheckBox integerCheckbox;
	private JCheckBox stringCheckbox;
	private JCheckBox realCheckbox;

	private IModel model;
	private File useFile;
	private File file;
	private SettingsConfiguration settingsConfiguration;
	//TODO replace with configuration manager
	private LinkedHashMap<String, Configuration> propertiesConfigurationSections;
	private Configuration propertiesConfiguration;

	private IClass selectedClass;
	private boolean readyToValidate;

	private JTabbedPane center;
	private JPanel main;
	private JPanel northNorth;
	private JPanel northSouth;
	private JPanel north;
	private JPanel southWest;
	private JPanel southCenter;
	private JPanel south;
	private JButton validateButton;

	private JComboBox<String> sectionSelectionComboBox;
	private ComboBoxItemListener comboBoxActionListener;
	private ActionListener validateActionListener;


	/*
	 * Listens for changed selection in the drop down menu, puts previous propertiesConfiguration
	 * into propertiesConfigurationSection with former selectedSection, then gets the current
	 * selected Section and get its propertiesConfiguration, eventually puts the loaded configurations
	 * into settingsConfiguration.
	 */
	private class ComboBoxItemListener implements ItemListener {
		@Override
		public void itemStateChanged(ItemEvent e) {
			if(e.getStateChange() == ItemEvent.SELECTED){
				if (propertiesConfiguration != null) {
					propertiesConfiguration = ChangeConfiguration.toProperties(settingsConfiguration, model);
					propertiesConfigurationSections.put(selectedSection, propertiesConfiguration);
				}
				selectedSection = (String) e.getItem();
				propertiesConfiguration = propertiesConfigurationSections.get(selectedSection);
				boolean beforeChange = settingsConfiguration.isChanged();
				ChangeConfiguration.resetSettings(settingsConfiguration);
				ChangeConfiguration.toSettings(model, propertiesConfiguration, settingsConfiguration);
				settingsConfiguration.setChanged(beforeChange);
				update();
			}
		}
	}

	/*
	 * Listens for changed class row selection in the class table
	 */
	private class ClassTableSelectionHandler implements ListSelectionListener {
		@Override
		public void valueChanged(ListSelectionEvent e) {
			if (!e.getValueIsAdjusting()) {
				ListSelectionModel lsm = (ListSelectionModel) e.getSource();
				int selectedRow = 0;
				int minIndex = lsm.getMinSelectionIndex();
				int maxIndex = lsm.getMaxSelectionIndex();
				for (int i = minIndex; i <= maxIndex; i++) {
					if (lsm.isSelectedIndex(i)) {
						selectedRow = i;
						break;
					}
				}
				selectedClass = (IClass) classTable.getValueAt(selectedRow, 0);
				attributesLabel.setText("Attributes of class " + selectedClass.name());
				associationsLabel.setText("Associations of class " + selectedClass.name());
				updateClassAttributes(selectedClass);
				updateClassAssociations(selectedClass);
			}
		}
	}

	public ModelValidatorConfigurationWindow(final JFrame parent, final IModel model, final String useFile) {
		super(parent, "Model Validator Configuration");
		this.model = model;
		this.useFile = new File(useFile);

		setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
		setModalityType(ModalityType.APPLICATION_MODAL);
		setSize(1024,300);

		settingsConfiguration = new SettingsConfiguration(model);
		TableBuilder tableBuilder = new TableBuilder(settingsConfiguration);

		integerTable = tableBuilder.integer();
		realTable = tableBuilder.real();
		stringTable = tableBuilder.string();
		optionsTable = tableBuilder.options();
		classTable = tableBuilder.classes();
		attributeTable = tableBuilder.attributes();
		associationTable = tableBuilder.associations();
		invariantsTable = tableBuilder.invariants();

		classTable.getSelectionModel().addListSelectionListener(new ClassTableSelectionHandler());

		file = new File(useFile.replaceAll("\\.use", "") + ".properties");
		if (file.exists()) {
			currentFileLabel = new JLabel(file.getAbsolutePath());
		} else {
			currentFileLabel = new JLabel("");
		}
		propertiesConfigurationSections = new LinkedHashMap<String, Configuration>();
		readyToValidate = false;

		center = new JTabbedPane(SwingConstants.TOP);
		main = new JPanel(new BorderLayout());
		northNorth = new JPanel(new FlowLayout(FlowLayout.LEFT));
		northSouth = new JPanel(new FlowLayout(FlowLayout.LEFT));
		north = new JPanel(new BorderLayout());
		southWest = new JPanel(new FlowLayout(FlowLayout.LEFT));
		southCenter = new JPanel(new BorderLayout());
		southCenter.setBorder(new BevelBorder(BevelBorder.LOWERED));
		south = new JPanel(new BorderLayout());

		sectionSelectionComboBox = new JComboBox<String>();
		comboBoxActionListener = new ComboBoxItemListener();

		attributeCheckBox = new JCheckBox("Show specific bounds", true);
		attributeCheckBox.addItemListener(new ItemListener() {
			
			private TableColumn[] attributeColumnsToHide = new TableColumn[]{
				attributeTable.getColumn(ConfigurationTerms.ATTRIBUTES_MIN),
				attributeTable.getColumn(ConfigurationTerms.ATTRIBUTES_MAX),
				attributeTable.getColumn(ConfigurationTerms.ATTRIBUTES_MINSIZE),
				attributeTable.getColumn(ConfigurationTerms.ATTRIBUTES_MAXSIZE)
			};
			private boolean isExpanded = true;
			
			@Override
			public void itemStateChanged(ItemEvent e) {
				if(e.getStateChange() == ItemEvent.SELECTED && !isExpanded) {
					TableColumn tempColumn = attributeTable.getColumnModel().getColumn(1);
					attributeTable.removeColumn(tempColumn);
					for (int i = 0; i < attributeColumnsToHide.length; i++) {
						attributeTable.addColumn(attributeColumnsToHide[i]);
					}
					attributeTable.addColumn(tempColumn);
					isExpanded = true;
				} else if (e.getStateChange() == ItemEvent.DESELECTED && isExpanded) {
					for (int i = 0; i < attributeColumnsToHide.length; i++) {
						attributeTable.removeColumn(attributeColumnsToHide[i]);
					}
					isExpanded = false;
				}
			}
		});
		attributeCheckBox.setSelected(false);
		
		validateActionListener = new ActionListener() {
			@Override
			public void actionPerformed( ActionEvent e ) {
				if (settingsConfiguration.isChanged()) {
					int result = JOptionPane.showConfirmDialog(parent,
							"Do you want to save them before Validation?",
							"Configurations are not saved yet!",
							JOptionPane.YES_NO_CANCEL_OPTION);
					if (result == 2) {
						return;
					} else if (result == 0) {
						saveConfigurationsToFile(file);
					}
				}
				propertiesConfiguration = ChangeConfiguration.toProperties(settingsConfiguration, model);
				readyToValidate = true;
				dispose();
			}
		};

		if (file.exists()) {
			extractConfigurations(file);
			ChangeConfiguration.resetSettings(settingsConfiguration);
			ChangeConfiguration.toSettings(model, propertiesConfiguration, settingsConfiguration);
		} else {
			setAllDefault();
		}

		settingsConfiguration.setChanged(false);
		
		validateButton = new JButton("Validate");
		validateButton.addActionListener(validateActionListener);

		statusArea = new JTextArea();
		statusArea.setEditable(false);
		statusArea.setBackground(getParent().getBackground());
		statusArea.setLineWrap(true);
		statusArea.setWrapStyleWord(true);
		statusArea.setText("");

		northNorth.add(new JLabel("Loaded properties file: "));
		northNorth.add(currentFileLabel);
		northSouth.add(new JLabel("Loaded configuration: "));
		northSouth.add(sectionSelectionComboBox);
		north.add(northNorth, BorderLayout.NORTH);
		north.add(northSouth, BorderLayout.SOUTH);
		center.add("Basic Types and Options", buildBasicTypesAndOptionsTab());
		center.add("Classes and Associations", buildClassesAndAssociationsTab());
		center.add("Invariants", buildInvariantsTab());
		southWest.add(validateButton);
		southCenter.add(statusArea, BorderLayout.CENTER);
		south.add(southWest, BorderLayout.WEST);
		south.add(southCenter, BorderLayout.CENTER);
		main.add(north, BorderLayout.NORTH);
		main.add(center, BorderLayout.CENTER);
		main.add(south, BorderLayout.SOUTH);

		sectionSelectionComboBox.addItemListener(comboBoxActionListener);
		if(classTable.getModel().getRowCount() > 0){
			classTable.getSelectionModel().setSelectionInterval(0, 0);
		}
		
		ToolTipManager.sharedInstance().setDismissDelay(Integer.MAX_VALUE); // increases tooltip display time
		addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e){
				close();
			}
		});
		setJMenuBar(buildMenuBar());
		setContentPane(main);
		update();
		pack();
		setLocationRelativeTo(parent);
		setVisible(true);
	}

	private void openSaveDialog() {
		JFileChooser fileChooser = new JFileChooser();

		if (useFile != null) {
			fileChooser = new JFileChooser(useFile.getParentFile());
		} else {
			fileChooser = new JFileChooser();
		}
		fileChooser.setFileFilter(new ExtFileFilter("properties", "Properties files"));
		fileChooser.setSelectedFile(file);

		if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
			File chosenFileName = fileChooser.getSelectedFile();
			if (chosenFileName.exists()) {
				int result = JOptionPane.showConfirmDialog(this, "Do you want to overwrite the existing file?", "File already exists!", JOptionPane.OK_CANCEL_OPTION);
				if (result == 0) {
					saveConfigurationsToFile(chosenFileName);
				}
			} else {
				saveConfigurationsToFile(chosenFileName);
			}
			file = chosenFileName;
			currentFileLabel.setText(file.getAbsolutePath());
		}
	}

	/**
	 * extracts all configuration from a .properties-File and puts them in the
	 * hashtable hierarchicalINIConfiguration and sets the first read configuration as
	 * selected propertiesConfiguration
	 * @param file
	 */
	private void extractConfigurations(File file) {
		HierarchicalINIConfiguration hierarchicalINIConfiguration;
		try {
			hierarchicalINIConfiguration = new HierarchicalINIConfiguration(file);
		} catch (ConfigurationException e) {
			JOptionPane.showMessageDialog(this, "Error while loading properties file!", "Error!", JOptionPane.ERROR_MESSAGE);
			return;
		}
		sectionSelectionComboBox.removeAllItems();
		propertiesConfigurationSections = new LinkedHashMap<String, Configuration>();
		
		String firstSection = null;
		if (hierarchicalINIConfiguration.getSections().isEmpty()) {
			// create default configuration
			Configuration configuration = hierarchicalINIConfiguration.getSection(null);
			propertiesConfigurationSections.put(PropertyEntry.DEFAULT_SECTION_NAME, configuration);
			sectionSelectionComboBox.addItem(PropertyEntry.DEFAULT_SECTION_NAME);
			firstSection = PropertyEntry.DEFAULT_SECTION_NAME;
		} else {
			boolean first = true;
			for (String section : hierarchicalINIConfiguration.getSections()) {
				Configuration conf = hierarchicalINIConfiguration.getSection(section);
				if(section == null){
					//TODO what if there is a default section already? ;)
					section = PropertyEntry.DEFAULT_SECTION_NAME;
				}
				propertiesConfigurationSections.put(section, conf);
				sectionSelectionComboBox.addItem(section);
				if(first){
					firstSection = section;
					first = false;
				}
			}
		}
		
		selectedSection = firstSection;
		propertiesConfiguration = propertiesConfigurationSections.get(firstSection);
		sectionSelectionComboBox.setSelectedItem(firstSection);
	}

	private void setAllDefault() {
		sectionSelectionComboBox.removeAllItems();
		selectedSection = PropertyEntry.DEFAULT_SECTION_NAME;

		ChangeConfiguration.resetSettings(settingsConfiguration);
		propertiesConfiguration = ChangeConfiguration.toProperties(settingsConfiguration, model);

		propertiesConfigurationSections.put(selectedSection, propertiesConfiguration);
		sectionSelectionComboBox.addItem(selectedSection);
	}

	public Configuration getChosenConfiguration() {
		return propertiesConfiguration;
	}

	public Boolean isReadyToValidate() {
		return readyToValidate;
	}

	private JMenuBar buildMenuBar() {
		JMenuBar menuBar = new JMenuBar();
		JMenu fileMenu = new JMenu("File");

		JMenuItem openMenuItem = new JMenuItem("Open");
		openMenuItem.addActionListener( new ActionListener() {
			@Override
			public void actionPerformed( ActionEvent e ) {
				JFileChooser fileChooser = new JFileChooser();
				if (useFile != null) {
					fileChooser = new JFileChooser(useFile.getParentFile());
				} else {
					fileChooser = new JFileChooser();
				}
				fileChooser.setFileFilter(new ExtFileFilter("properties", "Properties files"));

				if (fileChooser.showOpenDialog(ModelValidatorConfigurationWindow.this) == JFileChooser.APPROVE_OPTION) {
					file = fileChooser.getSelectedFile();
					extractConfigurations(file);
					ChangeConfiguration.resetSettings(settingsConfiguration);
					ChangeConfiguration.toSettings(model, propertiesConfiguration, settingsConfiguration);
					classTable.clearSelection();
					update();
					settingsConfiguration.setChanged(false);
					currentFileLabel.setText(file.getAbsolutePath());
				}

			}
		});
		fileMenu.add(openMenuItem);

		JMenuItem saveMenuItem = new JMenuItem("Save");
		saveMenuItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (file.exists()) {
					saveConfigurationsToFile(file);
				} else {
					openSaveDialog();
				}
			}
		});
		fileMenu.add(saveMenuItem);

		JMenuItem saveAsMenuItem = new JMenuItem("Save as...");
		saveAsMenuItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				openSaveDialog();
			}
		});
		fileMenu.add(saveAsMenuItem);
		
		JMenuItem closeMenuItem = new JMenuItem("Close");
		closeMenuItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				close();
			}
		});
		fileMenu.add(closeMenuItem);

		JMenu configurationMenu = new JMenu("Configuration");
		
		JMenuItem newMenuItem = new JMenuItem("New");
		newMenuItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				String newName = JOptionPane.showInputDialog("Please input the name of the new configuration:", DEFAULT_CONFIG_PREFIX + defaultNameCount);
				defaultNameCount++;
				
				if (newName == null || newName.isEmpty()) {
					return;
				}
				
				Configuration pc = ChangeConfiguration.toProperties(settingsConfiguration, model);
				propertiesConfigurationSections.put(selectedSection, pc);
				ChangeConfiguration.resetSettings(settingsConfiguration);
				propertiesConfigurationSections.put(newName, ChangeConfiguration.toProperties(settingsConfiguration, model));
				
				selectedSection = newName;
				settingsConfiguration.setChanged(true);
				sectionSelectionComboBox.addItem(newName);
				sectionSelectionComboBox.setSelectedItem(newName);
			}
		});
		configurationMenu.add(newMenuItem);

		JMenuItem cloneMenuItem = new JMenuItem("Clone");
		cloneMenuItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				String newName = JOptionPane.showInputDialog("Please input the name of the cloned configuration:", DEFAULT_CONFIG_PREFIX + defaultNameCount);
				defaultNameCount++;
				
				if (newName == null || newName.isEmpty()) {
					return;
				}
				
				PropertiesConfiguration conf = new PropertiesConfiguration();
				conf.append(propertiesConfiguration);
				propertiesConfigurationSections.put(newName, conf);
				
				selectedSection = newName;
				settingsConfiguration.setChanged(true);
				sectionSelectionComboBox.addItem(newName);
				sectionSelectionComboBox.setSelectedItem(newName);
			}
		});
		configurationMenu.add(cloneMenuItem);
		
		JMenuItem renameMenuItem = new JMenuItem("Rename");
		renameMenuItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				String sectionToDelete = selectedSection;
				String newName = JOptionPane.showInputDialog("Please input the new name of this configuration:", selectedSection);
				if (newName != null && !newName.equals("")) {
					propertiesConfiguration = ChangeConfiguration.toProperties(settingsConfiguration, model);
					propertiesConfigurationSections.put(newName, propertiesConfiguration);
					propertiesConfigurationSections.remove(sectionToDelete);
					selectedSection = newName;
					settingsConfiguration.setChanged(true);
					sectionSelectionComboBox.addItem(newName);
					sectionSelectionComboBox.removeItem(sectionToDelete);
					sectionSelectionComboBox.setSelectedItem(newName);
				}
			}
		});
		configurationMenu.add(renameMenuItem);

		JMenuItem deleteMenuItem = new JMenuItem("Delete");
		deleteMenuItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (propertiesConfigurationSections.size() > 1) {
					String currentSection = (String) sectionSelectionComboBox.getSelectedItem();
					int currentIndex = sectionSelectionComboBox.getSelectedIndex();
					String futureSection;
					int futureIndex;
					if (currentIndex > 0) {
						futureIndex = currentIndex-1;
					} else {
						futureIndex = 1;
					}
					futureSection = sectionSelectionComboBox.getItemAt(futureIndex);
					selectedSection = futureSection;
					ChangeConfiguration.resetSettings(settingsConfiguration);
					ChangeConfiguration.toSettings(model, propertiesConfigurationSections.get(selectedSection), settingsConfiguration);
					propertiesConfigurationSections.remove(currentSection);
					sectionSelectionComboBox.setSelectedItem(selectedSection);
					sectionSelectionComboBox.removeItem(currentSection);
				} else {
					propertiesConfigurationSections.clear();
					selectedSection = PropertyEntry.DEFAULT_SECTION_NAME;
					ChangeConfiguration.resetSettings(settingsConfiguration);
					propertiesConfigurationSections.put(selectedSection, ChangeConfiguration.toProperties(settingsConfiguration, model));
					sectionSelectionComboBox.removeAllItems();
					sectionSelectionComboBox.addItem(selectedSection);
					sectionSelectionComboBox.setSelectedItem(selectedSection);
				}
				settingsConfiguration.setChanged(true);
			}
		});
		configurationMenu.add(deleteMenuItem);

		configurationMenu.add(new JSeparator());
		
		JMenuItem validateMenuItem = new JMenuItem("Validate");
		validateMenuItem.addActionListener(validateActionListener);
		configurationMenu.add(validateMenuItem);

		menuBar.add(fileMenu);
		menuBar.add(configurationMenu);

		return menuBar;
	}

	private JSplitPane buildBasicTypesAndOptionsTab() {
		JSplitPane basicTypesAndOptionsPanel;

		JPanel leftUpper = new JPanel(new BorderLayout());
		leftUpper.add(basicTypesPanel(), BorderLayout.NORTH);
		JPanel rightUpper = new JPanel(new BorderLayout());
		rightUpper.add(new JScrollPane(optionsTable), BorderLayout.CENTER);

		JPanel rightLower = new JPanel(new BorderLayout());
		JTextArea legendText = new JTextArea(LegendEntry.LEGEND);
		legendText.setEditable(false);
		legendText.setBackground(getParent().getBackground());
		legendText.setLineWrap(true);
		legendText.setWrapStyleWord(true);
		JScrollPane legenScroll = new JScrollPane(legendText);
		rightLower.add(legenScroll, BorderLayout.CENTER);

		JPanel right = new JPanel(new BorderLayout());
		right.add(rightUpper, BorderLayout.NORTH);
		right.add(rightLower, BorderLayout.CENTER);

		JScrollPane jScrollPane = new JScrollPane(leftUpper);
		jScrollPane.setBorder(null);
		
		basicTypesAndOptionsPanel = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, jScrollPane, right);
		basicTypesAndOptionsPanel.setDividerLocation(512);

		return basicTypesAndOptionsPanel;
	}

	private JPanel basicTypesPanel() {
		JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

		Dimension space = new Dimension(0,10);

		final JPanel integerPanel = new JPanel(new BorderLayout());
		final JScrollPane intScroll = new JScrollPane(integerTable);
		integerCheckbox = new JCheckBox(TypeConstants.INTEGER, settingsConfiguration.getIntegerTypeSettings().isEnabled());
		Font captionFont = integerCheckbox.getFont().deriveFont(Font.BOLD, 12f);
		integerCheckbox.setFont(captionFont);
		integerCheckbox.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent e) {
				boolean on = e.getStateChange() == ItemEvent.SELECTED;
				if(on){
					integerPanel.add(intScroll, BorderLayout.CENTER);
					
				} else {
					integerPanel.remove(intScroll);
				}
				settingsConfiguration.getIntegerTypeSettings().setEnabled(on);
				integerPanel.revalidate();
			}
		});
		
		integerPanel.add(integerCheckbox, BorderLayout.NORTH);
		if(settingsConfiguration.getIntegerTypeSettings().isEnabled()){
			integerPanel.add(intScroll, BorderLayout.CENTER);
		}
		integerPanel.add(new JLabel("Here will be more information about what integers are actually generated by the settings."), BorderLayout.SOUTH);
		panel.add(integerPanel);

		panel.add(Box.createRigidArea(space));
		
		final JPanel stringPanel = new JPanel(new BorderLayout());
		final JScrollPane stringScroll = new JScrollPane(stringTable);
		stringCheckbox = new JCheckBox(TypeConstants.STRING, settingsConfiguration.getStringTypeSettings().isEnabled());
		stringCheckbox.setFont(captionFont);
		stringCheckbox.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent e) {
				boolean on = e.getStateChange() == ItemEvent.SELECTED;
				if(on){
					stringPanel.add(stringScroll, BorderLayout.CENTER);
					
				} else {
					stringPanel.remove(stringScroll);
				}
				settingsConfiguration.getStringTypeSettings().setEnabled(on);
				stringPanel.revalidate();
			}
		});
		
		stringPanel.add(stringCheckbox, BorderLayout.NORTH);
		if(settingsConfiguration.getStringTypeSettings().isEnabled()){
			stringPanel.add(stringScroll, BorderLayout.CENTER);
		}
		panel.add(stringPanel);
		
		panel.add(Box.createRigidArea(space));
		
		final JPanel realPanel = new JPanel(new BorderLayout());
		final JScrollPane realScroll = new JScrollPane(realTable);
		realCheckbox = new JCheckBox(TypeConstants.REAL, settingsConfiguration.getRealTypeSettings().isEnabled());
		realCheckbox.setFont(captionFont);
		realCheckbox.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent e) {
				boolean on = e.getStateChange() == ItemEvent.SELECTED;
				if(on){
					realPanel.add(realScroll, BorderLayout.CENTER);
					
				} else {
					realPanel.remove(realScroll);
				}
				settingsConfiguration.getRealTypeSettings().setEnabled(on);
				realPanel.revalidate();
			}
		});
		
		realPanel.add(realCheckbox, BorderLayout.NORTH);
		if(settingsConfiguration.getRealTypeSettings().isEnabled()){
			realPanel.add(realScroll, BorderLayout.CENTER);
		}
		panel.add(realPanel);

		return panel;
	}

	private JSplitPane buildClassesAndAssociationsTab() {
		JTextArea abstractClassesText = new JTextArea();
		abstractClassesText.setBackground(getBackground());
		abstractClassesText.setText(abstractClassesChildren(model));
		abstractClassesText.setLineWrap(true);
		abstractClassesText.setWrapStyleWord(true);
		attributesLabel = new JLabel("Attributes");
		associationsLabel = new JLabel("Associations");
		
		Font captionFont = attributesLabel.getFont().deriveFont(Font.BOLD, 12f);
		attributesLabel.setFont(captionFont);
		associationsLabel.setFont(captionFont);

		JScrollPane classesScrollPane = new JScrollPane(classTable);
		JScrollPane abstractClsScrollPane = new JScrollPane(abstractClassesText);
		JScrollPane attributesScrollPane = new JScrollPane(attributeTable);
		JScrollPane associationsScrollPane = new JScrollPane(associationTable);
		JPanel classesPanel = new JPanel(new BorderLayout());
		JPanel attributesPanel = new JPanel(new BorderLayout());
		JPanel associationsPanel = new JPanel(new BorderLayout());
		JPanel attributeLabelPanel = new JPanel(new BorderLayout());
		JSplitPane caaTabMainSplitPane;
		JSplitPane caaTabLeftSplitPane;
		JSplitPane caaTabRightSplitPane;

		classesScrollPane.setPreferredSize(new Dimension(getWidth()/2,(getHeight())));
		abstractClsScrollPane.setPreferredSize(new Dimension(getWidth()/2,getHeight()/4));
		attributesScrollPane.setPreferredSize(new Dimension(getWidth()/2,getHeight()/2));
		associationsScrollPane.setPreferredSize(new Dimension(getWidth()/2,getHeight()/2));
		classesPanel.add(classesScrollPane, BorderLayout.CENTER);
		attributeLabelPanel.setLayout(new BoxLayout(attributeLabelPanel, BoxLayout.LINE_AXIS));
		attributeLabelPanel.add(attributesLabel);
		attributeLabelPanel.add(Box.createHorizontalGlue());
		attributeLabelPanel.add(attributeCheckBox);
		attributesPanel.add(attributeLabelPanel, BorderLayout.NORTH);
		attributesPanel.add(attributesScrollPane, BorderLayout.CENTER);
		associationsPanel.add(associationsLabel, BorderLayout.NORTH);
		associationsPanel.add(associationsScrollPane, BorderLayout.CENTER);
		caaTabRightSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, attributesPanel, associationsPanel);
		caaTabLeftSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, classesPanel, abstractClsScrollPane);
		caaTabMainSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, caaTabLeftSplitPane, caaTabRightSplitPane);
		return caaTabMainSplitPane;
	}

	private JPanel buildInvariantsTab() {
		JScrollPane invariantsScrollPane = new JScrollPane(invariantsTable);
		invariantsScrollPane.setPreferredSize(new Dimension(getWidth()/2,getHeight()));
		JPanel invariantsPanel = new JPanel(new BorderLayout());
		invariantsPanel.add(invariantsScrollPane, BorderLayout.CENTER);

		return invariantsPanel;
	}

	/**
	 * updates the attributes table referring to given className and gets them from the SettingsConfiguration
	 */
	private void updateClassAttributes(IClass className) {
		TableModelAttribute attributeModel = (TableModelAttribute)attributeTable.getModel();
		attributeModel.setClass(settingsConfiguration.getClassSettings(className));
	}

	private void updateClassAssociations(IClass className) {
		TableModelAssociation associationModel = (TableModelAssociation)associationTable.getModel();
		associationModel.setClass(settingsConfiguration.getClassSettings(className));
	}

	private void saveConfigurationsToFile(File file) {
		try {
			propertiesConfiguration = ChangeConfiguration.toProperties(settingsConfiguration, model);
			propertiesConfigurationSections.put(selectedSection, propertiesConfiguration);

			PropertiesWriter pw = new PropertiesWriter(model);
			pw.writeToFile(file, propertiesConfigurationSections);

			settingsConfiguration.setChanged(false);
			currentFileLabel.setText(file.getAbsolutePath());
		} catch (IOException e) {
			JOptionPane.showMessageDialog(this, "Error while saving configuration to file!", "Error!", JOptionPane.ERROR_MESSAGE);
		}
	}

	private void update() {
		integerTable.repaint();
		realTable.repaint();
		stringTable.repaint();
		optionsTable.repaint();
		classTable.repaint();
		attributeTable.repaint();
		associationTable.repaint();
		invariantsTable.repaint();
		
		integerCheckbox.setSelected(settingsConfiguration.getIntegerTypeSettings().isEnabled());
		stringCheckbox.setSelected(settingsConfiguration.getStringTypeSettings().isEnabled());
		realCheckbox.setSelected(settingsConfiguration.getRealTypeSettings().isEnabled());
	}
	
	private void close() {
		boolean isToBeClosed = true;
		if (settingsConfiguration.isChanged()) {
			int result = JOptionPane.showConfirmDialog(ModelValidatorConfigurationWindow.this,
					"Do you want to save changes before closing?",
					"Configurations are not saved yet!",
					JOptionPane.YES_NO_CANCEL_OPTION);
			if (result == 2) {
				isToBeClosed = false;
			} else if (result == 0) {
				saveConfigurationsToFile(file);
				propertiesConfiguration = ChangeConfiguration.toProperties(settingsConfiguration, model);
			}
		}
		if (isToBeClosed) {
			readyToValidate = false;
			dispose();
		}
	}
	
	private String abstractClassesChildren(IModel model) {
		StringBuilder abstractText = new StringBuilder("Abstract Classes:");
		abstractText.append(StringUtil.NEWLINE);

		Collection<IClass> abstractClasses = Collections2.filter(model.classes(), new Predicate<IClass>() {
			@Override
			public boolean apply(IClass input) {
				return input.isAbstract();
			}
		});
		if(abstractClasses.isEmpty()){
			abstractText.append("None.");
			return abstractText.toString();
		} else {
			StringUtil.fmtSeq(abstractText, abstractClasses, ", ");
		}

		if(abstractClasses.size() > 0){
			abstractText.append(StringUtil.NEWLINE);
			abstractText.append(StringUtil.NEWLINE);
			abstractText.append("Inheriting Classes:");
			abstractText.append(StringUtil.NEWLINE);

			for(final IClass abstractClass : model.classes()){
				if(abstractClass.allChildren().isEmpty()){
					continue;
				}
				Collection<IClass> inheritingClasses = abstractClass.allChildren();
				StringUtil.fmtSeq(abstractText, inheritingClasses, StringUtil.NEWLINE, new StringUtil.IElementFormatter<IClass>() {
					@Override
					public String format(IClass element) {
						return abstractClass.name() + " > " + element.name();
					}
				});

			}
		}

		return abstractText.toString();
	}

}
