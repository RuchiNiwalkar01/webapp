package com.neu.edu.validation;

import javax.net.ssl.HttpsURLConnection;

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
import com.neu.edu.exception.BillException;

@ControllerAdvice
public class RestExceptionHandler{
	    
	     
	    @ExceptionHandler
	    public ResponseEntity<?> onHttpMessageNotReadable(final Exception e) throws Throwable {
	        final Throwable cause = e.getCause();
	        if (cause == null) 
	        {
	            return new ResponseEntity<String>("Invalid format",HttpStatus.BAD_REQUEST);
	        } 
	      
	        else if (cause instanceof InvalidFormatException) 
	        {
	        	 return new ResponseEntity<String>("Amount should be a number or Payment_status field should be in [paid, due, past_due, no_payment_required] ", HttpStatus.BAD_REQUEST);
	        } 
	        
	        return new ResponseEntity<String>("Unauthorized", HttpStatus.UNAUTHORIZED);
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
