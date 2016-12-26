package com.dpillay.tools.tail4j.readers;

import java.io.BufferedInputStream;
import java.io.InputStream;

import com.dpillay.tools.tail4j.configuration.TailConfiguration;
import com.dpillay.tools.tail4j.core.TailListener;
import com.dpillay.tools.tail4j.core.TailedReader;
import com.dpillay.tools.tail4j.model.SkipInfo;
import com.dpillay.tools.tail4j.model.TailEvent;

public class InputStreamReader extends AbstractReader implements
		TailedReader<String, InputStream> {
	private InputStream stream;

	public InputStreamReader(TailConfiguration configuration,
			InputStream stream, TailListener<String> listener) {
		this.configuration = configuration;
		this.listener = listener;
		if (!(stream instanceof BufferedInputStream)) {
			this.stream = new BufferedInputStream(stream);
		} else {
			this.stream = stream;
		}
	}

	@Override
	public String call() throws Exception {
		if (this.stream != null && this.stream.markSupported()) {
			this.stream.mark(Integer.MAX_VALUE);
			long showLineCount = (this.configuration.getShowLines() >= 0) ? this.configuration
					.getShowLines() : 10;
			SkipInfo fileInfo = this.getSkipLinesLength(stream, showLineCount);
			long skipLinesLength = fileInfo.getSkipLength();
			this.stream.reset();
			this.stream.skip(skipLinesLength);
			int readBytes = 0;
			byte[] c = new byte[1024];
			while ((readBytes = this.stream.read(c)) != -1) {
				if (c == null) {
					continue;
				} else {
					String line = new String(c, 0, readBytes);
					TailEvent<String> event = TailEvent.generateEvent(line,
							line.length(), true);
					this.listener.onTail(event);
				}
			}
		}
		return null;
	}

	@Override
	public InputStream getSource() {
		return this.stream;
	}

	@Override
	public void setSource(InputStream source) {
		if (!(source instanceof BufferedInputStream)) {
			this.stream = new BufferedInputStream(source);
		} else {
			this.stream = source;
		}
	}
}
