/*
 * Copyright (c) 2013. All rights reserved.
 * Original Author: ms
 * Creation Date: Dec 19, 2013
 */
package org.jarmoni.jocu.common.impl;

import static org.junit.Assert.assertEquals;
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
import org.jarmoni.jocu.common.api.JobQueueException;
import org.jarmoni.jocu.common.api.JobState;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.google.common.collect.Lists;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = { AbstractJobQueueServiceItst.QueueContext.class,
		AbstractJobQueueServiceItst.PersisterContext.class })
@DirtiesContext(classMode = ClassMode.AFTER_EACH_TEST_METHOD)
public abstract class AbstractJobQueueServiceItst {

	// CHECKSTYLE:OFF
	@Rule
	public ExpectedException ee = ExpectedException.none();
	// CHECKSTYLE:ON

	private final Logger logger = LoggerFactory.getLogger(AbstractJobQueueServiceItst.class);

	@Autowired
	private ConfigurableApplicationContext ctx;

	@Autowired
	private IJobQueue jobQueue;
	@Autowired
	private IJobPersister persister;
	@Autowired
	private IJobReceiver jobReceiver;
	@Autowired
	private IJobReceiver finishedReceiver;
	@Autowired
	private IJobFinishedStrategy jobFinishedStrategy;
	@Autowired
	private IJobGroup jobGroup;

	abstract IJobPersister getJobPersister();

	@Test
	public void testRegular() throws Exception {

		final String id = this.testRegularInternal();
		final IJobEntity jobEntity = this.persister.getJobEntity(id);
		assertEquals(JobState.COMPLETED, jobEntity.getJobState());
	}

	@Test
	public void testRegularFinishedDeleted() throws Exception {

		this.jobGroup.setDeleteFinishedJobs(true);
		final String id = this.testRegularInternal();
		assertNull(this.persister.getJobEntity(id));
	}

	private String testRegularInternal() throws Exception {

		final Object testObject = new Object();

		this.jobReceiver.receive(EasyMock.anyObject(IJob.class));
		EasyMock.expectLastCall().times(1);
		EasyMock.expect(this.jobFinishedStrategy.isFinished(EasyMock.anyObject(IJob.class))).andReturn(false);

		this.jobReceiver.receive(EasyMock.anyObject(IJob.class));
		EasyMock.expectLastCall().times(1);
		EasyMock.expect(this.jobFinishedStrategy.isFinished(EasyMock.anyObject(IJob.class))).andReturn(false);

		this.jobReceiver.receive(EasyMock.anyObject(IJob.class));
		EasyMock.expectLastCall().times(1);
		EasyMock.expect(this.jobFinishedStrategy.isFinished(EasyMock.anyObject(IJob.class))).andReturn(true);

		this.finishedReceiver.receive(EasyMock.anyObject(IJob.class));
		EasyMock.expectLastCall().times(1);

		EasyMock.replay(this.jobReceiver, this.finishedReceiver, this.jobFinishedStrategy);

		final String id = this.jobQueue.push(testObject, "foobar");

		Thread.sleep(5000L);
		EasyMock.verify(this.jobReceiver, this.finishedReceiver, this.jobFinishedStrategy);
		return id;
	}

	@Test
	public void testGroupNotExisting() throws Exception {

		this.ee.expect(JobQueueException.class);
		this.ee.expectMessage("Group does not exist. Group='foo'");
		this.jobQueue.push(new Object(), "foo");
	}

	@Test
	public void testError() throws Exception {

		final Object testObject = new Object();

		this.jobReceiver.receive(EasyMock.anyObject(IJob.class));
		EasyMock.expectLastCall().andThrow(new RuntimeException("abc"));

		final String id = this.jobQueue.push(testObject, "foobar");

		this.replay();

		Thread.sleep(2000L);
		this.verify();
		final IJobEntity jobEntity = this.persister.getJobEntity(id);
		assertEquals(JobState.ERROR, jobEntity.getJobState());
	}

	@Test
	public void testTimeout() throws Exception {

		this.runBlockingReceiver();

		this.replay();

		final String id = this.jobQueue.push(new Object(), "foobar", 1L);

		Thread.sleep(3000L);
		this.verify();
		assertEquals(JobState.EXCEEDED, this.persister.getJobEntity(id).getJobState());
	}

	@Test
	public void testTimeoutViaGroup() throws Exception {

		this.jobGroup.setTimeout(1L);

		this.runBlockingReceiver();

		this.replay();

		final String id = this.jobQueue.push(new Object(), "foobar");

		Thread.sleep(3000L);
		this.verify();
		assertEquals(JobState.EXCEEDED, this.persister.getJobEntity(id).getJobState());
	}

	@Test
	public void testPausedNoTimeout() throws Exception {

		this.runBlockingReceiver();

		this.replay();

		final String id = this.jobQueue.push(new Object(), "foobar", 1000L);
		Thread.sleep(2000L);
		this.persister.pause(id);

		Thread.sleep(3000L);

		this.verify();
		assertEquals(JobState.PAUSED, this.persister.getJobEntity(id).getJobState());
	}

	private void runBlockingReceiver() throws Exception {

		this.jobReceiver.receive(EasyMock.anyObject(IJob.class));
		EasyMock.expectLastCall().andDelegateTo((IJobReceiver)job -> {
			try {
				Thread.sleep(100000L);
			} catch (final InterruptedException e) {
				logger.info("Thread interrupted due to end of test");
			}

		});;
	}

	private void replay() {
		EasyMock.replay(this.jobReceiver, this.finishedReceiver, this.jobFinishedStrategy);
	}

	private void verify() {
		EasyMock.verify(this.jobReceiver, this.finishedReceiver, this.jobFinishedStrategy);
	}

	@Configuration
	public static class QueueContext {

		private final IJobReceiver jobReceiver = EasyMock.createMock(IJobReceiver.class);

		private final IJobReceiver finishedReceiver = EasyMock.createMock(IJobReceiver.class);

		private final IJobReceiver timeoutReceiver = EasyMock.createMock(IJobReceiver.class);

		private final IJobFinishedStrategy jobFinishedStrategy = EasyMock.createMock(IJobFinishedStrategy.class);

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
			return this.jobFinishedStrategy;
		}

		@Bean
		public IJobGroup jobGroup() {
			final IJobGroup jobGroup = new JobGroup("foobar", this.jobReceiver, this.finishedReceiver);
			((JobGroup) jobGroup).setJobFinishedStrategy(this.jobFinishedStrategy);
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
