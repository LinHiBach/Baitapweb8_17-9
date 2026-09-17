package vn.itstar.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class CategoryController {
    @GetMapping({"/categories", "/categories/ajax"})
    public String page() { return "categories/ajax"; }
}
