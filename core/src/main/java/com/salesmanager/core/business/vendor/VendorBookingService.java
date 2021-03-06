package com.salesmanager.core.business.vendor;

import java.util.List;

import com.salesmanager.core.business.services.common.generic.SalesManagerEntityService;
import com.salesmanager.core.model.customer.VendorBooking;

public interface VendorBookingService extends SalesManagerEntityService<Long, VendorBooking> {

	List<VendorBooking> getAllVendorBookings();

	List<VendorBooking> getClosedVendorBookings();

	List<VendorBooking> getOpenedVendorBookings();

	List<VendorBooking> getVendorBookingsByVendorType(String vendorType);

	List<VendorBooking> getVendorBookingBasedOnStatus(String status,String vendorType);
	
	
}
