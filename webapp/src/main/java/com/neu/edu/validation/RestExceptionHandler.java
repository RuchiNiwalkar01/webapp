package com.neu.edu.validation;

import javax.net.ssl.HttpsURLConnection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.fasterxml.jackson.databind.exc.InvalidTypeIdException;
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;
import com.google.gson.JsonObject;
import com.neu.edu.controller.BillController;
import com.neu.edu.exception.BillException;
import com.timgroup.statsd.StatsDClient;

@ControllerAdvice
public class RestExceptionHandler{
	
	@Autowired
	StatsDClient statsDClient;

    final static Logger logger = LoggerFactory.getLogger(BillController.class);
	     
//	    @ExceptionHandler
//	    public ResponseEntity<?> onHttpMessageNotReadable(final Exception e) throws Throwable {
//	        final Throwable cause = e.getCause();
//	        if (cause == null) 
//	        {
//	            return new ResponseEntity<String>("Invalid format",HttpStatus.BAD_REQUEST);
//	        } 
//	      
//	        else if (cause instanceof InvalidFormatException) 
//	        {
//	        	 return new ResponseEntity<String>("Amount should be a number or Payment_status field should be in [paid, due, past_due, no_payment_required] ", HttpStatus.BAD_REQUEST);
//	        } 
//	        
//	        return new ResponseEntity<String>("Unauthorized", HttpStatus.BAD_REQUEST);
//	    }
	    @ExceptionHandler
	    public ResponseEntity<?> onExceptions( Exception e) {
	      
	    	JsonObject entity = new JsonObject();
	    	
	        if (e.getMessage().contains("Enum")) 
	        {
	        	long start = System.currentTimeMillis();
	        	entity.addProperty("message", "Payment_status field should be in [ paid, due, past_due, no_payment_required ]");
	        	long end = System.currentTimeMillis();
				statsDClient.recordExecutionTime("postBillApiTime", (end-start));
				logger.error("Payment_status field should be in [ paid, due, past_due, no_payment_required ]");
	            return new ResponseEntity<String>(entity.toString(),HttpStatus.BAD_REQUEST);
	        } 
	      
	        else if (e.getMessage().contains("double")) 
	        {
	        	 entity.addProperty("message", "Amount should be a number");
	        	 return new ResponseEntity<String>(entity.toString(), HttpStatus.BAD_REQUEST);
	        } 
	        entity.addProperty("message", "Invalid type");
	        return new ResponseEntity<String>(entity.toString(), HttpStatus.BAD_REQUEST);
	    }

	
//	   @ExceptionHandler(Exception.class)
//	    public ResponseEntity<?> onHttpMessageNotReadable(Exception e)
//	   {
//	        return new ResponseEntity<String>("ERROR HANDLED", HttpStatus.BAD_REQUEST);
//	        
//	    }
//	   
//	   @ExceptionHandler(InvalidFormatException.class)
//	    public ResponseEntity<?> onHttpMessageNotReadable(InvalidFormatException e)
//	   {
//	        return new ResponseEntity<String>("Enum or amount", HttpStatus.BAD_REQUEST);
//	        
//	    }
//	
}
