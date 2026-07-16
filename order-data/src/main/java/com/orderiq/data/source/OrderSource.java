package com.orderiq.data.source;

import com.orderiq.data.model.RawOrderRow;

import java.nio.file.Path;
import java.util.List;

/** Extracts raw order rows from one source without applying business transformations. */
public interface OrderSource {

	List<RawOrderRow> read(Path source);
}
