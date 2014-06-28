package org.jarmoni.jocu.common.impl;

import java.util.Optional;
import java.util.function.Predicate;

import org.jarmoni.jocu.common.api.IJob;
import org.jarmoni.jocu.common.api.IJobGroup;
import org.jarmoni.jocu.common.api.IJobReceiver;
import org.jarmoni.util.lang.Asserts;

import com.google.common.base.Objects;

public class JobGroup implements IJobGroup {

	private final String name;

	private final IJobReceiver jobReceiver;

	private final IJobReceiver finishedReceiver;

	private Optional<Predicate<IJob>> jobFinishedStrategy;

	private long timeout = 30000L;

	boolean deleteFinishedJobs = false;

	public JobGroup(final String name, final IJobReceiver jobReceiver, final IJobReceiver finishedReceiver) {
		this.name = Asserts.notNullSimple(name, "name");
		this.jobReceiver = Asserts.notNullSimple(jobReceiver, "jobReceiver");
		this.finishedReceiver = Asserts.notNullSimple(finishedReceiver, "finishedReceiver");
	}

	@Override
	public String getName() {
		return this.name;
	}

	@Override
	public Optional<Predicate<IJob>> getJobFinishedStrategy() {
		return this.jobFinishedStrategy;
	}

	@Override
	public void setJobFinishedStrategy(final Predicate<IJob> jobFinishedStrategy) {
		this.jobFinishedStrategy = Optional.of(jobFinishedStrategy);
	}

	@Override
	public IJobReceiver getJobReceiver() {
		return this.jobReceiver;
	}

	@Override
	public IJobReceiver getFinishedReceiver() {
		return this.finishedReceiver;
	}

	@Override
	public long getTimeout() {
		return this.timeout;
	}

	@Override
	public void setTimeout(final long timeout) {
		this.timeout = timeout;
	}

	@Override
	public boolean isDeleteFinishedJobs() {
		return this.deleteFinishedJobs;
	}

	@Override
	public void setDeleteFinishedJobs(final boolean deleteFinishedJobs) {
		this.deleteFinishedJobs = deleteFinishedJobs;
	}

	@Override
	public String toString() {

		return Objects.toStringHelper(this.getClass()).add("name", this.name).add("jobReceiver", this.jobReceiver).add("finishedReceiver", this.finishedReceiver)
				.add("jobFinishedStrategy", this.jobFinishedStrategy).add("timeout", this.timeout).add("deleteFinishedJobs", this.deleteFinishedJobs).toString();
	}
}
