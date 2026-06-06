package com.example.springbootapp.controller.admin;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import com.example.springbootapp.business.admin.AdminWebBusiness;

@Controller
public class AdminWebController {

	private final AdminWebBusiness adminWebBusiness;

	public AdminWebController(AdminWebBusiness adminWebBusiness) {
		this.adminWebBusiness = adminWebBusiness;
	}

	@GetMapping({ "/admin", "/admin/" })
	public String adminPage() {
		return adminWebBusiness.getAdminPagePath();
	}

	@GetMapping("/admin/login")
	public String loginPage() {
		return adminWebBusiness.getLoginPagePath();
	}
}
