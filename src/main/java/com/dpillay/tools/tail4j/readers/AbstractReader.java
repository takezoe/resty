package com.dpillay.tools.tail4j.readers;

import java.io.BufferedInputStream;
import java.io.InputStream;

import com.dpillay.tools.tail4j.configuration.TailConfiguration;
import com.dpillay.tools.tail4j.core.TailListener;
import com.dpillay.tools.tail4j.model.SkipInfo;

public class AbstractReader {
	protected TailListener<String> listener = null;
	protected TailConfiguration configuration = null;

	public TailListener<String> getListener() {
		return listener;
	}

	public void setListener(TailListener<String> listener) {
		this.listener = listener;
	}

	public TailConfiguration getConfiguration() {
		return configuration;
	}

	public void setConfiguration(TailConfiguration configuration) {
		this.configuration = configuration;
	}

	protected SkipInfo getSkipLinesLength(InputStream stream, long showLineCount) {
		InputStream is = (stream instanceof BufferedInputStream) ? stream
				: new BufferedInputStream(stream);
		long count = 0;
		try {
			long[] lineChars = new long[(int) showLineCount + 1];
			byte[] c = new byte[1024];
			int index = 0;
			int readChars = 0;
			long totalCharsRead = 0;
			while ((readChars = is.read(c)) != -1) {
				for (int i = 0; i < readChars; ++i) {
					if (c[i] >= 0x0a && c[i] <= 0x0d) {
						++count;
						if (index == lineChars.length)
							index = 0;
						i += ((i + 1 < readChars) && (c[i] == 0x0d) && (c[i + 1] == 0x0a)) ? 1
								: 0;
						lineChars[index++] = totalCharsRead + i + 1;
					}
				}
				totalCharsRead += readChars;
			}
			if (count >= showLineCount) {
				return new SkipInfo(count,
						(index == lineChars.length) ? lineChars[0]
								: lineChars[index]);
			}
		} catch (Exception e) {
		}
		return new SkipInfo(count, 0);
	}
}
