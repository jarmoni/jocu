package org.jarmoni.jocu.common.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.jarmoni.jocu.common.api.IJobEntity;
import org.jarmoni.jocu.common.api.IJobPersister;
import org.jarmoni.jocu.common.api.JobState;
import org.junit.Before;
import org.junit.Test;

public abstract class AbstractJobPersisterTst {

	private IJobPersister jobPersister;

	protected abstract IJobPersister getJobPersister();

	@Before
	public void setUp() throws Exception {
		this.jobPersister = this.getJobPersister();
	}

	@Test
	public void testInsertAndGet() throws Exception {

		final Object obj = new Object();
		final String id = this.jobPersister.insert(obj, "foobar", 1000L);
		final IJobEntity entity = this.jobPersister.getJobEntity(id);
		assertEquals(id, entity.getId());
		assertEquals(Long.valueOf(1000L), entity.getTimeout());
		assertEquals(Long.valueOf(1000L), entity.getCurrentTimeout());
		assertEquals("foobar", entity.getJobGroup());
		assertEquals(JobState.WAITING, entity.getJobState());
		assertNotNull(entity.getLastUpdate());
		assertEquals(obj, entity.getJobObject());
	}

	@Test
	public void testDelete() throws Exception {

		final String id = this.jobPersister.insert(new Object(), "foobar", 1000L);
		this.jobPersister.delete(id);
		assertNull(this.jobPersister.getJobEntity(id));
	}

	@Test
	public void testPause() throws Exception {

		final String id = this.jobPersister.insert(new Object(), "foobar", 1000L);
		this.jobPersister.pause(id);
		final IJobEntity entity = this.jobPersister.getJobEntity(id);
		assertEquals(JobState.PAUSED, entity.getJobState());
		assertNull(entity.getCurrentTimeout());
	}

	@Test
	public void testResume() throws Exception {

		final String id = this.jobPersister.insert(new Object(), "foobar", 1000L);
		this.jobPersister.pause(id);
		final IJobEntity entity = this.jobPersister.getJobEntity(id);
		final long oldLastUpdateAsLong = entity.getLastUpdate().getTime();
		Thread.sleep(1L);
		this.jobPersister.resume(id);
		assertEquals(JobState.WAITING, entity.getJobState());
		assertTrue(oldLastUpdateAsLong < entity.getLastUpdate().getTime());
		assertEquals(Long.valueOf(1000L), entity.getCurrentTimeout());
	}

	@Test
	public void testUpdate() throws Exception {

		final Object obj = new Object();
		final String id = this.jobPersister.insert(obj, "foobar", 1000L);
		this.jobPersister.pause(id);
		final IJobEntity entity = this.jobPersister.getJobEntity(id);
		final long oldLastUpdateAsLong = entity.getLastUpdate().getTime();
		Thread.sleep(1L);
		this.jobPersister.update(id, new Object());
		assertEquals(JobState.WAITING, entity.getJobState());
		assertTrue(oldLastUpdateAsLong < entity.getLastUpdate().getTime());
		assertNotSame(obj, entity.getJobObject());
	}

	@Test
	public void testSetFinished() throws Exception {

		final String id = this.jobPersister.insert(new Object(), "foobar", 1000L);
		this.jobPersister.setFinished(id);
		assertEquals(JobState.FINISHED, this.jobPersister.getJobEntity(id).getJobState());
	}

	@Test
	public void testSetError() throws Exception {

		final String id = this.jobPersister.insert(new Object(), "foobar", 1000L);
		this.jobPersister.setError(id);
		assertEquals(JobState.ERROR, this.jobPersister.getJobEntity(id).getJobState());
	}

	@Test
	public void testSetExceeded() throws Exception {

		final String id = this.jobPersister.insert(new Object(), "foobar", 1000L);
		this.jobPersister.setExceeded(id);
		assertEquals(JobState.EXCEEDED, this.jobPersister.getJobEntity(id).getJobState());
	}

	@Test
	public void testGetExceededJobsNonePresent() throws Exception {

		assertTrue(this.jobPersister.getExceededJobs().isEmpty());
	}

	@Test
	public void testGetJobs() throws Exception {

		final String id1 = this.jobPersister.insert(new Object(), "foobar", 1000L);
		final String id2 = this.jobPersister.insert(new Object(), "foobar", 1000L);
		final String id3 = this.jobPersister.insert(new Object(), "foobar", 1000L);

		this.jobPersister.setExceeded(id3);

		final Collection<IJobEntity> waitingJobs = this.jobPersister.getJobs(JobState.WAITING);
		assertEquals(2, waitingJobs.size());
		assertTrue(waitingJobs.stream().map(IJobEntity::getId).collect(Collectors.toList()).contains(id1));
		assertTrue(waitingJobs.stream().map(IJobEntity::getId).collect(Collectors.toList()).contains(id2));

		final Collection<IJobEntity> exceededJobs = this.jobPersister.getJobs(JobState.EXCEEDED);
		assertEquals(1, exceededJobs.size());
		assertEquals(id3, exceededJobs.iterator().next().getId());
	}

	@Test
	public void testGetWaitingJobsForProcessing() throws Exception {

		this.jobPersister.insert(new Object(), "foobar", 1000L);
		final String id2 = this.jobPersister.insert(new Object(), "foobar", 1000L);

		final IJobEntity entity = this.jobPersister.getJobEntity(id2);
		final long oldLastUpdateAsLong = entity.getLastUpdate().getTime();

		Thread.sleep(1L);
		final List<IJobEntity> waitingJobs = new ArrayList<IJobEntity>(this.jobPersister.getWaitingJobsForProcessing());
		assertEquals(2, waitingJobs.size());
		assertEquals(JobState.PROCESSING, waitingJobs.get(0).getJobState());
		assertEquals(JobState.PROCESSING, waitingJobs.get(1).getJobState());

		assertTrue(oldLastUpdateAsLong < waitingJobs.get(0).getLastUpdate().getTime());
		assertTrue(oldLastUpdateAsLong < waitingJobs.get(1).getLastUpdate().getTime());
	}

	@Test
	public void testGetExceededJobs() throws Exception {

		this.jobPersister.insert(new Object(), "foobar", 0L);
		this.jobPersister.getWaitingJobsForProcessing();
		Thread.sleep(1L);
		final Collection<IJobEntity> jobs = this.jobPersister.getExceededJobs();
		assertEquals(1, jobs.size());

	}

	@Test
	public void testRefresh() throws Exception {

		final String id = this.jobPersister.insert(new Object(), "foobar", 0L);
		final IJobEntity entity = this.jobPersister.getJobEntity(id);
		final long oldLastUpdateAsLong = entity.getLastUpdate().getTime();

		this.jobPersister.getWaitingJobsForProcessing();
		assertEquals(JobState.PROCESSING, entity.getJobState());
		Thread.sleep(1L);

		this.jobPersister.refresh();

		assertEquals(JobState.WAITING, entity.getJobState());
		assertTrue(oldLastUpdateAsLong < entity.getLastUpdate().getTime());
	}
}
