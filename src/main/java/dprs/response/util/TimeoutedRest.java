package dprs.response.util;

import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

/**
 * Created by igordanis on 22/05/16.
 */
public class TimeoutedRest {

    public static RestTemplate getTimeoutedRestTemplate(){
        RestTemplate restTemplate = new RestTemplate();
        ((SimpleClientHttpRequestFactory)restTemplate.getRequestFactory()).setReadTimeout(1000*5);
        return restTemplate;
    }


}
