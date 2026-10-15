
package com.dpt.demo;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Public home page controller.
 *
 * <p>The home page is public; it does not require authentication.
 */
@Controller
public class HomeController {

    /**
     * Render the landing page.
     */
    @RequestMapping("/home")
    public String home() {
        return "home";
    }

    /**
     * Render the landing page at the root context.
     */
    @RequestMapping("/")
    public String Default() {
        return "home";
    }
}