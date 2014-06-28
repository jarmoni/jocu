/*
 * Copyright (c) 2013. All rights reserved.
 * Original Author: ms
 * Creation Date: Dec 7, 2013
 */
package org.jarmoni.jocu.jpa;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.persistence.Version;

import org.jarmoni.jocu.common.api.IJobEntity;
import org.jarmoni.jocu.common.api.JobState;

import com.google.common.base.Objects;

@Entity
@Table(name = "JOB_ENTITY")
//@formatter:off
@NamedQueries({ 
	@NamedQuery(name = JpaJobEntity.QUERY_DELETE_BY_ID, query = "delete from JpaJobEntity x where x.id like :id"),
	@NamedQuery(name = JpaJobEntity.QUERY_PAUSE, query = "update JpaJobEntity x set x.jobState = 'PAUSED', x.currentTimeout = null where x.id like :id"),
	@NamedQuery(name = JpaJobEntity.QUERY_RESUME, query = "update JpaJobEntity x set x.jobState = 'NEW', x.lastUpdate = :lastUpdate, x.currentTimeout = x.timeout where x.id like :id"),
	@NamedQuery(name = JpaJobEntity.QUERY_UPDATE, query = "update JpaJobEntity x set x.jobState = 'NEW', x.lastUpdate = :lastUpdate, x.currentTimeout = x.timeout, x.jobBytes = :jobBytes where x.id like :id"),
	@NamedQuery(name = JpaJobEntity.QUERY_FINISH, query = "update JpaJobEntity x set x.jobState = 'FINISHED' where x.id like :id"),
	@NamedQuery(name = JpaJobEntity.QUERY_ERROR, query = "update JpaJobEntity x set x.jobState = 'ERROR' where x.id like :id"),
	@NamedQuery(name = JpaJobEntity.QUERY_SELECT_FOR_STATE, query = "select x from JpaJobEntity x where x.jobState like :jobState order by x.lastUpdate asc"),
	@NamedQuery(name = JpaJobEntity.QUERY_SELECT_ALL, query = "select x from JpaJobEntity x")
})
//@formatter:on
public class JpaJobEntity implements IJobEntity {

	public static final String QUERY_DELETE_BY_ID = "JpaJobEntity.deleteById";
	public static final String QUERY_PAUSE = "JpaJobEntity.pause";
	public static final String QUERY_RESUME = "JpaJobEntity.resume";
	public static final String QUERY_UPDATE = "JpaJobEntity.update";
	public static final String QUERY_FINISH = "JpaJobEntity.finish";
	public static final String QUERY_ERROR = "JpaJobEntity.error";
	public static final String QUERY_SELECT_FOR_STATE = "JpaJobEntity.selectForState";
	public static final String QUERY_SELECT_ALL = "JpaJobEntity.selectAll";

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	@Column(name = "ID", unique = true, nullable = false, updatable = false)
	private String id;

	@Version
	@Column(name = "VERSION", nullable = false)
	private Integer version;

	@Column(name = "LAST_UPDATE", nullable = false)
	private Date lastUpdate;

	@Column(name = "TIMEOUT")
	private Long timeout;

	@Column(name = "CURRENT_TIMEOUT")
	private Long currentTimeout;

	@Enumerated(EnumType.STRING)
	@Column(name = "JOB_STATE", nullable = false)
	private JobState jobState;

	@Lob
	@Column(name = "JOB_OBJECT", nullable = false)
	private byte[] jobBytes;

	@Column(name = "JOB_GROUP", nullable = false)
	private String jobGroup;

	@Override
	public String getId() {
		return id;
	}

	public void setId(final String id) {
		this.id = id;
	}

	public Integer getVersion() {
		return version;
	}

	public void setVersion(final Integer version) {
		this.version = version;
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
		return jobBytesToJobObject(this.jobBytes);
	}

	@Override
	public void setJobObject(final Object jobObject) {
		this.jobBytes = jobObjectToJobBytes(jobObject);
	}

	public byte[] getJobBytes() {
		return this.jobBytes;
	}

	public void setJobBytes(final byte[] jobBytes) {
		this.jobBytes = jobBytes;
	}

	@Override
	public String getJobGroup() {
		return jobGroup;
	}

	@Override
	public void setJobGroup(final String jobGroup) {
		this.jobGroup = jobGroup;
	}

	public static byte[] jobObjectToJobBytes(final Object jobObject) {
		final ByteArrayOutputStream bos = new ByteArrayOutputStream();
		try {
			final ObjectOutputStream oos = new ObjectOutputStream(bos);
			oos.writeObject(jobObject);
			return bos.toByteArray();
		} catch (final IOException e) {
			throw new RuntimeException("Could not create blob", e);
		}
	}

	public static Object jobBytesToJobObject(final byte[] jobBytes) {
		final ByteArrayInputStream bis = new ByteArrayInputStream(jobBytes);
		ObjectInputStream ois;
		try {
			ois = new ObjectInputStream(bis);
			return ois.readObject();
		} catch (final Exception e) {
			throw new RuntimeException("Could not read blob", e);
		}
	}

	@Override
	public String toString() {

		return Objects.toStringHelper(this.getClass()).add("id", this.id).add("version", this.version).add("lastUpdate", this.lastUpdate)
				.add("timeout", this.timeout).add("currentTimeout", this.currentTimeout).add("jobState", this.jobState)
				.add("jobBytes", this.jobBytes).add("jobGroup", this.jobGroup).toString();
	}

	public static JobEntityBuilder builder() {
		return new JobEntityBuilder();
	}

	public static final class JobEntityBuilder {

		private final JpaJobEntity jobEntity;

		private JobEntityBuilder() {
			this.jobEntity = new JpaJobEntity();
		}

		public JpaJobEntity build() {
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
