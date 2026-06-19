package com.tomolog.common.error;

/** Domain-level exception carrying an {@link ErrorCode}; mapped to the error envelope. */
public class ApiException extends RuntimeException {

  private final transient ErrorCode errorCode;

  public ApiException(ErrorCode errorCode) {
    super(errorCode.defaultMessage());
    this.errorCode = errorCode;
  }

  public ApiException(ErrorCode errorCode, String message) {
    super(message);
    this.errorCode = errorCode;
  }

  public ErrorCode getErrorCode() {
    return errorCode;
  }
}
