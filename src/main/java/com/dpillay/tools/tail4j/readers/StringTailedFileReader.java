package com.dpillay.tools.tail4j.readers;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;

import com.dpillay.tools.tail4j.configuration.TailConfiguration;
import com.dpillay.tools.tail4j.core.TailListener;
import com.dpillay.tools.tail4j.core.TailedReader;
import com.dpillay.tools.tail4j.exception.ApplicationException;
import com.dpillay.tools.tail4j.exception.ErrorCode;
import com.dpillay.tools.tail4j.model.SkipInfo;
import com.dpillay.tools.tail4j.model.TailEvent;

/**
 * Implements a tailed file reader for string based contents
 * 
 * @author dpillay
 */
public class StringTailedFileReader extends AbstractReader implements
		TailedReader<String, File> {
	@SuppressWarnings("unused")
	private static char newLine = System.getProperty("line.separator")
			.charAt(0);

	private File file = null;

	public StringTailedFileReader(TailConfiguration tc, File file,
			TailListener<String> listener) {
		super();
		this.file = file;
		this.listener = listener;
		this.configuration = tc;
	}

	public StringTailedFileReader() {
	}

	@Override
	public File getSource() {
		return file;
	}

	@Override
	public void setSource(File file) {
		this.file = file;
	}

	@Override
	public String call() throws Exception {
		try {
			BufferedReader br = new BufferedReader(new FileReader(file));
			long showLineCount = (this.configuration.getShowLines() >= 0) ? this.configuration
					.getShowLines() : 10;
			SkipInfo fileInfo = this.getSkipLinesLength(new FileInputStream(
					file), showLineCount);
			long skipLinesLength = fileInfo.getSkipLength();
			br.skip(skipLinesLength);
			while (this.configuration.isForce() || showLineCount-- > 0) {
				String line = br.readLine();
				if (line == null) {
					if (!this.configuration.isForce())
						break;
					Thread.sleep(200);
					continue;
				}
				TailEvent<String> event = TailEvent.generateEvent(line,
						line.length(), false);
				this.listener.onTail(event);
			}
		} catch (Throwable t) {
			throw new ApplicationException(t, ErrorCode.DEFAULT_ERROR,
					"Could not finish tailing file");
		}
		return null;
	}
}
