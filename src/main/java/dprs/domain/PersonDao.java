package dprs.domain;

import com.google.gson.Gson;
import dprs.entity.Person;
import org.elasticsearch.action.admin.indices.create.CreateIndexRequest;
import org.elasticsearch.action.exists.ExistsResponse;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.Requests;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

@Service
public class PersonDao extends BaseDao {
    private static final Logger logger = LoggerFactory.getLogger(PersonDao.class);

    private final Client client = getClient();
    private final static String tableName = "person";

    public List<Person> getAllPersons() {
        List<Person> personList = new ArrayList<>();

        if (!indexExists()) {
            client.admin().indices().create(new CreateIndexRequest(indexName)).actionGet();
        }

        SearchResponse response = client.prepareSearch(indexName)
                .setTypes(tableName)
                .setQuery(QueryBuilders.matchAllQuery())
                .execute()
                .actionGet();

        for (SearchHit searchHit : response.getHits()) {
            Person person = new Gson().fromJson(searchHit.getSourceAsString(), Person.class);
            person.setId(searchHit.getId());
            personList.add(person);
        }

        return personList;
    }

    public String savePerson(Person person) {
        IndexResponse response = client.prepareIndex(indexName, tableName)
                .setSource(new Gson().toJson(person)).get();
        return response.getId();
    }

}
