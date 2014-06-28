/*
 * Copyright (c) 2013. All rights reserved.
 * Original Author: ms
 * Creation Date: Dec 19, 2013
 */
package org.jarmoni.jocu.common.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.List;

import org.easymock.EasyMock;
import org.jarmoni.jocu.common.api.IJob;
import org.jarmoni.jocu.common.api.IJobEntity;
import org.jarmoni.jocu.common.api.IJobFinishedStrategy;
import org.jarmoni.jocu.common.api.IJobGroup;
import org.jarmoni.jocu.common.api.IJobPersister;
import org.jarmoni.jocu.common.api.IJobQueue;
import org.jarmoni.jocu.common.api.IJobQueueService;
import org.jarmoni.jocu.common.api.IJobReceiver;
import org.jarmoni.jocu.common.api.JobState;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.google.common.collect.Lists;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {JobQueueIT.QueueContext.class, JobQueueIT.PersisterContext.class})
@DirtiesContext
public class JobQueueIT {

	@Autowired
	private ApplicationContext ctx;

	@Autowired
	private IJobQueue jobQueue;
	@Autowired
	private IJobPersister persister;
	@Autowired
	private IJobReceiver jobReceiver;
	@Autowired
	private IJobReceiver finishedReceiver;
	@Autowired
	private IJobFinishedStrategy finishedStrategy;
	@Autowired
	private IJobGroup jobGroup;

	@After
	public void tearDown() throws Exception {
		EasyMock.reset(this.jobReceiver);
		EasyMock.reset(this.finishedStrategy);
	}

	@Test
	public void testRegular() throws Exception {

		final String id = this.testRegularInternal();
		final IJobEntity jobEntity = this.persister.getJobEntity(id);
		assertEquals(JobState.FINISHED, jobEntity.getJobState());
	}

	@Test
	public void testRegularFinishedDeleted() throws Exception {

		final String id = this.testRegularInternal();
		assertNull(this.persister.getJobEntity(id));
	}

	private String testRegularInternal() throws Exception {

		final Object testObject = new Object();

		this.jobReceiver.receive(EasyMock.anyObject(IJob.class));
		EasyMock.expectLastCall().times(1);
		EasyMock.expect(this.finishedStrategy.finished(EasyMock.anyObject(IJob.class))).andReturn(false);

		this.jobReceiver.receive(EasyMock.anyObject(IJob.class));
		EasyMock.expectLastCall().times(1);
		EasyMock.expect(this.finishedStrategy.finished(EasyMock.anyObject(IJob.class))).andReturn(false);

		this.jobReceiver.receive(EasyMock.anyObject(IJob.class));
		EasyMock.expectLastCall().times(1);
		EasyMock.expect(this.finishedStrategy.finished(EasyMock.anyObject(IJob.class))).andReturn(true);

		this.finishedReceiver.receive(EasyMock.anyObject(IJob.class));
		EasyMock.expectLastCall().times(1);

		EasyMock.replay(this.jobReceiver, this.finishedReceiver, this.finishedStrategy);

		final String id = this.jobQueue.push(testObject, "foobar");

		Thread.sleep(10000);
		EasyMock.verify(this.jobReceiver, this.finishedReceiver, this.finishedStrategy);
		return id;
	}

	@Test
	public void testError() throws Exception {

		final Object testObject = new Object();

		this.jobReceiver.receive(EasyMock.anyObject(IJob.class));
		EasyMock.expectLastCall().andThrow(new RuntimeException("abc"));

		final String id = this.jobQueue.push(testObject, "myGroup");

		EasyMock.replay(this.jobReceiver, this.finishedReceiver, this.timeoutReceiver, this.finishedStrategy);

		Thread.sleep(2000L);
		EasyMock.verify(this.jobReceiver, this.finishedReceiver, this.timeoutReceiver, this.finishedStrategy);
		final IJobEntity jobEntity = this.persister.getJobEntity(id);
		assertEquals(JobState.ERROR, jobEntity.getJobState());
	}

	@Test
	public void testTimeout() throws Exception {

		final Object testObject = new Object();

		this.jobReceiver.receive(EasyMock.anyObject(IJob.class));
		EasyMock.expectLastCall().anyTimes();

		EasyMock.expect(this.finishedStrategy.finished(EasyMock.anyObject(IJob.class))).andReturn(false).anyTimes();

		this.timeoutReceiver.receive(EasyMock.anyObject(IJob.class));
		EasyMock.expectLastCall().times(1);

		EasyMock.replay(this.jobReceiver, this.finishedReceiver, this.timeoutReceiver, this.finishedStrategy);

		final String id = this.jobQueue.push(testObject, "myGroup", 200L);

		Thread.sleep(3000L);
		EasyMock.verify(this.jobReceiver, this.finishedReceiver, this.timeoutReceiver, this.finishedStrategy);
		assertNull(this.persister.getJobEntity(id));
	}

	@Test
	public void testPausedNoTimeout() throws Exception {

		final Object testObject = new Object();

		this.jobReceiver.receive(EasyMock.anyObject(IJob.class));
		EasyMock.expectLastCall().anyTimes();

		EasyMock.expect(this.finishedStrategy.finished(EasyMock.anyObject(IJob.class))).andReturn(false).anyTimes();

		EasyMock.replay(this.jobReceiver, this.finishedStrategy);

		final String id = this.jobQueue.push(testObject, "myGroup", 1000L);
		this.persister.pause(id);

		Thread.sleep(3000L);

		EasyMock.verify(this.jobReceiver, this.finishedStrategy);
		assertNotNull(this.persister.getJobEntity(id));
	}

	@Configuration
	public static class QueueContext {

		private final IJobReceiver jobReceiver = EasyMock.createMock(IJobReceiver.class);

		private final IJobReceiver finishedReceiver = EasyMock.createMock(IJobReceiver.class);

		private final IJobReceiver timeoutReceiver = EasyMock.createMock(IJobReceiver.class);

		private final IJobFinishedStrategy finishedStrategy = EasyMock.createMock(IJobFinishedStrategy.class);

		@Autowired
		private AbstractPersisterContext persisterContext;

		@Bean
		public IJobReceiver jobReceiver() {
			return this.jobReceiver;
		}

		@Bean
		public IJobReceiver finishedReceiver() {
			return this.finishedReceiver;
		}

		@Bean
		public IJobReceiver timeoutReceiver() {
			return this.timeoutReceiver;
		}

		@Bean
		public IJobFinishedStrategy finishedStrategy() {
			return this.finishedStrategy;
		}

		@Bean
		public List<IJobFinishedStrategy> finishedStrategies() {
			return Lists.newArrayList(this.finishedStrategy());
		}

		@Bean
		public IJobGroup jobGroup() {
			final IJobGroup jobGroup = new JobGroup("foobar", this.jobReceiver, this.finishedReceiver);
			((JobGroup) jobGroup).setJobFinishedStrategies(this.finishedStrategies());
			((JobGroup) jobGroup).setTimeout(5000L);
			return jobGroup;
		}

		@Bean
		public List<IJobGroup> jobGroups() {
			return Lists.newArrayList(jobGroup());
		}

		@Bean(initMethod = "start", destroyMethod = "stop")
		public IJobQueueService jobQueueService() {
			return new JobQueueService(this.jobGroups(), this.persisterContext.jobPersister());
		}

		@Bean
		public IJobQueue jobQueue() {
			return new JobQueue(this.jobQueueService());
		}
	}

	public static abstract class AbstractPersisterContext {

		public abstract IJobPersister jobPersister();
	}

	@Configuration
	public static class PersisterContext extends AbstractPersisterContext {

		@Bean
		@Override
		public IJobPersister jobPersister() {
			return new SimpleJobPersister();
		}
	}
}
