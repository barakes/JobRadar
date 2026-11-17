package com.jobradar.web;

import com.jobradar.model.SavedSearch;
import com.jobradar.repository.SavedSearchRepository;
import jakarta.validation.Valid;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/saved-searches")
public class SavedSearchController {

    private final SavedSearchRepository savedSearchRepository;

    public SavedSearchController(SavedSearchRepository savedSearchRepository) {
        this.savedSearchRepository = savedSearchRepository;
    }

    @GetMapping
    public List<SavedSearchResponse> listSavedSearches() {
        return savedSearchRepository.findAll(Sort.by("id")).stream()
                .map(SavedSearchResponse::from)
                .toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SavedSearchResponse createSavedSearch(@Valid @RequestBody CreateSavedSearchRequest request) {
        SavedSearch search = new SavedSearch(
                request.name().strip(),
                blankToNull(request.query()),
                blankToNull(request.country()),
                blankToNull(request.location()),
                request.email().strip(),
                Instant.now());

        return SavedSearchResponse.from(savedSearchRepository.save(search));
    }

    // Deactivates instead of deleting, so the history stays and the search can be restored later.
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deactivateSavedSearch(@PathVariable Long id) {
        SavedSearch search = savedSearchRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Saved search " + id + " was not found"));

        search.deactivate();
        savedSearchRepository.save(search);
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }
}