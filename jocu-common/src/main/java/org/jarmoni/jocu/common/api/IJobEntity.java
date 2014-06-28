package org.jarmoni.jocu.common.api;

import java.util.Date;

public interface IJobEntity {

	String getId();

	Date getLastUpdate();

	void setLastUpdate(Date lastUpdate);

	Long getTimeout();

	void setTimeout(Long timeout);

	Long getCurrentTimeout();

	void setCurrentTimeout(Long timeout);

	JobState getJobState();

	void setJobState(JobState jobState);

	Object getJobObject();

	void setJobObject(Object jobObject);

	String getJobGroup();

	void setJobGroup(String jobGroup);
}
