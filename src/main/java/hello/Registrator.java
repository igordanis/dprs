package hello;

import com.ecwid.consul.v1.agent.AgentClient;
import com.ecwid.consul.v1.agent.model.NewService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.consul.discovery.ConsulDiscoveryClient;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Component
public class Registrator implements ApplicationListener<ContextRefreshedEvent> {
    private static final Logger logger = LoggerFactory.getLogger(Registrator.class);

    @Autowired
    ConsulDiscoveryClient discoveryClient;

    @Autowired
    AgentClient agentClient;

    @Override
    public void onApplicationEvent(ContextRefreshedEvent contextRefreshedEvent) {
        // TODO
//        System.out.println("Registrating to proxy");
//        ServiceInstance localService = discoveryClient.getLocalServiceInstance();
//        logger.info("Local: " + localService);
//
//        logger.info("Local host: " + localService.getHost());
//
//        NewService service = new NewService();
//        service.setAddress(localService.getHost());
//        service.setId("dynamo-idea");
//        service.setPort(localService.getPort());
//        service.setName("dynamo-idea");
//        service.setTags(Arrays.asList(new String[]{"dynamo"}));
//
//        agentClient.agentServiceRegister(service);
    }
}
