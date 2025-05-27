package com.peaksolution.openatfx.api;

import org.asam.ods.AoException;
import org.asam.ods.ErrorCode;
import org.asam.ods.SeverityFlag;


public class OpenAtfxException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public static final String ERR_UNSUPPORTED_CONTEXT_DATATYPE = "The context parameter has an unsupported value type! Only String, Integer, Long and Boolean are supported, but following parameter received: ";

    private final ErrorCode errorCode;

    public OpenAtfxException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public ErrorCode getError() {
        return errorCode;
    }

    public AoException toAoException() {
        AoException ex = new AoException(errorCode, SeverityFlag.ERROR, 0, getLocalizedMessage());
        ex.initCause(this);
        return ex;
    }
}
