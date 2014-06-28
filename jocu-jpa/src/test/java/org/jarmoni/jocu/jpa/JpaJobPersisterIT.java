/*
 * Copyright (c) 2013. All rights reserved.
 * Original Author: ms
 * Creation Date: Dec 9, 2013
 */
package org.jarmoni.jocu.jpa;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Collection;
import java.util.Date;
import java.util.Properties;

import javax.persistence.EntityManagerFactory;

import org.apache.commons.dbcp.BasicDataSource;
import org.hibernate.jpa.HibernatePersistenceProvider;
import org.jarmoni.jocu.common.api.IJobEntity;
import org.jarmoni.jocu.common.api.IJobPersister;
import org.jarmoni.jocu.common.api.JobState;
import org.jarmoni.jocu.common.impl.JobQueueIT;
import org.jarmoni.jocu.common.impl.JobQueueIT.AbstractPersisterContext;
import org.jarmoni.unit.rule.LoggingRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = { JobQueueIT.QueueContext.class,
		JpaJobPersisterIT.PersisterContext.class })
@DirtiesContext(classMode = ClassMode.AFTER_EACH_TEST_METHOD)
public class JpaJobPersisterIT {
	
	//CHECKSTYLE:OFF
	@Rule
	public LoggingRule lr = new LoggingRule();
	//CHECKSTYLE:ON

	@Autowired
	private ApplicationContext ctx;

	@Test
	public void testInsert() throws Exception {

		final String id = this.insertObject();
		final IJobPersister persister = this.getPersister();

		final IJobEntity entity = persister.getJobEntity(id);
		assertNotNull(entity);
		assertEquals(id, entity.getId());
		assertEquals(entity.getTimeout(), entity.getCurrentTimeout());
		assertNotNull(entity.getLastUpdate());
		assertEquals(JobState.NEW, entity.getJobState());
		final TestObject testObject2 = (TestObject) entity.getJobObject();
		assertEquals("foo", testObject2.getName());
		assertEquals("bar", testObject2.getDescription());
		assertEquals("foobar", entity.getJobGroup());
	}

	@Test
	public void testDelete() throws Exception {

		final String id = this.insertObject();
		final IJobPersister persister = this.getPersister();

		persister.delete(id);

		final IJobEntity entity = persister.getJobEntity(id);
		assertNull(entity);
	}

	@Test
	public void testDeleteJobNotExists() throws Exception {

		final IJobPersister persister = this.getPersister();

		persister.delete("123");
	}

	@Test
	public void testPause() throws Exception {

		final String id = this.insertObject();
		final IJobPersister persister = this.getPersister();

		persister.pause(id);

		final IJobEntity entity = persister.getJobEntity(id);
		assertNotNull(entity);
		assertEquals(id, entity.getId());
		assertNotNull(entity.getTimeout());
		assertNull(entity.getCurrentTimeout());
		assertNotNull(entity.getLastUpdate());
		assertEquals(JobState.PAUSED, entity.getJobState());
		final TestObject testObject = (TestObject) entity.getJobObject();
		assertEquals("foo", testObject.getName());
		assertEquals("bar", testObject.getDescription());
		assertEquals("foobar", entity.getJobGroup());
	}

	@Test
	public void testPauseJobNotExists() throws Exception {

		final IJobPersister persister = this.getPersister();

		persister.pause("123");
	}

	@Test
	public void testResume() throws Exception {

		final String id = this.insertObject();
		final IJobPersister persister = this.getPersister();

		persister.pause(id);

		final IJobEntity jobEntity = persister.getJobEntity(id);
		final Date lastUpdate = jobEntity.getLastUpdate();

		Thread.sleep(1L);
		persister.resume(id);

		final IJobEntity entity2 = persister.getJobEntity(id);
		assertNotNull(entity2);
		assertEquals(id, entity2.getId());
		assertEquals(entity2.getTimeout(), entity2.getCurrentTimeout());
		assertNotNull(entity2.getLastUpdate());
		assertEquals(JobState.NEW, entity2.getJobState());
		assertTrue(lastUpdate.getTime() < entity2.getLastUpdate().getTime());
	}

	@Test
	public void testUpdate() throws Exception {

		final String id = this.insertObject();
		final IJobPersister persister = this.getPersister();

		final IJobEntity jobEntity = persister.getJobEntity(id);
		final Date lastUpdate = jobEntity.getLastUpdate();

		final Object newJobObject = new TestObject("foofoo", "barbar");

		Thread.sleep(1L);
		persister.update(id, newJobObject);

		final IJobEntity entity2 = persister.getJobEntity(id);
		assertNotNull(entity2);
		assertEquals(id, entity2.getId());
		assertEquals(JobState.NEW, entity2.getJobState());
		assertTrue(lastUpdate.getTime() < entity2.getLastUpdate().getTime());
	}

	@Test
	public void testSetFinished() throws Exception {

		final String id = this.insertObject();
		final IJobPersister persister = this.getPersister();

		persister.setFinished(id);

		final IJobEntity jobEntity = persister.getJobEntity(id);
		assertEquals(JobState.FINISHED, jobEntity.getJobState());
	}

	@Test
	public void testSetError() throws Exception {

		final String id = this.insertObject();
		final IJobPersister persister = this.getPersister();

		persister.setError(id);

		final IJobEntity jobEntity = persister.getJobEntity(id);
		assertEquals(JobState.ERROR, jobEntity.getJobState());
	}

	@Test
	public void testGetNewJobs() throws Exception {

		this.insertObject();

		final IJobPersister persister = this.getPersister();

		final Collection<IJobEntity> jobs = persister.getNewJobs();
		assertEquals(1, jobs.size());
		assertEquals(JobState.NEW_IN_PROGRESS, jobs.iterator().next()
				.getJobState());
	}

	@Test
	public void testGetFinishedJobs() throws Exception {

		final String id = this.insertObject();

		final IJobPersister persister = this.getPersister();
		persister.setFinished(id);

		final Collection<IJobEntity> jobs = persister.getFinishedJobs();
		assertEquals(1, jobs.size());
		assertEquals(JobState.FINISHED_IN_PROGRESS, jobs.iterator().next()
				.getJobState());
	}

	@Test
	public void testGetExceededJobs() throws Exception {

		this.insertObject();

		final IJobPersister persister = this.getPersister();
		Thread.sleep(10000L);

		final Collection<IJobEntity> jobs = persister.getTimeoutJobs();
		assertEquals(1, jobs.size());
		assertEquals(JobState.EXCEEDED_IN_PROGRESS, jobs.iterator().next().getJobState());
	}

	private IJobPersister getPersister() {
		return this.ctx.getBean(IJobPersister.class);
	}

	private String insertObject() throws Exception {
		return this.insertObject("foo");
	}

	private String insertObject(final String name) throws Exception {
		final IJobPersister persister = this.getPersister();
		final TestObject testObject = new TestObject(name, "bar");
		return persister.insert(testObject, "foobar", 1000L);
	}

	@Configuration
	@EnableTransactionManagement
	public static class PersisterContext extends AbstractPersisterContext {

		@Bean(destroyMethod = "close")
		public BasicDataSource dataSource() {

			BasicDataSource dataSource = new BasicDataSource();
			dataSource.setUrl("jdbc:h2:~/.jarmoni/jobqueue");
			dataSource.setDriverClassName("org.h2.Driver");
			dataSource.setUsername("abc");
			dataSource.setPassword("123");
			dataSource.setMaxActive(20);
			return dataSource;
		}

		@Bean(destroyMethod = "destroy")
		public LocalContainerEntityManagerFactoryBean entityManagerFactory() {

			LocalContainerEntityManagerFactoryBean factoryBean = new LocalContainerEntityManagerFactoryBean();
			factoryBean.setDataSource(this.dataSource());
			factoryBean.setPackagesToScan("org.jarmoni.jocu.jpa");
			factoryBean
					.setPersistenceProviderClass(HibernatePersistenceProvider.class);
			factoryBean.setPersistenceUnitName("jobqueueUnit");
			Properties props = new Properties();
			props.put("hibernate.dialect", "org.hibernate.dialect.H2Dialect");
			props.put("hibernate.hbm2ddl.auto", "create-drop");
			props.put("hibernate.ejb.naming_strategy",
					"org.hibernate.cfg.ImprovedNamingStrategy");
			props.put("hibernate.connection.charSet", "UTF-8");
			props.put("hibernate.show_sql", "true");
			factoryBean.setJpaProperties(props);
			return factoryBean;

		}

		@Bean
		public PlatformTransactionManager transactionManager(EntityManagerFactory emf) {

			return new JpaTransactionManager(emf);
		}

		@Bean
		public IJobPersister jobPersister() {

			return new JpaJobPersister();
		}
	}
}
