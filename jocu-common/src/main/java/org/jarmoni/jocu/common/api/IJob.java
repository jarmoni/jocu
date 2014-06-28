package org.jarmoni.jocu.common.api;

public interface IJob {

	String getJobId();

	String getGroupId();

	Object getJobObject();

	void update();

	boolean isPaused();

	void setFinished(IJob job);
}
