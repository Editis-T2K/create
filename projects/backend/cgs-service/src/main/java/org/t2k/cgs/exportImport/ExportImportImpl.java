package org.t2k.cgs.exportImport;

import com.t2k.configurations.Configuration;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.AgeFileFilter;
import org.apache.log4j.Logger;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.t2k.cgs.course.CourseDataService;
import org.t2k.cgs.dao.applets.AppletDao;
import org.t2k.cgs.dao.cms.FileDaoImpl;
import org.t2k.cgs.dao.courses.CoursesDao;
import org.t2k.cgs.dao.publisher.AccountDao;
import org.t2k.cgs.dao.standards.StandardsDao;
import org.t2k.cgs.dao.tocItem.TocItemDao;
import org.t2k.cgs.dataServices.exceptions.DsException;
import org.t2k.cgs.dataServices.exceptions.FileIsEmptyOrNoFileInRequestException;
import org.t2k.cgs.ebooks.EBookCleanupService;
import org.t2k.cgs.ebooks.EBookService;
import org.t2k.cgs.lock.LockService;
import org.t2k.cgs.model.course.Course;
import org.t2k.cgs.model.job.JobService;
import org.t2k.cgs.security.CGSUserDetails;
import org.t2k.cgs.sequences.SequenceService;
import org.t2k.cgs.tocItem.TocItemDataService;
import org.t2k.cgs.utils.FilesUtils;
import org.t2k.cgs.version.VersionService;
import org.t2k.sample.dao.exceptions.DaoException;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.*;

/**
 * Created by IntelliJ IDEA.
 * User: efrat.gur
 * Date: 24/09/13
 * Time: 06:10
 */
@Service
public class ExportImportImpl implements ExportImportService {

    private static Logger logger = Logger.getLogger(ExportImportImpl.class);

    private static final int DEFAULT_BUFFER_SIZE = 1000 * 1024; //1M bytes in memory while upload/download;

    @Autowired
    private CoursesDao coursesDao;

    @Autowired
    private CourseDataService courseDataService;

    @Autowired
    private TocItemDao lessonsDao;

    @Autowired
    private TocItemDao assessmentsDao;

    @Autowired
    private SequenceService sequenceService;

    @Autowired
    private LockService lockService;

    @Autowired
    private Configuration configuration;

    @Autowired
    private VersionService versionService;

    @Autowired
    private StandardsDao standardsDao;

    @Autowired
    private AppletDao appletDao;

    @Autowired
    private AccountDao publisherDao;

    @Autowired
    @Qualifier(value = "lessonsDataServiceBean")
    private TocItemDataService tocItemDataService;

    @Autowired
    private JobService jobService;

    @Autowired
    private FileDaoImpl fileDao;

    @Autowired
    private EBookService eBookService;

    @Autowired
    private EBookCleanupService eBookCleanupService;

    @Autowired
    private FilesUtils filesUtils;

    @Autowired
    private AsyncTaskExecutor asyncTaskExecutor;

    /**
     * Export full cgs course.
     *
     * @param publisherId
     * @param courseId
     * @param jobId
     * @param cgsUserDetails
     * @throws Exception
     */
    public void exportCourse(int publisherId, String courseId, String jobId, CGSUserDetails cgsUserDetails) throws Exception {
        logger.info(String.format("exportCourse: courseId: %s, publisherId: %d", courseId, publisherId));
        ExportImportPackage ExImPackage = createPackageForExport(publisherId, courseId, jobId, cgsUserDetails);
        addExImPackageToPending(ExImPackage);
    }

    /**
     * Import full cgs course.
     *
     * @param publisherId
     * @param jobId
     * @param validationId
     * @param cgsUserDetails
     * @throws Exception
     */
    public void importCourse(int publisherId, String jobId, String validationId, CGSUserDetails cgsUserDetails) throws Exception {
        logger.info(String.format("importCourse: publisherId %d", publisherId));
        ExportImportPackage ExImPackage = createPackageForImport(publisherId, jobId, validationId, cgsUserDetails);
        addExImPackageToPending(ExImPackage);
    }

    /**
     * @param items    List of FileItems, one of which is a file to upload. The list is typically generated by
     *                 ServletFileUpload.parseRequest.
     * @param filePath
     * @throws DsException
     */
    public String uploadFile(List items, String filePath) throws DsException {
        Iterator iter = items.iterator();

        String filename = "";
        String fullFileName = "";
        try {
            while (iter.hasNext()) {
                FileItem item = (FileItem) iter.next();
                if (!item.isFormField()) {
                    //make sure that the file isn't empty
                    if (item.getSize() == 0) {
                        throw new FileIsEmptyOrNoFileInRequestException(item.getName(), "Uploading empty files is not allowed");
                    }

                    //upload the file.
                    filename = item.getName();
                    fullFileName = String.format("%s/%s", filePath, filename);
                    File f = new File(fullFileName);
                    item.write(f);
                }
            }
        } catch (Exception e) {
            throw new DsException(String.format("failed uploading %s", filePath), e);
        }

        return fullFileName;
    }

    @Override
    public void downloadExportedCourse(String exportedCourseFileName, HttpServletResponse response) throws IOException {
        DataInputStream in = null;
        ServletOutputStream outStream = null;
        try {
            String output_directory = configuration.getProperty("exportedCourseLocation");
            String file_full_path = output_directory + exportedCourseFileName;

            File file = new File(file_full_path);

            int length;
            in = new DataInputStream(new FileInputStream(file));
            outStream = response.getOutputStream();

            String mimetype = "application/octet-stream";
            response.setContentType(mimetype);
            response.setContentLength((int) file.length());

            // sets HTTP header
            response.setHeader("Content-Disposition", String.format("attachment; filename=\"%s\"", exportedCourseFileName));
            logger.info(String.format("getCourse. file name: %s size: %d", exportedCourseFileName, file.length()));
            byte[] byteBuffer = new byte[DEFAULT_BUFFER_SIZE];

            // reads the file's bytes and writes them to the response stream
            while ((length = in.read(byteBuffer)) != -1) {
                outStream.write(byteBuffer, 0, length);
            }
            outStream.flush();
        } catch (IOException e) {
            logger.error(String.format("downloadExportedCourse for file %s IO Error ", exportedCourseFileName), e);
        } finally {
            if (in != null) {
                in.close();
            }
            if (outStream != null) {
                outStream.close();
            }
        }
    }

    /**
     * Validate the import - currently compare the export and the import version and verify that the versions match.
     *
     * @param zipFile
     * @throws Exception
     */
    public void validationBeforeImport(String zipFile, String jobId) throws Exception {
        ExportImportPackage exImPackage = createPackageForValidation(jobId);     // creates a package entity in DB
        exImPackage.setZipFileFullPathName(zipFile);
        addExImPackageToPending(exImPackage);
    }

    /**
     * Adds the package to the execution pool as a Future task.
     *
     * @param exImPackage
     * @throws DsException
     */
    private void addExImPackageToPending(ExportImportPackage exImPackage) throws DsException {
        Assert.notNull(exImPackage);
        logger.info(String.format("addExImPackageToPending: packageId: %s", exImPackage.getPackId()));

        ExportImportHandlerImpl exportImportHandler = new ExportImportHandlerImpl(exImPackage, coursesDao,
                courseDataService, lessonsDao, assessmentsDao, sequenceService, lockService, configuration,
                standardsDao, appletDao, publisherDao, jobService, tocItemDataService, eBookService, filesUtils, versionService,
                eBookCleanupService);

        ExportImportTask task = new ExportImportTask(exportImportHandler, "");
        asyncTaskExecutor.submit(task);
    }

    /**
     * Override the RejectedExecutionHandler to handle rejections.
     */
    public class PackRejectedExecutionHandlerImpl implements RejectedExecutionHandler {

        @Override
        public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
            String message = String.format("rejectedExecution: active: %d waiting: %d", executor.getActiveCount(), executor.getQueue().size());
            logger.warn(String.format("Rejected execution: %s", message));
            throw new RejectedExecutionException(message);
        }
    }

    /**
     * Create a ExportImportPackage for export process.
     *
     * @param publisherId
     * @param courseId
     * @param jobId
     * @param cgsUserDetails @return  @throws PackagingException
     */
    private ExportImportPackage createPackageForExport(int publisherId, String courseId, String jobId, CGSUserDetails cgsUserDetails) throws DaoException {
        ExportImportPackage.Type type = ExportImportPackage.Type.EXPORT;
        Course course = coursesDao.getCourse(publisherId, courseId);
        ExportImportPackage exportImportPackage = new ExportImportPackage(type, courseId, publisherId, jobId, cgsUserDetails.getUsername());
        return exportImportPackage;
    }

    /**
     * Create a ExportImportPackage for import process.
     *
     * @param publisherId
     * @param jobId
     * @param validationId
     * @param cgsUserDetails @return  @throws PackagingException
     */
    private ExportImportPackage createPackageForImport(int publisherId, String jobId, String validationId, CGSUserDetails cgsUserDetails) throws DsException {
        ExportImportPackage.Type type = ExportImportPackage.Type.IMPORT;
        ExportImportPackage exportImportPackage = new ExportImportPackage(type, publisherId, jobId, validationId, cgsUserDetails.getUsername());
        return exportImportPackage;
    }

    /**
     * Create a ExportImportPackage for validation import process.
     *
     * @return ExportImportPackage
     * @throws DsException
     */
    private ExportImportPackage createPackageForValidation(String jobId) throws DsException {
        ExportImportPackage.Type type = ExportImportPackage.Type.VALIDATE;
        ExportImportPackage exportImportPackage = new ExportImportPackage(type, jobId);
        return exportImportPackage;
    }

    /**
     * Removes files from the /export/output folder that are older than @daysBack days
     *
     * @param daysBack number of days to look back
     * @throws IOException
     */
    @Override
    public void removeOldExportedCoursesFiles(int daysBack) throws IOException {
        logger.debug("About to cleanup export courses files.");
        String exportedCoursesFilesLocation = getExportedCoursesLocation();
        File exportedCoursesFilesFolder = new File(exportedCoursesFilesLocation);
        Date xDaysAgo = new DateTime().minusDays(daysBack).toDate();
        File[] oldFiles = exportedCoursesFilesFolder.listFiles((FileFilter) new AgeFileFilter(xDaysAgo));
        logger.debug(String.format("Preparing to delete %d old files from folder %s", oldFiles.length, exportedCoursesFilesFolder.getAbsolutePath()));
        for (File file : oldFiles) {
            Date lastMod = new Date(file.lastModified());
            try {
                FileUtils.forceDelete(file);
                logger.debug(String.format("Deleted exported course file: %s. last modified in: %s", file.getAbsoluteFile(), lastMod));
            } catch (IOException e) {
                String msg = String.format("Could not delete exported course file: %s. Reason: %s", file.getAbsoluteFile(), e.getMessage());
                logger.error(msg);
                throw new IOException(msg, e);
            }
        }
    }

    public String getExportedCoursesLocation() {
        return configuration.getProperty("exportedCourseLocation") + "/export/output";
    }

    public Configuration getConfiguration() {
        return configuration;
    }

    public void seteBookCleanupService(EBookCleanupService eBookCleanupService) {
        this.eBookCleanupService = eBookCleanupService;
    }
}