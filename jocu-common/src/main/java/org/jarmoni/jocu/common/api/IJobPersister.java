package org.jarmoni.jocu.common.api;

import java.util.Collection;

public interface IJobPersister {

	/**
	 * Inserts job into store. Follwing properties must be set in implementation:
	 * <ul>
	 * <li>jobObject = <code>jobObject</code></li>
	 * <li>group = <code>group</code></li>
	 * <li>lastUpdate = current time</li>
	 * <li>jobState = <code>WAITING</code></li>
	 * <li>timeout = <code>timeout</code></li>
	 * <li>currentTimeout = <code>timeout</code></li>
	 * </ul>
	 *
	 * @param jobObject
	 * @param group
	 * @param timeout
	 * @return jobId
	 */
	String insert(Object jobObject, String group, Long timeout);

	/**
	 * Deletes job from store.
	 *
	 * @param jobId
	 */
	void delete(String jobId);

	/**
	 * Pauses a job. Following properties must be set in implementation:
	 * <ul>
	 * <li>jobState = <code>PAUSED</code></li>
	 * <li>currentTimeout = null</li>
	 * </ul>
	 *
	 * @param jobId
	 */
	void pause(String jobId);

	/**
	 * Resumes a (paused) job. Following properties must be set in implementation:
	 * <ul>
	 * <li>jobState = <code>WAITING</code></li>
	 * <li>currentTimeout = timeout of job</li>
	 * <li>lastUpdate = current time</code>
	 * </ul>
	 *
	 * @param jobId
	 */
	void resume(String jobId);

	/**
	 * Updates the job-object. Following properties must be set in implementation:
	 * <ul>
	 * <li><code>jobObject = jobObject</code></li>
	 * <li><code>jobState = WAITING</code>
	 * <li>lastUpdate = current time</code>
	 * </ul>
	 *
	 * @param jobId
	 * @param jobObject
	 */
	void update(String jobId, Object jobObject);

	/**
	 * Sets state to <code>FINISHED</code>
	 *
	 * @param jobId
	 */
	void setFinished(String jobId);

	/**
	 * Sets state to <code>COMPLETED</code>
	 *
	 * @param jobId
	 */
	void setCompleted(String jobId);

	/**
	 * Sets state to <code>ERROR</code>
	 *
	 * @param jobId
	 */
	void setError(String jobId);

	/**
	 * Sets state to <code>EXCEEDED</code>
	 *
	 * @param jobId
	 */
	void setExceeded(String jobId);

	/**
	 * Returns job with given <code>jobId</code>
	 *
	 * @param jobId
	 * @return {@link IJobEntity}
	 */
	IJobEntity getJobEntity(String jobId);

	/**
	 * Returns all jobs for given <code>jobState</code>. Method is 'read-only'. Means that implementation must not
	 * manipulate the states of the returned jobs
	 * @param jobState
	 * @return
	 */
	Collection<IJobEntity> getJobs(JobState jobState);

	/**
	 * Returns all Jobs with jobState=<code>WAITING</code>.<br>
	 * The following properties must be set for each job before method returns:<br>
	 * <ul>
	 * <li><code>jobState = PROCESSING</code></li>
	 * <li><code>lastUpdate = current time</code></li>
	 * </ul>
	 *
	 * @return {@link Collection}
	 */
	Collection<IJobEntity> getWaitingJobsForProcessing();

	/**
	 * Returns all Jobs with jobState=<code>FINISHED</code>.<br>
	 * The following properties must be set for each job before method returns:<br>
	 * <ul>
	 * <li><code>jobState = COMPLETING</code></li>
	 * <li><code>lastUpdate = current time</code></li>
	 * </ul>
	 * @return {@link Collection}
	 */
	Collection<IJobEntity> getFinishedJobsForCompleting();

	/**
	 * Returns all Jobs with:<br>
	 * <ul>
	 * <li><code>jobState = PROCESSING || jobState = COMPLETING</code> <b>and</b></li>
	 * <li><code>current time > lastUpdate + currentTimeout</code></li>
	 * </ul>
	 *
	 * @return {@link Collection}
	 */
	Collection<IJobEntity> getExceededJobs();

	/**
	 * Should be invoked before the scanner-threads start. Sets the following
	 * properties:
	 * <ul>
	 * <li>lastUpdate = current time</li>
	 * <li>if <code>jobState = PROCESSING</code> ->
	 * <code>jobState = WAITING</code></li>
	 * </ul>
	 *
	 */
	void refresh();
}
