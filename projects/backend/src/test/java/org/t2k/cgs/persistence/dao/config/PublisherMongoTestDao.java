package org.t2k.cgs.persistence.dao.config;

import com.mongodb.DBObject;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.t2k.cgs.persistence.dao.AccountMongoDao;

public class PublisherMongoTestDao extends AccountMongoDao {

    private static final String PUBLISHERS_COLLECTION = "publishers";

//    @Override
//    public void removeAllItems(String collectionName) throws DataAccessException {
//        Query q = Query.query(Criteria.where(ACCOUNT_ID).ne(AccountMongoDao.DEFAULT_PUBLISHER_ID));
//        getMongoTemplate().remove(q, PUBLISHERS_COLLECTION);
//    }

    public int getPublisherIdByPublisherName(String publisherName) {
        Query q = Query.query(Criteria.where(PUBLISHER_NAME).is(publisherName));
        q.fields().exclude(DB_ID);
        DBObject result = getMongoTemplate().findOne(q, DBObject.class, PUBLISHERS_COLLECTION);
        String resultStr = result == null ? null : result.get(ACCOUNT_ID).toString();
        int id = resultStr == null ? 0 : Integer.parseInt(resultStr);
        return id;
    }
}
