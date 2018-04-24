package org.t2k.cgs.dao.util;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;

/**
 * Created with IntelliJ IDEA.
 * User: micha.shlain
 * Date: 7/3/13
 * Time: 2:40 PM
 */
@ContextConfiguration("/springContext/applicationContext-MongoDaosTest.xml")
@Test(groups = "ignore")
public class UniqueIdDaoTest extends AbstractTestNGSpringContextTests {

    @Autowired
    UniqueIdMongoDao uniqueIdDao;

    @BeforeMethod
    private void reset() throws Exception{
        uniqueIdDao.reset();
    }

    @Test
    private void testIdGeneration() throws Exception {

        assertEquals(uniqueIdDao.getNextId("bbb"), 1);
        assertEquals(uniqueIdDao.getNextId("ccc"), 1);
        assertEquals(uniqueIdDao.getNextId("ddd"), 1);
        assertEquals(uniqueIdDao.getNextId("bbb"), 2);
        assertEquals(uniqueIdDao.getNextId("bbb"), 3);
        assertEquals(uniqueIdDao.getNextId("ccc"), 2);
    }

}