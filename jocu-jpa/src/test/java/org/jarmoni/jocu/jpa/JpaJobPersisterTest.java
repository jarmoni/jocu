/*
 * Copyright (c) 2013. All rights reserved.
 * Original Author: ms
 * Creation Date: Dec 9, 2013
 */
package org.jarmoni.jocu.jpa;

import java.util.Properties;

import javax.persistence.EntityManagerFactory;

import org.apache.commons.dbcp.BasicDataSource;
import org.hibernate.jpa.HibernatePersistenceProvider;
import org.jarmoni.jocu.common.api.IJobPersister;
import org.jarmoni.jocu.common.impl.AbstractJobPersisterTst;
import org.jarmoni.jocu.common.impl.AbstractJobQueueServiceItst.AbstractPersisterContext;
import org.jarmoni.unit.rule.LoggingRule;
import org.junit.Rule;
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
@ContextConfiguration(classes = JpaJobPersisterTest.PersisterContext.class)
@DirtiesContext(classMode = ClassMode.AFTER_EACH_TEST_METHOD)
public class JpaJobPersisterTest extends AbstractJobPersisterTst {

	// CHECKSTYLE:OFF
	@Rule
	public LoggingRule lr = new LoggingRule();
	// CHECKSTYLE:ON

	@Autowired
	private ApplicationContext ctx;

	@Override
	protected IJobPersister getJobPersister() {
		return this.ctx.getBean(IJobPersister.class);
	}

	@Configuration
	@EnableTransactionManagement
	public static class PersisterContext extends AbstractPersisterContext {

		@Bean(destroyMethod = "close")
		public BasicDataSource dataSource() {

			final BasicDataSource dataSource = new BasicDataSource();
			dataSource.setUrl("jdbc:h2:~/.jarmoni/jobqueue");
			dataSource.setDriverClassName("org.h2.Driver");
			dataSource.setUsername("abc");
			dataSource.setPassword("123");
			dataSource.setMaxActive(20);
			return dataSource;
		}

		@Bean(destroyMethod = "destroy")
		public LocalContainerEntityManagerFactoryBean entityManagerFactory() {

			final LocalContainerEntityManagerFactoryBean factoryBean = new LocalContainerEntityManagerFactoryBean();
			factoryBean.setDataSource(this.dataSource());
			factoryBean.setPackagesToScan("org.jarmoni.jocu.jpa");
			factoryBean.setPersistenceProviderClass(HibernatePersistenceProvider.class);
			factoryBean.setPersistenceUnitName("jobqueueUnit");
			final Properties props = new Properties();
			props.put("hibernate.dialect", "org.hibernate.dialect.H2Dialect");
			props.put("hibernate.hbm2ddl.auto", "create-drop");
			props.put("hibernate.ejb.naming_strategy", "org.hibernate.cfg.ImprovedNamingStrategy");
			props.put("hibernate.connection.charSet", "UTF-8");
			props.put("hibernate.show_sql", "true");
			factoryBean.setJpaProperties(props);
			return factoryBean;

		}

		@Bean
		public PlatformTransactionManager transactionManager(final EntityManagerFactory emf) {

			return new JpaTransactionManager(emf);
		}

		@Override
		@Bean
		public IJobPersister jobPersister() {

			return new JpaJobPersister();
		}
	}
}
