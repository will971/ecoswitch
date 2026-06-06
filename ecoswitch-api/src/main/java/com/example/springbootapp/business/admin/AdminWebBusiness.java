package com.example.springbootapp.business.admin;

import org.springframework.stereotype.Component;

@Component
public class AdminWebBusiness {

    public String getAdminPagePath() {
        return "forward:/admin/index.html";
    }

    public String getLoginPagePath() {
        return "forward:/admin/login.html";
    }
}
