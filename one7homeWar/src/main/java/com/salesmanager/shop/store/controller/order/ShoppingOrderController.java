package com.salesmanager.shop.store.controller.order;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.salesmanager.core.business.exception.ServiceException;
import com.salesmanager.core.business.services.catalog.product.PricingService;
import com.salesmanager.core.business.services.catalog.product.ProductService;
import com.salesmanager.core.business.services.customer.CustomerService;
import com.salesmanager.core.business.services.merchant.MerchantStoreService;
import com.salesmanager.core.business.services.order.OrderService;
import com.salesmanager.core.business.services.order.orderproduct.OrderProductDownloadService;
import com.salesmanager.core.business.services.payments.PaymentService;
import com.salesmanager.core.business.services.payments.TransactionService;
import com.salesmanager.core.business.services.reference.country.CountryService;
import com.salesmanager.core.business.services.reference.language.LanguageService;
import com.salesmanager.core.business.services.reference.zone.ZoneService;
import com.salesmanager.core.business.services.shipping.ShippingService;
import com.salesmanager.core.business.services.shoppingcart.ShoppingCartService;
import com.salesmanager.core.model.catalog.product.Product;
import com.salesmanager.core.model.common.Billing;
import com.salesmanager.core.model.customer.Customer;
import com.salesmanager.core.model.merchant.MerchantStore;
import com.salesmanager.core.model.order.Order;
import com.salesmanager.core.model.order.OrderTotal;
import com.salesmanager.core.model.order.OrderTotalSummary;
import com.salesmanager.core.model.order.orderproduct.OrderProductDownload;
import com.salesmanager.core.model.payments.PaymentMethod;
import com.salesmanager.core.model.payments.PaymentType;
import com.salesmanager.core.model.payments.Transaction;
import com.salesmanager.core.model.payments.TransactionType;
import com.salesmanager.core.model.reference.country.Country;
import com.salesmanager.core.model.reference.language.Language;
import com.salesmanager.core.model.reference.zone.Zone;
import com.salesmanager.core.model.shipping.ShippingMetaData;
import com.salesmanager.core.model.shipping.ShippingOption;
import com.salesmanager.core.model.shipping.ShippingQuote;
import com.salesmanager.core.model.shipping.ShippingSummary;
import com.salesmanager.core.model.shoppingcart.ShoppingCartItem;
import com.salesmanager.shop.admin.model.userpassword.UserReset;
import com.salesmanager.shop.constants.Constants;
import com.salesmanager.shop.model.customer.Address;
import com.salesmanager.shop.model.customer.AnonymousCustomer;
import com.salesmanager.shop.model.customer.PersistableCustomer;
import com.salesmanager.shop.model.customer.ReadableDelivery;
import com.salesmanager.shop.model.order.ReadableOrder;
import com.salesmanager.shop.model.order.ReadableOrderTotal;
import com.salesmanager.shop.model.order.ReadableShippingSummary;
import com.salesmanager.shop.model.order.ReadableShopOrder;
import com.salesmanager.shop.model.order.ShopOrder;
import com.salesmanager.shop.model.shoppingcart.ShoppingCartData;
import com.salesmanager.shop.populator.customer.ReadableCustomerDeliveryAddressPopulator;
import com.salesmanager.shop.populator.order.ReadableOrderTotalPopulator;
import com.salesmanager.shop.populator.order.ReadableShippingSummaryPopulator;
import com.salesmanager.shop.populator.order.ReadableShopOrderPopulator;
import com.salesmanager.shop.store.controller.AbstractController;
import com.salesmanager.shop.store.controller.ControllerConstants;
import com.salesmanager.shop.store.controller.customer.CustomerRequest;
import com.salesmanager.shop.store.controller.customer.CustomerResponse;
import com.salesmanager.shop.store.controller.customer.facade.CustomerFacade;
import com.salesmanager.shop.store.controller.order.facade.OrderFacade;
import com.salesmanager.shop.store.controller.shoppingCart.facade.ShoppingCartFacade;
import com.salesmanager.shop.utils.EmailTemplatesUtils;
import com.salesmanager.shop.utils.LabelUtils;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.Validate;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;

import java.math.BigDecimal;
import java.util.*;


/**
 * Displays checkout form and deals with ajax user input
 * @author carlsamson
 *
 */
@Controller
@RequestMapping(Constants.SHOP_URI+"/order")
public class ShoppingOrderController extends AbstractController {
	
	private static final Logger LOGGER = LoggerFactory
	.getLogger(ShoppingOrderController.class);
	
	@Inject
	private ShoppingCartFacade shoppingCartFacade;
	
    @Inject
    private ShoppingCartService shoppingCartService;

	@Inject
	private PaymentService paymentService;
	
	@Inject
	private CustomerService customerService;
	
	@Inject
	private ShippingService shippingService;
	
	@Inject
	private OrderService orderService;
	
	@Inject
	private CountryService countryService;
	
	@Inject
	private ZoneService zoneService;
	
	@Inject
	private OrderFacade orderFacade;
	
	@Inject
	private CustomerFacade customerFacade;
	
	@Inject
	private LabelUtils messages;
	
	@Inject
	private PricingService pricingService;
	
	@Inject
	private ProductService productService;
	
	@Inject
	private PasswordEncoder passwordEncoder;
	
	@Inject
    private AuthenticationManager customerAuthenticationManager;
	
	@Inject
	private TransactionService transactionService;
	
	@Inject
	private EmailTemplatesUtils emailTemplatesUtils;
	
	@Inject
	private OrderProductDownloadService orderProdctDownloadService;
	
    @Inject
    MerchantStoreService merchantStoreService ;

	@Inject
	private LanguageService languageService;

	@SuppressWarnings("unused")
	@RequestMapping("/checkout.html")
	public String displayCheckout(@CookieValue("cart") String cookie, Model model, HttpServletRequest request, HttpServletResponse response, Locale locale) throws Exception {

		Language language = (Language)request.getAttribute("LANGUAGE");
		MerchantStore store = (MerchantStore)request.getAttribute(Constants.MERCHANT_STORE);
		Customer customer = (Customer)request.getSession().getAttribute(Constants.CUSTOMER);
		if(customer == null){
			customer = customerService.getById(1l);
		}
		/**
		 * Shopping cart
		 * 
		 * ShoppingCart should be in the HttpSession
		 * Otherwise the cart id is in the cookie
		 * Otherwise the customer is in the session and a cart exist in the DB
		 * Else -> Nothing to display
		 */
		
		//check if an existing order exist
		ShopOrder order = null;
		order = super.getSessionAttribute(Constants.ORDER, request);
	
		//Get the cart from the DB
		String shoppingCartCode  = (String)request.getSession().getAttribute(Constants.SHOPPING_CART);
		com.salesmanager.core.model.shoppingcart.ShoppingCart cart = null;
	
	    if(StringUtils.isBlank(shoppingCartCode)) {
				
			if(cookie==null) {//session expired and cookie null, nothing to do
				return "redirect:/shop/cart/shoppingCart.html";
			}
			String merchantCookie[] = cookie.split("_");
			String merchantStoreCode = merchantCookie[0];
			if(!merchantStoreCode.equals(store.getCode())) {
				return "redirect:/shop/cart/shoppingCart.html";
			}
			shoppingCartCode = merchantCookie[1];
	    	
	    } 
	    
	    cart = shoppingCartFacade.getShoppingCartModel(shoppingCartCode, store);
	    
	
	    if(cart==null && customer!=null) {
				cart=shoppingCartFacade.getShoppingCartModel(customer, store);
	    }
	    boolean allAvailables = true;
	    //Filter items, delete unavailable
        Set<com.salesmanager.core.model.shoppingcart.ShoppingCartItem> availables = new HashSet<ShoppingCartItem>();
        //Take out items no more available
        Set<com.salesmanager.core.model.shoppingcart.ShoppingCartItem> items = cart.getLineItems();
        for(com.salesmanager.core.model.shoppingcart.ShoppingCartItem item : items) {
        	
        	Long id = item.getProduct().getId();
        	Product p = productService.getById(id);
        	if(p.isAvailable()) {
        		availables.add(item);
        	} else {
        		allAvailables = false;
        	}
        }
        cart.setLineItems(availables);

        if(!allAvailables) {
        	shoppingCartFacade.saveOrUpdateShoppingCart(cart);
        }
	    
	    super.setSessionAttribute(Constants.SHOPPING_CART, cart.getShoppingCartCode(), request);
	
	    if(shoppingCartCode==null && cart==null) {//error
				return "redirect:/shop/cart/shoppingCart.html";
	    }
			
	
/*	    if(customer!=null) {
			if(cart.getCustomerId()!=customer.getId().longValue()) {
					return "redirect:/shop/shoppingCart.html";
			}
	     }*/ 		
	     if(order==null) {
			order = orderFacade.initializeOrder(store, customer, cart, language);
		  }

		boolean freeShoppingCart = shoppingCartService.isFreeShoppingCart(cart);
		boolean requiresShipping = shoppingCartService.requiresShipping(cart);
		
		/**
		 * hook for displaying or not delivery address configuration
		 */
		ShippingMetaData shippingMetaData = shippingService.getShippingMetaData(store);
		model.addAttribute("shippingMetaData",shippingMetaData);//TODO DTO
		
		/** shipping **/
		ShippingQuote quote = null;
		if(requiresShipping) {
			//System.out.println("** Berfore default shipping quote **");
			//Get all applicable shipping quotes
			quote = orderFacade.getShippingQuote(customer, cart, order, store, language);
			model.addAttribute("shippingQuote", quote);
		}

		if(quote!=null) {
			String shippingReturnCode = quote.getShippingReturnCode();

			if(StringUtils.isBlank(shippingReturnCode) || shippingReturnCode.equals(ShippingQuote.NO_POSTAL_CODE)) {
			
				if(order.getShippingSummary()==null) {
					ShippingSummary summary = orderFacade.getShippingSummary(quote, store, language);
					order.setShippingSummary(summary);
					request.getSession().setAttribute(Constants.SHIPPING_SUMMARY, summary);//TODO DTO
				}
				if(order.getSelectedShippingOption()==null) {
					order.setSelectedShippingOption(quote.getSelectedShippingOption());
				}
				
				//save quotes in HttpSession
				List<ShippingOption> options = quote.getShippingOptions();
				request.getSession().setAttribute(Constants.SHIPPING_OPTIONS, options);//TODO DTO
				
				if(!CollectionUtils.isEmpty(options)) {
					
					for(ShippingOption shipOption : options) {
						
						StringBuilder moduleName = new StringBuilder();
						moduleName.append("module.shipping.").append(shipOption.getShippingModuleCode());
								
								
						String carrier = messages.getMessage(moduleName.toString(),locale);	
						String note = messages.getMessage(moduleName.append(".note").toString(), locale, "");
								
						shipOption.setDescription(carrier);
						shipOption.setNote(note);
						
						//option name
						if(!StringUtils.isBlank(shipOption.getOptionCode())) {
							//try to get the translate
							StringBuilder optionCodeBuilder = new StringBuilder();
							try {
								
								optionCodeBuilder.append("module.shipping.").append(shipOption.getShippingModuleCode());
								String optionName = messages.getMessage(optionCodeBuilder.toString(),locale);
								shipOption.setOptionName(optionName);
							} catch(Exception e) {//label not found
								LOGGER.warn("No shipping code found for " + optionCodeBuilder.toString());
							}
						}

					}
				
				}
			
			}
			
			if(quote.getDeliveryAddress()!=null) {
				ReadableCustomerDeliveryAddressPopulator addressPopulator = new ReadableCustomerDeliveryAddressPopulator();
				addressPopulator.setCountryService(countryService);
				addressPopulator.setZoneService(zoneService);
				ReadableDelivery deliveryAddress = new ReadableDelivery();
				addressPopulator.populate(quote.getDeliveryAddress(), deliveryAddress,  store, language);
				model.addAttribute("deliveryAddress", deliveryAddress);
				super.setSessionAttribute(Constants.KEY_SESSION_ADDRESS, deliveryAddress, request);
			}
			
			
			//get shipping countries
			List<Country> shippingCountriesList = orderFacade.getShipToCountry(store, language);
			model.addAttribute("countries", shippingCountriesList);
		} else {
			//get all countries
			List<Country> countries = countryService.getCountries(language);
			model.addAttribute("countries", countries);
		}
		
		if(quote!=null && quote.getShippingReturnCode()!=null && quote.getShippingReturnCode().equals(ShippingQuote.NO_SHIPPING_MODULE_CONFIGURED)) {
			LOGGER.error("Shipping quote error " + quote.getShippingReturnCode());
			model.addAttribute("errorMessages", quote.getShippingReturnCode());
		}
		
		if(quote!=null && !StringUtils.isBlank(quote.getQuoteError())) {
			LOGGER.error("Shipping quote error " + quote.getQuoteError());
			model.addAttribute("errorMessages", quote.getQuoteError());
		}
		
		if(quote!=null && quote.getShippingReturnCode()!=null && quote.getShippingReturnCode().equals(ShippingQuote.NO_SHIPPING_TO_SELECTED_COUNTRY)) {
			LOGGER.error("Shipping quote error " + quote.getShippingReturnCode());
			model.addAttribute("errorMessages", quote.getShippingReturnCode());
		}
		/** end shipping **/

		//get payment methods
		List<PaymentMethod> paymentMethods = paymentService.getAcceptedPaymentMethods(store);

		//not free and no payment methods
		if(CollectionUtils.isEmpty(paymentMethods) && !freeShoppingCart) {
			LOGGER.error("No payment method configured");
			model.addAttribute("errorMessages", "No payments configured");
		}
		
		if(!CollectionUtils.isEmpty(paymentMethods)) {//select default payment method
			PaymentMethod defaultPaymentSelected = null;
			for(PaymentMethod paymentMethod : paymentMethods) {
				if(paymentMethod.isDefaultSelected()) {
					defaultPaymentSelected = paymentMethod;
					break;
				}
			}
			
			if(defaultPaymentSelected==null) {//forced default selection
				defaultPaymentSelected = paymentMethods.get(0);
				defaultPaymentSelected.setDefaultSelected(true);
			}
			
			order.setDefaultPaymentMethodCode(defaultPaymentSelected.getPaymentMethodCode());
			
		}
		
		//readable shopping cart items for order summary box
        ShoppingCartData shoppingCart = shoppingCartFacade.getShoppingCartData(cart);
        model.addAttribute( "cart", shoppingCart );
		//TODO filter here


		//order total
		OrderTotalSummary orderTotalSummary = orderFacade.calculateOrderTotal(store, order, language);
		order.setOrderTotalSummary(orderTotalSummary);
		//if order summary has to be re-used
		super.setSessionAttribute(Constants.ORDER_SUMMARY, orderTotalSummary, request);

		model.addAttribute("order",order);
		model.addAttribute("paymentMethods", paymentMethods);
		
		/** template **/
		StringBuilder template = new StringBuilder().append(ControllerConstants.Tiles.Checkout.checkout).append(".").append(store.getStoreTemplate());
		return template.toString();
	}
	
	
	@RequestMapping("/commitPreAuthorized.html")
	public String commitPreAuthorizedOrder(Model model, HttpServletRequest request, HttpServletResponse response, Locale locale) throws Exception {
		
		MerchantStore store = (MerchantStore)request.getAttribute(Constants.MERCHANT_STORE);
		Language language = (Language)request.getAttribute("LANGUAGE");
		ShopOrder order = super.getSessionAttribute(Constants.ORDER, request);
		if(order==null) {
			StringBuilder template = new StringBuilder().append(ControllerConstants.Tiles.Pages.timeout).append(".").append(store.getStoreTemplate());
			return template.toString();	
		}
		
		@SuppressWarnings("unchecked")
		Map<String, Object> configs = (Map<String, Object>) request.getAttribute(Constants.REQUEST_CONFIGS);
		
		if(configs!=null && configs.containsKey(Constants.DEBUG_MODE)) {
			Boolean debugMode = (Boolean) configs.get(Constants.DEBUG_MODE);
			if(debugMode) {
				try {
					ObjectMapper mapper = new ObjectMapper();
					String jsonInString = mapper.writeValueAsString(order);
					LOGGER.debug("Commit pre-authorized order -> " + jsonInString);
				} catch(Exception de) {
					LOGGER.error(de.getMessage());
				}
			}
		}

		
		try {
			
			OrderTotalSummary totalSummary = super.getSessionAttribute(Constants.ORDER_SUMMARY, request);
			
			if(totalSummary==null) {
				totalSummary = orderFacade.calculateOrderTotal(store, order, language);
				super.setSessionAttribute(Constants.ORDER_SUMMARY, totalSummary, request);
			}
			
			
			order.setOrderTotalSummary(totalSummary);
			
			//already validated, proceed with commit
			Order orderModel = this.commitOrder(order, request, locale);
			super.setSessionAttribute(Constants.ORDER_ID, orderModel.getId(), request);
			
			return "redirect:/shop/order/confirmation.html";
			
		} catch(Exception e) {
			LOGGER.error("Error while commiting order",e);
			throw e;		
			
		}
	}
	
	private Order commitOrder(ShopOrder order, HttpServletRequest request, Locale locale) throws Exception, ServiceException {
			MerchantStore store = (MerchantStore)request.getAttribute(Constants.MERCHANT_STORE);
			Language language = (Language)request.getAttribute("LANGUAGE");
			
			PersistableCustomer customer = order.getCustomer();
			
/*	        if(order.isShipToBillingAdress()) {
	        	customer.setDelivery(customer.getBilling());
	        }*/
			Customer modelCustomer = getSessionAttribute(  Constants.CUSTOMER, request );

			//Customer modelCustomer = null;
			//try {//set groups
				//	modelCustomer = customerFacade.getCustomerModel(customer, store, language);
					//customerFacade.setCustomerModelDefaultProperties(modelCustomer, store);
					//userName = modelCustomer.getNick();
					//LOGGER.debug( "About to persist volatile customer to database." );
			        //customerService.saveOrUpdate( modelCustomer );
				/*} else {//use existing customer
					modelCustomer = customerFacade.populateCustomerModel(authCustomer, customer, store, language);
				}*/
			/*} catch(Exception e) {
				throw new ServiceException(e);
			}
*/	         
	        Order modelOrder = null;
			//Transaction transaction = createTransaction();
			//transactionService.create(transaction);
			
			//super.setSessionAttribute(Constants.INIT_TRANSACTION_KEY, transaction, request);

	        //Transaction initialTransaction = (Transaction)super.getSessionAttribute(Constants.INIT_TRANSACTION_KEY, request);
	        //if(initialTransaction!=null) {
	 //       	modelOrder=orderFacade.processOrder(order, modelCustomer, initialTransaction, store, language);
	        //} else {
	        	modelOrder=orderFacade.processOrder(order, modelCustomer, store, language);
	        //}
	        
	        //save order id in session
	        super.setSessionAttribute(Constants.ORDER_ID, modelOrder.getId(), request);
	        //set a unique token for confirmation
	        super.setSessionAttribute(Constants.ORDER_ID_TOKEN, modelOrder.getId(), request);
	        
			//get cart
			String cartCode = super.getSessionAttribute(Constants.SHOPPING_CART, request);
			if(StringUtils.isNotBlank(cartCode)) {
				try {
					shoppingCartFacade.deleteShoppingCart(cartCode, store);
				} catch(Exception e) {
					LOGGER.error("Cannot delete cart " + cartCode, e);
					throw new ServiceException(e);
				}
			}

			
	        //cleanup the order objects
	        super.removeAttribute(Constants.ORDER, request);
	        super.removeAttribute(Constants.ORDER_SUMMARY, request);
	        super.removeAttribute(Constants.INIT_TRANSACTION_KEY, request);
	        super.removeAttribute(Constants.SHIPPING_OPTIONS, request);
	        super.removeAttribute(Constants.SHIPPING_SUMMARY, request);
	        super.removeAttribute(Constants.SHOPPING_CART, request);
	        
	        try {
		        //refresh customer --
	        	//modelCustomer = customerFacade.getCustomerByUserName(modelCustomer.getNick(), store);
		        
	        	//if has downloads, authenticate
	        	
	        	//check if any downloads exist for this order6
	    		List<OrderProductDownload> orderProductDownloads = orderProdctDownloadService.getByOrderId(modelOrder.getId());
	    		if(CollectionUtils.isNotEmpty(orderProductDownloads)) {

/*		        	LOGGER.debug("Is user authenticated ? ",auth.isAuthenticated());
		        	if(auth != null &&
			        		 request.isUserInRole("AUTH_CUSTOMER")) {
			        	//already authenticated
			        } else {
				        //authenticate
				        customerFacade.authenticate(modelCustomer, userName, password);
				        super.setSessionAttribute(Constants.CUSTOMER, modelCustomer, request);
			        }
*/		        	//send new user registration template
					if(order.getCustomer().getId()==null || order.getCustomer().getId().longValue()==0) {
						//send email for new customer
						//customer.setClearPassword(password);//set clear password for email
						//customer.setUserName(userName);
						emailTemplatesUtils.sendRegistrationEmail( customer, store, locale, request.getContextPath() );
					}
	    		}
	    		
				//send order confirmation email to customer
				emailTemplatesUtils.sendOrderEmail(modelCustomer.getEmailAddress(), modelCustomer, modelOrder, locale, language, store, request.getContextPath());
		        if(orderService.hasDownloadFiles(modelOrder)) {
		        	emailTemplatesUtils.sendOrderDownloadEmail(modelCustomer, modelOrder, store, locale, request.getContextPath());
		
		        }
				//send order confirmation email to merchant
				emailTemplatesUtils.sendOrderEmail(store.getStoreEmailAddress(), modelCustomer, modelOrder, locale, language, store, request.getContextPath());
	        } catch(Exception e) {
	        	LOGGER.error("Error while post processing order",e);
	        }
	        return modelOrder;
	}

	private Transaction createTransaction(){
		 BigDecimal amount = BigDecimal.ONE; // revisit here
		 Transaction newTransaction = new Transaction();
		 newTransaction.setAmount(amount);
		 newTransaction.setTransactionDate(new Date());
		 newTransaction.setTransactionType(TransactionType.INIT);
		 newTransaction.setPaymentType(PaymentType.PAYPAL); // revist here
		 //newTransaction.getTransactionDetails().put("TRANSACTIONID", refundTransactionResponse.getRefundTransactionID());
		 //newTransaction.getTransactionDetails().put("CORRELATION", refundTransactionResponse.getCorrelationID());
		//	transaction.getTransactionDetails().put("TOKEN", token);
		//	transaction.getTransactionDetails().put("CORRELATION", correlationID);

		return newTransaction;
	}

	
    /*
     * preferedShippingAddress can be 0 -> default billing address
     * 								  1 -> delivery address
     * 								  2 -> secondary delivery address
     */
	@SuppressWarnings("unchecked")
	@RequestMapping(value="/commitOrder/{cartCode}/{preferedShippingAddress}", method = RequestMethod.POST)
	@ResponseBody
	public ReadableOrder commitOrder(@PathVariable("cartCode") String cartCode,@PathVariable("preferedShippingAddress") Integer preferedShippingAddress,HttpServletRequest request, Locale locale) throws Exception {
		 ReadableOrder readableOrder = new ReadableOrder();
		 System.out.println("preferedShippingAddress : "+preferedShippingAddress);
		 System.out.println("cartCode : "+cartCode);
		 //String shoppingCartCode  = (String)request.getSession().getAttribute(Constants.SHOPPING_CART);
		 Customer customer = getSessionAttribute(  Constants.CUSTOMER, request );
		 MerchantStore store = (MerchantStore)request.getAttribute(Constants.MERCHANT_STORE);
		 Language language = (Language)request.getAttribute("LANGUAGE");
		 com.salesmanager.core.model.shoppingcart.ShoppingCart cart = shoppingCartFacade.getShoppingCartModel(cartCode, store);
		 System.out.println("Cart Items "+cart.getLineItems());
		 ShopOrder shopOrder =  super.getSessionAttribute(Constants.ORDER, request);;
	     if(shopOrder==null) {
	    	 shopOrder = orderFacade.initializeOrder(store, customer, cart, language);
			 super.setSessionAttribute(Constants.ORDER, shopOrder, request);
		 }
	     //shopOrder.setCustomerAgreed(true);
	     shopOrder.setPaymentType(PaymentType.CCAvenue);
	     shopOrder.setPreferedShippingAddress(preferedShippingAddress);
	     shopOrder.setIpAddress(request.getLocalAddr());
 	     OrderTotalSummary totalSummary = super.getSessionAttribute(Constants.ORDER_SUMMARY, request);
		 if(totalSummary==null) {
			totalSummary = orderFacade.calculateOrderTotal(store, shopOrder, language);
			super.setSessionAttribute(Constants.ORDER_SUMMARY, totalSummary, request);
		 }
		 shopOrder.setOrderTotalSummary(totalSummary);
		 Order modelOrder = this.commitOrder(shopOrder, request, locale);
		 readableOrder = orderFacade.getReadableOrderByOrder(modelOrder, store, language);
		return readableOrder;
	}
	// url/orderDetails/1?userId=1
	@RequestMapping(value="/orderDetails/{orderId}", method = RequestMethod.POST)
	@ResponseBody
	public ReadableOrder getOrderDetails(@PathVariable Long orderId,HttpServletRequest request, Locale locale) throws Exception {
		MerchantStore store = (MerchantStore)request.getAttribute(Constants.MERCHANT_STORE);
		Language language = (Language)request.getAttribute("LANGUAGE");
		return orderFacade.getReadableOrder(orderId, store, language);
	}
	// url/allOrderDetails?userId=1
	@RequestMapping(value="/allOrderDetails", method = RequestMethod.POST)
	@ResponseBody
	public List<ReadableOrder> getAllCustomerOrders(HttpServletRequest request, Locale locale) throws Exception {
		MerchantStore store = (MerchantStore)request.getAttribute(Constants.MERCHANT_STORE);
	    Customer customer = getSessionAttribute(  Constants.CUSTOMER, request );
	    Long customerId = customer.getId();
		Language language = (Language)request.getAttribute("LANGUAGE");
		List<Order> orders = orderService.findOrdersByCustomer(customerId);
		List<ReadableOrder> allOrders = new ArrayList<ReadableOrder>();
		for(Order o : orders){
			allOrders.add(orderFacade.getReadableOrderByOrder(o, store, language));
		}
		return allOrders;
	}
	
	/**
	 * Recalculates shipping and tax following a change in country or province
	 * @param order
	 * @param request
	 * @param response
	 * @param locale
	 * @return
	 * @throws Exception
	 */
	@SuppressWarnings("unchecked")
	@RequestMapping(value={"/shippingQuotes.json"}, method=RequestMethod.POST)
	public @ResponseBody ReadableShopOrder calculateShipping(@ModelAttribute(value="order") ShopOrder order, HttpServletRequest request, HttpServletResponse response, Locale locale) throws Exception {
		
		Language language = (Language)request.getAttribute("LANGUAGE");
		MerchantStore store = (MerchantStore)request.getAttribute(Constants.MERCHANT_STORE);
		String shoppingCartCode  = getSessionAttribute(Constants.SHOPPING_CART, request);

		Map<String, Object> configs = (Map<String, Object>) request.getAttribute(Constants.REQUEST_CONFIGS);
		
/*		if(configs!=null && configs.containsKey(Constants.DEBUG_MODE)) {
			Boolean debugMode = (Boolean) configs.get(Constants.DEBUG_MODE);
			if(debugMode) {
				try {
					ObjectMapper mapper = new ObjectMapper();
					String jsonInString = mapper.writeValueAsString(order);
					LOGGER.info("Calculate order -> shoppingCartCode[ " + shoppingCartCode + "] -> " + jsonInString);
				} catch(Exception de) {
					LOGGER.error(de.getMessage());
				}
			}
		}*/

		Validate.notNull(shoppingCartCode,"shoppingCartCode does not exist in the session");
		
		ReadableShopOrder readableOrder = new ReadableShopOrder();
		try {

			//re-generate cart
			com.salesmanager.core.model.shoppingcart.ShoppingCart cart = shoppingCartFacade.getShoppingCartModel(shoppingCartCode, store);
	
			
			
			ReadableShopOrderPopulator populator = new ReadableShopOrderPopulator();
			populator.populate(order, readableOrder, store, language);
			
			boolean requiresShipping = shoppingCartService.requiresShipping(cart);
			
			/** shipping **/
			ShippingQuote quote = null;
			if(requiresShipping) {
				quote = orderFacade.getShippingQuote(order.getCustomer(), cart, order, store, language);
			}

			if(quote!=null) {
				String shippingReturnCode = quote.getShippingReturnCode();
				if(CollectionUtils.isNotEmpty(quote.getShippingOptions()) || ShippingQuote.NO_POSTAL_CODE.equals(shippingReturnCode)) {

					ShippingSummary summary = orderFacade.getShippingSummary(quote, store, language);
					order.setShippingSummary(summary);//for total calculation
					
					
					ReadableShippingSummary readableSummary = new ReadableShippingSummary();
					ReadableShippingSummaryPopulator readableSummaryPopulator = new ReadableShippingSummaryPopulator();
					readableSummaryPopulator.setPricingService(pricingService);
					readableSummaryPopulator.populate(summary, readableSummary, store, language);
					
					//additional informations
/*					if(quote.getQuoteInformations() != null && quote.getQuoteInformations().size() >0) {
						for(String k : quote.getQuoteInformations().keySet()) {
							Object o = quote.getQuoteInformations().get(k);
							try {
								readableSummary.getQuoteInformations().put(k, String.valueOf(o));
							} catch(Exception e) {
								LOGGER.error("Cannot cast value to string " + e.getMessage());
							}
						}
					}*/
					
					if(quote.getDeliveryAddress()!=null) {
						ReadableCustomerDeliveryAddressPopulator addressPopulator = new ReadableCustomerDeliveryAddressPopulator();
						addressPopulator.setCountryService(countryService);
						addressPopulator.setZoneService(zoneService);
						ReadableDelivery deliveryAddress = new ReadableDelivery();
						addressPopulator.populate(quote.getDeliveryAddress(), deliveryAddress,  store, language);
						//model.addAttribute("deliveryAddress", deliveryAddress);
						readableOrder.setDelivery(deliveryAddress);
						super.setSessionAttribute(Constants.KEY_SESSION_ADDRESS, deliveryAddress, request);
					}
					
					
					//save quotes in HttpSession
					List<ShippingOption> options = quote.getShippingOptions();
					
					if(!CollectionUtils.isEmpty(options)) {
					
						for(ShippingOption shipOption : options) {
							
							StringBuilder moduleName = new StringBuilder();
							moduleName.append("module.shipping.").append(shipOption.getShippingModuleCode());
											
							String carrier = messages.getMessage(moduleName.toString(),new String[]{store.getStorename()},locale);
							
							String note = messages.getMessage(moduleName.append(".note").toString(), locale, "");
							
									
							shipOption.setDescription(carrier);
							shipOption.setNote(note);
							
							//option name
							if(!StringUtils.isBlank(shipOption.getOptionCode())) {
								//try to get the translate
								StringBuilder optionCodeBuilder = new StringBuilder();
								try {
									
									optionCodeBuilder.append("module.shipping.").append(shipOption.getShippingModuleCode());
									String optionName = messages.getMessage(optionCodeBuilder.toString(),locale);
									shipOption.setOptionName(optionName);
								} catch(Exception e) {//label not found
									LOGGER.warn("No shipping code found for " + optionCodeBuilder.toString());
								}
							}

						}
					
					}
					
					readableSummary.setSelectedShippingOption(quote.getSelectedShippingOption());

					
					readableSummary.setShippingOptions(options);
					
					readableOrder.setShippingSummary(readableSummary);//TODO add readable address
					request.getSession().setAttribute(Constants.SHIPPING_SUMMARY, summary);
					request.getSession().setAttribute(Constants.SHIPPING_OPTIONS, options);
					request.getSession().setAttribute("SHIPPING_INFORMATIONS", readableSummary.getQuoteInformations());
					
					if(configs!=null && configs.containsKey(Constants.DEBUG_MODE)) {
						Boolean debugMode = (Boolean) configs.get(Constants.DEBUG_MODE);
						if(debugMode) {
							
							try {
								ObjectMapper mapper = new ObjectMapper();
								String jsonInString = mapper.writeValueAsString(readableOrder);
								LOGGER.debug("Readable order -> shoppingCartCode[ " + shoppingCartCode + "] -> " + jsonInString);
								System.out.println("Readable order -> shoppingCartCode[ " + shoppingCartCode + "] -> " + jsonInString);
							} catch(Exception de) {
								LOGGER.error(de.getMessage());
							}
							

						}
					}
					
				
				}

				if(quote.getShippingReturnCode()!=null && quote.getShippingReturnCode().equals(ShippingQuote.NO_SHIPPING_MODULE_CONFIGURED)) {
					LOGGER.error("Shipping quote error " + quote.getShippingReturnCode());
					readableOrder.setErrorMessage(messages.getMessage("message.noshipping", locale));
				}
				
				if(quote.getShippingReturnCode()!=null && quote.getShippingReturnCode().equals(ShippingQuote.NO_SHIPPING_TO_SELECTED_COUNTRY)) {
					if(CollectionUtils.isEmpty(quote.getShippingOptions())) {//only if there are no other options
						LOGGER.error("Shipping quote error " + quote.getShippingReturnCode());
						readableOrder.setErrorMessage(messages.getMessage("message.noshipping", locale));
					}
				}
				
				//if(quote.getShippingReturnCode()!=null && quote.getShippingReturnCode().equals(ShippingQuote.NO_POSTAL_CODE)) {
				//	LOGGER.error("Shipping quote error " + quote.getShippingReturnCode());
				//	readableOrder.setErrorMessage(messages.getMessage("message.noshipping", locale));
				//}
				
				if(!StringUtils.isBlank(quote.getQuoteError())) {
					LOGGER.error("Shipping quote error " + quote.getQuoteError());
					readableOrder.setErrorMessage(messages.getMessage("message.noshippingerror", locale));
				}
				
				
			}
			
			//set list of shopping cart items for core price calculation
			List<ShoppingCartItem> items = new ArrayList<ShoppingCartItem>(cart.getLineItems());
			order.setShoppingCartItems(items);
			
			OrderTotalSummary orderTotalSummary = orderFacade.calculateOrderTotal(store, order, language);
			super.setSessionAttribute(Constants.ORDER_SUMMARY, orderTotalSummary, request);
			
			
			ReadableOrderTotalPopulator totalPopulator = new ReadableOrderTotalPopulator();
			totalPopulator.setMessages(messages);
			totalPopulator.setPricingService(pricingService);

			List<ReadableOrderTotal> subtotals = new ArrayList<ReadableOrderTotal>();
			for(OrderTotal total : orderTotalSummary.getTotals()) {
				if(!total.getOrderTotalCode().equals("order.total.total")) {
					ReadableOrderTotal t = new ReadableOrderTotal();
					totalPopulator.populate(total, t, store, language);
					subtotals.add(t);
				} else {//grand total
					ReadableOrderTotal ot = new ReadableOrderTotal();
					totalPopulator.populate(total, ot, store, language);
					readableOrder.setGrandTotal(ot.getTotal());
				}
			}
			
			
			readableOrder.setSubTotals(subtotals);
		
		} catch(Exception e) {
			LOGGER.error("Error while getting shipping quotes",e);
			readableOrder.setErrorMessage(messages.getMessage("message.error", locale));
		}
		
		return readableOrder;
	}

	/**
	 * Calculates the order total following price variation like changing a shipping option
	 * @param order
	 * @param request
	 * @param response
	 * @param locale
	 * @return
	 * @throws Exception
	 */
	@RequestMapping(value={"/calculateOrderTotal.json"}, method=RequestMethod.POST)
	public @ResponseBody ReadableShopOrder calculateOrderTotal(@ModelAttribute(value="order") ShopOrder order, HttpServletRequest request, HttpServletResponse response, Locale locale) throws Exception {
		
		Language language = (Language)request.getAttribute("LANGUAGE");
		MerchantStore store = (MerchantStore)request.getAttribute(Constants.MERCHANT_STORE);
		String shoppingCartCode  = getSessionAttribute(Constants.SHOPPING_CART, request);
		
		Validate.notNull(shoppingCartCode,"shoppingCartCode does not exist in the session");
		
		ReadableShopOrder readableOrder = new ReadableShopOrder();
		try {

			//re-generate cart
			com.salesmanager.core.model.shoppingcart.ShoppingCart cart = shoppingCartFacade.getShoppingCartModel(shoppingCartCode, store);

			ReadableShopOrderPopulator populator = new ReadableShopOrderPopulator();
			populator.populate(order, readableOrder, store, language);
			
			ReadableDelivery readableDelivery = super.getSessionAttribute(Constants.KEY_SESSION_ADDRESS, request);

			if(order.getSelectedShippingOption()!=null) {
						ShippingSummary summary = (ShippingSummary)request.getSession().getAttribute(Constants.SHIPPING_SUMMARY);
						@SuppressWarnings("unchecked")
						List<ShippingOption> options = (List<ShippingOption>)request.getSession().getAttribute(Constants.SHIPPING_OPTIONS);
						
						
						order.setShippingSummary(summary);//for total calculation
						
						
						ReadableShippingSummary readableSummary = new ReadableShippingSummary();
						ReadableShippingSummaryPopulator readableSummaryPopulator = new ReadableShippingSummaryPopulator();
						readableSummaryPopulator.setPricingService(pricingService);
						readableSummaryPopulator.populate(summary, readableSummary, store, language);
						
						//override summary
						readableSummary.setDelivery(readableDelivery);
						
						if(!CollectionUtils.isEmpty(options)) {
						
							//get submitted shipping option
							ShippingOption quoteOption = null;
							ShippingOption selectedOption = order.getSelectedShippingOption();

							
							
							//check if selectedOption exist
							for(ShippingOption shipOption : options) {
																
								StringBuilder moduleName = new StringBuilder();
								moduleName.append("module.shipping.").append(shipOption.getShippingModuleCode());
										
										
								String carrier = messages.getMessage(moduleName.toString(),locale);		
								String note = messages.getMessage(moduleName.append(".note").toString(), locale, "");
										
								shipOption.setNote(note);
								
								shipOption.setDescription(carrier);
								if(!StringUtils.isBlank(shipOption.getOptionId()) && shipOption.getOptionId().equals(selectedOption.getOptionId())) {
									quoteOption = shipOption;
								}
								
								//option name
								if(!StringUtils.isBlank(shipOption.getOptionCode())) {
									//try to get the translate
									StringBuilder optionCodeBuilder = new StringBuilder();
									try {
										
										//optionCodeBuilder.append("module.shipping.").append(shipOption.getShippingModuleCode()).append(".").append(shipOption.getOptionCode());
										optionCodeBuilder.append("module.shipping.").append(shipOption.getShippingModuleCode());
										String optionName = messages.getMessage(optionCodeBuilder.toString(),locale);
										shipOption.setOptionName(optionName);
									} catch(Exception e) {//label not found
										LOGGER.warn("No shipping code found for " + optionCodeBuilder.toString());
									}
								}
							}
							
							if(quoteOption==null) {
								quoteOption = options.get(0);
							}
							
							
							readableSummary.setSelectedShippingOption(quoteOption);
							readableSummary.setShippingOptions(options);							

							summary.setShippingOption(quoteOption.getOptionId());
							summary.setShippingOptionCode(quoteOption.getOptionCode());
							summary.setShipping(quoteOption.getOptionPrice());
							order.setShippingSummary(summary);//override with new summary
							
							
							@SuppressWarnings("unchecked")
							Map<String,String> informations = (Map<String,String>)request.getSession().getAttribute("SHIPPING_INFORMATIONS");
							readableSummary.setQuoteInformations(informations);
						
						}

						
						readableOrder.setShippingSummary(readableSummary);//TODO readable address format
						readableOrder.setDelivery(readableDelivery);
			}
			
			//set list of shopping cart items for core price calculation
			List<ShoppingCartItem> items = new ArrayList<ShoppingCartItem>(cart.getLineItems());
			order.setShoppingCartItems(items);
			
			//order total calculation
			OrderTotalSummary orderTotalSummary = orderFacade.calculateOrderTotal(store, order, language);
			super.setSessionAttribute(Constants.ORDER_SUMMARY, orderTotalSummary, request);
			
			
			ReadableOrderTotalPopulator totalPopulator = new ReadableOrderTotalPopulator();
			totalPopulator.setMessages(messages);
			totalPopulator.setPricingService(pricingService);

			List<ReadableOrderTotal> subtotals = new ArrayList<ReadableOrderTotal>();
			for(OrderTotal total : orderTotalSummary.getTotals()) {
				if(total.getOrderTotalCode() == null || !total.getOrderTotalCode().equals("order.total.total")) {
					ReadableOrderTotal t = new ReadableOrderTotal();
					totalPopulator.populate(total, t, store, language);
					subtotals.add(t);
				} else {//grand total
					ReadableOrderTotal ot = new ReadableOrderTotal();
					totalPopulator.populate(total, ot, store, language);
					readableOrder.setGrandTotal(ot.getTotal());
				}
			}
			
			
			readableOrder.setSubTotals(subtotals);
		
		} catch(Exception e) {
			LOGGER.error("Error while getting shipping quotes",e);
			readableOrder.setErrorMessage(messages.getMessage("message.error", locale));
		}
		
		return readableOrder;
	}
	


}