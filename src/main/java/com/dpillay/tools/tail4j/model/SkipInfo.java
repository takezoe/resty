package com.dpillay.tools.tail4j.model;

public class SkipInfo {
	long skipLineCount = 0;
	long skipLength = 0;

	public SkipInfo(long skipLineCount, long skipLength) {
		super();
		this.skipLineCount = skipLineCount;
		this.skipLength = skipLength;
	}

	public long getSkipLineCount() {
		return skipLineCount;
	}

	public long getSkipLength() {
		return skipLength;
	}
}
