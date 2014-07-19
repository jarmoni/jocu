package org.jarmoni.jocu.common.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.easymock.EasyMock;
import org.jarmoni.jocu.common.api.IJobEntity;
import org.jarmoni.jocu.common.api.IJobPersister;
import org.jarmoni.jocu.common.api.JobState;
import org.junit.Before;
import org.junit.Test;

import com.google.common.base.Objects;

public abstract class AbstractJobPersisterTst {

	private IJobPersister jobPersister;

	private final Serializable testObj = new TestObject(UUID.randomUUID().toString());

	protected abstract IJobPersister getJobPersister();

	@Before
	public void setUp() throws Exception {
		this.jobPersister = this.getJobPersister();
	}

	@Test
	public void testInsertAndGet() throws Exception {

		final String id = this.jobPersister.insert(this.testObj, "foobar", 1000L);
		final IJobEntity entity = this.jobPersister.getJobEntity(id);
		assertEquals(id, entity.getId());
		assertEquals(Long.valueOf(1000L), entity.getTimeout());
		assertEquals(Long.valueOf(1000L), entity.getCurrentTimeout());
		assertEquals("foobar", entity.getJobGroup());
		assertEquals(JobState.WAITING, entity.getJobState());
		assertNotNull(entity.getLastUpdate());
		assertEquals(this.testObj, entity.getJobObject());
	}

	@Test
	public void testDelete() throws Exception {

		final String id = this.jobPersister.insert(this.testObj, "foobar", 1000L);
		this.jobPersister.delete(id);
		assertNull(this.jobPersister.getJobEntity(id));
	}

	@Test
	public void testPause() throws Exception {

		final String id = this.jobPersister.insert(this.testObj, "foobar", 1000L);
		this.jobPersister.pause(id);
		final IJobEntity entity = this.jobPersister.getJobEntity(id);
		assertEquals(JobState.PAUSED, entity.getJobState());
		assertNull(entity.getCurrentTimeout());
	}

	@Test
	public void testResume() throws Exception {

		final String id = this.jobPersister.insert(this.testObj, "foobar", 1000L);
		this.jobPersister.pause(id);
		IJobEntity entity = this.jobPersister.getJobEntity(id);
		final long oldLastUpdateAsLong = entity.getLastUpdate().getTime();
		Thread.sleep(1L);
		this.jobPersister.resume(id);
		entity = this.jobPersister.getJobEntity(id);
		assertEquals(JobState.WAITING, entity.getJobState());
		assertTrue(oldLastUpdateAsLong < entity.getLastUpdate().getTime());
		assertEquals(Long.valueOf(1000L), entity.getCurrentTimeout());
	}

	@Test
	public void testUpdate() throws Exception {

		final Serializable anotherObj = EasyMock.createMock(Serializable.class);
		final String id = this.jobPersister.insert(this.testObj, "foobar", 1000L);
		this.jobPersister.pause(id);
		IJobEntity entity = this.jobPersister.getJobEntity(id);
		final long oldLastUpdateAsLong = entity.getLastUpdate().getTime();
		Thread.sleep(1L);
		this.jobPersister.update(id, anotherObj);
		entity = this.jobPersister.getJobEntity(id);
		assertEquals(JobState.WAITING, entity.getJobState());
		assertTrue(oldLastUpdateAsLong < entity.getLastUpdate().getTime());
		assertNotSame(this.testObj, entity.getJobObject());
	}

	@Test
	public void testSetFinished() throws Exception {

		final String id = this.jobPersister.insert(this.testObj, "foobar", 1000L);
		this.jobPersister.setFinished(id);
		assertEquals(JobState.FINISHED, this.jobPersister.getJobEntity(id).getJobState());
	}

	@Test
	public void testSetError() throws Exception {

		final String id = this.jobPersister.insert(this.testObj, "foobar", 1000L);
		this.jobPersister.setError(id);
		assertEquals(JobState.ERROR, this.jobPersister.getJobEntity(id).getJobState());
	}

	@Test
	public void testGetWaitingJobsForProcessing() throws Exception {

		this.jobPersister.insert(this.testObj, "foobar", 1000L);
		final String id2 = this.jobPersister.insert(this.testObj, "foobar", 1000L);

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
	public void testGetExceededJobsProcessing() throws Exception {

		final String id = this.jobPersister.insert(this.testObj, "foobar", 0L);
		this.jobPersister.getWaitingJobsForProcessing();
		Thread.sleep(1L);
		this.jobPersister.exceedJobs();
		assertEquals(JobState.EXCEEDED, this.jobPersister.getJobEntity(id).getJobState());
	}

	@Test
	public void testGetExceededJobsCompleting() throws Exception {

		final String id = this.jobPersister.insert(this.testObj, "foobar", 0L);
		this.jobPersister.setFinished(id);
		this.jobPersister.getFinishedJobsForCompleting();
		Thread.sleep(1L);
		this.jobPersister.exceedJobs();
		assertEquals(JobState.EXCEEDED, this.jobPersister.getJobEntity(id).getJobState());
	}

	@Test
	public void testRefresh() throws Exception {

		final String id = this.jobPersister.insert(this.testObj, "foobar", 0L);
		IJobEntity entity = this.jobPersister.getJobEntity(id);
		final long oldLastUpdateAsLong = entity.getLastUpdate().getTime();

		this.jobPersister.getWaitingJobsForProcessing();
		entity = this.jobPersister.getJobEntity(id);
		assertEquals(JobState.PROCESSING, entity.getJobState());
		Thread.sleep(1L);

		this.jobPersister.refresh();

		entity = this.jobPersister.getJobEntity(id);
		assertEquals(JobState.WAITING, entity.getJobState());
		assertTrue(oldLastUpdateAsLong < entity.getLastUpdate().getTime());
	}

	public static class TestObject implements Serializable {

		private static final long serialVersionUID = -3704975212728880677L;

		private final String payload;

		public TestObject(final String payload) {
			this.payload = payload;
		}

		public String getPayload() {
			return payload;
		}

		@Override
		public boolean equals(final Object obj) {
			return Objects.equal(this.payload, ((TestObject) obj).getPayload());
		}

		@Override
		public int hashCode() {
			return Objects.hashCode(this.payload);
		}
	}
}
