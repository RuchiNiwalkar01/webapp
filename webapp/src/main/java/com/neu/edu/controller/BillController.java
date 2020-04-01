package com.neu.edu.controller;

import java.io.File;
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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
import com.amazonaws.AmazonServiceException;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.DeleteObjectRequest;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.google.gson.JsonObject;
import com.neu.edu.exception.BillException;
import com.neu.edu.model.Bill;
import com.neu.edu.model.FileImage;
import com.neu.edu.model.PaymentStatus;
import com.neu.edu.model.User;
import com.neu.edu.repository.BillRepository;
import com.neu.edu.repository.BillRepositoryfindaSpecificBill;
import com.neu.edu.repository.FileRepository;
import com.neu.edu.repository.UserRepository;
import com.timgroup.statsd.StatsDClient;
import com.amazonaws.services.sns.AmazonSNS;
import com.amazonaws.services.sns.AmazonSNSAsyncClientBuilder;
import com.amazonaws.services.sns.model.PublishRequest;
import com.amazonaws.services.sns.model.Topic;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;
import com.amazonaws.services.sqs.model.AmazonSQSException;
import com.amazonaws.services.sqs.model.CreateQueueResult;
import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.ReceiveMessageRequest;
import com.amazonaws.services.sqs.model.ReceiveMessageResult;
import com.amazonaws.services.sqs.model.SendMessageRequest;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
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

	@Autowired
	FileRepository fileRepository;
	

	@Autowired
	StatsDClient statsDClient;

    final static Logger logger = LoggerFactory.getLogger(BillController.class);
    
	String pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";
	SimpleDateFormat simpleDateFormat = new SimpleDateFormat(pattern);

	@Value("${bucket.name}")
	String bucketName;
    
	//Post a bill by authenticating User
	//201 created, 400 bad request, 401 for no auth or unauthorized
	@PostMapping(value= "/v1/bill")
	public ResponseEntity<?> postBillByUserId(@Validated @RequestBody(required = false) Bill bill, HttpServletRequest request, HttpServletResponse response)
	{
		statsDClient.incrementCounter("bill.post");
		logger.info("Inside Post Bill Api");
		long start = System.currentTimeMillis();
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
				long end = System.currentTimeMillis();
				statsDClient.recordExecutionTime("postBillApiTime", (end-start));
				logger.error("User does not exist. ");
			}
			else if(user != null && !bCryptPasswordEncoder.matches(password, user.getPassword()))
			{
				entity.addProperty("message", "The Password is Invalid");
				long end = System.currentTimeMillis();
				statsDClient.recordExecutionTime("postBillApiTime", (end-start));
				logger.error("The Password is Invalid");
				return new ResponseEntity<String>(entity.toString() , HttpStatus.UNAUTHORIZED);
			}
			else
			{	
				if(bill==null)
				{
					entity.addProperty("message", "The Request Body cannot be null");
					long end = System.currentTimeMillis();
					statsDClient.recordExecutionTime("postBillApiTime", (end-start));
					logger.error("The Request Body cannot be null");
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
								long startbilldb = System.currentTimeMillis();
								billRepository.save(b);
								b.setUser(null);
								long endbilldb = System.currentTimeMillis();
								statsDClient.recordExecutionTime("PostBilldb", (endbilldb-startbilldb));
								long end = System.currentTimeMillis();
								statsDClient.recordExecutionTime("postBillApiTime", (end-start));
								logger.info("Bill Successfully Created in time : "+(end-start));
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
							long end = System.currentTimeMillis();
							statsDClient.recordExecutionTime("postBillApiTime", (end-start));
							logger.error("Please enter a valid date in YYYY-MM-DD format ");
							return new ResponseEntity<String>(entity.toString() , HttpStatus.BAD_REQUEST);
						}
					}
					else
					{
						entity.addProperty("validation", " Categories cannot be empty ");
						long end = System.currentTimeMillis();
						statsDClient.recordExecutionTime("postBillApiTime", (end-start));
						logger.error("Categories cannot be empty ");
						return new ResponseEntity<String>(entity.toString() , HttpStatus.BAD_REQUEST);
					}
				
					
				}
				else
				{
					entity.addProperty("message", "vendor, bill_date, due_date, amount_due, categories, payment status cannot be empty or Amount cannot be below 0.00");
					long end = System.currentTimeMillis();
					statsDClient.recordExecutionTime("postBillApiTime", (end-start));
					logger.error("vendor, bill_date, due_date, amount_due, categories, payment status cannot be empty or Amount cannot be below 0.00");
					return new ResponseEntity<String>(entity.toString() , HttpStatus.BAD_REQUEST);
				}
			
			}		

			return new ResponseEntity<String>(entity.toString() , HttpStatus.UNAUTHORIZED);

		}

		entity.addProperty("message", "Invalid. Unable to Authenticate");
		long end = System.currentTimeMillis();
		statsDClient.recordExecutionTime("postBillApiTime", (end-start));
		logger.error("Invalid. Unable to Authenticate");
		return new ResponseEntity<String>(entity.toString() , HttpStatus.UNAUTHORIZED);

	}


	//Get all bill by Specific User
	//200 for success, 401 no authorization
	@GetMapping(value = "/v1/bills")
	public ResponseEntity<?> getAllBillsByUserId(HttpServletRequest request, HttpServletResponse response)
	{
		statsDClient.incrementCounter("bill.get");
		logger.info("Inside Get Bill Api");
		long start = System.currentTimeMillis();
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
				long end = System.currentTimeMillis();
				statsDClient.recordExecutionTime("getBillApiTime", (end-start));
				logger.error("User does not exist. ");
			}
			else if(user != null && !bCryptPasswordEncoder.matches(password, user.getPassword()))
			{
				entity.addProperty("message", "The Password is Invalid");
				long end = System.currentTimeMillis();
				statsDClient.recordExecutionTime("getBillApiTime", (end-start));
				logger.error("The Password is Invalid");
				return new ResponseEntity<String>(entity.toString() , HttpStatus.UNAUTHORIZED);
			}
			else
			{
				List<Bill> listOfBills = billRepository.findByUserId(user.getId());
				user.setPassword(null);
				if(listOfBills.size() ==0)
				{
					entity.addProperty("message", "The bills do not exist");
					long end = System.currentTimeMillis();
					statsDClient.recordExecutionTime("getBillApiTime", (end-start));
					logger.error("The bills do not exist ");
					return new ResponseEntity<String>(entity.toString() , HttpStatus.NOT_FOUND);
				}
				entity.addProperty("message", "The bills Found");
				long end = System.currentTimeMillis();
				statsDClient.recordExecutionTime("getBillApiTime", (end-start));
				logger.info("The bills found i time : "+(end-start));
				return new ResponseEntity<List<Bill>>(listOfBills , HttpStatus.OK);
			}					

			return new ResponseEntity<String>(entity.toString() , HttpStatus.UNAUTHORIZED);
		}

		entity.addProperty("message", "Invalid. Unable to Authenticate");
		long end = System.currentTimeMillis();
		statsDClient.recordExecutionTime("getBillApiTime", (end-start));
		logger.error("Unable to Authenticate");
		return new ResponseEntity<String>(entity.toString() , HttpStatus.UNAUTHORIZED);
	}
	

	//Get Specific bill by billId
	@GetMapping(value = "/v1/bill/{id}")
	public ResponseEntity<?> getSingleBillbyId(HttpServletRequest request, HttpServletResponse response, @PathVariable(value="id" ) String billId)
	{
		statsDClient.incrementCounter("bill.getById");
		logger.info("Inside Get Bill By Id Api");
		long start = System.currentTimeMillis();
		
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
				long end = System.currentTimeMillis();
				statsDClient.recordExecutionTime("getBillByIdApiTime", (end-start));
				logger.error("User does not exist. ");
			}
			else if(user != null && !bCryptPasswordEncoder.matches(password, user.getPassword()))
			{
				entity.addProperty("message", "The Password is Invalid");
				long end = System.currentTimeMillis();
				statsDClient.recordExecutionTime("getBillByIdApiTime", (end-start));
				logger.error("The Password is Invalid");
				return new ResponseEntity<String>(entity.toString() , HttpStatus.UNAUTHORIZED);
			}
			else
			{
				 UUID  uid  = null ;
		            try 
		            {
		                 uid = UUID.fromString(billId);
		                 //System.out.println("Bill UUID is : ");
		            }
		            catch (Exception e)
		            {

						entity.addProperty("message", "The bill does not exist.");
						long end = System.currentTimeMillis();
						statsDClient.recordExecutionTime("getBillByIdApiTime", (end-start));
						logger.error("The bill does not exist");
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
							long end = System.currentTimeMillis();
							statsDClient.recordExecutionTime("getBillByIdApiTime", (end-start));
							logger.info("The bill is found in time :"+(end-start));
							return new ResponseEntity<Bill>(bill , HttpStatus.OK);
						}			
						else
						{
							entity.addProperty("message", "The bill does not belong to particular user");
							long end = System.currentTimeMillis();
							statsDClient.recordExecutionTime("getBillByIdApiTime", (end-start));
							logger.error("The bill does not belong to particular user");
							return new ResponseEntity<String>(entity.toString(), HttpStatus.UNAUTHORIZED);
						}
					}
					else
					{
						entity.addProperty("message", "The bill does not exist.");
						long end = System.currentTimeMillis();
						statsDClient.recordExecutionTime("getBillByIdApiTime", (end-start));
						logger.error("The bill does not exist");
						return new ResponseEntity<String>(entity.toString(), HttpStatus.NOT_FOUND);
					}
				}
				else
				{
					entity.addProperty("message", "The bill does not exist.");
					long end = System.currentTimeMillis();
					statsDClient.recordExecutionTime("getBillByIdApiTime", (end-start));
					logger.error("The bill does not exist");
					return new ResponseEntity<String>(entity.toString(), HttpStatus.NOT_FOUND);
				}
				
				
			}					

			return new ResponseEntity<String>(entity.toString() , HttpStatus.UNAUTHORIZED);
		}

		entity.addProperty("message", "Invalid. Unable to Authenticate");	
		long end = System.currentTimeMillis();
		statsDClient.recordExecutionTime("getBillByIdApiTime", (end-start));
		logger.error("Unable to Authenticate");
		return new ResponseEntity<String>(entity.toString() , HttpStatus.UNAUTHORIZED);
	}
	

	
	//Update a bill 
	@PutMapping(value = "/v1/bill/{id}")
	public ResponseEntity<?> updateBillById(HttpServletRequest request, HttpServletResponse response, @RequestBody(required = false) Bill bill, @PathVariable(required = true, value = "id") @NotBlank @NotNull String billId )
	{
		statsDClient.incrementCounter("bill.put");
		logger.info("Inside Update bill Api");
		long start = System.currentTimeMillis();
		
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
				long end = System.currentTimeMillis();
				statsDClient.recordExecutionTime("PutBillApiTime", (end-start));
				logger.error("User does not exist. ");
			}
			else if(user != null && !bCryptPasswordEncoder.matches(password, user.getPassword()))
			{
				entity.addProperty("message", "The Password is Invalid");
				long end = System.currentTimeMillis();
				statsDClient.recordExecutionTime("PutBillApiTime", (end-start));
				logger.error("The Password is Invalid");
				return new ResponseEntity<String>(entity.toString() ,HttpStatus.UNAUTHORIZED);
			}
			else
			{
				if(bill==null)
				{
					entity.addProperty("message", "The Request Body cannot be null");
					long end = System.currentTimeMillis();
					statsDClient.recordExecutionTime("PutBillApiTime", (end-start));
					logger.error("The Request Body cannot be null");
					return new ResponseEntity<String>(entity.toString() , HttpStatus.BAD_REQUEST);
				}
				
				List<Bill> listOfBills = billRepository.findByUserId(user.getId());
				 UUID  uid  = null ;
		            try 
		            {
		                 uid = UUID.fromString(billId);
		                 //System.out.println("Bill UUID is : ");
		            }
		            catch (Exception e)
		            {

						entity.addProperty("message", "The bill does not exist or bill Id is incorrect.");
						long end = System.currentTimeMillis();
						statsDClient.recordExecutionTime("PutBillApiTime", (end-start));
						logger.error("The bill does not exist or bill Id is incorrect.l");
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
											long startbilldb = System.currentTimeMillis();
											billRepository.save(b);
											user.setPassword(null);
											long endbilldb = System.currentTimeMillis();
											statsDClient.recordExecutionTime("PutBilldb", (endbilldb-startbilldb));
											long end = System.currentTimeMillis();
											statsDClient.recordExecutionTime("PutBillApiTime", (end-start));
											logger.info("The bill is updated in time : "+(end-start));
											return new ResponseEntity<Bill>(b , HttpStatus.OK);
											
										}		
										else
										{
											entity.addProperty("validation", " Please enter a valid date in YYYY-MM-DD format ");
											long end = System.currentTimeMillis();
											statsDClient.recordExecutionTime("PutBillApiTime", (end-start));
											logger.error("Please enter a valid date in YYYY-MM-DD format ");
											return new ResponseEntity<String>(entity.toString() , HttpStatus.BAD_REQUEST);
										}
									}
									else
									{
										entity.addProperty("validation", " categories cannot be empty ");
										long end = System.currentTimeMillis();
										statsDClient.recordExecutionTime("PutBillApiTime", (end-start));
										logger.error("categories cannot be empty  ");
										return new ResponseEntity<String>(entity.toString() , HttpStatus.BAD_REQUEST);
									}
										
									}
								else
								{
									entity.addProperty("message", "vendor, bill_date, due_date, amount_due, categories or payment status cannot be empty");
									long end = System.currentTimeMillis();
									statsDClient.recordExecutionTime("PutBillApiTime", (end-start));
									logger.error("vendor, bill_date, due_date, amount_due, categories or payment status cannot be empty");
									return new ResponseEntity<String>(entity.toString() , HttpStatus.BAD_REQUEST);
								}
								
							}			
							else
							{
								entity.addProperty("message", "The bill does not belong to particular user");
								long end = System.currentTimeMillis();
								statsDClient.recordExecutionTime("PutBillApiTime", (end-start));
								logger.error("The bill does not belong to particular user");
								return new ResponseEntity<String>(entity.toString(), HttpStatus.UNAUTHORIZED);
							}
						}
						else
						{
							entity.addProperty("message", "The bill does not exist.");
							long end = System.currentTimeMillis();
							statsDClient.recordExecutionTime("PutBillApiTime", (end-start));
							logger.error("The bill does not exist");
							return new ResponseEntity<String>(entity.toString(), HttpStatus.NOT_FOUND);
							
						}
						
					}
					else
					{
						entity.addProperty("message", "The bill does not exist.");
						long end = System.currentTimeMillis();
						statsDClient.recordExecutionTime("PutBillApiTime", (end-start));
						logger.error("The bill does not exist");
						return new ResponseEntity<String>(entity.toString(), HttpStatus.NOT_FOUND);
						
					}
				}
				else
				{
					entity.addProperty("message", "The bill id cannot be null.");
					long end = System.currentTimeMillis();
					statsDClient.recordExecutionTime("PutBillApiTime", (end-start));
					logger.error("The bill id cannot be null");
					return new ResponseEntity<String>(entity.toString(), HttpStatus.BAD_REQUEST);
				}
			}					

			return new ResponseEntity<String>(entity.toString() , HttpStatus.UNAUTHORIZED);
		}

		entity.addProperty("message", "Invalid. Unable to Authenticate");	
		long end = System.currentTimeMillis();
		statsDClient.recordExecutionTime("PutBillApiTime", (end-start));
		logger.info(" Unable to Authenticate");
		return new ResponseEntity<String>(entity.toString() , HttpStatus.UNAUTHORIZED);
	}
	

	//Delete a bill 
	@DeleteMapping(value = "/v1/bill/{id}")
	public ResponseEntity<?> deleteBillById(HttpServletRequest request, HttpServletResponse response, @PathVariable(value = "id") @NotBlank @NotNull String billId )
	{
		statsDClient.incrementCounter("bill.delete");
		logger.info("Inside delete Bill Api");
		long start = System.currentTimeMillis();
		
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
				long end = System.currentTimeMillis();
				statsDClient.recordExecutionTime("DeleteBillApiTime", (end-start));
				logger.error("User does not exist. ");
			}
			else if(user != null && !bCryptPasswordEncoder.matches(password, user.getPassword()))
			{
				entity.addProperty("message", "The Password is Invalid");
				long end = System.currentTimeMillis();
				statsDClient.recordExecutionTime("DeleteBillApiTime", (end-start));
				logger.error("The Password is Invalid");
				return new ResponseEntity<String>(entity.toString() , HttpStatus.UNAUTHORIZED);
			}
			else
			{
				List<Bill> listOfBills = billRepository.findByUserId(user.getId());
				 UUID  uid  = null ;
		            try 
		            {
		                 uid = UUID.fromString(billId);
		                 //System.out.println("Bill UUID is : ");
		            }
		            catch (Exception e)
		            {

						entity.addProperty("message", "The bill does not exist or bill Id is incorrect.");
						long end = System.currentTimeMillis();
						statsDClient.recordExecutionTime("DeleteBillApiTime", (end-start));
						logger.error("The bill does not exist or bill Id is incorrect.");
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
							long end = System.currentTimeMillis();
							statsDClient.recordExecutionTime("DeleteBillApiTime", (end-start));
							logger.error("The bill does not exist ");
							return new ResponseEntity<String>(entity.toString(), HttpStatus.NOT_FOUND);
							
						}
						if(listOfBills.contains(b))
						{			
							if(b.getFileImage()!=null)
							{
								FileImage singleFile = fileRepository.findByfileId(b.getFileImage().getFileId());
								try {
									  String fileUrl=singleFile.getUrl();
									  String fileName=fileUrl.substring(fileUrl.lastIndexOf("/") + 1);
									  AmazonS3 s3Client = AmazonS3ClientBuilder.standard().withRegion(Regions.US_EAST_1).build();
							          DeleteObjectRequest deleteAttach=new DeleteObjectRequest(this.bucketName,fileName);
							          s3Client.deleteObject(deleteAttach);
									  
								  }
								catch(AmazonServiceException e){
									  e.printStackTrace();
									  
								  }
							
								//File CurrentFile = new File(singleFile.getUrl());
								//CurrentFile.delete();
								b.setFileImage(null);
								long startbilldb = System.currentTimeMillis();
								fileRepository.delete(singleFile);
								billRepository.delete(b);
								long endbilldb = System.currentTimeMillis();
								statsDClient.recordExecutionTime("DeleteBillApiTime", (endbilldb-startbilldb));
								long end = System.currentTimeMillis();
								statsDClient.recordExecutionTime("DeleteBillApiTime", (end-start));
								logger.info("Deleted bill successfully in time : "+(end-start));
								return new ResponseEntity<String>(HttpStatus.NO_CONTENT);
							}
							long startbilldb = System.currentTimeMillis();
							billRepository.delete(b);
							long endbilldb = System.currentTimeMillis();
							statsDClient.recordExecutionTime("DeleteBillApiTime", (endbilldb-startbilldb));
							long end = System.currentTimeMillis();
							statsDClient.recordExecutionTime("DeleteBillApiTime", (end-start));
							logger.info("Deleted bill successfully in time : "+(end-start));
							return new ResponseEntity<String>(HttpStatus.NO_CONTENT);
						}			
						else
						{
							entity.addProperty("message", "The bill does not belong to particular ");
							long end = System.currentTimeMillis();
							statsDClient.recordExecutionTime("DeleteBillApiTime", (end-start));
							logger.error("The bill does not belong to particular ");
							return new ResponseEntity<String>(entity.toString(), HttpStatus.UNAUTHORIZED);
						}
					}
					else
					{
						entity.addProperty("message", "The bill does not exist.");
						long end = System.currentTimeMillis();
						statsDClient.recordExecutionTime("DeleteBillApiTime", (end-start));
						logger.error("The bill does not exist ");
						return new ResponseEntity<String>(entity.toString(), HttpStatus.NOT_FOUND);
					}
				
				
				}
				else
				{
					entity.addProperty("message", "The bill is null.");
					long end = System.currentTimeMillis();
					statsDClient.recordExecutionTime("DeleteBillApiTime", (end-start));
					logger.error("The bill is null ");
					return new ResponseEntity<String>(entity.toString(), HttpStatus.BAD_REQUEST);
					
				}
				
			}					

			return new ResponseEntity<String>(entity.toString() , HttpStatus.UNAUTHORIZED);
		}

		entity.addProperty("message", "Invalid. Unable to Authenticate");
		long end = System.currentTimeMillis();
		statsDClient.recordExecutionTime("DeleteBillApiTime", (end-start));
		logger.error("Unable to Authenticate");
		return new ResponseEntity<String>(entity.toString() , HttpStatus.UNAUTHORIZED);
	}

	  //Get Due bill by Specific User 
	//200 for success, 401 no authorization
	@GetMapping(value = "/v1/bills/due/x")
	public ResponseEntity<?> getDueBillsByUserId(HttpServletRequest request, HttpServletResponse response)
	{
		logger.info("Inside days to caulcate API");
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
				
				if(listOfBills.size() ==0)
				{
					entity.addProperty("message", "The bills do not exist");
					return new ResponseEntity<String>(entity.toString() , HttpStatus.NOT_FOUND);
				}
				
				JSONArray jsonArray = new JSONArray();
				JSONObject jsonObject = new JSONObject();
				jsonObject.put("username", user.getEmail());
				//jsonObject.put("NumOfDays", noOfDays);
				
				
//				for(int i=0 ; i<listOfBills.size() ; i++)
//				{
//					jsonArray.add(listOfBills.get(i).getId());
//					//logger.info("Entries: " + listOfBills.get(i).getId());
//					
//				}
//				jsonObject.put("bills",jsonArray );
			
				//  System.out.println("The bills " + jsonObject.get("bills"));
			   //   System.out.println();
			  	logger.info("The email address " + jsonObject.get("username"));
			     //create SQS queue and send message
			      
//			      final AmazonSQS sqs = AmazonSQSClientBuilder.defaultClient();
//
//			        try 
//			        {
//			            CreateQueueResult create_result = sqs.createQueue(QUEUE_NAME);
//			        } 
//			        catch (AmazonSQSException e) 
//			        {
//			            if (!e.getErrorCode().equals("QueueAlreadyExists")) 
//			            {
//			                throw e;
//			            }
//			        }
//
//			        String queueUrl = sqs.getQueueUrl(QUEUE_NAME).getQueueUrl();
//
//			        SendMessageRequest send_msg_request = new SendMessageRequest()
//			                .withQueueUrl(queueUrl)
//			                .withMessageBody(jsonObject.toString())
//			                .withDelaySeconds(5);
//			        sqs.sendMessage(send_msg_request);
//			       
//			     //SQS polling
//			        List<Message> messages = sqs.receiveMessage(queueUrl).getMessages();
//			        String messageReceiptHandle = messages.get(0).getBody();				        
//			        JSONParser parser = new JSONParser();
//			        try 
//			        {
//						JSONObject json = (JSONObject) parser.parse(messageReceiptHandle);
//						int num = (int) json.get("NumOfDays");
//						
//					} 
//			        catch (ParseException e) 
//			        {
//					
//						e.printStackTrace();
//					}
//			        
//			     
			        
			      //publish topic to SNS
			      AmazonSNS sns = AmazonSNSAsyncClientBuilder.standard().withRegion(Regions.US_EAST_1).build();
			      List<Topic> topics = sns.listTopics().getTopics();
			      for(Topic topic : topics)
			      {
			    	  if(topic.getTopicArn().startsWith("BillsDue"))
			    	  {
			    		  logger.info("The topic is " + topic.getTopicArn());
			    		  PublishRequest pubRequest = new PublishRequest(topic.getTopicArn(), jsonObject.toString());
					      sns.publish(pubRequest);
					      break;
			    	  }
			    	  
			      }
			     entity.addProperty("message", "Bills Due added succesfully");
				return new ResponseEntity<String>(entity.toString() , HttpStatus.OK);
			    
			}
	}
		
		logger.error("Unable to Authenticate");
		return new ResponseEntity<String>(entity.toString() , HttpStatus.UNAUTHORIZED);
	}


	  
    
	public Boolean validateDate(String date) 
	{
		if (date != null || (!date.equalsIgnoreCase(""))) 
		{
			String datevalidator = "^[12]\\d{3}([- \\/.])((((0[13578])|(1[02]))[\\-\\s]?(([0-2][0-9])|(3[01])))|(((0[469])|(11))[\\-\\s]?(([0-2][0-9])|(30)))|(02[\\-\\s]?[0-2][0-9]))$";
			//^\d{4}\-(0[1-9]|1[012])\-(0[1-9]|[12][0-9]|3[01])$
			//older one String datevalidator = "^[12]\\d{3}([- \\/.])(((0[13578]|(10|12))\\1(0[1-9]|[1-2][0-9]|3[0-1]))|(02\\1(0[1-9]|[1-2][0-9]))|((0[469]|11)\\1(0[1-9]|[1-2][0-9]|30)))$";

			//^[12]\d{3}([- \/.])((((0[13578])|(1[02]))[\-\s]?(([0-2][0-9])|(3[01])))|(((0[469])|(11))[\-\s]?(([0-2][0-9])|(30)))|(02[\-\s]?[0-2][0-9]))$
			return date.matches(datevalidator);
		} 
		
		else
		{
			return Boolean.FALSE;
		}

	}
}
