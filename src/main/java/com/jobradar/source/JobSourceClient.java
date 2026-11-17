package com.jobradar.source;

import com.jobradar.model.JobSource;

import java.util.List;

public interface JobSourceClient {

    // Which source this client knows how to read.
    JobSource source();

    // Returns all the jobs currently open on the given job board.
    // Throws SourceFetchException if the source cannot be read.
    List<FetchedJob> fetchJobs(String boardToken);
}