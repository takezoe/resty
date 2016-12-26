package com.dpillay.tools.tail4j.core;

import java.util.concurrent.Callable;

/**
 * Tails a file
 * 
 * @author dpillay
 * 
 * @param <T>
 *            Contents
 * @param <S>
 *            Source
 */
public interface TailedReader<T, S> extends Callable<T> {

	/**
	 * Get the file being tailed
	 * 
	 * @return
	 */
	public S getSource();

	/**
	 * Set the file to be tailed
	 * 
	 * @param file
	 */
	public void setSource(S source);

	/**
	 * Get the tailed listener
	 * 
	 * @return
	 */
	public TailListener<T> getListener();

	/**
	 * Set the tailed listener
	 * 
	 * @param listener
	 */
	public void setListener(TailListener<T> listener);

}
