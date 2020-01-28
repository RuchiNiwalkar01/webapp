package com.neu.edu.repository;
import org.springframework.data.jpa.repository.JpaRepository;

import com.neu.edu.model.Bill;

public interface BillRepositoryfindaSpecificBill extends JpaRepository<Bill, Long> {
	
	Bill findById(String id);

}
