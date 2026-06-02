package com.example.springbootapp.controller.admin;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AdminWebController {

	@GetMapping({ "/admin", "/admin/" })
	public String adminPage() {
		return "forward:/admin/index.html";
	}

	@GetMapping("/admin/login")
	public String loginPage() {
		return "forward:/admin/login.html";
	}
}
