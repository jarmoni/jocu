/*
 * Copyright (c) 2013. All rights reserved.
 * Original Author: ms
 * Creation Date: Nov 23, 2013
 */
package org.jarmoni.jocu.common.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Optional;

import mockit.Expectations;
import mockit.Mocked;

import org.jarmoni.jocu.common.api.IJob;
import org.jarmoni.jocu.common.api.IJobEntity;
import org.jarmoni.jocu.common.api.IJobGroup;
import org.jarmoni.jocu.common.api.IJobPersister;
import org.jarmoni.jocu.common.api.IJobReceiver;
import org.jarmoni.jocu.common.api.JobQueueException;
import org.jarmoni.jocu.common.api.JobState;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.google.common.collect.Lists;

public class JobQueueServiceTest {

	// CHECKSTYLE:OFF
	@Rule
	public ExpectedException ee = ExpectedException.none();
	// CHECKSTYLE:ON

	private JobQueueService jobQueueService;

	@Mocked
	private IJobPersister persister;

	@Mocked
	private IJobReceiver jobReceiver;
	@Mocked
	private IJobReceiver finishedReceiver;

	private IJobGroup group;
	private IJob job;
	private final long timeout = 10000L;

	@Before
	public void setUp() throws Exception {

		this.group = new JobGroup("myGroup", this.jobReceiver, this.finishedReceiver);
		this.jobQueueService = new JobQueueService(Lists.newArrayList(this.group), this.persister);

		this.job = new Job("abc", this.group.getName(), "123", this.jobQueueService.getQueueServiceAccess());
	}

	@Test
	public void testPushGroup() throws Exception {

		new Expectations() {
			{
				persister.insert(job.getJobObject(), group.getName(), timeout);
				result = "abc";
			}
		};
		assertEquals("abc", this.jobQueueService.push(this.job.getJobObject(), this.group.getName(), Optional.of(this.timeout)));
	}

	@Test
	public void testPushGroupNoGroup() throws Exception {

		this.ee.expect(JobQueueException.class);
		this.ee.expectMessage("Group does not exist. Group='myGroup'");
		this.jobQueueService.getJobGroups().clear();

		new Expectations() {
			{
				persister.insert(job.getJobObject(), group.getName(), timeout);
				result = "abc";
			}
		};
		this.jobQueueService.push(this.job.getJobObject(), this.group.getName(), Optional.of(this.timeout));
	}

	@Test
	public void testCancel() throws Exception {

		new Expectations() {
			{
				persister.delete("abc");
			}
		};

		this.jobQueueService.cancel("abc");
	}

	@Test
	public void testPause() throws Exception {

		new Expectations() {
			{
				persister.pause("abc");
			}
		};

		this.jobQueueService.pause("abc");
	}

	@Test
	public void testResume() throws Exception {

		new Expectations() {
			{
				persister.resume("abc");
			}
		};

		this.jobQueueService.resume("abc");
	}

	@Test
	public void testUpdate() throws Exception {

		new Expectations() {
			{
				persister.update(job.getJobId(), job.getJobObject());
			}
		};

		this.jobQueueService.update(this.job);
	}

	@Test
	public void testSetFinished() throws Exception {

		new Expectations() {
			{
				persister.setFinished(job.getJobId());
			}
		};

		this.jobQueueService.setFinished(this.job);
	}

	@Test
	public void testIsPausedTrue() throws Exception {

		final IJobEntity jobEntity = JobEntity.builder().id("abc").jobState(JobState.PAUSED).build();

		new Expectations() {
			{
				persister.getJobEntity("abc");
				result = jobEntity;
			}
		};

		assertTrue(this.jobQueueService.isPaused(this.job));
	}

	@Test
	public void testIsPausedFalse() throws Exception {

		final IJobEntity jobEntity = JobEntity.builder().id("abc").jobState(JobState.WAITING).build();

		new Expectations() {
			{
				persister.getJobEntity("abc");
				result = jobEntity;
			}
		};

		assertFalse(this.jobQueueService.isPaused(this.job));
	}
}
