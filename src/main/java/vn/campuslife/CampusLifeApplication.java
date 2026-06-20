package vn.campuslife;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.TimeZone;

@SpringBootApplication
public class CampusLifeApplication {

	public static void main(String[] args) {
		TimeZone.setDefault(TimeZone.getTimeZone("Asia/Ho_Chi_Minh"));
		SpringApplication.run(CampusLifeApplication.class, args);
		System.out.println(new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder().encode("123456"));

	}

}
