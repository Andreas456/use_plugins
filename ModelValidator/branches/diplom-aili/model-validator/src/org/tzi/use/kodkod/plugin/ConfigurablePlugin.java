package org.tzi.use.kodkod.plugin;

import java.io.File;
import java.util.Iterator;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.HierarchicalINIConfiguration;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.configuration.SubnodeConfiguration;
import org.tzi.kodkod.helper.LogMessages;
import org.tzi.kodkod.model.config.impl.DefaultConfigurationVisitor;
import org.tzi.kodkod.model.config.impl.PropertyConfigurationVisitor;

/**
 * A base class for plugins that can be configured using property files.
 * With just a file name without a sector name given, the first sector of
 * the properties file will be used. 
 * 
 * @author Frank Hilken
 */
public abstract class ConfigurablePlugin extends AbstractPlugin {

	/**
	 * Configuration of the model with the data from the given file.
	 * 
	 * @param file
	 * @throws ConfigurationException
	 */
	protected PropertyConfigurationVisitor configureModel(File file) throws ConfigurationException {
		HierarchicalINIConfiguration hierarchicalINIConfiguration = new HierarchicalINIConfiguration(file);
		String section;
		if (hierarchicalINIConfiguration.getSections().iterator().hasNext()) {
			section = (String) hierarchicalINIConfiguration.getSections().iterator().next();
		} else {
			section = "default"; //from here on, it doesn't matter, what the content of this section string is
		}
		return configureModel(file, section);
	}
	
	/**
	 * Configuration of the model with the data from the given file.
	 * 
	 * @param file
	 * @throws ConfigurationException
	 */
	protected PropertyConfigurationVisitor configureModel(File file, String section) throws ConfigurationException {
		model().reset();
		PropertyConfigurationVisitor newConfigurationVisitor = new PropertyConfigurationVisitor(getConfigurationFromSector(file, section));
		model().accept(newConfigurationVisitor);
		
		if (newConfigurationVisitor.containErrors()) {
			throw new ConfigurationException();
		}
		
		LOG.info(LogMessages.modelConfigurationSuccessful);
		return newConfigurationVisitor;
	}

	/**
	 * Configuration with the default search space.
	 * 
	 * @return
	 * @throws Exception
	 */
	protected File configureModel() throws Exception {
		model().reset();
		DefaultConfigurationVisitor configurationVisitor = new DefaultConfigurationVisitor(mModel.filename());
		model().accept(configurationVisitor);

		LOG.info(LogMessages.modelConfigurationSuccessful);

		return configurationVisitor.getFile();
	}
	
	private PropertiesConfiguration getConfigurationFromSector(File file, String section) throws ConfigurationException {
		PropertiesConfiguration sectorConfiguration = new PropertiesConfiguration();
		HierarchicalINIConfiguration hierarchicalINIConfiguration = new HierarchicalINIConfiguration(file);

		if (!hierarchicalINIConfiguration.getSections().isEmpty()) {
			SubnodeConfiguration sectionConfigurations = hierarchicalINIConfiguration.getSection(section);
			Iterator<?> keysIterator = sectionConfigurations.getKeys();
			while (keysIterator.hasNext()) {
				String key = (String) keysIterator.next();
				if (!key.startsWith("--"))
					sectorConfiguration.addProperty(key, sectionConfigurations.getString(key));
			}
		} else {
			Iterator<?> keysIterator = hierarchicalINIConfiguration.getKeys();
			while (keysIterator.hasNext()) {
				String key = (String) keysIterator.next();
				if (!key.startsWith("--"))
					sectorConfiguration.addProperty(key, hierarchicalINIConfiguration.getString(key));
			}
		}
		
		return sectorConfiguration;
	}
	
}