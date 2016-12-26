package com.dpillay.tools.tail4j.configuration;

import java.util.ArrayList;
import java.util.List;

public class TailConfiguration {
	private long showLines = -1;
	private boolean force = false;
	private List<String> files = new ArrayList<String>();

	public TailConfiguration(long showLines, boolean force, List<String> files) {
		super();
		this.showLines = showLines;
		this.force = force;
		this.files = files;
	}

	public TailConfiguration() {
		super();
	}

	public long getShowLines() {
		return showLines;
	}

	public void setShowLines(long skipLines) {
		this.showLines = skipLines;
	}

	public boolean isForce() {
		return force;
	}

	public void setForce(boolean force) {
		this.force = force;
	}

	public List<String> getFiles() {
		return files;
	}

	public void setFiles(List<String> files) {
		this.files = files;
	}

	@Override
	public String toString() {
		return "TailConfiguration [files=" + files + ", force=" + force
				+ ", showLines=" + showLines + "]";
	}
}
