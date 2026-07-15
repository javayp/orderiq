package com.orderiq.data.service;

import com.orderiq.data.model.IngestionReport;

import java.nio.file.Path;
import java.util.List;

public interface OrderIngestionService {

	IngestionReport load(List<Path> sources);
}
