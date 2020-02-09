package com.neu.edu.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import org.hibernate.annotations.GenericGenerator;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

@Entity
public class FileImage {

	
	@JsonProperty("file_name")
	@Column(name="file_name")	
	private String fileName;
	
	
	@Id 
	@GeneratedValue(generator="uuid2")
	@GenericGenerator(name="uuid2", strategy = "uuid2")
	@JsonProperty("id")
	@Column(name="fileId")	
	private String fileId;


	@JsonProperty("url")
	@Column(name="url")	
	private String url;
	
	
	@JsonProperty("upload_date")
	@Column(name="upload_date")	
	private String uploadDate;

	public String getFileName() {
		return fileName;
	}

	public void setFileName(String fileName) {
		this.fileName = fileName;
	}



	public String getFileId() {
		return fileId;
	}

	public void setFileId(String fileId) {
		this.fileId = fileId;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public String getUploadDate() {
		return uploadDate;
	}

	public void setUploadDate(String uploadDate) {
		this.uploadDate = uploadDate;
	}
	
	

	public FileImage()
	{
		
	}

	
	
	
	@Override
	public String toString() {
		return "FileImage [fileName=" + fileName + ", fileId=" + fileId + ", url=" + url + ", uploadDate=" + uploadDate
				+ "]";
	}


	
	
}
