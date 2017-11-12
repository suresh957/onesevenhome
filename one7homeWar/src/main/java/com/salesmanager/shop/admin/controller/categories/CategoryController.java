package com.salesmanager.shop.admin.controller.categories;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import com.salesmanager.core.business.services.merchant.MerchantStoreService;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;
import com.fasterxml.jackson.databind.ObjectMapper;


import com.salesmanager.core.business.services.catalog.category.CategoryService;
import com.salesmanager.core.business.services.merchant.MerchantStoreService;
import com.salesmanager.core.business.services.reference.country.CountryService;
import com.salesmanager.core.business.services.reference.language.LanguageService;
import com.salesmanager.core.business.utils.ajax.AjaxResponse;
import com.salesmanager.core.model.catalog.category.Category;
import com.salesmanager.core.model.catalog.category.CategoryDescription;
import com.salesmanager.core.model.merchant.MerchantStore;
import com.salesmanager.core.model.reference.language.Language;
import com.salesmanager.shop.admin.controller.products.ProductImageRequest;
import com.salesmanager.shop.admin.controller.products.ProductImageResponse;
import com.salesmanager.shop.admin.model.web.Menu;
import com.salesmanager.shop.constants.Constants;
import com.salesmanager.shop.fileupload.services.StorageException;
import com.salesmanager.shop.fileupload.services.StorageService;
import com.salesmanager.shop.utils.LabelUtils;
import com.sun.prism.Image;



@Controller
@CrossOrigin
public class CategoryController {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(CategoryController.class);
	
    @Inject
    private StorageService storageService;

    @Inject
	LanguageService languageService;
	
	@Inject
	CategoryService categoryService;
	
	@Inject
	CountryService countryService;
	
	@Inject
	LabelUtils messages;
	
	@Inject
	MerchantStoreService merchantStoreService;

	@RequestMapping(value="/getCategories", method = RequestMethod.GET, 
			produces = MediaType.APPLICATION_JSON_VALUE)
	@ResponseBody
	public CategoryResponse getCategories() {
		CategoryResponse categoryResponse = new CategoryResponse();
		try
		{
			LOGGER.debug("Entered getCategories");
			List<Category> categories = categoryService.listByStore();
			//CategoryJson[] categoryjson = new CategoryJson[categories.size()];
			Map<String,List<Category>> parentMap = new HashMap<String, List<Category>>();
			
			for(Category category:categories) {
				
				if(category.getParent() != null) {
					List<Category> childCategories = null;
					
					if(parentMap.containsKey(category.getParent().getCode())) {
						childCategories = parentMap.get(category.getParent().getCode());
					}
					else
					{
						childCategories = new ArrayList<Category>();
					}
					childCategories.add(category);
					parentMap.put(category.getParent().getCode(),childCategories);
						
				}
			}
		
			List<CategoryJson> CategoryJsonList = new ArrayList<CategoryJson>();
			int i=0;
			for(Category category:categories) {
				
				if(category.getParent() == null) {
					CategoryJson categoryjson = new CategoryJson();
					categoryjson.setType("category");
					categoryjson.setTitle((category.getDescriptions().get(0)).getName());
					categoryjson.setUrl("/building_materials");
					
					if(parentMap != null && parentMap.containsKey(category.getCode())) {
						System.out.println("child found..."+category.getCode());
						List<Category> subList = parentMap.get(category.getCode());
						
						
						List<SubCategoryJson> subcategoryjsonList = new ArrayList<SubCategoryJson>();
						int j=0;
						for(Category subcategory:subList) {
							
							SubCategoryJson subcategoryjson = new SubCategoryJson();
							subcategoryjson.setType("sub_category");
							subcategoryjson.setTitle((subcategory.getDescriptions().get(0)).getName());
							Images images = new Images(); 
							images.setImage1((subcategory.getDescriptions().get(0)).getSeUrl());
							images.setImage2((subcategory.getDescriptions().get(0)).getMetatagKeywords());
							subcategoryjson.setImages(images);
							j++;
							subcategoryjsonList.add(subcategoryjson);
						}
						categoryjson.setSubCategory(subcategoryjsonList);
						CategoryJsonList.add(categoryjson);
					}
					categoryResponse.setCategoryData(CategoryJsonList);
					i++;
				}
			}
			
			

		}catch(Exception e){
			LOGGER.error("Error while getting categories"+e.getMessage());
			
		}
		LOGGER.debug("Ended getCategories");
		return categoryResponse;
	}

	@RequestMapping(value="/getAllCategories", method = RequestMethod.GET, 
			produces = MediaType.APPLICATION_JSON_VALUE)
	@ResponseBody
	public CategoryResponse getAllCategories() {
		LOGGER.debug("Entered getAllCategories");
		CategoryResponse categoryResponse = new CategoryResponse();
		try
		{
			
			List<Category> categories = categoryService.listByStore();
			Map<String,List<Category>> parentMap = new HashMap<String, List<Category>>();
			
			for(Category category:categories) {
				
				if(category.getParent() != null) {
					List<Category> childCategories = null;
					
					if(parentMap.containsKey(category.getParent().getCode())) {
						childCategories = parentMap.get(category.getParent().getCode());
					}
					else
					{
						childCategories = new ArrayList<Category>();
					}
					childCategories.add(category);
					parentMap.put(category.getParent().getCode(),childCategories);
						
				}
			}
			
			List<CategoryJson> CategoryJsonList = new ArrayList<CategoryJson>();
			int i=0;
			String catTitle = null;
			String subCatTitle = null;
			for(Category category:categories) {
				
				if(category.getParent() == null) {
					CategoryJson categoryjson = new CategoryJson();
					categoryjson.setType("category");
					catTitle = category.getCode();
					categoryjson.setTitle((category.getDescriptions().get(0)).getName());
					catTitle = "/categories/"+catTitle.replaceAll(" ", "_");
					categoryjson.setImageURL((category.getDescriptions().get(0)).getSeUrl());
					categoryjson.setUrl(catTitle);
				
					if(parentMap != null && parentMap.containsKey(category.getCode())) {
						
						List<Category> subList = parentMap.get(category.getCode());
						
						
						List<SubCategoryJson> subcategoryjsonList = new ArrayList<SubCategoryJson>();
						int j=0;
						for(Category subcategory:subList) {
							
							SubCategoryJson subcategoryjson = new SubCategoryJson();
							subcategoryjson.setType("sub_category");
							subCatTitle = subcategory.getCode();
							subcategoryjson.setTitle((subcategory.getDescriptions().get(0)).getName());
							subcategoryjson.setImageURL((subcategory.getDescriptions().get(0)).getSeUrl());
							subCatTitle = subCatTitle.replaceAll(" ", "_");
							subcategoryjson.setUrl("/categories/"+subCatTitle);
							j++;
							subcategoryjsonList.add(subcategoryjson);
						}
						categoryjson.setSubCategory(subcategoryjsonList);
					}
					CategoryJsonList.add(categoryjson);
					categoryResponse.setCategoryData(CategoryJsonList);
					i++;
				}
			}
			
			;

		}catch(Exception e){
			LOGGER.error("Error while getting all categories"+e.getMessage());
			
		}
		LOGGER.debug("Ended getAllCategories");
		return categoryResponse;
	}

	@PreAuthorize("hasRole('PRODUCTS')")
	@RequestMapping(value="/admin/categories/editCategory.html", method=RequestMethod.GET)
	public String displayCategoryEdit(@RequestParam("id") long categoryId, Model model, HttpServletRequest request, HttpServletResponse response) throws Exception {
		return displayCategory(categoryId,model,request,response);

	}
	
	@PreAuthorize("hasRole('PRODUCTS')")
	@RequestMapping(value="/admin/categories/createCategory.html", method=RequestMethod.GET)
	public String displayCategoryCreate(Model model, HttpServletRequest request, HttpServletResponse response) throws Exception {
		return displayCategory(null,model,request,response);

	}
	
	
	
	private String displayCategory(Long categoryId, Model model, HttpServletRequest request, HttpServletResponse response) throws Exception {
		

		//display menu
		setMenu(model,request);
		
		
		MerchantStore store = (MerchantStore)request.getAttribute(Constants.ADMIN_STORE);
		Language language = (Language)request.getAttribute("LANGUAGE");
		
		//get parent categories
		List<Category> categories = categoryService.listByStore(store,language);
		

		List<Language> languages = store.getLanguages();
		Category category = new Category();
		
		if(categoryId!=null && categoryId!=0) {//edit mode
			category = categoryService.getById(categoryId);
		
			
			
			if(category==null || category.getMerchantStore().getId().intValue()!=store.getId().intValue()) {
				return "catalogue-categories";
			}
		} else {
			
			category.setVisible(true);
			
		}
		
		List<CategoryDescription> descriptions = new ArrayList<CategoryDescription>();
		
		for(Language l : languages) {
			
			CategoryDescription description = null;
			if(category!=null) {
				for(CategoryDescription desc : category.getDescriptions()) {
					
					
					if(desc.getLanguage().getCode().equals(l.getCode())) {
						description = desc;
					}
					
					
				}
			}
			
			if(description==null) {
				description = new CategoryDescription();
				description.setLanguage(l);
			}
			
			descriptions.add(description);

		}
		
		category.setDescriptions(descriptions);
	

		
		model.addAttribute("category", category);
		model.addAttribute("categories", categories);
		

		
		return "catalogue-categories-category";
	}
	
	@PreAuthorize("hasRole('PRODUCTS')")
	@RequestMapping(value="/admin/categories/save.html", method=RequestMethod.POST)
	public String saveCategory(@Valid @ModelAttribute("category") Category category, BindingResult result, Model model, HttpServletRequest request) throws Exception {

		Language language = (Language)request.getAttribute("LANGUAGE");
		
		//display menu
		setMenu(model,request);
		
		MerchantStore store = (MerchantStore)request.getAttribute(Constants.ADMIN_STORE);

		if(category.getId() != null && category.getId() >0) { //edit entry
			
			//get from DB
			Category currentCategory = categoryService.getById(category.getId());
			
			if(currentCategory==null || currentCategory.getMerchantStore().getId().intValue()!=store.getId().intValue()) {
				return "catalogue-categories";
			}

		}

			
			Map<String,Language> langs = languageService.getLanguagesMap();
			


			List<CategoryDescription> descriptions = category.getDescriptions();
			if(descriptions!=null) {
				
				for(CategoryDescription description : descriptions) {
					
					String code = description.getLanguage().getCode();
					Language l = langs.get(code);
					description.setLanguage(l);
					description.setCategory(category);
					
					
				}
				
			}
			
			//save to DB
			category.setMerchantStore(store);
		//}
		
		if (result.hasErrors()) {
			return "catalogue-categories-category";
		}
		
		//check parent
		if(category.getParent()!=null) {
			if(category.getParent().getId()==-1) {//this is a root category
				category.setParent(null);
				category.setLineage("/");
				category.setDepth(0);
			}
		}
		
		category.getAuditSection().setModifiedBy(request.getRemoteUser());
		categoryService.saveOrUpdate(category);

			
		//ajust lineage and depth
		if(category.getParent()!=null && category.getParent().getId()!=-1) { 
		
			Category parent = new Category();
			parent.setId(category.getParent().getId());
			parent.setMerchantStore(store);
			
			categoryService.addChild(parent, category);
		
		}
		
		
		//get parent categories
		List<Category> categories = categoryService.listByStore(store,language);
		model.addAttribute("categories", categories);
		

		model.addAttribute("success","success");
		return "catalogue-categories-category";
	}
	
	
	//category list
	@PreAuthorize("hasRole('PRODUCTS')")
	@RequestMapping(value="/admin/categories/categories.html", method=RequestMethod.GET)
	public String displayCategories(Model model, HttpServletRequest request, HttpServletResponse response) throws Exception {

		
		
		setMenu(model,request);
		
		//does nothing, ajax subsequent request
		
		return "catalogue-categories";
	}
	
	@SuppressWarnings({ "unchecked"})
	@PreAuthorize("hasRole('PRODUCTS')")
	@RequestMapping(value="/admin/categories/paging.html", method=RequestMethod.POST)
	public @ResponseBody ResponseEntity<String> pageCategories(HttpServletRequest request, HttpServletResponse response) {
		String categoryName = request.getParameter("name");
		String categoryCode = request.getParameter("code");


		AjaxResponse resp = new AjaxResponse();

		
		try {
			
			Language language = (Language)request.getAttribute("LANGUAGE");
				
		
			MerchantStore store = (MerchantStore)request.getAttribute(Constants.ADMIN_STORE);
			
			List<Category> categories = null;
					
			if(!StringUtils.isBlank(categoryName)) {
				
				
				categories = categoryService.getByName(store, categoryName, language);
				
			} else if(!StringUtils.isBlank(categoryCode)) {
				
				categoryService.listByCodes(store, new ArrayList<String>(Arrays.asList(categoryCode)), language);
			
			} else {
				
				categories = categoryService.listByStore(store, language);
				
			}
					
					
			
			for(Category category : categories) {
				
				@SuppressWarnings("rawtypes")
				Map entry = new HashMap();
				entry.put("categoryId", category.getId());
				
				CategoryDescription description = category.getDescriptions().get(0);
				
				entry.put("name", description.getName());
				entry.put("code", category.getCode());
				entry.put("visible", category.isVisible());
				resp.addDataEntry(entry);
				
				
			}
			
			resp.setStatus(AjaxResponse.RESPONSE_STATUS_SUCCESS);
			

		
		} catch (Exception e) {
			LOGGER.error("Error while paging categories", e);
			resp.setStatus(AjaxResponse.RESPONSE_STATUS_FAIURE);
		}
		
		String returnString = resp.toJSONString();
		final HttpHeaders httpHeaders= new HttpHeaders();
	    httpHeaders.setContentType(MediaType.APPLICATION_JSON_UTF8);
		
	    return new ResponseEntity<String>(returnString,httpHeaders,HttpStatus.OK);
	}
	
	@PreAuthorize("hasRole('PRODUCTS')")
	@RequestMapping(value="/admin/categories/hierarchy.html", method=RequestMethod.GET)
	public String displayCategoryHierarchy(Model model, HttpServletRequest request, HttpServletResponse response) throws Exception {

		
		
		setMenu(model,request);
		
		//get the list of categories
		Language language = (Language)request.getAttribute("LANGUAGE");
		MerchantStore store = (MerchantStore)request.getAttribute(Constants.ADMIN_STORE);
		
		List<Category> categories = categoryService.listByStore(store, language);
		
		System.out.println("categories =="+categories);
		
		model.addAttribute("categories", categories);
		
		return "catalogue-categories-hierarchy";
	}
	
	@PreAuthorize("hasRole('PRODUCTS')")
	@RequestMapping(value="/admin/categories/remove.html", method=RequestMethod.POST)
	public @ResponseBody ResponseEntity<String> deleteCategory(HttpServletRequest request, HttpServletResponse response, Locale locale) {
		String sid = request.getParameter("categoryId");

		MerchantStore store = (MerchantStore)request.getAttribute(Constants.ADMIN_STORE);
		
		AjaxResponse resp = new AjaxResponse();

		
		try {
			
			Long id = Long.parseLong(sid);
			
			Category category = categoryService.getById(id);
			
			if(category==null || category.getMerchantStore().getId().intValue() !=store.getId().intValue() ) {

				resp.setStatusMessage(messages.getMessage("message.unauthorized", locale));
				resp.setStatus(AjaxResponse.RESPONSE_STATUS_FAIURE);			
				
			} else {
				
				categoryService.delete(category);
				resp.setStatus(AjaxResponse.RESPONSE_OPERATION_COMPLETED);
				
			}
		
		
		} catch (Exception e) {
			LOGGER.error("Error while deleting category", e);
			resp.setStatus(AjaxResponse.RESPONSE_STATUS_FAIURE);
			resp.setErrorMessage(e);
		}
		
		String returnString = resp.toJSONString();
		final HttpHeaders httpHeaders= new HttpHeaders();
	    httpHeaders.setContentType(MediaType.APPLICATION_JSON_UTF8);
		return new ResponseEntity<String>(returnString,httpHeaders,HttpStatus.OK);
	}
	
	@PreAuthorize("hasRole('PRODUCTS')")
	@RequestMapping(value="/admin/categories/moveCategory.html", method=RequestMethod.POST)
	public @ResponseBody ResponseEntity<String> moveCategory(HttpServletRequest request, HttpServletResponse response, Locale locale) {
		String parentid = request.getParameter("parentId");
		String childid = request.getParameter("childId");

		MerchantStore store = (MerchantStore)request.getAttribute(Constants.ADMIN_STORE);
		
		AjaxResponse resp = new AjaxResponse();
		final HttpHeaders httpHeaders= new HttpHeaders();
	    httpHeaders.setContentType(MediaType.APPLICATION_JSON_UTF8);

		
		try {
			
			Long parentId = Long.parseLong(parentid);
			Long childId = Long.parseLong(childid);
			
			Category child = categoryService.getById(childId);
			Category parent = categoryService.getById(parentId);
			
			if(child.getParent().getId()==parentId) {
				resp.setStatus(AjaxResponse.RESPONSE_OPERATION_COMPLETED);
				String returnString = resp.toJSONString();
			}

			if(parentId!=1) {
			
				if(child==null || parent==null || child.getMerchantStore().getId()!=store.getId() || parent.getMerchantStore().getId()!=store.getId()) {
					resp.setStatusMessage(messages.getMessage("message.unauthorized", locale));
					
					resp.setStatus(AjaxResponse.RESPONSE_STATUS_FAIURE);
					String returnString = resp.toJSONString();
					return new ResponseEntity<String>(returnString,httpHeaders,HttpStatus.OK);
				}
				
				if(child.getMerchantStore().getId()!=store.getId() || parent.getMerchantStore().getId()!=store.getId()) {
					resp.setStatusMessage(messages.getMessage("message.unauthorized", locale));
					resp.setStatus(AjaxResponse.RESPONSE_STATUS_FAIURE);
					String returnString = resp.toJSONString();
					return new ResponseEntity<String>(returnString,httpHeaders,HttpStatus.OK);
				}
			
			}
			

			parent.getAuditSection().setModifiedBy(request.getRemoteUser());
			categoryService.addChild(parent, child);
			resp.setStatus(AjaxResponse.RESPONSE_OPERATION_COMPLETED);

		} catch (Exception e) {
			LOGGER.error("Error while moving category", e);
			resp.setStatus(AjaxResponse.RESPONSE_STATUS_FAIURE);
			resp.setErrorMessage(e);
		}
		
		String returnString = resp.toJSONString();
		
		return new ResponseEntity<String>(returnString,httpHeaders,HttpStatus.OK);
	}
	
	@PreAuthorize("hasRole('PRODUCTS')")
	@RequestMapping(value="/admin/categories/checkCategoryCode.html", method=RequestMethod.POST)
	public @ResponseBody ResponseEntity<String> checkCategoryCode(HttpServletRequest request, HttpServletResponse response, Locale locale) {
		String code = request.getParameter("code");
		String id = request.getParameter("id");


		MerchantStore store = (MerchantStore)request.getAttribute(Constants.ADMIN_STORE);
		
		
		AjaxResponse resp = new AjaxResponse();
		
		
		final HttpHeaders httpHeaders= new HttpHeaders();
	    httpHeaders.setContentType(MediaType.APPLICATION_JSON_UTF8);
		
		if(StringUtils.isBlank(code)) {
			resp.setStatus(AjaxResponse.CODE_ALREADY_EXIST);
			String returnString = resp.toJSONString();
			return new ResponseEntity<String>(returnString,httpHeaders,HttpStatus.OK);
		}

		
		try {
			
		Category category = categoryService.getByCode(store, code);
		
		if(category!=null && StringUtils.isBlank(id)) {
			resp.setStatus(AjaxResponse.CODE_ALREADY_EXIST);
			String returnString = resp.toJSONString();
			return new ResponseEntity<String>(returnString,httpHeaders,HttpStatus.OK);
		}
		
		
		if(category!=null && !StringUtils.isBlank(id)) {
			try {
				Long lid = Long.parseLong(id);
				
				if(category.getCode().equals(code) && category.getId().longValue()==lid) {
					resp.setStatus(AjaxResponse.CODE_ALREADY_EXIST);
					String returnString = resp.toJSONString();
					return new ResponseEntity<String>(returnString,httpHeaders,HttpStatus.OK);
				}
			} catch (Exception e) {
				resp.setStatus(AjaxResponse.CODE_ALREADY_EXIST);
				String returnString = resp.toJSONString();
				return new ResponseEntity<String>(returnString,httpHeaders,HttpStatus.OK);
			}

		}
		
		
		
		

	
		
			


			resp.setStatus(AjaxResponse.RESPONSE_OPERATION_COMPLETED);

		} catch (Exception e) {
			LOGGER.error("Error while getting category", e);
			resp.setStatus(AjaxResponse.RESPONSE_STATUS_FAIURE);
			resp.setErrorMessage(e);
		}
		
		String returnString = resp.toJSONString();

		return new ResponseEntity<String>(returnString,httpHeaders,HttpStatus.OK);
	}
	
	private void setMenu(Model model, HttpServletRequest request) throws Exception {
		
		//display menu
		Map<String,String> activeMenus = new HashMap<String,String>();
		activeMenus.put("catalogue", "catalogue");
		activeMenus.put("catalogue-categories", "catalogue-categories");
		
		@SuppressWarnings("unchecked")
		Map<String, Menu> menus = (Map<String, Menu>)request.getAttribute("MENUMAP");
		
		System.out.println("menus =="+menus);
		Menu currentMenu = (Menu)menus.get("catalogue");
		model.addAttribute("currentMenu",currentMenu);
		model.addAttribute("activeMenus",activeMenus);
		//
		
	}

	@RequestMapping(value="/createCategory", method = RequestMethod.POST, 
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
	@ResponseBody
	public CreateCategoryResponse createCategory(@RequestBody CreateCategoryRequest createCategoryRequest) throws Exception {
		LOGGER.debug("Entered createCategory");
		CreateCategoryResponse createCategoryResponse = new CreateCategoryResponse();
		try {
			MerchantStore merchantStore=merchantStoreService.getMerchantStore(MerchantStore.DEFAULT_STORE);
			String categoryName = createCategoryRequest.getCategoryName();
			String parentName = createCategoryRequest.getParentName();
			categoryName = categoryName.replaceAll("_", " ");
			parentName = parentName.replaceAll("_", " ");
			Category category = categoryService.getByCategoryCode(categoryName);
			Category categoryParent = categoryService.getByCategoryCode(parentName);
			
			if(category != null){
				LOGGER.debug("category already exists.");
				createCategoryResponse.setStatus(false);
				createCategoryResponse.setErrorMessage("category already exists.");
			} else {
				Category childCat = new Category();
				childCat.setCode(categoryName);
				childCat.setMerchantStore(merchantStore);
				//check parent
				if(categoryParent == null) {
						childCat.setParent(null);
						childCat.setLineage("/");
						childCat.setDepth(0);
					} else {
						categoryService.addChild(categoryParent, childCat);
					}
				
				categoryService.saveOrUpdate(childCat);
				LOGGER.debug("Category created");
				createCategoryResponse.setStatus(true);
				createCategoryResponse.setCategoryId(childCat.getId());
				
			}
		} catch (Exception e){
			LOGGER.error("Category already exist"+e.getMessage());
			createCategoryResponse.setStatus(false);
			createCategoryResponse.setErrorMessage("category already exists.");
		}
		LOGGER.debug("Ended createCategory");
		return createCategoryResponse;

	}
	
	@RequestMapping(value="/createCategoryWithImage", method = RequestMethod.POST)
	@ResponseBody
	public CreateCategoryResponse createCategoryWithImage(@RequestPart("createCategoryRequest") String createCategoryRequestStr,
			@RequestPart("file") MultipartFile categoryUploadedImage) throws Exception {
		LOGGER.debug("Entered createCategoryWithImage");
		CreateCategoryRequest createCategoryRequest = new ObjectMapper().readValue(createCategoryRequestStr, CreateCategoryRequest.class);
		CreateCategoryResponse createCategoryResponse = new CreateCategoryResponse();
    	String fileName = "";
    	if(categoryUploadedImage.getSize() != 0) {
    		try{
    			LOGGER.debug("Storing image"+categoryUploadedImage.getName());
    			fileName = storageService.store(categoryUploadedImage,"category");
    			System.out.println("fileName "+fileName);
    		}catch(StorageException se){
    			LOGGER.error("StoreException occured, do we need continue "+se);
    			createCategoryResponse.setErrorMessage("Failed while storing image");
    			createCategoryResponse.setStatus(false);
    			return createCategoryResponse;
    		}
    	}
		try {
			MerchantStore merchantStore=merchantStoreService.getMerchantStore(MerchantStore.DEFAULT_STORE);
			String categoryName = createCategoryRequest.getCategoryName();
			String parentName = createCategoryRequest.getParentName();
			categoryName = categoryName.replaceAll("_", " ");
			parentName = parentName.replaceAll("_", " ");
			Category category = categoryService.getByCategoryCode(categoryName);
			Category categoryParent = categoryService.getByCategoryCode(parentName);
			Language language = new Language();
			language.setId(1);

			if(category != null){
				LOGGER.debug("category already exists with code:"+categoryName);
				createCategoryResponse.setStatus(false);
				createCategoryResponse.setErrorMessage("category already exists with code:"+categoryName);
			} else {
				Category childCat = new Category();
				childCat.setCode(categoryName);
				childCat.setMerchantStore(merchantStore);
				CategoryDescription categoryDescription = new CategoryDescription();
				categoryDescription.setCategory(childCat);
				categoryDescription.setName(categoryName);
				categoryDescription.setDescription(categoryName);
				categoryDescription.setSeUrl(fileName);
				categoryDescription.setLanguage(language);
				categoryDescription.setMetatagDescription(categoryName);
				categoryDescription.setMetatagTitle(categoryName);
				categoryDescription.setMetatagKeywords(categoryName);
				List<CategoryDescription> descriptions = new ArrayList<CategoryDescription>();
				descriptions.add(categoryDescription);
				childCat.setDescriptions(descriptions);
				//check parent
				if(categoryParent == null) {
						childCat.setParent(null);
						childCat.setLineage("/");
						childCat.setDepth(0);
					} else {
						categoryService.addChild(categoryParent, childCat);
					}
				
				
				categoryService.saveOrUpdate(childCat);
				LOGGER.debug("Category created with image");
				createCategoryResponse.setStatus(true);
				createCategoryResponse.setCategoryId(childCat.getId());
				
			}
		} catch (Exception e){
			LOGGER.error("Fail while creating category with image");
			createCategoryResponse.setStatus(false);
			createCategoryResponse.setErrorMessage("Fail while creating category");
		}
		LOGGER.debug("Ended createCategoryWithImage");
		return createCategoryResponse;

	}
	@RequestMapping(value="/deleteCategory", method = RequestMethod.POST, 
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
	@ResponseBody
	public CreateCategoryResponse deleteCategory(@RequestBody CreateCategoryRequest createCategoryRequest) throws Exception {
		LOGGER.debug("Entered deleteCategory");
		CreateCategoryResponse createCategoryResponse = new CreateCategoryResponse();
		try {
			String categoryName = createCategoryRequest.getCategoryName();
			categoryName = categoryName.replaceAll("_", " ");
			Category category = categoryService.getByCategoryCode(categoryName);
			
			if(category == null){
				LOGGER.debug("No category exists with category code:"+categoryName);
				createCategoryResponse.setStatus(false);
				createCategoryResponse.setErrorMessage("No category exists with category code:"+categoryName);
			} else {
				categoryService.delete(category);
				LOGGER.debug("Category deleted");
				createCategoryResponse.setStatus(true);
				createCategoryResponse.setCategoryId(category.getId());
			}
		} catch (Exception e){
			LOGGER.error("Unable to delete the category"+e.getMessage());
			createCategoryResponse.setStatus(false);
			createCategoryResponse.setErrorMessage("unable to delete the category");
		}
		LOGGER.debug("Ended deleteCategory");
		return createCategoryResponse;

	}
	@RequestMapping(value="/getCategoryWithImages", method = RequestMethod.GET, 
			produces = MediaType.APPLICATION_JSON_VALUE)
	@ResponseBody
	public CategoryImageResponse getAllCategoryWithImages() {
		LOGGER.debug("Entered getAllCategoryWithImages");
		CategoryImageResponse categoryImageResponse = new CategoryImageResponse();
		try
		{
			List<CategoryImageJson> categoryJsonList = new ArrayList<CategoryImageJson>();
			List<Category> categories = categoryService.listByStore();
			int i=0;
			String catTitle = null;
			for(Category category:categories) {
				if(category.getParent()==null) {
					CategoryImageJson categoryImageJson = new CategoryImageJson();
					
					categoryImageJson.setType("category");
					catTitle = category.getCode();
					categoryImageJson.setTitle((category.getDescriptions().get(0)).getName());
					catTitle = "/categories/"+catTitle.replaceAll(" ", "_");
					categoryImageJson.setImageURL((category.getDescriptions().get(0)).getSeUrl());
					categoryImageJson.setUrl(catTitle);
					categoryImageJson.setImageURL1(category.getCategoryImage());
					categoryImageJson.setImageURL2(category.getCategoryImage1());
					categoryJsonList.add(categoryImageJson);	
					i++;
				}
				
			}
			categoryImageResponse.setCategoryImagedata(categoryJsonList);
		
		}catch(Exception e) {
			LOGGER.error("Error while getting category with images");
		}
	
		return categoryImageResponse;	
	}
}
