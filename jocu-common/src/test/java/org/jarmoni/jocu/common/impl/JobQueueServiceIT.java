package org.jarmoni.jocu.common.impl;

import org.jarmoni.jocu.common.api.IJobPersister;

public class JobQueueServiceIT extends JobQueueIT {

	@Override
	IJobPersister getJobPersister() {
		return new SimpleJobPersister();
	}



}
