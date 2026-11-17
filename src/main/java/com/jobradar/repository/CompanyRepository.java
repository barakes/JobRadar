package com.jobradar.repository;

import com.jobradar.model.Company;
import com.jobradar.model.JobSource;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CompanyRepository extends JpaRepository<Company, Long> {

    List<Company> findByActiveTrue();

    Optional<Company> findBySourceAndBoardToken(JobSource source, String boardToken);
}