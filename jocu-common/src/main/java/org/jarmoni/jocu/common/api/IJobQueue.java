package org.jarmoni.jocu.common.api;

public interface IJobQueue {

	String push(Object jobObject, String group);

	String push(Object jobObject, String group, long timeout);

	void pause(String jobId);

	void resume(String jobId);

	void cancel(String jobId);

	void setQueueService(IJobQueueService queueService);
}
