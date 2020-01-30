package com.neu.edu.controller;

import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.HttpClientErrorException.BadRequest;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.google.gson.JsonObject;
import com.neu.edu.exception.BillException;
import com.neu.edu.model.Bill;
import com.neu.edu.model.PaymentStatus;
import com.neu.edu.model.User;
import com.neu.edu.repository.BillRepository;
import com.neu.edu.repository.BillRepositoryfindaSpecificBill;
import com.neu.edu.repository.UserRepository;


@RestController
public class BillController {

	@Autowired
	BillRepository billRepository;

	@Autowired
	BillRepositoryfindaSpecificBill billRepositoryfindaSpecificBill;

	@Autowired
	UserRepository userRepository;

	@Autowired
	private BCryptPasswordEncoder bCryptPasswordEncoder;

	String pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";
	SimpleDateFormat simpleDateFormat = new SimpleDateFormat(pattern);

	
    
	//Post a bill by authenticating User
	//201 created, 400 bad request, 401 for no auth or unauthorized
	@PostMapping(value= "/v1/bill")
	public ResponseEntity<?> postBillByUserId(@Validated @RequestBody(required = false) Bill bill, HttpServletRequest request, HttpServletResponse response)
	{
		
		
		String authorization = request.getHeader("Authorization");
		JsonObject entity = new JsonObject();
		if(authorization != null && authorization.toLowerCase().startsWith("basic"))
		{
			// Authorization: Basic base64credentials
			authorization = authorization.replaceFirst("Basic ", "");

			String credentials = new String(Base64.getDecoder().decode(authorization.getBytes()));

			// authorization = username:password
			String [] userCredentials = credentials.split(":", 2);
			String email = userCredentials[0];
			String password = userCredentials[1];

			User user = userRepository.findByemail(email);
			if(user == null)
			{
				entity.addProperty("message", "User does not exist. Please enter correct Username or Password");
			}
			else if(user != null && !bCryptPasswordEncoder.matches(password, user.getPassword()))
			{
				entity.addProperty("message", "The Password is Invalid");
				return new ResponseEntity<String>(entity.toString() , HttpStatus.UNAUTHORIZED);
			}
			else
			{	
				if(bill==null)
				{
					entity.addProperty("message", "The Request Body cannot be null");
					return new ResponseEntity<String>(entity.toString() , HttpStatus.BAD_REQUEST);
				}
				if( (bill.getVendor()!=null && bill.getVendor().trim().length() >0) && bill.getBilldate()!=null  && bill.getDuedate()!=null && 
					(bill.getAmountdue()>=0.01 && bill.getAmountdue()< Double.MAX_VALUE)
					&&  bill.getCategories()!= null && bill.getPaymentStatus() != null )
				{
					if(bill.getCategories().size()>0 )
					{
						if(validateDate(bill.getBilldate()) && validateDate(bill.getDuedate()))
						{
							if(bill.getPaymentStatus().equals(PaymentStatus.paid) || bill.getPaymentStatus().equals(PaymentStatus.due) ||  bill.getPaymentStatus().equals(PaymentStatus.no_payment_required) || bill.getPaymentStatus().equals(PaymentStatus.past_due))
							{
								String dateFormat = simpleDateFormat.format(new Date());	
								Bill b = new Bill();
								b.setUser(user);
								b.setVendor(bill.getVendor());
								b.setBilldate(bill.getBilldate());
								b.setDuedate(bill.getDuedate());
								b.setAmountdue(bill.getAmountdue());
								b.setCategories(bill.getCategories());
								b.setCreated_ts(dateFormat.toString());
								b.setUpdated_ts(dateFormat.toString());
								b.setPaymentStatus(bill.getPaymentStatus());
								b.setOwner_id(user.getId());
								billRepository.save(b);
								b.setUser(null);
								return new ResponseEntity<Bill>(b , HttpStatus.CREATED);
							}
							else
							{
								entity.addProperty("validation", " Please enter a valid Payment Status 	");
								return new ResponseEntity<String>(entity.toString() , HttpStatus.BAD_REQUEST);
							}
							
						}
						else
						{
							entity.addProperty("validation", " Please enter a valid date in YYYY-MM-DD format ");
							return new ResponseEntity<String>(entity.toString() , HttpStatus.BAD_REQUEST);
						}
					}
					else
					{
						entity.addProperty("validation", " Categories cannot be empty ");
						return new ResponseEntity<String>(entity.toString() , HttpStatus.BAD_REQUEST);
					}
				
					
				}
				else
				{
					entity.addProperty("message", "vendor, bill_date, due_date, amount_due, categories, payment status cannot be empty or Amount cannot be below 0.00");
					return new ResponseEntity<String>(entity.toString() , HttpStatus.BAD_REQUEST);
				}
			
			}		

			return new ResponseEntity<String>(entity.toString() , HttpStatus.UNAUTHORIZED);

		}

		entity.addProperty("message", "Invalid. Unable to Authenticate");	
		return new ResponseEntity<String>(entity.toString() , HttpStatus.UNAUTHORIZED);

	}


	//Get all bill by Specific User
	//200 for success, 401 no authorization
	@GetMapping(value = "/v1/bills")
	public ResponseEntity<?> getAllBillsByUserId(HttpServletRequest request, HttpServletResponse response)
	{
		String authorization = request.getHeader("Authorization");
		JsonObject entity = new JsonObject();
		if(authorization != null && authorization.toLowerCase().startsWith("basic"))
		{
			// Authorization: Basic base64credentials
			authorization = authorization.replaceFirst("Basic ", "");

			String credentials = new String(Base64.getDecoder().decode(authorization.getBytes()));

			// authorization = username:password
			String [] userCredentials = credentials.split(":", 2);
			String email = userCredentials[0];

			String password = userCredentials[1];

			User user = userRepository.findByemail(email);
			if(user == null)
			{
				entity.addProperty("message", "User does not exist. Please enter correct Username or Password");
			}
			else if(user != null && !bCryptPasswordEncoder.matches(password, user.getPassword()))
			{
				entity.addProperty("message", "The Password is Invalid");
				return new ResponseEntity<String>(entity.toString() , HttpStatus.UNAUTHORIZED);
			}
			else
			{
				List<Bill> listOfBills = billRepository.findByUserId(user.getId());
				user.setPassword(null);
				if(listOfBills.size() ==0)
				{
					entity.addProperty("message", "The bills do not exist");
					return new ResponseEntity<String>(entity.toString() , HttpStatus.NOT_FOUND);
				}
				return new ResponseEntity<List<Bill>>(listOfBills , HttpStatus.OK);
			}					

			return new ResponseEntity<String>(entity.toString() , HttpStatus.UNAUTHORIZED);
		}

		entity.addProperty("message", "Invalid. Unable to Authenticate");	
		return new ResponseEntity<String>(entity.toString() , HttpStatus.UNAUTHORIZED);
	}
	

	//Get Specific bill by billId
	@GetMapping(value = "/v1/bill/{id}")
	public ResponseEntity<?> getSingleBillbyId(HttpServletRequest request, HttpServletResponse response, @PathVariable(value="id" ) String billId)
	{
		
		String authorization = request.getHeader("Authorization");
		JsonObject entity = new JsonObject();
		if(authorization != null && authorization.toLowerCase().startsWith("basic"))
		{
			// Authorization: Basic base64credentials
			authorization = authorization.replaceFirst("Basic ", "");

			String credentials = new String(Base64.getDecoder().decode(authorization.getBytes()));

			// authorization = username:password
			String [] userCredentials = credentials.split(":", 2);
			String email = userCredentials[0];

			String password = userCredentials[1];

			User user = userRepository.findByemail(email);
			if(user == null)
			{
				entity.addProperty("message", "User does not exist. Please enter correct Username or Password");
			}
			else if(user != null && !bCryptPasswordEncoder.matches(password, user.getPassword()))
			{
				entity.addProperty("message", "The Password is Invalid");
				return new ResponseEntity<String>(entity.toString() , HttpStatus.UNAUTHORIZED);
			}
			else
			{
				 UUID  uid  = null ;
		            try 
		            {
		                 uid = UUID.fromString(billId);
		                 System.out.println("Bill UUID is : ");
		            }
		            catch (Exception e)
		            {

						entity.addProperty("message", "The bill does not exist.");
						return new ResponseEntity<String>(entity.toString(), HttpStatus.NOT_FOUND);
		            }
		            
				List<Bill> listOfBills = billRepository.findByUserId(user.getId());
				 
				
				Bill bill = billRepositoryfindaSpecificBill.findById(billId);
				if(listOfBills.size() > 0)
				{
					if(bill != null)
					{
						if(listOfBills.contains(bill))
						{
							user.setPassword(null);
							return new ResponseEntity<Bill>(bill , HttpStatus.OK);
						}			
						else
						{
							entity.addProperty("message", "The bill does not belong to particular user");
							return new ResponseEntity<String>(entity.toString(), HttpStatus.UNAUTHORIZED);
						}
					}
					else
					{
						entity.addProperty("message", "The bill does not exist.");
						return new ResponseEntity<String>(entity.toString(), HttpStatus.NOT_FOUND);
					}
				}
				else
				{
					entity.addProperty("message", "The bill does not exist.");
					return new ResponseEntity<String>(entity.toString(), HttpStatus.NOT_FOUND);
				}
				
				
			}					

			return new ResponseEntity<String>(entity.toString() , HttpStatus.UNAUTHORIZED);
		}

		entity.addProperty("message", "Invalid. Unable to Authenticate");	
		return new ResponseEntity<String>(entity.toString() , HttpStatus.UNAUTHORIZED);
	}
	

	
	//Update a bill 
	@PutMapping(value = "/v1/bill/{id}")
	public ResponseEntity<?> updateBillById(HttpServletRequest request, HttpServletResponse response, @RequestBody(required = false) Bill bill, @PathVariable(required = true, value = "id") @NotBlank @NotNull String billId )
	{
		String authorization = request.getHeader("Authorization");
		JsonObject entity = new JsonObject();
		if(authorization != null && authorization.toLowerCase().startsWith("basic"))
		{
			// Authorization: Basic base64credentials
			authorization = authorization.replaceFirst("Basic ", "");

			String credentials = new String(Base64.getDecoder().decode(authorization.getBytes()));

			// authorization = username:password
			String [] userCredentials = credentials.split(":", 2);
			String email = userCredentials[0];

			String password = userCredentials[1];

			User user = userRepository.findByemail(email);
			if(user == null)
			{
				entity.addProperty("message", "User does not exist. Please enter correct Username or Password");
			}
			else if(user != null && !bCryptPasswordEncoder.matches(password, user.getPassword()))
			{
				entity.addProperty("message", "The Password is Invalid");
				return new ResponseEntity<String>(entity.toString() ,HttpStatus.UNAUTHORIZED);
			}
			else
			{
				if(bill==null)
				{
					entity.addProperty("message", "The Request Body cannot be null");
					return new ResponseEntity<String>(entity.toString() , HttpStatus.BAD_REQUEST);
				}
				
				List<Bill> listOfBills = billRepository.findByUserId(user.getId());
				 UUID  uid  = null ;
		            try 
		            {
		                 uid = UUID.fromString(billId);
		                 System.out.println("Bill UUID is : ");
		            }
		            catch (Exception e)
		            {

						entity.addProperty("message", "The bill does not exist or bill Id is incorrect.");
						return new ResponseEntity<String>(entity.toString(), HttpStatus.NOT_FOUND);
		            }
		            
			//&& 
				if(billId != null )
				{
					Bill b = billRepositoryfindaSpecificBill.findById(billId);
					if(listOfBills.size() > 0)
					{
						if(b != null)
						{
							if(listOfBills.contains(b))
							{
								if( (bill.getVendor()!=null && bill.getVendor().trim().length() >0) && bill.getBilldate()!=null  && bill.getDuedate()!=null && 
										(bill.getAmountdue()>=0.01 && bill.getAmountdue()< Double.MAX_VALUE)
										&& bill.getCategories() != null  && bill.getPaymentStatus() != null )
									{
									if(bill.getCategories().size()>0 )
									{
										if(validateDate(bill.getBilldate()) && validateDate(bill.getDuedate()))
										{
											String dateFormat = simpleDateFormat.format(new Date());	
											//b.setUser(user);
											b.setVendor(bill.getVendor());
											b.setBilldate(bill.getBilldate());
											b.setDuedate(bill.getDuedate());
											b.setAmountdue(bill.getAmountdue());
											b.setCategories(bill.getCategories());
											b.setUpdated_ts(dateFormat.toString());
											b.setPaymentStatus(bill.getPaymentStatus());
											//b.setOwner_id(user.getId());
											billRepository.save(b);
											user.setPassword(null);
											return new ResponseEntity<Bill>(b , HttpStatus.OK);
											
										}		
										else
										{
											entity.addProperty("validation", " Please enter a valid date in YYYY-MM-DD format ");
											return new ResponseEntity<String>(entity.toString() , HttpStatus.BAD_REQUEST);
										}
									}
									else
									{
										entity.addProperty("validation", " categories cannot be empty ");
										return new ResponseEntity<String>(entity.toString() , HttpStatus.BAD_REQUEST);
									}
										
									}
								else
								{
									entity.addProperty("message", "vendor, bill_date, due_date, amount_due, categories or payment status cannot be empty");
									return new ResponseEntity<String>(entity.toString() , HttpStatus.BAD_REQUEST);
								}
								
							}			
							else
							{
								entity.addProperty("message", "The bill does not belong to particular user");
								return new ResponseEntity<String>(entity.toString(), HttpStatus.UNAUTHORIZED);
							}
						}
						else
						{
							entity.addProperty("message", "The bill does not exist.");
							return new ResponseEntity<String>(entity.toString(), HttpStatus.NOT_FOUND);
							
						}
						
					}
					else
					{
						entity.addProperty("message", "The bill does not exist.");
						return new ResponseEntity<String>(entity.toString(), HttpStatus.NOT_FOUND);
						
					}
				}
				else
				{
					entity.addProperty("message", "The bill id cannot be null.");
					return new ResponseEntity<String>(entity.toString(), HttpStatus.BAD_REQUEST);
				}
			}					

			return new ResponseEntity<String>(entity.toString() , HttpStatus.UNAUTHORIZED);
		}

		entity.addProperty("message", "Invalid. Unable to Authenticate");	
		return new ResponseEntity<String>(entity.toString() , HttpStatus.UNAUTHORIZED);
	}
	

	//Delete a bill 
	@DeleteMapping(value = "/v1/bill/{id}")
	public ResponseEntity<?> deleteBillById(HttpServletRequest request, HttpServletResponse response, @PathVariable(value = "id") @NotBlank @NotNull String billId )
	{
		String authorization = request.getHeader("Authorization");
		JsonObject entity = new JsonObject();
		if(authorization != null && authorization.toLowerCase().startsWith("basic"))
		{
			// Authorization: Basic base64credentials
			authorization = authorization.replaceFirst("Basic ", "");

			String credentials = new String(Base64.getDecoder().decode(authorization.getBytes()));

			// authorization = username:password
			String [] userCredentials = credentials.split(":", 2);
			String email = userCredentials[0];

			String password = userCredentials[1];

			User user = userRepository.findByemail(email);
			if(user == null)
			{
				entity.addProperty("message", "User does not exist. Please enter correct Username or Password");
			}
			else if(user != null && !bCryptPasswordEncoder.matches(password, user.getPassword()))
			{
				entity.addProperty("message", "The Password is Invalid");
				return new ResponseEntity<String>(entity.toString() , HttpStatus.UNAUTHORIZED);
			}
			else
			{
				List<Bill> listOfBills = billRepository.findByUserId(user.getId());
				 UUID  uid  = null ;
		            try 
		            {
		                 uid = UUID.fromString(billId);
		                 System.out.println("Bill UUID is : ");
		            }
		            catch (Exception e)
		            {

						entity.addProperty("message", "The bill does not exist or bill Id is incorrect.");
						return new ResponseEntity<String>(entity.toString(), HttpStatus.NOT_FOUND);
		            }
		            
				if(billId != null )
				{
					if(listOfBills.size() > 0)
					{	
						Bill b = billRepositoryfindaSpecificBill.findById(billId);
						if(b==null)
						{
							entity.addProperty("message", "The bill does not exist.");
							return new ResponseEntity<String>(entity.toString(), HttpStatus.NOT_FOUND);
							
						}
						if(listOfBills.contains(b))
						{			
							billRepository.delete(b);
							return new ResponseEntity<String>(HttpStatus.NO_CONTENT);
						}			
						else
						{
							entity.addProperty("message", "The bill does not belong to particular ");
							return new ResponseEntity<String>(entity.toString(), HttpStatus.UNAUTHORIZED);
						}
					}
					else
					{
						entity.addProperty("message", "The bill does not exist.");
						return new ResponseEntity<String>(entity.toString(), HttpStatus.NOT_FOUND);
					}
				
				
				}
				else
				{
					entity.addProperty("message", "The bill is null.");
					return new ResponseEntity<String>(entity.toString(), HttpStatus.BAD_REQUEST);
					
				}
				
			}					

			return new ResponseEntity<String>(entity.toString() , HttpStatus.UNAUTHORIZED);
		}

		entity.addProperty("message", "Invalid. Unable to Authenticate");	
		return new ResponseEntity<String>(entity.toString() , HttpStatus.UNAUTHORIZED);
	}


	  
    
	public Boolean validateDate(String date) 
	{
		if (date != null || (!date.equalsIgnoreCase(""))) 
		{
			String datevalidator = "^[12]\\d{3}([- \\/.])(((0[13578]|(10|12))\\1(0[1-9]|[1-2][0-9]|3[0-1]))|(02\\1(0[1-9]|[1-2][0-9]))|((0[469]|11)\\1(0[1-9]|[1-2][0-9]|30)))$";
			//^\d{4}\-(0[1-9]|1[012])\-(0[1-9]|[12][0-9]|3[01])$
			//[12]\\d{3}

			return date.matches(datevalidator);
		} 
		else
		{
			return Boolean.FALSE;
		}

	}
}
