package com.neu.edu.controller;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.google.gson.JsonObject;
import com.neu.edu.model.Bill;
import com.neu.edu.model.FileImage;
import com.neu.edu.model.User;
import com.neu.edu.repository.BillRepository;
import com.neu.edu.repository.BillRepositoryfindaSpecificBill;
import com.neu.edu.repository.FileRepository;
import com.neu.edu.repository.UserRepository;

@RestController
public class FileController {


	@Autowired
	FileRepository fileRepository;

	@Autowired
	BillRepository billRepository;

	@Autowired
	BillRepositoryfindaSpecificBill billRepositoryfindaSpecificBill;

	@Autowired
	UserRepository userRepository;

	@Autowired
	private BCryptPasswordEncoder bCryptPasswordEncoder;

	String pattern = "yyyy-MM-dd";
	SimpleDateFormat simpleDateFormat = new SimpleDateFormat(pattern);

	//private static final String USER_HOME=System.getProperty("user.home");

	//Post a file for particular Bill for particular User
	@SuppressWarnings("unused")
	@PostMapping(value = "/v1/bill/{id}/file")
	public ResponseEntity<?> uploadFile(@RequestParam(required = false) MultipartFile file,  @PathVariable(value="id" ) String billId,HttpServletRequest request, HttpServletResponse response) throws Exception
	{
		//		String filePath = USER_HOME+"/Desktop/Images";
		//        String fileName = file.getOriginalFilename();
		//        String NewPath = filePath + fileName;


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
					//System.out.println("Bill UUID is : ");
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
							
							//	user.setPassword(null);
							if(file == null)
							{
								entity.addProperty("message", "Please select a file");
								return new ResponseEntity<String>(entity.toString(), HttpStatus.BAD_REQUEST);
							}

							if(!file.getContentType().contains("image/png") && !file.getContentType().contains("image/jpg") && !file.getContentType().contains("image/jpeg") && !file.getContentType().contains("image/pdf"))
							{
								entity.addProperty("message", "Incorrect File Format");
								return new ResponseEntity<String>(entity.toString(), HttpStatus.BAD_REQUEST);
							}

							if(bill.getFileImage() != null)
							{
								
								entity.addProperty("message", "File already exists for particular bill");
								return new ResponseEntity<String>(entity.toString(), HttpStatus.BAD_REQUEST);
							}

							
							FileImage fileImage = new FileImage();
							fileImage.setFileName(file.getOriginalFilename());
							//System.out.println("File_Name is : "+file.getOriginalFilename());
							String newGeneratedFile = generateFileName(file);
							//System.out.println("photonewName : "+newGeneratedFile);
							String filePath = uploadnewFile(file,newGeneratedFile);
							fileImage.setUrl(filePath);                 

							fileImage.setUploadDate(simpleDateFormat.format(new Date()).toString());
							fileRepository.save(fileImage);
							bill.setFileImage(fileImage);
							billRepository.save(bill);
							return new ResponseEntity<FileImage>(fileImage , HttpStatus.CREATED);
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

	public String generateFileName(MultipartFile file) {


		String uploadDir = System.getProperty("user.home") + "/Desktop/Images/";
		File f = new File(uploadDir);
		return uploadDir + file.getOriginalFilename().replace(" ", "_");
	}
	public String uploadnewFile(MultipartFile multipartFile,String filePath) throws Exception {

		try
		{
			String a = UUID.randomUUID().toString();
			filePath = filePath +a;
			//String filePath = USER_HOME+"/Desktop/ImagesFolder";
			//String fileName = file.getOriginalFilename();
			//String NewPath = filePath + fileName;
			File file = new File(filePath);
			FileOutputStream fos = new FileOutputStream(file);
			fos.write(multipartFile.getBytes());
			fos.close();
		}
		catch (Exception e) 
		{
			throw e;
		}

		return filePath;

	}


	//get a file for particular Bill for particular User
	@GetMapping(value = "/v1/bill/{id}/file/{fileId}")
	public ResponseEntity<?> getSingleBillbyId(HttpServletRequest request, @PathVariable(value="fileId" ) String fileId, @PathVariable(value="id" ) String billId)
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
					//System.out.println("Bill UUID is : ");
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
							//user.setPassword(null);
							FileImage singleFile = fileRepository.findByfileId(fileId);
							if(singleFile == null)
							{
								entity.addProperty("message", "The file does not exist");
								return new ResponseEntity<String>(entity.toString(), HttpStatus.NOT_FOUND);
							}
							if(bill.getFileImage().getFileId().equals(singleFile.getFileId()))
							{
								return new ResponseEntity<FileImage>(singleFile , HttpStatus.OK);
							}
							entity.addProperty("message", "The file cannot be viewed as it does not belong to this particular bill");
							return new ResponseEntity<String>(entity.toString(), HttpStatus.UNAUTHORIZED);

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


}
