package com.dpillay.tools.tail4j.core;

import java.util.concurrent.Callable;

import com.dpillay.tools.tail4j.model.TailContents;

/**
 * Prints the tail
 * 
 * @author dpillay
 * 
 * @param <T>
 */
public interface TailPrinter<T> extends Callable<T> {

	/**
	 * Prints the contents of the tail.
	 * 
	 * @param tailContents
	 */
	public void print(TailContents<T> tailContents);
}
