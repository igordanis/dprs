package fiit.dprs.team4.chaos;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class ChaosTestingApplication {

	public static void main(String[] args) {
		SpringApplication
				.run(ChaosTestingApplication.class, args);
	}

}
