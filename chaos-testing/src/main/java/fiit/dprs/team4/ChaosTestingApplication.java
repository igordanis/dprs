package fiit.dprs.team4;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class ChaosTestingApplication {

	public static void main(String[] args) {
		SpringApplication.run(ChaosTestingApplication.class, args);
	}









//
//    @Scheduled(fixedDelay = 5000)
//    public void scheduledChaos(){
//        System.out.println("chaoooss !!");
//    }



}
