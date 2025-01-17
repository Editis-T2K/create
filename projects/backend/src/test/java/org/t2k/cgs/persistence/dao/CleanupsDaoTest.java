package org.t2k.cgs.persistence.dao;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.t2k.cgs.Application;
import org.t2k.cgs.domain.model.cleanup.CleanupJob;
import org.t2k.cgs.domain.model.cleanup.CleanupType;
import org.t2k.cgs.domain.model.cleanup.CleanupsDao;
import org.t2k.cgs.domain.model.tocItem.TocItemCGSObject;
import org.t2k.cgs.domain.model.tocItem.TocItemDao;
import org.t2k.sample.dao.exceptions.DaoException;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.Date;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: micha.shlain
 * Date: 11/7/12
 * Time: 6:06 PM
 */
@SpringApplicationConfiguration(classes = Application.class)
@ActiveProfiles("test")
public class CleanupsDaoTest extends AbstractTestNGSpringContextTests {

    @Autowired
    private CleanupsDao cleanupsDao;

    @Autowired
    private TocItemDao lessonsDao;

    private Date lastModified = new Date();
    private Date created = new Date();
    private String courseId = "abc";
    private CleanupType courseType = CleanupType.COURSE;
    private int pubId = 1;
    private String tocItemId = "tocId";

    @Test
    public void insertCleanupTest() throws DaoException {
        CleanupJob c = createNewCleanupJob();
        cleanupsDao.insertOrUpdateCleanup(c);
        CleanupJob c2 = cleanupsDao.getCleanup(c.getPublisherId(), c.getCourseId(), c.getTocItemId(), c.getCleanupType(), c.getStatus());
//        Assert.assertEquals(c.getLastModified(), c2.getLastModified());
        Assert.assertEquals(c.getCourseId(), c2.getCourseId());
        Assert.assertEquals(c.getStatus(), c.getStatus());
        Assert.assertEquals(c.getTocItemId(), c.getTocItemId());
    }

    @Test
    public void getWaitingCleanupsTest() throws DaoException {
        CleanupJob c1 = createNewCleanupJob();
        CleanupJob c2 = createNewCleanupJob();
        CleanupJob c3 = createNewCleanupJob();
        c1.setCourseId("a");
        c1.setLastModified(new Date(-2000));

        c2.setCourseId("b");
        c2.setLastModified(new Date(-1000));

        c3.setCourseId("c");
        c3.setLastModified(new Date(0));
        cleanupsDao.insertOrUpdateCleanup(c1);
        cleanupsDao.insertOrUpdateCleanup(c2);
        cleanupsDao.insertOrUpdateCleanup(c3);

        List<CleanupJob> waitingCleanups = cleanupsDao.getWaitingCleanupJobs(3, CleanupType.COURSE);

        int index = 0;
        CleanupJob curr = waitingCleanups.get(index);
        while (index < waitingCleanups.size() - 1) {
            CleanupJob next = waitingCleanups.get(index + 1);
            Assert.assertTrue(curr.getLastModified().before(next.getLastModified()));
            logger.debug(curr);
            curr = next;
            logger.debug(curr);
            index++;
        }
    }

    @Test
    public void saveAllCourseLessonsToCleanUpCollection() throws DaoException {
        int publisherId = 1;
        String courseId = "ff61397d-c4ac-4fa9-9ebf-c6f8edb62454";
        List<TocItemCGSObject> lessons = lessonsDao.getByCourse(publisherId, courseId, false);

        for (TocItemCGSObject lesson : lessons) {
            CleanupJob cleanupJob = new CleanupJob(lesson.getPublisherId(), lesson.getCourseId(), lesson.getContentId(), lesson.getEntityId(), CleanupType.LESSON);
            cleanupsDao.insertOrUpdateCleanup(cleanupJob);
        }
    }

    private CleanupJob createNewCleanupJob() {
        CleanupJob cleanupJob = new CleanupJob(pubId, courseId, null, null, courseType);
        return cleanupJob;
    }
}
