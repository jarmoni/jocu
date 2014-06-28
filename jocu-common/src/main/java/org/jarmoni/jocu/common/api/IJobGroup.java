package org.jarmoni.jocu.common.api;

import java.util.Optional;
import java.util.function.Predicate;

public interface IJobGroup {

	String getName();

	Optional<Predicate<IJob>> getJobFinishedStrategy();

	void setJobFinishedStrategy(Predicate<IJob> jobFinishedStrategy);

	IJobReceiver getJobReceiver();

	IJobReceiver getFinishedReceiver();

	long getTimeout();

	void setTimeout(long timeout);

	boolean isDeleteFinishedJobs();

	void setDeleteFinishedJobs(boolean deleteFinishedJobs);
}
