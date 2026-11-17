package com.jobradar.repository;

import com.jobradar.model.Company;
import com.jobradar.model.Job;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface JobRepository extends JpaRepository<Job, Long> {

    List<Job> findByCompany(Company company);

    Page<Job> findByActiveTrue(Pageable pageable);
}