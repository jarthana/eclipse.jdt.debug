/*******************************************************************************
 * Copyright (c) 2016 Google, Inc and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Stefan Xenos (Google) - Initial implementation
 *******************************************************************************/
package org.eclipse.jdt.debug.tests;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.debug.testplugin.JavaTestPlugin;
import org.eclipse.swt.widgets.Display;
import org.junit.Assert;

public class TestUtil {

	/**
	 * Call this in the tearDown method of every test to clean up state that can
	 * otherwise leak through SWT between tests.
	 */
	public static void cleanUp(String owner) {
		// Ensure that the Thread.interrupted() flag didn't leak.
		Assert.assertFalse("The main thread should not be interrupted at the end of a test", Thread.interrupted());

		// Wait for any outstanding jobs to finish. Protect against deadlock by
		// terminating the wait after a timeout.
		boolean timedOut = waitForJobs(owner, 5, 5000);
		if (timedOut) {
			// We don't expect any extra jobs run during the test: try to cancel them
			log(IStatus.INFO, owner, "Trying to cancel running jobs: " + getRunningOrWaitingJobs(null));
			getRunningOrWaitingJobs(null).forEach(job -> job.cancel());
			waitForJobs(owner, 5, 1000);
		}

		// Ensure that the Thread.interrupted() flag didn't leak.
		Assert.assertFalse("The main thread should not be interrupted at the end of a test", Thread.interrupted());
	}

	public static void log(int severity, String owner, String message, Throwable... optionalError) {
		message = "[" + owner + "] " + message;
		Throwable error = null;
		if (optionalError != null && optionalError.length > 0) {
			error = optionalError[0];
		}
		Status status = new Status(severity, JavaTestPlugin.getDefault().getBundle().getSymbolicName(), message, error);
		JavaTestPlugin.getDefault().getLog().log(status);
	}

	/**
	 * Process all queued UI events. If called from background thread, does
	 * nothing.
	 */
	public static void runEventLoop() {
		Display display = Display.getCurrent();
		if (display != null && !display.isDisposed()) {
			while (display.readAndDispatch()) {
				// Keep pumping events until the queue is empty
			}
		}
	}

	/**
	 * Utility for waiting until the execution of jobs of any family has finished or timeout is reached. If no jobs are running, the method waits
	 * given minimum wait time. While this method is waiting for jobs, UI events are processed.
	 *
	 * @param owner
	 *            name of the caller which will be logged as prefix if the wait times out
	 * @param minTimeMs
	 *            minimum wait time in milliseconds
	 * @param maxTimeMs
	 *            maximum wait time in milliseconds
	 * @return true if the method timed out, false if all the jobs terminated before the timeout
	 */
	public static boolean waitForJobs(String owner, long minTimeMs, long maxTimeMs) {
		if (maxTimeMs < minTimeMs) {
			throw new IllegalArgumentException("Max time is smaller as min time!");
		}
		final long start = System.currentTimeMillis();
		while (System.currentTimeMillis() - start < minTimeMs) {
			runEventLoop();
			try {
				Thread.sleep(Math.min(10, minTimeMs));
			} catch (InterruptedException e) {
				// Uninterruptable
			}
		}
		while (!Job.getJobManager().isIdle()) {
			List<Job> jobs = getRunningOrWaitingJobs((Object) null);

			if (!Collections.disjoint(runningJobs, jobs)) {
				// There is a job which runs already quite some time, don't wait for it to avoid test timeouts
				dumpRunningOrWaitingJobs(owner, jobs);
				return true;
			}

			if (System.currentTimeMillis() - start >= maxTimeMs) {
				dumpRunningOrWaitingJobs(owner, jobs);
				return true;
			}
			runEventLoop();
			try {
				Thread.sleep(10);
			} catch (InterruptedException e) {
				// Uninterruptable
			}
		}
		runningJobs.clear();
		return false;
	}

	static Set<Job> runningJobs = new LinkedHashSet<>();

	private static void dumpRunningOrWaitingJobs(String owner, List<Job> jobs) {
		String message = "Some job is still running or waiting to run: " + dumpRunningOrWaitingJobs(jobs);
		log(IStatus.ERROR, owner, message);
	}

	private static String dumpRunningOrWaitingJobs(List<Job> jobs) {
		if (jobs.isEmpty()) {
			return "";
		}
		// clear "old" running jobs, we only remember most recent
		runningJobs.clear();
		StringBuilder sb = new StringBuilder();
		for (Job job : jobs) {
			runningJobs.add(job);
			sb.append("'").append(job.getName()).append("'/");
			sb.append(job.getClass().getName());
			sb.append(", ");
		}
		sb.setLength(sb.length() - 2);
		return sb.toString();
	}

	private static List<Job> getRunningOrWaitingJobs(Object jobFamily) {
		List<Job> running = new ArrayList<>();
		Job[] jobs = Job.getJobManager().find(jobFamily);
		for (Job job : jobs) {
			if (isRunningOrWaitingJob(job)) {
				running.add(job);
			}
		}
		return running;
	}

	private static boolean isRunningOrWaitingJob(Job job) {
		int state = job.getState();
		return (state == Job.RUNNING || state == Job.WAITING);
	}

}