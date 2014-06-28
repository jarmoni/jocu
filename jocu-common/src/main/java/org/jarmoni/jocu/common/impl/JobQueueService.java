package org.jarmoni.jocu.common.impl;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.jarmoni.jocu.common.api.IJob;
import org.jarmoni.jocu.common.api.IJobEntity;
import org.jarmoni.jocu.common.api.IJobGroup;
import org.jarmoni.jocu.common.api.IJobPersister;
import org.jarmoni.jocu.common.api.IJobQueueService;
import org.jarmoni.jocu.common.api.IJobReceiver;
import org.jarmoni.jocu.common.api.JobQueueException;
import org.jarmoni.jocu.common.api.JobState;
import org.jarmoni.util.lang.Asserts;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

//CHECKSTYLE:OFF
public class JobQueueService implements IJobQueueService {

	private static final long SLEEP_INTERVAL = 1000L;

	private final Map<String, IJobGroup> jobGroups;

	private final JobQueueServiceAccess queueServiceAccess;

	private final IJobPersister persister;

	private int numReceiverThreads = 1;

	private ExecutorService jobExecutorPool;
	private ExecutorService jobScannerPool;

	public static final String foo = "";

	private final Logger logger = LoggerFactory.getLogger(JobQueueService.class);

	public JobQueueService(final Collection<IJobGroup> jobGroups, final IJobPersister jobPersister) {

		Asserts.notNullSimple(jobGroups, "jobGroups");
		this.jobGroups = jobGroups.stream().collect(Collectors.toMap(IJobGroup::getName, group -> group));
		this.persister = Asserts.notNullSimple(jobPersister, "jobPersister");
		this.queueServiceAccess = new JobQueueServiceAccess(this);
		this.init();
	}

	private void init() {

		this.jobExecutorPool = Executors.newFixedThreadPool(this.numReceiverThreads,
				new ThreadFactoryBuilder().setNameFormat("new-job-executor-%d").build());
		this.jobScannerPool = Executors.newSingleThreadExecutor(new ThreadFactoryBuilder().setNameFormat("job-scanner-%d")
				.build());
	}

	/**
	 * lifecycle-method
	 */
	public void start() {

		this.logger.info("JobQueueService#start()");

		try {
			this.persister.refresh();
		}
		catch (final JobQueueException e) {
			throw new RuntimeException("Could not refresh jobs", e);
		}

		this.jobScannerPool.execute(new JobScanner(this.persister,
				jobEntity -> processJob(jobEntity),
				jobEntity -> processFinishedJob(jobEntity),
				jobEntity -> processExceededJob(jobEntity)));
	}

	/**
	 * lifecycle-method
	 */
	public void stop() {

		this.logger.info("JobQueueService#stop()");
		this.jobScannerPool.shutdownNow();
		this.jobExecutorPool.shutdownNow();
	}

	@Override
	public String push(final Object jobObject, final String group, final Optional<Long> timeout) {

		Asserts.notNullSimple(jobObject, "jobObject", JobQueueException.class);
		Asserts.notNullOrEmptySimple(group, "group", JobQueueException.class);
		Asserts.state(this.jobGroups.containsKey(group), "Group does not exist. Group='" + group + "'", JobQueueException.class);

		return this.persister.insert(jobObject, group, timeout.orElseGet(() -> this.jobGroups.get(group).getTimeout()));
	}

	@Override
	public void cancel(final String jobId) {

		Asserts.notNullOrEmptySimple(jobId, "jobId", JobQueueException.class);
		this.persister.delete(jobId);
	}

	@Override
	public void pause(final String jobId) {

		Asserts.notNullOrEmptySimple(jobId, "jobId", JobQueueException.class);
		this.persister.pause(jobId);
	}

	@Override
	public void resume(final String jobId) {

		Asserts.notNullOrEmptySimple(jobId, "jobId", JobQueueException.class);
		this.persister.resume(jobId);
	}

	@Override
	public void update(final IJob job) {

		Asserts.notNullSimple(job, "job", JobQueueException.class);
		this.persister.update(job.getJobId(), job.getJobObject());
	}

	@Override
	public void setFinished(final IJob job) {

		Asserts.notNullSimple(job, "job", JobQueueException.class);
		this.persister.setFinished(job.getJobId());
	}

	@Override
	public boolean isPaused(final IJob job) {

		Asserts.notNullSimple(job, "job", JobQueueException.class);
		return this.persister.getJobEntity(job.getJobId()).getJobState().equals(JobState.PAUSED) ? true : false;
	}

	@Override
	public void setJobGroups(final Collection<IJobGroup> jobGroups) {

		Asserts.notNullSimple(jobGroups, "jobGroups");
		for (final IJobGroup jobGroup : jobGroups) {
			this.jobGroups.put(jobGroup.getName(), jobGroup);
		}
	}

	private void processJob(final IJobEntity jobEntity) {

		final IJob job = new Job(jobEntity.getId(), jobEntity.getJobGroup(), jobEntity.getJobObject(), this.queueServiceAccess);
		final IJobGroup jobGroup = this.getJobGroupInternal(job.getJobId());
		this.jobExecutorPool.execute(() -> this.executeJob(job, jobGroup.getJobReceiver(), jobGroup.getJobFinishedStrategy()));
	}

	private void processExceededJob(final IJobEntity jobEntity) {

		this.persister.setExceeded(jobEntity.getId());
	}

	private void processFinishedJob(final IJobEntity jobEntity) {

		final IJob job = new Job(jobEntity.getId(), jobEntity.getJobGroup(), jobEntity.getJobObject(), this.queueServiceAccess);
		final IJobGroup jobGroup = this.getJobGroupInternal(job.getJobId());
		this.jobExecutorPool.execute(() -> this.executeFinishedJob(job, jobGroup.getJobReceiver(), jobGroup.isDeleteFinishedJobs()));
	}

	private void executeJob(final IJob job, final IJobReceiver jobReceiver, final Optional<Predicate<IJob>> finishedStrategy) {
		logger.info("Executing receiver='jobReceiver'");
		try {
			try {
				jobReceiver.receive(job);
				if(finishedStrategy.isPresent() && finishedStrategy.get().test(job)) {
					persister.setFinished(job.getJobId());
					return;
				}
				else {
					persister.resume(job.getJobId());
				}
			}
			catch (final Exception ex) {
				throw new JobQueueException(String.format("Receiver threw exception for job='%S'", job), ex);
			}
		}
		catch (final JobQueueException ex) {
			try {
				persister.setError(job.getJobId());
			}
			catch (final Exception jex) {
				logger.error("Could not set state='ERROR' for job='{}'", job, jex);
			}
		}
	}

	private void executeFinishedJob(final IJob job, final IJobReceiver finishedReceiver, final boolean deleteFinishedJob) {
		logger.info("Executing receiver='finishedReceiver'");
		try {
			try {
				finishedReceiver.receive(job);
				if(deleteFinishedJob) {
					persister.delete(job.getJobId());
				}
				else {
					persister.setCompleted(job.getJobId());
				}
			}
			catch (final Exception ex) {
				throw new JobQueueException(String.format("finishedReceiver threw exception for job='%S'", job), ex);
			}
		}
		catch (final JobQueueException ex) {
			try {
				persister.setError(job.getJobId());
			}
			catch (final Exception jex) {
				logger.error("Could not set state='ERROR' for job='{}'", job, jex);
			}
		}
	}

	private IJobGroup getJobGroupInternal(final String jobId) {

		Asserts.notNullOrEmptySimple(jobId, "jobId", JobQueueException.class);

		final IJobEntity jobEntity = this.persister.getJobEntity(jobId);
		Asserts.notNullSimple(jobEntity, "jobEntity", JobQueueException.class);
		return Asserts.notNull(this.jobGroups.get(jobEntity.getJobGroup()),
				"No JobGroup found for id='" + jobEntity.getJobGroup() + "'", JobQueueException.class);
	}

	public void setNumReceiverThreads(final int numReceiverThreads) {
		this.numReceiverThreads = numReceiverThreads;
	}

	Map<String, IJobGroup> getJobGroups() {
		return this.jobGroups;
	}

	JobQueueServiceAccess getQueueServiceAccess() {
		return this.queueServiceAccess;
	}

	public static class JobScanner extends Thread {

		private final IJobPersister jobPersister;
		private final Consumer<IJobEntity> jobConsumer;
		private final Consumer<IJobEntity> finishedConsumer;
		private final Consumer<IJobEntity> timeoutConsumer;

		private final Logger logger = LoggerFactory.getLogger(JobScanner.class);

		public JobScanner(final IJobPersister jobPersister, final Consumer<IJobEntity> jobConsumer,
				final Consumer<IJobEntity> finishedConsumer, final Consumer<IJobEntity> timeoutConsumer) {

			this.jobPersister = Asserts.notNullSimple(jobPersister, "jobPersister");
			this.jobConsumer = Asserts.notNullSimple(jobConsumer, "jobSubmitter");
			this.finishedConsumer = Asserts.notNull(finishedConsumer, "finishedConsumer");
			this.timeoutConsumer = Asserts.notNullSimple(timeoutConsumer, "timeoutConsumer");
		}

		@Override
		public void run() {

			while (true) {
				boolean noJobs = true;
				try {
					final Collection<IJobEntity> newJobs = jobPersister.getWaitingJobsForProcessing();
					for (final IJobEntity jobEntity : newJobs) {
						this.jobConsumer.accept(jobEntity);
						noJobs = false;
					}
				}
				catch (final Exception ex) {
					logger.warn("Getting jobs threw exception", ex);
				}
				try {
					final Collection<IJobEntity> newJobs = jobPersister.getFinishedJobsForCompleting();
					for (final IJobEntity jobEntity : newJobs) {
						this.finishedConsumer.accept(jobEntity);
						noJobs = false;
					}
				}
				catch (final Exception ex) {
					logger.warn("Getting jobs threw exception", ex);
				}
				try {
					final Collection<IJobEntity> exceededJobs = jobPersister.getExceededJobs();
					for (final IJobEntity jobEntity : exceededJobs) {
						this.timeoutConsumer.accept(jobEntity);
						noJobs = false;
					}
				}
				catch (final Exception ex) {
					logger.warn("Getting exceeded jobs threw exception", ex);
				}
				if (noJobs) {
					try {
						Thread.sleep(SLEEP_INTERVAL);
					}
					catch (final InterruptedException iex) {
						logger.info("Received interrupt");
					}
				}
			}
		}
	}
}
//CHECKSTYLE:ON
