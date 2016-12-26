package com.dpillay.tools.tail4j.core;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import com.dpillay.tools.tail4j.exception.ApplicationException;
import com.dpillay.tools.tail4j.exception.ErrorCode;
import com.dpillay.tools.tail4j.model.TailContents;
import com.dpillay.tools.tail4j.model.TailEvent;

public class TailListener<T> {
	private BlockingQueue<TailEvent<T>> tailEventQueue = new LinkedBlockingQueue<TailEvent<T>>();

	public void onTail(TailEvent<T> te) {
		this.tailEventQueue.offer(te);
	}

	public TailContents<T> poll() throws ApplicationException {
		try {
			TailEvent<T> te = this.tailEventQueue.take();
			if (te != null)
				return te.getTailContents();
		} catch (InterruptedException e) {
			throw new ApplicationException(e, ErrorCode.IO_ERROR,
					"Could not take tail event from the queue");
		}
		throw new ApplicationException(ErrorCode.POLL_ERROR,
				"Could not take tail event from the queue");
	}
}
