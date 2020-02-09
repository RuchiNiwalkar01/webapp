package com.neu.edu.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.neu.edu.model.FileImage;

@Repository
public interface FileRepository extends JpaRepository<FileImage, Long>{

	FileImage findByfileId(String id);
}
