package com.neu.edu.controller;

import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Date;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.google.gson.JsonObject;
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
	public ResponseEntity<?> postBillByUserId(@Valid @RequestBody Bill bill, HttpServletRequest request, HttpServletResponse response)
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
				entity.addProperty("message", "Please enter correct Username or Password");
			}
			else if(user != null && !bCryptPasswordEncoder.matches(password, user.getPassword()))
			{
				entity.addProperty("message", "The Password is Invalid");
				return new ResponseEntity<String>(entity.toString() , HttpStatus.BAD_REQUEST);
			}
			else
			{		
				if( (bill.getVendor()!=null && bill.getVendor().trim().length() >0) && bill.getBilldate()!=null  && bill.getDuedate()!=null && 
					(bill.getAmountdue()>0.00 && bill.getAmountdue()< Double.MAX_VALUE)
					&& bill.getCategories().size()>0 && bill.getCategories()!= null && bill.getPaymentStatus() != null )
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
							return new ResponseEntity<Bill>(b , HttpStatus.OK);
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
					entity.addProperty("message", "vendor, bill_date, due_date, amount_due, categories or payment status cannot be empty and amount_due should be greater than 0.00");
					return new ResponseEntity<String>(entity.toString() , HttpStatus.BAD_REQUEST);
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
			String datevalidator = "^[0-9]{4}([- \\/.])(((0[13578]|(10|12))\\1(0[1-9]|[1-2][0-9]|3[0-1]))|(02\\1(0[1-9]|[1-2][0-9]))|((0[469]|11)\\1(0[1-9]|[1-2][0-9]|30)))$";
			//^\d{4}\-(0[1-9]|1[012])\-(0[1-9]|[12][0-9]|3[01])$
			

			return date.matches(datevalidator);
		} 
		else
		{
			return Boolean.FALSE;
		}

	}
}
