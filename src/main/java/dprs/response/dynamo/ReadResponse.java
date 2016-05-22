package dprs.response.dynamo;

import java.util.Set;

/**
 * Created by igordanis on 21/05/16.
 */
public class ReadResponse {

    public String nextVectorClockToken;
    public Set<String> uniqValues;
    public boolean successful;

    public ReadResponse() {
    }

    public ReadResponse(String nextVectorClockToken, Set<String> uniqValues, boolean successful) {
        this.nextVectorClockToken = nextVectorClockToken;
        this.uniqValues = uniqValues;
        this.successful = successful;
    }

    public boolean isSuccessful() {
        return successful;
    }

    public void setSuccessful(boolean successful) {
        this.successful = successful;
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
