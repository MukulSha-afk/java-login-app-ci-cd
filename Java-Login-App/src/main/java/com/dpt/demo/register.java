package com.dpt.demo;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.PreparedStatement;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

@Controller
public class register {
	

	@Value("${spring.datasource.url}")
	private String url;

	@Value("${spring.datasource.username}")
	private String DBusername;

	@Value("${spring.datasource.password}")
	private String DBpassword;

	private final org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder passwordEncoder = new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder();
	
	
	@RequestMapping(value = "register", method = RequestMethod.GET)
	public ModelAndView registerform()
	{
		ModelAndView mv=new ModelAndView("register");
		
		return mv;		
	}
	
	
	@RequestMapping(value = "register", method = RequestMethod.POST)
	public ModelAndView register(@RequestParam("firstName") String firstName, @RequestParam("lastName") String lastName, @RequestParam("email") String email, @RequestParam("userName") String userName, @RequestParam("password") String password)
	{
		ModelAndView mv = new ModelAndView("register");
		if (firstName == null || userName == null || password == null || userName.trim().isEmpty()
				|| password.length() < 8) {
			mv.addObject("message", "Please provide valid details (password min 8 chars).");
			return mv;
		}
		String hashed = passwordEncoder.encode(password);
		//Add employee here (parameterized)
		try (Connection con = DriverManager.getConnection(url, DBusername, DBpassword);
				PreparedStatement st = con.prepareStatement(
						"insert into Employee (first_name,last_name,email,username,password,regdate) values(?,?,?,?,?,CURDATE())")) {

			st.setString(1, firstName);
			st.setString(2, lastName);
			st.setString(3, email);
			st.setString(4, userName);
			st.setString(5, hashed);
			st.executeUpdate();

		} catch (SQLException ex) {
			ex.printStackTrace();
			mv.addObject("message", "Registration failed. Username or email may already exist.");
			return mv;
		}
				
		
		mv.addObject("message", "user account has been added for " + userName);
		return mv;		
	}
	

}
