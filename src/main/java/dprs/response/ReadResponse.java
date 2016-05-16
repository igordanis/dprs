package dprs.response;

import java.util.List;


public class ReadResponse{
    private List<Tuple> values;


    public ReadResponse(){}

    public ReadResponse(List<Tuple> values){
        this.values = values;
    }

    public List<Tuple> getValues() {
        return this.values;
    }

    public void setValues(List<Tuple> values) {
        this.values = values;
    }
}
