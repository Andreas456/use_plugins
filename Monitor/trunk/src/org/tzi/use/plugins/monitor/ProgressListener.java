package org.tzi.use.plugins.monitor;

public interface ProgressListener {
	void progressStart(ProgressArgs args);
	void progress(ProgressArgs args);
	void progressEnd();
}
