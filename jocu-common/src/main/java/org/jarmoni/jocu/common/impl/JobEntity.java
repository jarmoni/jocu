package org.jarmoni.jocu.common.impl;

import java.util.Date;

import org.jarmoni.jocu.common.api.IJobEntity;
import org.jarmoni.jocu.common.api.JobState;

import com.google.common.base.Objects;

public class JobEntity implements IJobEntity {

	private String id;
	private Date lastUpdate;
	private Long timeout;
	private Long currentTimeout;
	private JobState jobState;
	private Object jobObject;
	private String jobGroup;

	@Override
	public String getId() {
		return id;
	}

	public void setId(final String id) {
		this.id = id;
	}

	@Override
	public Date getLastUpdate() {
		return lastUpdate;
	}

	@Override
	public void setLastUpdate(final Date lastUpdate) {
		this.lastUpdate = lastUpdate;
	}

	@Override
	public Long getTimeout() {
		return timeout;
	}

	@Override
	public void setTimeout(final Long timeout) {
		this.timeout = timeout;
	}

	@Override
	public Long getCurrentTimeout() {
		return this.currentTimeout;
	}

	@Override
	public void setCurrentTimeout(final Long currentTimeout) {
		this.currentTimeout = currentTimeout;
	}

	@Override
	public JobState getJobState() {
		return jobState;
	}

	@Override
	public void setJobState(final JobState jobState) {
		this.jobState = jobState;
	}

	@Override
	public Object getJobObject() {
		return jobObject;
	}

	@Override
	public void setJobObject(final Object jobObject) {
		this.jobObject = jobObject;
	}

	@Override
	public String getJobGroup() {
		return jobGroup;
	}

	@Override
	public void setJobGroup(final String jobGroup) {
		this.jobGroup = jobGroup;
	}

	@Override
	public String toString() {

		return Objects.toStringHelper(this.getClass()).add("id", this.id).add("lastUpdate", this.lastUpdate)
				.add("timeout", this.timeout).add("currentTimeout", this.currentTimeout).add("jobState", this.jobState)
				.add("jobObject", this.jobObject).add("jobGroup", this.jobGroup).toString();
	}

	public static JobEntityBuilder builder() {
		return new JobEntityBuilder();
	}

	public static final class JobEntityBuilder {

		private final JobEntity jobEntity;

		private JobEntityBuilder() {
			this.jobEntity = new JobEntity();
		}

		public JobEntity build() {
			return this.jobEntity;
		}

		public JobEntityBuilder id(final String id) {
			this.jobEntity.setId(id);
			return this;
		}

		public JobEntityBuilder lastUpdate(final Date lastUpdate) {
			this.jobEntity.setLastUpdate(lastUpdate);
			return this;
		}

		public JobEntityBuilder timeout(final Long timeout) {
			this.jobEntity.setTimeout(timeout);
			return this;
		}

		public JobEntityBuilder currentTimeout(final Long currentTimeout) {
			this.jobEntity.setCurrentTimeout(currentTimeout);
			return this;
		}

		public JobEntityBuilder jobState(final JobState jobState) {
			this.jobEntity.setJobState(jobState);
			return this;
		}

		public JobEntityBuilder jobObject(final Object jobObject) {
			this.jobEntity.setJobObject(jobObject);
			return this;
		}

		public JobEntityBuilder jobGroup(final String jobGroup) {
			this.jobEntity.setJobGroup(jobGroup);
			return this;
		}
	}
}
