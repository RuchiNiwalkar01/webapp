package com.neu.edu.exception;

public class BillException extends Exception{
	   private int status;

	    public BillException(String errMessage) {
	        super(errMessage);
	        this.status = 501;
	    }

	    public BillException(int status, String errMessage) {
	        super(errMessage);
	        this.status = status;
	    }

	    public int getStatus() {
	        return status;
	    }
	
	
}
