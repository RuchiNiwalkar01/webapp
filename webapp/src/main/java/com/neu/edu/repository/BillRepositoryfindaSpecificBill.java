package com.neu.edu.repository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.neu.edu.model.Bill;

@Repository
public interface BillRepositoryfindaSpecificBill extends JpaRepository<Bill, Long> {
	
	Bill findById(String id);

}
