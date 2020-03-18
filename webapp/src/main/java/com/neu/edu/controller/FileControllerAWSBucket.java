package com.neu.edu.controller;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

import org.apache.commons.codec.digest.DigestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.DeleteObjectRequest;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.PutObjectResult;
import com.google.gson.JsonObject;
import com.neu.edu.model.Bill;
import com.neu.edu.model.FileImage;
import com.neu.edu.model.User;
import com.neu.edu.repository.BillRepository;
import com.neu.edu.repository.BillRepositoryfindaSpecificBill;
import com.neu.edu.repository.FileRepository;
import com.neu.edu.repository.UserRepository;
import com.timgroup.statsd.StatsDClient;

@Profile("dev")
@RestController
public class FileControllerAWSBucket {

	

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


	@Autowired
	StatsDClient statsDClient;

    final static Logger logger = LoggerFactory.getLogger(FileControllerAWSBucket.class);
    
	String pattern = "yyyy-MM-dd";
	SimpleDateFormat simpleDateFormat = new SimpleDateFormat(pattern);

	String patterndf = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";
	SimpleDateFormat df = new SimpleDateFormat(patterndf);

	
	String checksum = null;
	//private static final String USER_HOME=System.getProperty("user.home");

	@Value("${bucket.name}")
	String bucketName;
	
	private String clientRegion = "us-east-1";
    private String endPointUrl="https://s3.amazonaws.com";

    
  //Post a file for particular Bill for particular User
  	@SuppressWarnings("unused")
  	@PostMapping(value = "/v1/bill/{id}/file")
  	public ResponseEntity<?> uploadFile(@RequestParam(required = false) MultipartFile file,  @PathVariable(value="id" ) String billId,HttpServletRequest request, HttpServletResponse response) throws Exception
  	{
  		//		String filePath = USER_HOME+"/Desktop/Images";
  		//        String fileName = file.getOriginalFilename();
  		//        String NewPath = filePath + fileName;
  		statsDClient.incrementCounter("file.post");
		logger.info("Inside Post File Api");
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
				statsDClient.recordExecutionTime("postFileApiTime", (start-end));
  				logger.error("User does not exist. ");
  			}
  			else if(user != null && !bCryptPasswordEncoder.matches(password, user.getPassword()))
  			{
  				entity.addProperty("message", "The Password is Invalid");
  				long end = System.currentTimeMillis();
				statsDClient.recordExecutionTime("postFileApiTime", (start-end));
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
  					statsDClient.recordExecutionTime("postFileApiTime", (start-end));
  					logger.error("The bill does not exist.");
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
  								long end = System.currentTimeMillis();
  								statsDClient.recordExecutionTime("postFileApiTime", (start-end));
  								logger.error("Please select a file");
  								return new ResponseEntity<String>(entity.toString(), HttpStatus.BAD_REQUEST);
  							}

  							if(!file.getContentType().contains("image/png") && !file.getContentType().contains("image/jpg") && !file.getContentType().contains("image/jpeg") && !file.getContentType().contains("application/pdf"))
  							{
  								entity.addProperty("message", "Incorrect File Format");
  								long end = System.currentTimeMillis();
  								statsDClient.recordExecutionTime("postFileApiTime", (start-end));
  								logger.error("Incorrect File Format");
  								return new ResponseEntity<String>(entity.toString(), HttpStatus.BAD_REQUEST);
  							}

  							if(bill.getFileImage() != null)
  							{
  								
  								entity.addProperty("message", "File already exists for particular bill");
  								long end = System.currentTimeMillis();
  								statsDClient.recordExecutionTime("postFileApiTime", (start-end));
  								logger.error("File already exists for particular bill");
  								return new ResponseEntity<String>(entity.toString(), HttpStatus.BAD_REQUEST);
  							}

  							FileImage fileImage = new FileImage();
  							fileImage.setFileName(file.getOriginalFilename());
  							//System.out.println("File_Name is : "+file.getOriginalFilename());
  							//String newGeneratedFile = generateFileName(file);
  							//System.out.println("photonewName : "+newGeneratedFile);
  							String filePath = uploadnewFile(file.getOriginalFilename());
  							String fileUrl = null;
  						     try {
						    	 
					    	     AmazonS3 s3Client = AmazonS3ClientBuilder.standard().withRegion(Regions.US_EAST_1).build();
					    	     fileUrl = endPointUrl + "/" + bucketName + "/" + filePath;
					    	   //  String md5 = new String(org.apache.commons.codec.binary.Base64.encodeBase64(DigestUtils.md5(fileUrl.getBytes())));
					    	     ObjectMetadata metadata = new ObjectMetadata();
					    	     metadata.setContentType(file.getContentType());
					    	     metadata.setContentLength(file.getSize());
					    	    // metadata.setContentMD5(md5);
		                         PutObjectRequest p = new PutObjectRequest(this.bucketName , filePath , file.getInputStream(), metadata);
		                         p.withCannedAcl(CannedAccessControlList.Private);
		                         s3Client.putObject(p);
		                         new PutObjectResult();
		                        
		                         ObjectMetadata obj = s3Client.getObjectMetadata(this.bucketName,filePath);
		                       //  fileImage.setMd5hash(obj.getContentMD5());
		                         fileImage.setSize(obj.getContentLength());
		                         fileImage.setLastModifiedTime(obj.getLastModified().toString());
		                         fileImage.setContentType(obj.getContentType());
		                         
		                      
					    	  
							} 
						        catch (AmazonServiceException e) {
								System.out.println("Error in uploading");
								e.printStackTrace();
								logger.error("Error in uploading file. Key file should be present");
								return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Error in uploading file. Key file should be present");
								
							}
  							
  							fileImage.setUrl(fileUrl);     
  							fileImage.setUploadDate(simpleDateFormat.format(new Date()).toString());
  							//Meta Data information
//  							String fname = StringUtils.cleanPath(file.getOriginalFilename());
//  							Path path = Paths.get(filePath);
//  							BasicFileAttributes attributes = Files.readAttributes(path, BasicFileAttributes.class);
//  							String creationtime = df.format(attributes.creationTime().toMillis());
//  							String lastAccessTime = df.format(attributes.lastAccessTime().toMillis());
//  							String lastModifiedTime = df.format(attributes.lastModifiedTime().toMillis());
//  							
//  							Long size = attributes.size();
//  							//String contentType = Files.probeContentType(path);
//  							fileImage.setContentType(file.getContentType());
//  							fileImage.setSize(size);
//  							fileImage.setCreationtime(creationtime);
//  							fileImage.setLastAccessTime(lastAccessTime);
//  							fileImage.setLastModifiedTime(lastModifiedTime);
//  							fileImage.setMd5hash(checksum);
//  							fileImage.setFileOwner(Files.getOwner(path).getName());
  							long startfiledb = System.currentTimeMillis();
  							fileRepository.save(fileImage);
  							bill.setFileImage(fileImage);
  							billRepository.save(bill);
  							long endfiledb = System.currentTimeMillis();
  							statsDClient.recordExecutionTime("DbPostFileApiTime", (startfiledb-endfiledb));
  							long end = System.currentTimeMillis();
  							statsDClient.recordExecutionTime("postFileApiTime", (start-end));
  							logger.info("File uploaded in time : "+(start-end));
  							return new ResponseEntity<FileImage>(fileImage , HttpStatus.CREATED);
  						}	
  						
  						else
  						{
  							entity.addProperty("message", "The bill does not belong to particular user");
  							long end = System.currentTimeMillis();
  							statsDClient.recordExecutionTime("postFileApiTime", (start-end));
  							logger.error("The bill does not belong to particular user");
  							return new ResponseEntity<String>(entity.toString(), HttpStatus.UNAUTHORIZED);
  						}
  					}
  					else
  					{
  						entity.addProperty("message", "The bill does not exist.");
  						long end = System.currentTimeMillis();
  						statsDClient.recordExecutionTime("postFileApiTime", (start-end));
  						logger.error("The bill does not exist.");
  						return new ResponseEntity<String>(entity.toString(), HttpStatus.NOT_FOUND);
  					}
  				}
  				else
  				{
  					entity.addProperty("message", "The bill does not exist.");
  					long end = System.currentTimeMillis();
  					statsDClient.recordExecutionTime("postFileApiTime", (start-end));
  					logger.error("The bill does not exist.");
  					return new ResponseEntity<String>(entity.toString(), HttpStatus.NOT_FOUND);
  				}


  			}					

  			return new ResponseEntity<String>(entity.toString() , HttpStatus.UNAUTHORIZED);
  		}

  		entity.addProperty("message", "Invalid. Unable to Authenticate");	
  		long end = System.currentTimeMillis();
		statsDClient.recordExecutionTime("postFileApiTime", (start-end));
  		logger.error("Unable to Authenticate");
  		return new ResponseEntity<String>(entity.toString() , HttpStatus.UNAUTHORIZED);
  	}

  	public String generateFileName(MultipartFile file) {

//
//  		String uploadDir = System.getProperty("user.home") + "/Desktop/Images/";
//  		File f = new File(uploadDir);
//  		return uploadDir + file.getOriginalFilename().replace(" ", "_");
  		 return new Date().getTime() + "-" + file.getOriginalFilename().replace(" ", "_");
  	}
  	public String uploadnewFile(String filePath) throws Exception {

  		try
  		{
  			String a = UUID.randomUUID().toString();
  			filePath = filePath +a;
  			//String filePath = USER_HOME+"/Desktop/ImagesFolder";
  			//String fileName = file.getOriginalFilename();
  			//String NewPath = filePath + fileName;
//  			File file = new File(filePath);
//  			FileOutputStream fos = new FileOutputStream(file);
//  			fos.write(multipartFile.getBytes());
//  			fos.close();
//  		
//  			//Use MD5 algorithm
//  			MessageDigest md5Digest = MessageDigest.getInstance("MD5");
//  			 
//  			//Get the checksum
//  			 checksum = getFileChecksum(md5Digest, file);
//  			 
//  			//see checksum
//  			System.out.println(checksum);
  			
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
  		statsDClient.incrementCounter("file.get");
		logger.info("Inside Get File Api");
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
  				statsDClient.recordExecutionTime("getFileApiTime", (start-end));
  				logger.error("User does not exist. ");
  			}
  			else if(user != null && !bCryptPasswordEncoder.matches(password, user.getPassword()))
  			{
  				entity.addProperty("message", "The Password is Invalid");
  				long end = System.currentTimeMillis();
  				statsDClient.recordExecutionTime("getFileApiTime", (start-end));
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
  							try
  							{
  								String abc = bill.getFileImage().getFileId();
  								
  							}
  							catch (Exception e) 
  							{
  								entity.addProperty("message", "The file does not exist for this bill");
  								long end = System.currentTimeMillis();
  				  				statsDClient.recordExecutionTime("getFileApiTime", (start-end));
  								logger.error("The file does not exist for this bill");
  								return new ResponseEntity<String>(entity.toString(), HttpStatus.NOT_FOUND);
  							}
  							
  							//user.setPassword(null);
  							FileImage singleFile = fileRepository.findByfileId(fileId);
  							if(singleFile == null)
  							{
  								entity.addProperty("message", "The file does not exist");
  								long end = System.currentTimeMillis();
  				  				statsDClient.recordExecutionTime("getFileApiTime", (start-end));
  								logger.error("The file does not exist");
  								return new ResponseEntity<String>(entity.toString(), HttpStatus.NOT_FOUND);
  							}
  							if(bill.getFileImage().getFileId().equals(singleFile.getFileId()))
  							{
  								long end = System.currentTimeMillis();
  				  				statsDClient.recordExecutionTime("getFileApiTime", (start-end));
  								logger.info("The file successfuly retrieved in time : "+(start-end));
  								return new ResponseEntity<FileImage>(singleFile , HttpStatus.OK);
  							}
  							entity.addProperty("message", "The file cannot be viewed as it does not belong to this particular bill");
  							long end = System.currentTimeMillis();
  			  				statsDClient.recordExecutionTime("getFileApiTime", (start-end));
  							logger.error("The file cannot be viewed as it does not belong to this particular bill");
  							return new ResponseEntity<String>(entity.toString(), HttpStatus.UNAUTHORIZED);

  						}			
  						
  						else
  						{
  							entity.addProperty("message", "The bill does not belong to particular user");
  							long end = System.currentTimeMillis();
  			  				statsDClient.recordExecutionTime("getFileApiTime", (start-end));
  							logger.error("The bill does not belong to particular user");
  							return new ResponseEntity<String>(entity.toString(), HttpStatus.UNAUTHORIZED);
  						}
  					}
  					else
  					{
  						entity.addProperty("message", "The bill does not exist.");
  						long end = System.currentTimeMillis();
  		  				statsDClient.recordExecutionTime("getFileApiTime", (start-end));
  						logger.error("The bill does not exist.");
  						return new ResponseEntity<String>(entity.toString(), HttpStatus.NOT_FOUND);
  					}
  				}
  				else
  				{
  					entity.addProperty("message", "The bill does not exist.");
  					long end = System.currentTimeMillis();
  	  				statsDClient.recordExecutionTime("getFileApiTime", (start-end));
  					logger.error("The bill does not exist.");
  					return new ResponseEntity<String>(entity.toString(), HttpStatus.NOT_FOUND);
  				}


  			}					

  			return new ResponseEntity<String>(entity.toString() , HttpStatus.UNAUTHORIZED);
  		}

  		entity.addProperty("message", "Invalid. Unable to Authenticate");	
		long end = System.currentTimeMillis();
		statsDClient.recordExecutionTime("getFileApiTime", (start-end));
  		logger.error("Unable to Authenticate");
  		return new ResponseEntity<String>(entity.toString() , HttpStatus.UNAUTHORIZED);
  	}



  	//Delete a file 
  	@DeleteMapping(value = "/v1/bill/{id}/file/{fileId}")
  	public ResponseEntity<?> deleteBillById(HttpServletRequest request, @PathVariable(value="fileId" ) String fileId , @PathVariable(value = "id") @NotBlank @NotNull String billId )
  	{
  		statsDClient.incrementCounter("file.delete");
		logger.info("Inside Delete File Api");
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
				statsDClient.recordExecutionTime("deleteFileApiTime", (start-end));
  				logger.error("User does not exist. ");
  			}
  			else if(user != null && !bCryptPasswordEncoder.matches(password, user.getPassword()))
  			{
  				entity.addProperty("message", "The Password is Invalid");
  				long end = System.currentTimeMillis();
				statsDClient.recordExecutionTime("deleteFileApiTime", (start-end));
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
  					statsDClient.recordExecutionTime("deleteFileApiTime", (start-end));
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
  							statsDClient.recordExecutionTime("deleteFileApiTime", (start-end));
  							logger.error("The bill does not exist ");
  							return new ResponseEntity<String>(entity.toString(), HttpStatus.NOT_FOUND);

  						}
  						if(listOfBills.contains(b))
  						{		

  							try
  							{
  								String abc = b.getFileImage().getFileId();
  								
  							}
  							catch (Exception e) 
  							{
  								entity.addProperty("message", "The file does not exist for this bill");
  								long end = System.currentTimeMillis();
  								statsDClient.recordExecutionTime("deleteFileApiTime", (start-end));
  								logger.error("The file does not exist for this bill ");
  								return new ResponseEntity<String>(entity.toString(), HttpStatus.NOT_FOUND);
  							}
  							FileImage singleFile = fileRepository.findByfileId(fileId);
  							if(singleFile == null)
  							{
  								entity.addProperty("message", "The file does not exist");
  								long end = System.currentTimeMillis();
  								statsDClient.recordExecutionTime("deleteFileApiTime", (start-end));
  								logger.error("The file does not exist");
  								return new ResponseEntity<String>(entity.toString(), HttpStatus.NOT_FOUND);
  							}
  							if(b.getFileImage().getFileId().equals(singleFile.getFileId()))
  							{
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
  						//	File CurrentFile = new File(singleFile.getUrl());
  						//	CurrentFile.delete();
  							b.setFileImage(null);
  							long startfiledb = System.currentTimeMillis();
  							fileRepository.delete(singleFile);
  							long endfiledb = System.currentTimeMillis();
  							statsDClient.recordExecutionTime("deleteFileApiTime", (startfiledb-endfiledb));
  							long end = System.currentTimeMillis();
  							statsDClient.recordExecutionTime("deleteFileApiTime", (start-end));
  							logger.info("The file is deleted in time : "+(start-end));
  							return new ResponseEntity<>(singleFile , HttpStatus.NO_CONTENT);

  							}
  							entity.addProperty("message", "The file cannot be deleted as it does not belong to this particular bill");
  							long end = System.currentTimeMillis();
  							statsDClient.recordExecutionTime("deleteFileApiTime", (start-end));
  							logger.error("The file cannot be deleted as it does not belong to this particular bill");
  							return new ResponseEntity<String>(entity.toString(), HttpStatus.UNAUTHORIZED);
  						}		
  						
  						else
  						{
  							entity.addProperty("message", "The bill does not belong to particular ");
  							long end = System.currentTimeMillis();
  							statsDClient.recordExecutionTime("deleteFileApiTime", (start-end));
  							logger.error("The bill does not belong to particular ");
  							return new ResponseEntity<String>(entity.toString(), HttpStatus.UNAUTHORIZED);
  						}
  					}
  					else
  					{
  						entity.addProperty("message", "The bill does not exist.");
  						long end = System.currentTimeMillis();
  						statsDClient.recordExecutionTime("deleteFileApiTime", (start-end));
  						logger.error("The bill does not exist.");
  						return new ResponseEntity<String>(entity.toString(), HttpStatus.NOT_FOUND);
  					}


  					
  				}
  				else
  				{
  					entity.addProperty("message", "The bill is null.");
  					long end = System.currentTimeMillis();
  					statsDClient.recordExecutionTime("deleteFileApiTime", (start-end));
  					logger.error("The bill is null.");
  					return new ResponseEntity<String>(entity.toString(), HttpStatus.BAD_REQUEST);

  				}

  			}					

  			return new ResponseEntity<String>(entity.toString() , HttpStatus.UNAUTHORIZED);
  		}

  		entity.addProperty("message", "Invalid. Unable to Authenticate");
  		long end = System.currentTimeMillis();
		statsDClient.recordExecutionTime("deleteFileApiTime", (start-end));
  		logger.error("Unable to Authenticate.");
  		return new ResponseEntity<String>(entity.toString() , HttpStatus.UNAUTHORIZED);
  	}


    
  	private static String getFileChecksum(MessageDigest digest, File file) throws IOException
	{
	    //Get file input stream for reading the file content
	    FileInputStream fis = new FileInputStream(file);
	     
	    //Create byte array to read data in chunks
	    byte[] byteArray = new byte[1024];
	    int bytesCount = 0; 
	      
	    //Read file data and update in message digest
	    while ((bytesCount = fis.read(byteArray)) != -1) {
	        digest.update(byteArray, 0, bytesCount);
	    };
	     
	    //close the stream; We don't need it now.
	    fis.close();
	     
	    //Get the hash's bytes
	    byte[] bytes = digest.digest();
	     
	    //This bytes[] has bytes in decimal format;
	    //Convert it to hexadecimal format
	    StringBuilder sb = new StringBuilder();
	    for(int i=0; i< bytes.length ;i++)
	    {
	        sb.append(Integer.toString((bytes[i] & 0xff) + 0x100, 16).substring(1));
	    }
	     
	    //return complete hash
	   return sb.toString();
	}
	
  	
  	
    
    
    
    
    
    
    
    
    
    
    
    
}
