package vn.campuslife;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class CampusLifeApplication {

	public static void main(String[] args) {
		SpringApplication.run(CampusLifeApplication.class, args);
		System.out.println(new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder().encode("123456"));

	}

}
