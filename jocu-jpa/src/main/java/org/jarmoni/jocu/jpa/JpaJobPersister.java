/*
 * Copyright (c) 2013. All rights reserved.
 * Original Author: ms
 * Creation Date: Dec 7, 2013
 */
package org.jarmoni.jocu.jpa;

import java.util.Calendar;
import java.util.Collection;
import java.util.List;

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
	public String insert(final Object jobObject, final String group, final Long timeout) throws JobQueueException {

		final IJobEntity jobEntity = JpaJobEntity.builder().jobObject(jobObject).currentTimeout(timeout).timeout(timeout).jobGroup(group)
				.jobState(JobState.NEW).lastUpdate(Calendar.getInstance().getTime()).build();
		try {
			em.persist(jobEntity);
			return jobEntity.getId();
		} catch (final Exception ex) {
			throw new JobQueueException(String.format("Persisting of JobEntity failed. JobObject='%s', JobGroup='%s'", jobObject, group), ex);
		} 
	}

	@SuppressWarnings("unchecked")
	@Override
	@Transactional
	public void delete(final String jobId) throws JobQueueException {

		try {
			this.executeQuery(JpaJobEntity.QUERY_DELETE_BY_ID, new Tuple<String, Object>("id", jobId));
		} catch (final Exception ex) {
			throw new JobQueueException(String.format("Deletion of JobEntity failed. ID='%s'", jobId), ex);
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	@Transactional
	public void pause(final String jobId) throws JobQueueException {

		try {
			this.executeQuery(JpaJobEntity.QUERY_PAUSE, new Tuple<String, Object>("id", jobId));
		} catch (final Exception ex) {
			throw new JobQueueException(String.format("Pausing of JobEntity failed. ID='%s'", jobId), ex);
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	@Transactional
	public void resume(final String jobId) throws JobQueueException {

		try {
			this.executeQuery(JpaJobEntity.QUERY_RESUME, new Tuple<String, Object>("id", jobId), new Tuple<String, Object>("lastUpdate", Calendar
					.getInstance().getTime()));
		} catch (final Exception ex) {
			throw new JobQueueException(String.format("Resuming of JobEntity failed. ID='%s'", jobId), ex);
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	@Transactional
	public void update(final String jobId, final Object jobObject) throws JobQueueException {

		try {
			this.executeQuery(JpaJobEntity.QUERY_UPDATE, new Tuple<String, Object>("id", jobId), new Tuple<String, Object>("lastUpdate", Calendar
					.getInstance().getTime()), new Tuple<String, Object>("jobBytes", JpaJobEntity.jobObjectToJobBytes(jobObject)));
		} catch (final Exception ex) {
			throw new JobQueueException(String.format("Update of JobEntity failed. ID='%s'", jobId), ex);
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	@Transactional
	public void setFinished(final String jobId) throws JobQueueException {

		try {
			this.executeQuery(JpaJobEntity.QUERY_FINISH, new Tuple<String, Object>("id", jobId));
		} catch (final Exception ex) {
			throw new JobQueueException(String.format("Finishing of JobEntity failed. ID='%s'", jobId), ex);
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	@Transactional
	public void setError(final String jobId) throws JobQueueException {

		try {
			this.executeQuery(JpaJobEntity.QUERY_ERROR, new Tuple<String, Object>("id", jobId));
		} catch (final Exception ex) {
			throw new JobQueueException(String.format("Finishing of JobEntity failed. ID='%s'", jobId), ex);
		}
	}

	@Override
	@Transactional
	public IJobEntity getJobEntity(final String jobId) throws JobQueueException {

		try {
			return em.find(JpaJobEntity.class, jobId);
		} catch (final Exception ex) {
			throw new JobQueueException(String.format("Could not get JobEntity. ID='%s'", jobId), ex);
		} 
	}

	@Override
	@Transactional
	public Collection<IJobEntity> getNewJobs() throws JobQueueException {

		try {
			return this.getEntitesForState(JobState.NEW, JobState.NEW_IN_PROGRESS);
		} catch (final Exception ex) {
			throw new JobQueueException("Could not obtain new jobs", ex);
		}
	}

	@Override
	@Transactional
	public Collection<IJobEntity> getFinishedJobs() throws JobQueueException {

		try {
			return this.getEntitesForState(JobState.FINISHED, JobState.FINISHED_IN_PROGRESS);
		} catch (final Exception ex) {
			throw new JobQueueException("Could not obtain finished jobs", ex);
		}
	}

	@SuppressWarnings("unchecked")
	@Transactional
	@Override
	public Collection<IJobEntity> getTimeoutJobs() throws JobQueueException {

		final List<IJobEntity> currentJobs = Lists.newArrayList();
		try {
			final Query query = em.createNamedQuery(JpaJobEntity.QUERY_SELECT_ALL);
			final List<IJobEntity> jobs = query.getResultList();
			for (final IJobEntity jobEntity : jobs) {
				if (JobState.PAUSED.equals(jobEntity.getJobState())) {
					continue;
				}
				if (jobEntity.getCurrentTimeout() == null) {
					continue;
				}
				if (JobState.EXCEEDED_IN_PROGRESS.equals(jobEntity.getJobState())) {
					continue;
				}
				if (jobEntity.getLastUpdate().getTime() + jobEntity.getCurrentTimeout() < System.currentTimeMillis()) {
					jobEntity.setJobState(JobState.EXCEEDED_IN_PROGRESS);
					em.merge(jobEntity);
					currentJobs.add(jobEntity);
				}
			}
			return currentJobs;
		} catch (final Exception ex) {
			throw new JobQueueException("Could not obtain exceeded jobs", ex);
		}
	}

	@SuppressWarnings("unchecked")
	@Transactional
	@Override
	public void refresh() throws JobQueueException {

		try {
			final Query query = em.createNamedQuery(JpaJobEntity.QUERY_SELECT_ALL);
			final List<IJobEntity> jobs = query.getResultList();
			for (final IJobEntity jobEntity : jobs) {

				if (JobState.NEW_IN_PROGRESS.equals(jobEntity.getJobState()) || JobState.EXCEEDED_IN_PROGRESS.equals(jobEntity.getJobState())
						|| JobState.FINISHED_IN_PROGRESS.equals(jobEntity.getJobState())) {

					if (JobState.NEW_IN_PROGRESS.equals(jobEntity.getJobState())) {
						jobEntity.setJobState(JobState.NEW);
					} else if (JobState.EXCEEDED_IN_PROGRESS.equals(jobEntity.getJobState())) {
						jobEntity.setJobState(JobState.EXCEEDED);
					} else if (JobState.FINISHED_IN_PROGRESS.equals(jobEntity.getJobState())) {
						jobEntity.setJobState(JobState.FINISHED);
					}
					jobEntity.setLastUpdate(Calendar.getInstance().getTime());
					em.merge(jobEntity);

				}
			}
		} catch (final Exception ex) {
			throw new JobQueueException("Could not refresh jobs", ex);
		} 
	}

	private void executeQuery(final String queryName, @SuppressWarnings("unchecked") final Tuple<String, Object>... parameters) throws Exception {
		
		final Query query = em.createNamedQuery(queryName);
		if (parameters != null) {
			for (final Tuple<String, Object> tuple : parameters) {
				query.setParameter(tuple.getFirst(), tuple.getSecond());
			}
		}
		query.executeUpdate();
	}

	@SuppressWarnings("unchecked")
	private List<IJobEntity> getEntitesForState(final JobState state, final JobState newState) throws Exception {
		
		final Query query = em.createNamedQuery(JpaJobEntity.QUERY_SELECT_FOR_STATE);
		query.setParameter("jobState", state);
		final List<IJobEntity> results = query.getResultList();
		for (final IJobEntity jobEntity : results) {
			jobEntity.setJobState(newState);
			em.merge(jobEntity);
		}
		return results;
	}
}
