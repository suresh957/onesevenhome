package com.salesmanager.shop.controller.vendor;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.MediaType;

import com.salesmanager.core.business.exception.ServiceException;
import com.salesmanager.core.business.modules.email.Email;
import com.salesmanager.core.business.services.catalog.product.PricingService;
import com.salesmanager.core.business.services.catalog.product.ProductService;
import com.salesmanager.core.business.services.customer.CustomerService;
import com.salesmanager.core.business.services.merchant.MerchantStoreService;
import com.salesmanager.core.business.services.services.MachineryPortfolioService;
import com.salesmanager.core.business.services.system.EmailService;
import com.salesmanager.core.business.vendor.product.services.VendorProductService;
import com.salesmanager.core.model.catalog.product.Product;
import com.salesmanager.core.model.catalog.product.image.ProductImage;
import com.salesmanager.core.model.customer.ArchitectsPortfolio;
import com.salesmanager.core.model.customer.Customer;
import com.salesmanager.core.model.customer.MachineryPortfolio;
import com.salesmanager.core.model.merchant.MerchantStore;
import com.salesmanager.core.model.product.vendor.VendorProduct;
import com.salesmanager.shop.admin.controller.products.ProductImageRequest;
import com.salesmanager.shop.admin.controller.products.ProductImageResponse;
import com.salesmanager.shop.constants.Constants;
import com.salesmanager.shop.constants.EmailConstants;
import com.salesmanager.shop.fileupload.services.StorageException;
import com.salesmanager.shop.store.controller.AbstractController;
import com.salesmanager.shop.store.controller.customer.VendorResponse;
import com.salesmanager.shop.utils.EmailUtils;
import com.salesmanager.shop.utils.LabelUtils;
import com.salesmanager.shop.fileupload.services.StorageService;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

@Controller
@CrossOrigin
public class MachineryController extends AbstractController {

	@Inject
	MachineryPortfolioService machineryPortfolioService;
	
	@Inject
	CustomerService customerService;

	@Inject
	EmailService emailService;

	@Inject
	private LabelUtils messages;

	@Inject
	MerchantStoreService merchantStoreService ;

	@Inject
	private EmailUtils emailUtils;
	
    @Inject
    private StorageService storageService;


	private static final Logger LOGGER = LoggerFactory.getLogger(MachineryController.class);

	private final static String VENDOR_ADD_PRODUCTS_TPL = "email_template_vendor_add_products.ftl";

	@RequestMapping(value="/addMachineryPortfolio", method = RequestMethod.POST) 
	@ResponseBody
	public MachineryResponse addMachineryPortfolio(@RequestPart("machineryRequest") String machineryRequestStr,
			@RequestPart("file") MultipartFile uploadedImage) throws Exception {
		LOGGER.debug("Entered addMachineryPortfolio");
		MachineryRequest machineryRequest = new ObjectMapper().readValue(machineryRequestStr, MachineryRequest.class);
		MachineryResponse machineryResponse = new MachineryResponse();
		MachineryPortfolio machineryPortfolio = new MachineryPortfolio();
		
    	String fileName = "";
    	if(uploadedImage.getSize() != 0) {
    		try{
    			fileName = storageService.store(uploadedImage,"machinery");
    			LOGGER.debug("architect portfolio fileName "+fileName);
    		
	    		Customer customer = customerService.getById(machineryRequest.getVendorId());
	    		if(customer == null){
	    			LOGGER.error("customer not found while uploading portfolio for customer id=="+machineryRequest.getVendorId());
	    			machineryResponse.setErrorMessage("Failed while storing image");
	    			machineryResponse.setStatus(false);
	    			return machineryResponse;
	    		}
	    		machineryPortfolio.setCreateDate(new Date());
	    		machineryPortfolio.setImageURL(fileName);
	    		machineryPortfolio.setPortfolioName(machineryRequest.getPortfolioName());
	    		machineryPortfolio.setCustomer(customer);
	    		machineryPortfolioService.save(machineryPortfolio);
	    		
	    		machineryResponse.setStatus(true);
	    		machineryResponse.setSuccessMessage("New portfolio details uploaded successfully.");
	    		
    		}catch(StorageException se){
    			LOGGER.error("Failed while uploading portfolio for machinery=="+se.getMessage());
    			machineryResponse.setErrorMessage("Failed while uploading portfolio for machinery=="+machineryRequest.getPortfolioName());
    			machineryResponse.setStatus(false);
    			return machineryResponse;
    		}
    	}
    	return machineryResponse;
	}
    @RequestMapping(value="/getMachineryPortfolio", method=RequestMethod.POST, consumes=MediaType.APPLICATION_JSON_VALUE, produces=MediaType.APPLICATION_JSON_VALUE)
  	@ResponseBody
  	public MachineryResponse getMachineryPortfolio(@RequestBody MachineryRequest machineryRequest) throws Exception {
		MachineryResponse machineryResponse = new MachineryResponse();
		MachineryPortfolio machineryPortfolio = new MachineryPortfolio();
		
		List<VendorPortfolioData> vendorPortfolioList = new ArrayList<VendorPortfolioData>();
		try {
			
			List<MachineryPortfolio> portfolioList = machineryPortfolioService.findByVendorId(machineryRequest.getVendorId());
	    	for(MachineryPortfolio portfolio:portfolioList){
	    		VendorPortfolioData vendorPortfolioData = new VendorPortfolioData();
	    		vendorPortfolioData.setPortfolioId(portfolio.getId());
	    		vendorPortfolioData.setPortfolioName(portfolio.getPortfolioName());
	    		vendorPortfolioData.setVendorId(machineryRequest.getVendorId());
	    		vendorPortfolioList.add(vendorPortfolioData);
	    	}
	    	machineryResponse.setStatus(true);
	    	machineryResponse.setVendorPortfolioList(vendorPortfolioList);
		}catch(StorageException se){
			LOGGER.error("Failed while fetching portfolio list for machinery=="+se.getMessage());
			machineryResponse.setErrorMessage("Failed while fetching portfolio list for machinery=="+machineryRequest.getVendorId());
			machineryResponse.setStatus(false);
			return machineryResponse;
		}
		return machineryResponse;
    }
	
}
