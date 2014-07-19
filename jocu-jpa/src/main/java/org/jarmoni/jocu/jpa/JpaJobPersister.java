/*
 * Copyright (c) 2013. All rights reserved.
 * Original Author: ms
 * Creation Date: Dec 7, 2013
 */
package org.jarmoni.jocu.jpa;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

import org.jarmoni.jocu.common.api.IJobEntity;
import org.jarmoni.jocu.common.api.IJobPersister;
import org.jarmoni.jocu.common.api.JobQueueException;
import org.jarmoni.jocu.common.api.JobState;
import org.jarmoni.util.datastruct.Tuple;
import org.springframework.transaction.annotation.Transactional;

import com.google.common.collect.Lists;

public class JpaJobPersister implements IJobPersister {

	@PersistenceContext
	private EntityManager em;

	@Override
	@Transactional
	public String insert(final Object jobObject, final String group, final Long timeout) {

		final IJobEntity jobEntity = JpaJobEntity.builder().jobObject(jobObject).currentTimeout(timeout).timeout(timeout)
				.jobGroup(group).jobState(JobState.WAITING).lastUpdate(Calendar.getInstance().getTime()).build();
		try {
			em.persist(jobEntity);
			return jobEntity.getId();
		}
		catch (final Exception ex) {
			throw new JobQueueException(String.format("Persisting of JobEntity failed. JobObject='%s', JobGroup='%s'", jobObject,
					group), ex);
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	@Transactional
	public void delete(final String jobId) {

		try {
			this.executeQuery(JpaJobEntity.QUERY_DELETE_BY_ID, new Tuple<String, Object>("id", jobId));
		}
		catch (final Exception ex) {
			throw new JobQueueException(String.format("Deletion of JobEntity failed. ID='%s'", jobId), ex);
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	@Transactional
	public void pause(final String jobId) {

		try {
			this.executeQuery(JpaJobEntity.QUERY_PAUSE, new Tuple<String, Object>("id", jobId));
		}
		catch (final Exception ex) {
			throw new JobQueueException(String.format("Pausing of JobEntity failed. ID='%s'", jobId), ex);
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	@Transactional
	public void resume(final String jobId) {

		try {
			this.executeQuery(JpaJobEntity.QUERY_RESUME, new Tuple<String, Object>("id", jobId), new Tuple<String, Object>(
					"lastUpdate", Calendar.getInstance().getTime()));
		}
		catch (final Exception ex) {
			throw new JobQueueException(String.format("Resuming of JobEntity failed. ID='%s'", jobId), ex);
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	@Transactional
	public void update(final String jobId, final Object jobObject) {

		try {
			this.executeQuery(JpaJobEntity.QUERY_UPDATE, new Tuple<String, Object>("id", jobId), new Tuple<String, Object>(
					"lastUpdate", Calendar.getInstance().getTime()),
					new Tuple<String, Object>("jobBytes", JpaJobEntity.jobObjectToJobBytes(jobObject)));
		}
		catch (final Exception ex) {
			throw new JobQueueException(String.format("Update of JobEntity failed. ID='%s'", jobId), ex);
		}
	}

	@Override
	@Transactional
	public void setFinished(final String jobId) throws JobQueueException {

		try {
			this.updateJobStateInternal(JobState.FINISHED, jobId);
		}
		catch (final Exception ex) {
			throw new JobQueueException(String.format("Finishing of JobEntity failed. ID='%s'", jobId), ex);
		}
	}

	@Override
	@Transactional
	public void setCompleted(final String jobId) {

		try {
			this.updateJobStateInternal(JobState.COMPLETED, jobId);
		}
		catch (final Exception ex) {
			throw new JobQueueException(String.format("Completing of JobEntity failed. ID='%s'", jobId), ex);
		}

	}

	@Override
	@Transactional
	public void setError(final String jobId) {

		try {
			this.updateJobStateInternal(JobState.ERROR, jobId);
		}
		catch (final Exception ex) {
			throw new JobQueueException(String.format("Finishing of JobEntity failed. ID='%s'", jobId), ex);
		}
	}

	@Override
	@Transactional
	public void exceedJobs() {

		try {
			final List<IJobEntity> jobs = this.getJobsForStateInternal(Lists.newArrayList(JobState.PROCESSING, JobState.COMPLETING));
			jobs.stream()
			.filter(jobEntity -> jobEntity.getLastUpdate().getTime() + jobEntity.getCurrentTimeout() < Calendar.getInstance().getTimeInMillis())
			.forEach(entity -> entity.setJobState(JobState.EXCEEDED));
		} catch (final Exception ex) {
			throw new JobQueueException(String.format("Exceeding of JobEntities failed."), ex);
		}

	}

	@Override
	@Transactional
	public IJobEntity getJobEntity(final String jobId) {

		try {
			return em.find(JpaJobEntity.class, jobId);
		}
		catch (final Exception ex) {
			throw new JobQueueException(String.format("Could not get JobEntity. ID='%s'", jobId), ex);
		}
	}

	@Override
	@Transactional
	public Collection<IJobEntity> getWaitingJobsForProcessing() {

		try {
			return this.updateJobStateInternal(JobState.WAITING, JobState.PROCESSING);
		}
		catch (final Exception ex) {
			throw new JobQueueException("Could not obtain new jobs", ex);
		}
	}

	@Override
	@Transactional
	public Collection<IJobEntity> getFinishedJobsForCompleting() {

		try {
			return this.updateJobStateInternal(JobState.FINISHED, JobState.COMPLETING);
		}
		catch (final Exception ex) {
			throw new JobQueueException("Could not obtain finished jobs", ex);
		}
	}

	@Transactional
	@Override
	public void refresh() {

		try {
			final List<IJobEntity> jobs = this.getJobsForStateInternal(Lists.newArrayList(JobState.PROCESSING, JobState.COMPLETING));
			jobs.forEach(job -> {
				final JobState newState = JobState.PROCESSING.equals(job.getJobState()) ? JobState.WAITING : JobState.FINISHED;
				job.setJobState(newState);
				job.setLastUpdate(Calendar.getInstance().getTime());
				em.merge(job);
			});
		} catch (final Exception ex) {
			throw new JobQueueException("Could not refresh jobs", ex);
		}
	}

	@SuppressWarnings("unchecked")
	private void updateJobStateInternal(final JobState newState, final String jobId) throws Exception {

		this.executeQuery(JpaJobEntity.QUERY_UPDATE_STATE, new Tuple<String, Object>("state", newState),
				new Tuple<String, Object>("id", jobId));
	}

	private void executeQuery(final String queryName, @SuppressWarnings("unchecked") final Tuple<String, Object>... parameters) throws Exception {

		final Query query = em.createNamedQuery(queryName);
		Arrays.asList(parameters).forEach(tuple -> query.setParameter(tuple.getFirst(), tuple.getSecond()));
		query.executeUpdate();
	}

	private List<IJobEntity> updateJobStateInternal(final JobState state, final JobState newState) throws Exception {

		final List<IJobEntity> results = this.getJobsForStateInternal(Lists.newArrayList(state));
		return results.stream().map(job -> {
			job.setJobState(newState);
			job.setLastUpdate(Calendar.getInstance().getTime());
			em.merge(job);
			return job;
		}).collect(Collectors.toList());
	}

	@SuppressWarnings("unchecked")
	private List<IJobEntity> getJobsForStateInternal(final List<JobState> jobStates) {
		final Query query = em.createNamedQuery(JpaJobEntity.QUERY_SELECT_FOR_STATE);
		query.setParameter("jobStates", jobStates);
		return query.getResultList();
	}
}
