package com.neu.edu.model;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import org.hibernate.annotations.GenericGenerator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

@Entity
public class User {

	@Id 
	@GeneratedValue(generator="uuid2")
	@GenericGenerator(name="uuid2", strategy = "uuid2")
	@Column(name="id")	
	private String id;
	
	@JsonProperty("first_name")
	@Column(name="first_name")	
	private String firstName;
	
	@JsonProperty("last_name")
	@Column(name="last_name")	
	private String lastName;
	
	@JsonProperty("password")
	@JsonInclude(Include.NON_NULL)
	@Column(name = "password")
	private String password;
	
	@JsonProperty("email_address")
	@Column(name = "email_address")
	private String email;
	
	@JsonProperty("account_created")
	@Column(name = "account_created")
	private String account_created;
	
	
	@JsonProperty("account_updated")
	@Column(name = "account_updated")
	private String account_updated;

	
	public User()
	{
		
	}
	
	public User(String id, String email, String password)
	{
		this.id = id;
		this.email = email;
		this.password = password;
	}
	
	
	//getter setters
	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getFirstName() {
		return firstName;
	}

	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}

	public String getLastName() {
		return lastName;
	}

	public void setLastName(String lastName) {
		this.lastName = lastName;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getAccount_created() {
		return account_created;
	}

	public void setAccount_created(String account_created) {
		this.account_created = account_created;
	}

	public String getAccount_updated() {
		return account_updated;
	}

	public void setAccount_updated(String account_updated) {
		this.account_updated = account_updated;
	}

	@Override
	public String toString() {
		return "User [id=" + id + ", firstName=" + firstName + ", lastName=" + lastName + ", email=" + email
				+ ", account_created=" + account_created + ", account_updated=" + account_updated + "]";
	}
	
	
	
	

}
