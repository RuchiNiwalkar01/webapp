package com.neu.edu.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.neu.edu.model.User;

@Repository
public interface UserRepository extends JpaRepository<User, String> {

	
	User findByemail(String email);
}
