package org.jarmoni.jocu.common.impl;

import java.util.Optional;

import org.jarmoni.jocu.common.api.IJobQueue;
import org.jarmoni.jocu.common.api.IJobQueueService;
import org.jarmoni.util.lang.Asserts;

public class JobQueue implements IJobQueue {

	private IJobQueueService queueService;

	public JobQueue(final IJobQueueService queueService) {
		this.queueService = Asserts.notNullSimple(queueService, "queueService");
	}

	@Override
	public String push(final Object jobObject, final String group) {
		return this.queueService.push(jobObject, group, Optional.empty());
	}

	@Override
	public String push(final Object jobObject, final String group, final long timeout) {
		return this.queueService.push(jobObject, group, Optional.of(timeout));
	}

	@Override
	public void pause(final String jobId) {
		this.queueService.pause(jobId);

	}

	@Override
	public void resume(final String jobId) {
		this.queueService.resume(jobId);

	}

	@Override
	public void cancel(final String jobId) {
		this.queueService.cancel(jobId);
	}

	@Override
	public void setQueueService(final IJobQueueService queueService) {
		this.queueService = queueService;
	}

}
