package com.neu.edu.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import org.hibernate.annotations.GenericGenerator;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
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
	

	@JsonIgnore
	@Column(name="lastModifiedTime")	
	private String lastModifiedTime;
	
	@JsonIgnore
	@Column(name="size")	
	private long size;
	
	@JsonIgnore
	@Column(name="contentType")	
	private String contentType;

	
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

	public String getLastModifiedTime() {
		return lastModifiedTime;
	}

	public void setLastModifiedTime(String lastModifiedTime) {
		this.lastModifiedTime = lastModifiedTime;
	}

	public long getSize() {
		return size;
	}

	public void setSize(long size) {
		this.size = size;
	}

	public String getContentType() {
		return contentType;
	}

	public void setContentType(String contentType) {
		this.contentType = contentType;
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
