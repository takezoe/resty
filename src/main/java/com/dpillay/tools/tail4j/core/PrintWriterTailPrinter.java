package com.dpillay.tools.tail4j.core;

import java.io.PrintStream;

import com.dpillay.tools.tail4j.model.TailContents;

public class PrintWriterTailPrinter<T> implements TailPrinter<T> {
	private PrintStream printer = null;
	private TailListener<T> listener = null;

	public PrintWriterTailPrinter() {
		super();
	}

	public PrintWriterTailPrinter(PrintStream printer, TailListener<T> listener) {
		super();
		this.printer = printer;
		this.listener = listener;
	}

	public PrintStream getPrinter() {
		return printer;
	}

	public void setPrinter(PrintStream printer) {
		this.printer = printer;
	}

	public TailListener<T> getListener() {
		return listener;
	}

	public void setListener(TailListener<T> listener) {
		this.listener = listener;
	}

	@Override
	public void print(TailContents<T> tailContents) {
		if (tailContents.hasNewLine())
			printer.print(tailContents.getContents());
		else
			printer.println(tailContents.getContents());
	}

	@Override
	public T call() throws Exception {
		TailContents<T> tailContents = null;
		while ((tailContents = this.listener.poll()) != null) {
			this.print(tailContents);
		}
		return null;
	}
}
