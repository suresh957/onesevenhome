package com.salesmanager.core.business.services.services;

import java.util.List;

import com.salesmanager.core.business.services.common.generic.SalesManagerEntityService;
import com.salesmanager.core.model.customer.MachineryPortfolio;

public interface MachineryPortfolioService extends SalesManagerEntityService<Long, MachineryPortfolio> {

	List<MachineryPortfolio> findByVendorId(Long vendorId);
}