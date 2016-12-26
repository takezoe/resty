package com.dpillay.tools.tail4j.exception;

public class ApplicationException extends Exception {
	private static final long serialVersionUID = 4594480176794789043L;

	private ErrorCode errorCode = ErrorCode.DEFAULT_ERROR;
	private String errorDescription = null;

	public ApplicationException() {
		super();
	}

	public ApplicationException(ErrorCode errorCode, String errorDescription) {
		super();
		this.errorCode = errorCode;
		this.errorDescription = errorDescription;
	}

	public ApplicationException(Throwable cause, ErrorCode errorCode,
			String errorDescription) {
		super(cause);
		this.errorCode = errorCode;
		this.errorDescription = errorDescription;
	}

	public ErrorCode getErrorCode() {
		return this.errorCode;
	}

	public String getErrorDescription() {
		return this.errorDescription;
	}

	@Override
	public String toString() {
		return "ApplicationException [errorCode=" + errorCode
				+ ", errorDescription=" + errorDescription
				+ ", getLocalizedMessage()=" + getLocalizedMessage() + "]";
	}
}
