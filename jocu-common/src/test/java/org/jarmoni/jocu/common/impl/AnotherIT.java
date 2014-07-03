package org.jarmoni.jocu.common.impl;

import org.jarmoni.jocu.common.api.IJobPersister;
import org.junit.runner.RunWith;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(inheritLocations = false, classes = {JobQueueIT.QueueContext.class, JobQueueIT.PersisterContext.class})
@DirtiesContext(classMode = ClassMode.AFTER_EACH_TEST_METHOD)
public class AnotherIT extends JobQueueIT {

	@Override
	IJobPersister getJobPersister() {
		return null;
	}



}