package org.fao.geonet;

import org.apache.commons.io.FileUtils;
import org.fao.geonet.domain.Setting;
import org.fao.geonet.domain.SettingDataType;
import org.fao.geonet.kernel.setting.SettingManager;
import org.fao.geonet.repository.*;
import org.fao.geonet.repository.statistic.SearchRequestParamRepository;
import org.fao.geonet.repository.statistic.SearchRequestRepository;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.test.context.ContextConfiguration;

import javax.annotation.PreDestroy;
import java.io.File;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.Callable;

import static org.junit.Assert.*;

/**
 * Test migration from a 2.8.0 database to current.  The DatabaseMigration postprocessor is configured in the
 * {@link ContextConfiguration} annotation on this class.  Therefore the migration is performed on startup and the test
 * merely checks that it completed correctly.
 */
@ContextConfiguration(inheritLocations = true,
        locations = {
                "classpath:migration-repository-test-context.xml",
                "classpath:services-repository-test-context.xml"})
public class DatabaseMigrationTest extends AbstractSpringDataTest {

    public static final String DATABASE_MIGRATION_XML = "WEB-INF/config-db/database_migration.xml";

    @Autowired
    private DatabasePathLocator locator;

    @Autowired
    private MetadataRepository _mdRepo;
    @Autowired
    private AddressRepository _addressRepo;
    @Autowired
    private CswCapabilitiesInfoFieldRepository _capInfoFieldRepo;
    @Autowired
    private CustomElementSetRepository _customElementRepo;
    @Autowired
    private GroupRepository _groupRepo;
    @Autowired
    private HarvesterSettingRepository _harvesterSettingRepo;
    @Autowired
    private HarvestHistoryRepository _harvestHistoryRepo;
    @Autowired
    private IsoLanguageRepository _isoLangRepo;
    @Autowired
    private LanguageRepository _LanguageRepo;
    @Autowired
    private MetadataCategoryRepository _metadataCategoryRepo;
    @Autowired
    private MetadataNotificationRepository _metadataNotificationRepo;
    @Autowired
    private MetadataNotifierRepository _metadataNotificatierRepo;
    @Autowired
    private MetadataRatingByIpRepository _ratingRepo;
    @Autowired
    private MetadataRelationRepository _relationRepo;
    @Autowired
    private MetadataStatusRepository _metadataStatusRepo;
    @Autowired
    private MetadataValidationRepository _metadataValidationRepo;
    @Autowired
    private OperationAllowedRepository _opAllowedRepo;
    @Autowired
    private OperationRepository _operationRepo;
    @Autowired
    private ServiceRepository _serviceRepo;
    @Autowired
    private SettingRepository _settingRepo;
    @Autowired
    private SourceRepository _sourceRepo;
    @Autowired
    private StatusValueRepository _statusValueRepo;
    @Autowired
    private ThesaurusActivationRepository _thesaurusActivationRepo;
    @Autowired
    private UserGroupRepository _userGroupRepo;
    @Autowired
    private UserRepository _userRepo;
    @Autowired
    private SearchRequestRepository _searchRequestRepo;
    @Autowired
    private SearchRequestParamRepository _searchRequestParamRepo;

    @Test
    public void testMigrate() throws Exception {
        assertEquals(2, _addressRepo.count());
        assertEquals(64, _capInfoFieldRepo.count());
        assertEquals(0, _customElementRepo.count());
        assertEquals(4, _groupRepo.count());
        assertEquals(16, _groupRepo.findOne(1).getLabelTranslations().size());
        assertEquals(22, _harvesterSettingRepo.count());
        assertEquals(1, _harvesterSettingRepo.findByName("name").size());
        assertEquals(1, _harvestHistoryRepo.count());
        assertEquals(484, _isoLangRepo.count());
        assertEquals(16, _LanguageRepo.count());
        assertEquals(8, _mdRepo.count());
        assertEquals(13, _metadataCategoryRepo.count());
        assertEquals(1, _metadataNotificatierRepo.count());
        assertEquals(0, _metadataNotificationRepo.count());
        assertEquals(1, _metadataStatusRepo.count());
        assertEquals(3, _metadataValidationRepo.count());
        assertEquals(72, _opAllowedRepo.count());
        assertEquals(6, _operationRepo.count());
        assertEquals(2, _ratingRepo.count());
        assertEquals(0, _relationRepo.count());
        assertEquals(4, _searchRequestParamRepo.count());
        assertEquals(39, _searchRequestRepo.count());
        assertEquals(0, _serviceRepo.count());
        assertTrue(_settingRepo.count() > 0);
        final Setting ignoreChars = _settingRepo.findOne(SettingManager.SYSTEM_LUCENE_IGNORECHARS);
        assertNotNull(ignoreChars);
        assertEquals(SettingDataType.STRING, ignoreChars.getDataType());
        assertEquals("", ignoreChars.getValue());
        assertEquals(1, _sourceRepo.count());
        assertEquals(6, _statusValueRepo.count());
        assertEquals(0, _thesaurusActivationRepo.count());
        assertEquals(1, _userGroupRepo.count());
        assertEquals(2, _userRepo.count());
    }


    static File findwebappDir() {
        File current = new File(".").getAbsoluteFile();
        final String pathToFile = "src/main/webapp/" + DATABASE_MIGRATION_XML;
        final String pathToFile2 = "web/src/main/webapp/" + DATABASE_MIGRATION_XML;
        while (!new File(current, pathToFile).exists() && !new File(current, pathToFile2).exists()) {
            current = current.getParentFile();
        }

        if (new File(current, pathToFile).exists()) {
            return new File(current, pathToFile).getParentFile().getParentFile().getParentFile();
        } else {
            return new File(current, pathToFile2).getParentFile().getParentFile().getParentFile();
        }
    }

    public static class DatabasePathLocator implements Callable<String> {
        public TemporaryFolder dbFileContainer = new TemporaryFolder();

        @Override
        public String call() throws Exception {
            final String pathToDbFile = "2_8/geonetwork.h2.db";
            final String resource = DatabaseMigrationTest.class.getClassLoader().getResource(pathToDbFile).getFile();
            dbFileContainer.create();
            String parentFile = new File(resource).getParent();
            FileUtils.copyDirectory(new File(parentFile), dbFileContainer.getRoot());
            String path = dbFileContainer.getRoot() + "/geonetwork";
            return path;
        }

        @PreDestroy
        public void deleteTmpFiles() {
            dbFileContainer.delete();
        }
    }

    public static class MigrationConfigLoader implements Callable<LinkedHashMap<Integer, List<String>>> {

        @Override
        public LinkedHashMap<Integer, List<String>> call() throws Exception {
            final String configLocation = new File(findwebappDir(), DATABASE_MIGRATION_XML).toURI().toString();
            ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(configLocation);
            return context.getBean(DatabaseMigration.class).getMigrationConfig();
        }
    }
}