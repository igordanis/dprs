package dprs.domain;

import org.elasticsearch.action.exists.ExistsResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.transport.InetSocketTransportAddress;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.concurrent.ExecutionException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class BaseDao {
    private static final Logger logger = LoggerFactory.getLogger(BaseDao.class);

    protected static final String indexName = "database";

    protected Client getClient() {
        TransportClient transportClient = new TransportClient();
        try {
            transportClient.addTransportAddress(new InetSocketTransportAddress(InetAddress.getLocalHost(), 9300));
        } catch (UnknownHostException e) {
            logger.error("Failed to get local host address " + e, e);
        }
        return transportClient;
    }

    protected boolean indexExists() {
        try {
            ExistsResponse indexExistsResponse = getClient().prepareExists(indexName).execute().get();
            return indexExistsResponse.exists();
        } catch (InterruptedException | ExecutionException e) {
            logger.error("Index exists check error", e);
            return false;
        }
    }
}
