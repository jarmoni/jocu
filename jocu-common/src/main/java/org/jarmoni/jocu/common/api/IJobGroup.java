package org.jarmoni.jocu.common.api;


public interface IJobGroup {

	String getName();

	IJobFinishedStrategy getJobFinishedStrategy();

	void setJobFinishedStrategy(IJobFinishedStrategy jobFinishedStrategy);

	IJobReceiver getJobReceiver();

	IJobReceiver getFinishedReceiver();

	long getTimeout();

	void setTimeout(long timeout);

	boolean isDeleteFinishedJobs();

	void setDeleteFinishedJobs(boolean deleteFinishedJobs);
}
