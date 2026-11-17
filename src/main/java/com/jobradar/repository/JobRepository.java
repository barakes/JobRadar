package com.jobradar.repository;

import com.jobradar.model.Company;
import com.jobradar.model.Job;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface JobRepository extends JpaRepository<Job, Long> {

    List<Job> findByCompany(Company company);

    @Query(
            value = """
                    SELECT j.id            AS "id",
                           j.title         AS "title",
                           c.name          AS "companyName",
                           j.location      AS "location",
                           j.country       AS "country",
                           j.department    AS "department",
                           j.url           AS "url",
                           j.first_seen_at AS "firstSeenAt"
                    FROM jobs j
                    JOIN companies c ON c.id = j.company_id
                    WHERE j.active = TRUE
                      AND (CAST(:query AS TEXT) IS NULL
                           OR j.search_vector @@ websearch_to_tsquery('simple', CAST(:query AS TEXT)))
                      AND (CAST(:location AS TEXT) IS NULL
                           OR j.location ILIKE '%' || CAST(:location AS TEXT) || '%')
                      AND (CAST(:country AS TEXT) IS NULL
                           OR LOWER(j.country) = LOWER(CAST(:country AS TEXT)))
                    ORDER BY j.first_seen_at DESC, j.id DESC
                    """,
            countQuery = """
                    SELECT COUNT(*)
                    FROM jobs j
                    WHERE j.active = TRUE
                      AND (CAST(:query AS TEXT) IS NULL
                           OR j.search_vector @@ websearch_to_tsquery('simple', CAST(:query AS TEXT)))
                      AND (CAST(:location AS TEXT) IS NULL
                           OR j.location ILIKE '%' || CAST(:location AS TEXT) || '%')
                      AND (CAST(:country AS TEXT) IS NULL
                           OR LOWER(j.country) = LOWER(CAST(:country AS TEXT)))
                    """,
            nativeQuery = true)
    Page<JobSummary> search(@Param("query") String query,
                            @Param("location") String location,
                            @Param("country") String country,
                            Pageable pageable);

    // Same filters as search(), but only jobs first seen after the given moment.
    @Query(
            value = """
                    SELECT j.id            AS "id",
                           j.title         AS "title",
                           c.name          AS "companyName",
                           j.location      AS "location",
                           j.country       AS "country",
                           j.department    AS "department",
                           j.url           AS "url",
                           j.first_seen_at AS "firstSeenAt"
                    FROM jobs j
                    JOIN companies c ON c.id = j.company_id
                    WHERE j.active = TRUE
                      AND j.first_seen_at > :since
                      AND (CAST(:query AS TEXT) IS NULL
                           OR j.search_vector @@ websearch_to_tsquery('simple', CAST(:query AS TEXT)))
                      AND (CAST(:location AS TEXT) IS NULL
                           OR j.location ILIKE '%' || CAST(:location AS TEXT) || '%')
                      AND (CAST(:country AS TEXT) IS NULL
                           OR LOWER(j.country) = LOWER(CAST(:country AS TEXT)))
                    ORDER BY j.first_seen_at DESC, j.id DESC
                    """,
            countQuery = """
                    SELECT COUNT(*)
                    FROM jobs j
                    WHERE j.active = TRUE
                      AND j.first_seen_at > :since
                      AND (CAST(:query AS TEXT) IS NULL
                           OR j.search_vector @@ websearch_to_tsquery('simple', CAST(:query AS TEXT)))
                      AND (CAST(:location AS TEXT) IS NULL
                           OR j.location ILIKE '%' || CAST(:location AS TEXT) || '%')
                      AND (CAST(:country AS TEXT) IS NULL
                           OR LOWER(j.country) = LOWER(CAST(:country AS TEXT)))
                    """,
            nativeQuery = true)
    Page<JobSummary> findNewMatches(@Param("query") String query,
                                    @Param("location") String location,
                                    @Param("country") String country,
                                    @Param("since") Instant since,
                                    Pageable pageable);
}