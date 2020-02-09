package com.neu.edu.model;

import java.util.Set;
import javax.persistence.CascadeType;
import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;

import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

@Entity
public class Bill {
	
	@Id 
	@GeneratedValue(generator="uuid2")
	@GenericGenerator(name="uuid2", strategy = "uuid2")
	@JsonProperty("id")
	@Column(name="id")	
	private String id;
	
	
	@JsonProperty("created_ts")
	@Column(name="created_ts")	
	private String created_ts;
	
	@JsonProperty("updated_ts")
	@Column(name="updated_ts")	
	private String updated_ts;
	
	
	@JsonProperty("owner_id")
	private String owner_id;
	
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false)
	@OnDelete(action = OnDeleteAction.CASCADE)
	@JsonIgnore
	private User user;
	

	@JsonProperty("vendor")
	@Column(name="vendor")	
	private String vendor;
	
	@JsonProperty("bill_date")
	@Column(name="bill_date")	
	private String billdate;
	
	@JsonProperty("due_date")
	@Column(name="due_date")	
	private String duedate;


	@JsonProperty("amount_due")
	@Column(name="amount_due")
	private double amountdue;
	
	
	@JsonProperty("categories")
    @ElementCollection
    @CollectionTable(name = "categories", joinColumns = @JoinColumn(name = "id"))
    private Set<String> categories;
    
	
	@JsonProperty("paymentStatus")
    @Enumerated(EnumType.STRING)
    @Column(name="paymentStatus")
    private PaymentStatus paymentStatus;
	
	@JsonProperty("attachment")
	@OneToOne(optional = true, cascade=CascadeType.ALL)
	@JoinColumn(name = "FileImageId", nullable = true)
	private FileImage fileImage;
	

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public User getUser() {
		return user;
	}

	public void setUser(User user) {
		this.user = user;
	}

	public String getCreated_ts() {
		return created_ts;
	}

	public void setCreated_ts(String created_ts) {
		this.created_ts = created_ts;
	}

	public String getUpdated_ts() {
		return updated_ts;
	}

	public void setUpdated_ts(String updated_ts) {
		this.updated_ts = updated_ts;
	}

	public String getVendor() {
		return vendor;
	}

	public void setVendor(String vendor) {
		this.vendor = vendor;
	}

	public String getBilldate() {
		return billdate;
	}

	public void setBilldate(String billdate) {
		this.billdate = billdate;
	}

	public String getDuedate() {
		return duedate;
	}

	public void setDuedate(String duedate) {
		this.duedate = duedate;
	}

	public double getAmountdue() {
		return amountdue;
	}

	public void setAmountdue(double amountdue) {
		this.amountdue = amountdue;
	}


	public Set<String> getCategories() {
		return categories;
	}

	public void setCategories(Set<String> categories) {
		this.categories = categories;
	}

	public PaymentStatus getPaymentStatus() {
		return paymentStatus;
	}

	public void setPaymentStatus(PaymentStatus paymentStatus) {
		this.paymentStatus = paymentStatus;
	}
	
	

	public String getOwner_id() {
		return owner_id;
	}

	public void setOwner_id(String owner_id) {
		this.owner_id = owner_id;
	}

	
	
	public FileImage getFileImage() {
		return fileImage;
	}

	public void setFileImage(FileImage fileImage) {
		this.fileImage = fileImage;
	}

	@Override
	public String toString() {
		return "Bill [id=" + id + ", created_ts=" + created_ts + ", updated_ts=" + updated_ts + ", owner_id=" + owner_id
				+ ", user=" + user + ", vendor=" + vendor + ", billdate=" + billdate + ", duedate=" + duedate
				+ ", amountdue=" + amountdue + ", categories=" + categories + ", paymentStatus=" + paymentStatus
				+ ", fileImage=" + fileImage + "]";
	}


    
	
    
	
}
