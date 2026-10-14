package com.dpt.demo;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.PreparedStatement;

import javax.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

@Controller
public class login {

	@Value("${spring.datasource.url}")
	private String url;

	@Value("${spring.datasource.username}")
	private String DBusername;

	@Value("${spring.datasource.password}")
	private String DBpassword;

	private final org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder passwordEncoder = new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder();

	private String errorMessage = "";// legacy field, kept for compat (per-request locals used instead)
	
	@RequestMapping(value = "login", method = RequestMethod.POST)
	public ModelAndView login(@RequestParam("userName") String userName, @RequestParam("password") String password, HttpSession session) {

		String userId = "";
		String reqError = "";

		
		// validate user credentials (parameterized query prevents SQL injection)
		String query = "select username, password from Employee where username=?";
		try (Connection con = DriverManager.getConnection(url, DBusername, DBpassword);
				PreparedStatement st = con.prepareStatement(query)) {
			st.setString(1, userName);
			try (ResultSet rs = st.executeQuery()) {
				if (rs.next()) {
					String dbUser = rs.getString(1);
					String stored = rs.getString(2);
					boolean ok = false;
					try {
						ok = passwordEncoder.matches(password, stored);
					} catch (IllegalArgumentException e) {
						ok = false;
					}
					if (ok || password.equals(stored)) {
						userId = dbUser;
						session.setAttribute("username", userId);
					}
				}
			}
		} catch (SQLException ex) {
			System.out.println("Login DB error: " + ex.getMessage());
			reqError = "Login failed due to a server error. Please try again later.";
		}

		ModelAndView mv;
		if (!userId.equals(""))
		{			
			mv = new ModelAndView("user");
			mv.addObject("username", userId);
		}
		else
		{
			
			mv = new ModelAndView("login");
			mv.addObject("errorMessage", reqError.isEmpty() ? "Invalid username or password." : reqError);
		}

		return mv;
	}
	
	
	
	@RequestMapping(value = "login", method = RequestMethod.GET)
	public ModelAndView registerform()
	{
		ModelAndView mv=new ModelAndView("login");
		
		return mv;		
	}

	@RequestMapping(value = "logout", method = RequestMethod.GET)
	public ModelAndView logout(HttpSession session)
	{
		if (session != null) {
			session.invalidate();
		}
		return new ModelAndView("logout");
	}

}
