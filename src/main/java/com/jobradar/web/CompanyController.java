package com.jobradar.web;

import com.jobradar.model.Company;
import com.jobradar.repository.CompanyRepository;
import com.jobradar.source.SourceFetchException;
import com.jobradar.sync.JobSyncService;
import com.jobradar.sync.SyncResult;
import jakarta.validation.Valid;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/companies")
public class CompanyController {

    private final CompanyRepository companyRepository;
    private final JobSyncService jobSyncService;

    public CompanyController(CompanyRepository companyRepository, JobSyncService jobSyncService) {
        this.companyRepository = companyRepository;
        this.jobSyncService = jobSyncService;
    }

    @GetMapping
    public List<CompanyResponse> listCompanies() {
        return companyRepository.findAll(Sort.by("name")).stream()
                .map(CompanyResponse::from)
                .toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CompanyResponse addCompany(@Valid @RequestBody CreateCompanyRequest request) {
        if (companyRepository.findBySourceAndBoardToken(request.source(), request.boardToken()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This company is already tracked");
        }

        Company company = companyRepository.save(
                new Company(request.name().strip(), request.source(), request.boardToken()));

        return CompanyResponse.from(company);
    }

    @PostMapping("/{id}/sync")
    public SyncResult syncCompany(@PathVariable Long id) {
        if (!companyRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Company " + id + " was not found");
        }

        try {
            return jobSyncService.syncCompany(id);
        } catch (SourceFetchException e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, e.getMessage(), e);
        }
    }
}