package org.jarmoni.jocu.common.impl;

import org.jarmoni.jocu.common.api.IJobPersister;

public class JobQueueServiceIT extends AbstractJobQueueServiceItst {

	@Override
	IJobPersister getJobPersister() {
		return new SimpleJobPersister();
	}

}
