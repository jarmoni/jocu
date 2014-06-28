package org.jarmoni.jocu.common.impl;

import org.jarmoni.jocu.common.api.IJobPersister;

public class SimpleJobPersisterTest extends AbstractJobPersisterTst {

	@Override
	protected IJobPersister getJobPersister() {
		return new SimpleJobPersister();
	}
}
