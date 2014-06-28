package org.jarmoni.jocu.common.api;

import java.util.Collection;
import java.util.Optional;

public interface IJobQueueService {

	String push(Object jobObject, String group, Optional<Long> timeout);

	void cancel(String jobId);

	void pause(String jobId);

	void resume(final String jobId);

	void update(IJob job);

	boolean isPaused(IJob job);

	void setFinished(IJob job);

	void setJobGroups(Collection<IJobGroup> jobGroups);
}
