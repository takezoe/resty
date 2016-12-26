package com.dpillay.tools.tail4j.core;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class TailExecutor {
	public <T> void execute(List<TailedReader<T, ?>> tailedFiles,
			TailPrinter<T> printer) {
		ExecutorService executor = Executors.newFixedThreadPool(tailedFiles
				.size() + 1);
		TaskChecker<T> taskCheck = new TaskChecker<T>(executor);
		for (TailedReader<T, ?> tailedFile : tailedFiles) {
			Future<T> future = executor.submit(tailedFile);
			taskCheck.getFutures().add(future);
		}
		new Thread(taskCheck).start();
		executor.submit(printer);
	}

	private static class TaskChecker<T> implements Runnable {
		private List<Future<T>> futures = new ArrayList<Future<T>>();
		private ExecutorService executorService = null;

		public TaskChecker(ExecutorService executorService) {
			super();
			this.executorService = executorService;
		}

		public List<Future<T>> getFutures() {
			return futures;
		}

		@Override
		public void run() {
			while (!this.isDone()) {
				try {
					Thread.sleep(200);
				} catch (InterruptedException e) {
					// FIXME
				}
			}
			this.executorService.shutdownNow();
		}

		public boolean isDone() {
			for (Future<T> future : this.futures) {
				if (!(future.isCancelled() || future.isDone())) {
					return false;
				}
			}
			return true;
		}
	}
}
