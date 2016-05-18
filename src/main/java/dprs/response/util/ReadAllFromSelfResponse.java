package dprs.response.util;

import dprs.util.Tuple;

import java.util.List;


public class ReadAllFromSelfResponse {
    private List<Tuple> values;


    public ReadAllFromSelfResponse(){}

    public ReadAllFromSelfResponse(List<Tuple> values){
        this.values = values;
    }

    public List<Tuple> getValues() {
        return this.values;
    }

    public void setValues(List<Tuple> values) {
        this.values = values;
    }
}
