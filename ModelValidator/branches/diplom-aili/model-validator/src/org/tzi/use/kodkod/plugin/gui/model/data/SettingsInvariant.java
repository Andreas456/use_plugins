package org.tzi.use.kodkod.plugin.gui.model.data;

import org.tzi.kodkod.model.config.impl.DefaultConfigurationValues;
import org.tzi.use.uml.mm.MClassInvariant;

public class SettingsInvariant {
	
	private MClassInvariant invariant;
	private Boolean active;
	private Boolean negate;
	private SettingsConfiguration configurationSettings;
	
	public SettingsConfiguration getConfigurationSettings() {
		return configurationSettings;
	}

	public SettingsInvariant(MClassInvariant invariant, SettingsConfiguration configurationSettings) {
		this.invariant = invariant;
		this.active = DefaultConfigurationValues.INVARIANT_ACTIVE;
		this.negate = DefaultConfigurationValues.INVARIANT_NEGATE;
		this.configurationSettings = configurationSettings;
	}

	public MClassInvariant getInvariant() {
		return invariant;
	}

	public Boolean getActive() {
		return active;
	}

	public void setActive(Boolean active) {
		this.active = active;
		this.configurationSettings.setChanged(true);
	}

	public Boolean getNegate() {
		return negate;
	}

	public void setNegate(Boolean negate) {
		this.negate = negate;
		this.configurationSettings.setChanged(true);
	}
	
}