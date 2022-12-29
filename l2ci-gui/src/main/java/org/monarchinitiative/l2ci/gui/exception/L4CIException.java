package org.monarchinitiative.l2ci.gui.exception;

public class L4CIException extends Exception{
    public L4CIException(){super();}
    public L4CIException(String msg) { super(msg);}
    public L4CIException(String message, Throwable throwable) {
        super(message, throwable);
    }
    public L4CIException(Throwable cause) { super(cause); }
}
