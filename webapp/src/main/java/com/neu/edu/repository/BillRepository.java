package com.neu.edu.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.neu.edu.model.Bill;

@Repository
public interface BillRepository extends JpaRepository<Bill, Long> {
	
	List<Bill> findByUserId(String user_id);

}
