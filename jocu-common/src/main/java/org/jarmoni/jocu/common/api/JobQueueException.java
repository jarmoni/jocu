/*
 * Copyright (c) 2013. All rights reserved.
 * Original Author: ms
 * Creation Date: Nov 16, 2013
 */
package org.jarmoni.jocu.common.api;

public class JobQueueException extends RuntimeException {

	private static final long serialVersionUID = -4102299607184694771L;

	public JobQueueException(final String message) {
		super(message);
	}

	public JobQueueException(final Throwable cause) {
		super(cause);
	}

	public JobQueueException(final String message, final Throwable cause) {
		super(message, cause);
	}
}
