/*
 * Copyright (c) 2013. All rights reserved.
 * Original Author: ms
 * Creation Date: Dec 9, 2013
 */
package org.jarmoni.jocu.jpa;

import java.io.Serializable;

public class TestObject implements Serializable {

	private static final long serialVersionUID = -6861827154759631658L;
	private final String name;
	private final String description;

	public TestObject(final String name, final String description) {
		this.name = name;
		this.description = description;
	}

	public String getName() {
		return name;
	}

	public String getDescription() {
		return description;
	}
}
