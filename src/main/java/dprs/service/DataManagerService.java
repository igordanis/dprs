package dprs.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class DataManagerService {

    @Autowired
    ChordService chordService;

    @Value("${quorum.replication}")
    int replicationQuorum;

    private static final Logger logger = LoggerFactory.getLogger(DataManagerService.class);

}
