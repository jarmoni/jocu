package org.jarmoni.jocu.common.impl;

import org.jarmoni.jocu.common.api.IJob;
import org.jarmoni.jocu.common.api.IJobQueueService;
import org.jarmoni.util.lang.Asserts;

public class JobQueueServiceAccess {

	private final IJobQueueService queueService;

	public JobQueueServiceAccess(final IJobQueueService queueService) {
		this.queueService = Asserts.notNullSimple(queueService, "queueService");
	}

	void update(final IJob job) {
		this.queueService.update(job);
	}

	boolean isPaused(final IJob job) {
		return this.queueService.isPaused(job);
	}

	void setFinished(final IJob job) {
		this.queueService.setFinished(job);
	}
}
