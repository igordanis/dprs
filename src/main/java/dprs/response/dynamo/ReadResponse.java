package dprs.response.dynamo;

import dprs.entity.VectorClock;
import dprs.util.Tuple;

import java.util.Set;

/**
 * Created by igordanis on 21/05/16.
 */
public class ReadResponse {

    public String nextVectorClockToken;
    public Set<String> uniqValues;

    public ReadResponse() {
    }

    public ReadResponse(String nextVectorClockToken, Set<String> uniqValues) {
        this.nextVectorClockToken = nextVectorClockToken;
        this.uniqValues = uniqValues;
    }


    public String getNextVectorClockToken() {
        return this.nextVectorClockToken;
    }

    public void setNextVectorClockToken(String nextVectorClockToken) {
        this.nextVectorClockToken = nextVectorClockToken;
    }

    public Set<String> getUniqValues() {
        return this.uniqValues;
    }

    public void setUniqValues(Set<String> uniqValues) {
        this.uniqValues = uniqValues;
    }
}
