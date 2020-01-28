package com.neu.edu.controller;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.neu.edu.model.User;
import com.neu.edu.repository.UserRepository;

@RestController
public class UserController {

	@Autowired
	UserRepository userRepository;

	@Autowired
	BCryptPasswordEncoder bCryptPasswordEncoder;

	String pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";
	SimpleDateFormat simpleDateFormat = new SimpleDateFormat(pattern);

	//create a User
	@PostMapping(value="/v1/user")
	public ResponseEntity<?> createUser(@Valid @RequestBody User user)
	{
		if(userRepository.findByemail(user.getEmail()) != null)
		{
			JsonObject entity = new JsonObject();
			entity.addProperty("message", "User already exists");
			return new ResponseEntity<String>(entity.toString(), HttpStatus.BAD_REQUEST);
		}
	
		
		String dateFormat = simpleDateFormat.format(new Date());
		if( (user.getFirstName()!=null && user.getFirstName().trim().length() >0) && (user.getLastName()!=null && user.getLastName().trim().length() >0) && user.getEmail()!=null && user.getPassword()!=null)
		{
			if(validatePassword(user.getPassword()) && validateEmail(user.getEmail()))
			{
				User u = new User();
				u.setFirstName(user.getFirstName());
				u.setLastName(user.getLastName());
				u.setPassword(BCrypt.hashpw(user.getPassword(), BCrypt.gensalt()));
				u.setEmail(user.getEmail());		
				u.setAccount_created(dateFormat.toString());
				u.setAccount_updated(dateFormat.toString());
				userRepository.save(u);
				u.setPassword(null);
				JsonObject entity = new JsonObject();
				return new ResponseEntity<User>(u, HttpStatus.CREATED);	
			}
			 
			JsonObject entity = new JsonObject();
			entity.addProperty("Validation Error", "Please input correct email id or a strong Password with atleast 8 chars including 1 number and 1 special char");
			return new ResponseEntity<String>(entity.toString(), HttpStatus.BAD_REQUEST);
		}

		JsonObject entity = new JsonObject();
		entity.addProperty("message", "Email, FirstName, LastName and Password all four fields cannot be null or First and Last Name cannot be blank");
		return new ResponseEntity<String>(entity.toString(), HttpStatus.BAD_REQUEST);
	}


	//get user
	@GetMapping(value="/v1/user/self")
	public ResponseEntity<?> getUser(HttpServletRequest request, HttpServletResponse response)
	{
		HttpHeaders responseHeaders = new HttpHeaders();
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
			}
			else
			{
				user.setPassword(null);
				//String jsonUser=new GsonBuilder().setPrettyPrinting().create().toJson(user);
				responseHeaders.set("MyResponseHeader", "MyValue");
				return new ResponseEntity<User>(user , HttpStatus.OK);
			}					

			return new ResponseEntity<String>(entity.toString() , HttpStatus.UNAUTHORIZED);

		}

		entity.addProperty("message", "Invalid. Unable to Authenticate");	
		return new ResponseEntity<String>(entity.toString() , HttpStatus.UNAUTHORIZED);
	}

	//Update User
	@PutMapping(value = "/v1/user/self")
	public ResponseEntity<String> updateUser(@Valid @RequestBody User user, HttpServletRequest request, HttpServletResponse response)
	{
		String authorization = request.getHeader("Authorization");
		JsonObject entity = new JsonObject();
		String dateFormat = simpleDateFormat.format(new Date());
		if(authorization != null && authorization.toLowerCase().startsWith("basic"))
		{
			// Authorization: Basic base64credentials
			authorization = authorization.replaceFirst("Basic ", "");
			String credentials = new String(Base64.getDecoder().decode(authorization.getBytes()));
			// authorization = username:password
			String [] userCredentials = credentials.split(":", 2);
			String email = userCredentials[0];
			String password = userCredentials[1];		
			User u = userRepository.findByemail(email);
			if(u == null)
			{
				entity.addProperty("message", "Please enter correct Username or Password");
			}
			else if(u != null && !bCryptPasswordEncoder.matches(password, u.getPassword()))
			{
				entity.addProperty("message", "The Password is Invalid");
			}
			else
			{ 
				if((user.getFirstName()!=null && user.getFirstName().trim().length() >0) && (user.getLastName()!=null && user.getLastName().trim().length() >0)  && user.getEmail()!=null && user.getPassword()!=null)
				{
					//update firstname, lastname, password here
					if(user.getEmail().equals(u.getEmail()))
					{
						
						if(validatePassword(user.getPassword()) && validateEmail(user.getEmail()))
						{
							String pw_hash = BCrypt.hashpw(user.getPassword(), BCrypt.gensalt());
							u.setFirstName(user.getFirstName());
							u.setLastName(user.getLastName());
							u.setPassword(pw_hash);
							u.setAccount_updated(dateFormat.toString());
							userRepository.save(u);
							return new ResponseEntity<String>(HttpStatus.NO_CONTENT);
						}
						else
						{
							//JsonObject jsonObject = new JsonObject();
							entity.addProperty("Validation Error", "Please input correct email id or a strong Password with atleast 8 chars including 1 number and 1 special char");
							return new ResponseEntity<String>(entity.toString(), HttpStatus.BAD_REQUEST);
						}
						
					}
					else if(userRepository.findByemail(user.getEmail())==null)
					{
						//JsonObject jsonObject = new JsonObject();
						entity.addProperty("Message", "User cannot update email ");
						return new ResponseEntity<String>(entity.toString(), HttpStatus.BAD_REQUEST);
					}
					else if(userRepository.findByemail(user.getEmail())!=null && !user.getEmail().equals(u.getEmail()))
					{
						//JsonObject jsonObject = new JsonObject();
						entity.addProperty("Message", "user cannot update information of another user");
						return new ResponseEntity<String>(entity.toString(), HttpStatus.BAD_REQUEST);
					}
					else
					{
						entity.addProperty("Message", "Email cannot be updated ");
						return new ResponseEntity<String>(entity.toString(), HttpStatus.BAD_REQUEST);
					}
				}
				else
				{
					//JsonObject jsonObject = new JsonObject();
					entity.addProperty("message", "Email, FirstName, LastName and Password all four fields cannot be null or First and Last Name cannot be blank");
					return new ResponseEntity<String>(entity.toString(), HttpStatus.BAD_REQUEST);
				}
				
			}
			return new ResponseEntity<String>(entity.toString() , HttpStatus.UNAUTHORIZED);

		}
		
		entity.addProperty("message", "Invalid. Unable to Authenticate");	
		return new ResponseEntity<String>(entity.toString() , HttpStatus.UNAUTHORIZED);
	}
	
	
	public Boolean validatePassword(String password) 
	{
		if (password != null || (!password.equalsIgnoreCase(""))) 
		{
			String pattern = "^(?=.*?[A-Z])(?=(.*[a-z]){1,})(?=(.*[\\d]){1,})(?=(.*[\\W]){1,})(?!.*\\s).{9,16}$";
			return (password.matches(pattern));
		} 
		else 
		{
			return Boolean.FALSE;
		}

	}

	public Boolean validateEmail(String email) 
	{
		if (email != null || (!email.equalsIgnoreCase(""))) 
		{
			String emailvalidator = "^[a-zA-Z0-9_+&*-]+(?:\\." + "[a-zA-Z0-9_+&*-]+)*@" + "(?:[a-zA-Z0-9-]+\\.)+[a-z"
					+ "A-Z]{2,7}$";

			return email.matches(emailvalidator);
		} 
		else
		{
			return Boolean.FALSE;
		}

	}
}

