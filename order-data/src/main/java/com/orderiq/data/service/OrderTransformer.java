package com.orderiq.data.service;

import com.orderiq.data.model.NormalizationResult;
import com.orderiq.data.model.RawOrderRow;

public interface OrderTransformer {

	NormalizationResult transform(RawOrderRow row);
}
