package org.jarmoni.jocu.common.impl;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.jarmoni.jocu.common.api.IJobEntity;
import org.jarmoni.jocu.common.api.IJobPersister;
import org.jarmoni.jocu.common.api.JobState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Simple implementation of {@link IJobPersister} which holds all
 * {@link IJobEntity} in a {@link Map}.<br>
 * This impl is not suited for productional use but may offer some hints when
 * creating a more sophisticated implementation.
 */
public class SimpleJobPersister implements IJobPersister {

	private final Logger logger = LoggerFactory.getLogger(SimpleJobPersister.class);

	private final Map<String, IJobEntity> jobs = new ConcurrentHashMap<>();

	@Override
	public String insert(final Object jobObject, final String group, final Long timeout) {

		final String id = UUID.randomUUID().toString();
		this.jobs.put(id,
				JobEntity.builder().id(id).jobObject(jobObject).jobGroup(group).lastUpdate(Calendar.getInstance().getTime())
				.timeout(timeout).currentTimeout(timeout).jobState(JobState.WAITING).build());
		return id;
	}

	@Override
	public void delete(final String jobId) {

		this.jobs.remove(jobId);
	}

	@Override
	public void pause(final String jobId) {

		final IJobEntity jobEntity = this.getJobEntityInternal(jobId);
		jobEntity.setCurrentTimeout(null);
		jobEntity.setJobState(JobState.PAUSED);
	}

	@Override
	public void resume(final String jobId) {

		final IJobEntity jobEntity = this.getJobEntityInternal(jobId);
		jobEntity.setLastUpdate(Calendar.getInstance().getTime());
		jobEntity.setCurrentTimeout(jobEntity.getTimeout());
		jobEntity.setJobState(JobState.WAITING);
	}

	@Override
	public void update(final String jobId, final Object jobObject) {

		final IJobEntity jobEntity = this.getJobEntityInternal(jobId);
		jobEntity.setLastUpdate(Calendar.getInstance().getTime());
		jobEntity.setJobState(JobState.WAITING);
		jobEntity.setJobObject(jobObject);
	}

	@Override
	public void setFinished(final String jobId) {

		this.setJobStateInternal(jobId, JobState.FINISHED);
	}

	@Override
	public void setCompleted(final String jobId) {

		this.setJobStateInternal(jobId, JobState.COMPLETED);
	}

	@Override
	public void setError(final String jobId) {

		this.setJobStateInternal(jobId, JobState.ERROR);
	}

	@Override
	public void setExceeded(final String jobId) {

		this.setJobStateInternal(jobId, JobState.EXCEEDED);
	}

	@Override
	public IJobEntity getJobEntity(final String jobId) {

		return this.getJobEntityInternal(jobId);
	}

	@Override
	public Collection<IJobEntity> getWaitingJobsForProcessing() {

		return this.getJobsForStateInternal(JobState.WAITING, JobState.PROCESSING);
	}

	@Override
	public Collection<IJobEntity> getFinishedJobsForCompleting() {

		return this.getJobsForStateInternal(JobState.FINISHED, JobState.COMPLETING);
	}

	@Override
	public Collection<IJobEntity> getExceededJobs() {

		return this.getJobs(JobState.PROCESSING, JobState.COMPLETING).stream()
				.filter(jobEntity -> jobEntity.getLastUpdate().getTime() + jobEntity.getCurrentTimeout() < Calendar.getInstance().getTimeInMillis())
				.collect(Collectors.toList());
	}


	@Override
	public void refresh() {

		this.getJobs(JobState.PROCESSING, JobState.COMPLETING).forEach(job -> {
			final JobState newState = JobState.PROCESSING.equals(job.getJobState()) ? JobState.WAITING : JobState.FINISHED;
			job.setJobState(newState);
			job.setLastUpdate(Calendar.getInstance().getTime());
		});
	}

	private Collection<IJobEntity> getJobs(final JobState... jobState) {

		return this.jobs.entrySet().stream()
				.map(e -> e.getValue())
				.filter(job -> Arrays.asList(jobState).contains(job.getJobState()))
				.peek(job -> logger.info(job.toString()))
				.collect(Collectors.toList());
	}

	private Collection<IJobEntity> getJobsForStateInternal(final JobState oldState, final JobState newState) {

		return this.getJobs(oldState).stream().map(job -> {
			job.setJobState(newState);
			job.setLastUpdate(Calendar.getInstance().getTime());
			return job;
		}).collect(Collectors.toList());
	}

	private void setJobStateInternal(final String jobId, final JobState state) {

		final IJobEntity entity = this.getJobEntityInternal(jobId);
		if(entity != null) {
			entity.setJobState(state);
		}
	}

	private IJobEntity getJobEntityInternal(final String jobId) {

		return this.jobs.get(jobId);
	}

}
