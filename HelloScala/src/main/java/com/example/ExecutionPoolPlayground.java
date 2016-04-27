package com.example;

import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveAction;
import java.util.concurrent.atomic.AtomicInteger;

public class ExecutionPoolPlayground {

	public static AtomicInteger counter = new AtomicInteger(0);
	public static final long startTime = System.currentTimeMillis();

	public static void main(String[] argv) throws InterruptedException {
		System.out.println("Thread pool test");

		System.out.println("All task added, starting...");
		// Set Size with JVM arg: -Djava.util.concurrent.ForkJoinPool.common.parallelism=2
//		ForkJoinPool forkJoinPool = ForkJoinPool.commonPool();
		ForkJoinPool forkJoinPool = new ForkJoinPool(2);

//		List<RecursiveAction > actions = new ArrayList<>();
		for (int i = 0; i < 5; i++) {
//			actions.add(new MyTask());
//			forkJoinPool.submit(new MyTask());
			forkJoinPool.execute(new MyTask());
		}

//		forkJoinPool.invokeAll(actions);


		Thread.sleep(6000);
	}

	public static class MyTask extends RecursiveAction {
		protected void compute() {
			try {
//				this.fork();
				(new Thread(new MyBgTask())).run(); // run is blocking, was want to test blocking.
//				this.join();
			} catch (Exception e) {
				System.out.println("Calling callable interrupted");
				e.printStackTrace();
			}
		}
	}

	public static class MyBgTask implements Runnable {

		@Override
		public void run() {
			try {
				Thread.sleep(1000);
				int finalCounter = counter.incrementAndGet();
				if (finalCounter == 5) {
					System.out
						.println("Completed " + finalCounter + ". Time taken: " + (System.currentTimeMillis() - startTime));
				} else {
					System.out.println("Still running... " + finalCounter);
				}
			} catch (InterruptedException e) {
				System.out.println("Sleep interrupted");
				e.printStackTrace();
			}
		}
	}
}
