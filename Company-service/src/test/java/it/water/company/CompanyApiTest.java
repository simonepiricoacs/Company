package it.water.company;

import it.water.company.api.CompanyApi;
import it.water.company.api.CompanyRepository;
import it.water.company.api.CompanySystemApi;
import it.water.company.model.Company;
import it.water.core.api.bundle.Runtime;
import it.water.core.api.model.PaginableResult;
import it.water.core.api.model.Role;
import it.water.core.api.permission.PermissionManager;
import it.water.core.api.registry.ComponentRegistry;
import it.water.core.api.repository.query.Query;
import it.water.core.api.role.RoleManager;
import it.water.core.api.service.Service;
import it.water.core.api.user.UserManager;
import it.water.core.interceptors.annotations.Inject;
import it.water.core.model.exceptions.ValidationException;
import it.water.core.model.exceptions.WaterRuntimeException;
import it.water.core.permission.exceptions.UnauthorizedException;
import it.water.core.testing.utils.bundle.TestRuntimeInitializer;
import it.water.core.testing.utils.junit.WaterTestExtension;
import it.water.core.testing.utils.runtime.TestRuntimeUtils;
import it.water.repository.entity.model.exceptions.DuplicateEntityException;
import it.water.repository.entity.model.exceptions.NoResultException;
import lombok.Setter;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Generated with Water Generator.
 * Test class for Company Services.
 * <p>
 * Please use CompanyRestTestApi for ensuring format of the json response
 */
@ExtendWith(WaterTestExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class CompanyApiTest implements Service {

    @Inject
    @Setter
    private ComponentRegistry componentRegistry;

    @Inject
    @Setter
    private CompanyApi companyApi;

    @Inject
    @Setter
    private Runtime runtime;

    @Inject
    @Setter
    private CompanyRepository companyRepository;

    @Inject
    @Setter
    //default permission manager in test environment;
    private PermissionManager permissionManager;

    @Inject
    @Setter
    //test role manager
    private UserManager userManager;

    @Inject
    @Setter
    //test role manager
    private RoleManager roleManager;

    //admin user
    @SuppressWarnings("unused")
    private it.water.core.api.model.User adminUser;
    private it.water.core.api.model.User companyManagerUser;
    private it.water.core.api.model.User companyViewerUser;
    private it.water.core.api.model.User companyEditorUser;

    private Role companyManagerRole;
    private Role companyViewerRole;
    private Role companyEditorRole;

    @BeforeAll
    void beforeAll() {
        //getting user
        companyManagerRole = roleManager.getRole(Company.DEFAULT_MANAGER_ROLE);
        companyViewerRole = roleManager.getRole(Company.DEFAULT_VIEWER_ROLE);
        companyEditorRole = roleManager.getRole(Company.DEFAULT_EDITOR_ROLE);
        Assertions.assertNotNull(companyManagerRole);
        Assertions.assertNotNull(companyViewerRole);
        Assertions.assertNotNull(companyEditorRole);
        //impersonate admin so we can test the happy path
        adminUser = userManager.findUser("admin");
        companyManagerUser = userManager.addUser("manager", "name", "lastname", "manager@a.com", "TempPassword1_", "salt", false);
        companyViewerUser = userManager.addUser("viewer", "name", "lastname", "viewer@a.com", "TempPassword1_", "salt", false);
        companyEditorUser = userManager.addUser("editor", "name", "lastname", "editor@a.com", "TempPassword1_", "salt", false);
        //starting with admin permissions
        roleManager.addRole(companyManagerUser.getId(), companyManagerRole);
        roleManager.addRole(companyViewerUser.getId(), companyViewerRole);
        roleManager.addRole(companyEditorUser.getId(), companyEditorRole);
        //default security context is admin
        TestRuntimeUtils.impersonateAdmin(componentRegistry);
    }

    /**
     * Testing basic injection of basic component for company entity.
     */
    @Test
    @Order(1)
    void componentsInsantiatedCorrectly() {
        this.companyApi = this.componentRegistry.findComponent(CompanyApi.class, null);
        Assertions.assertNotNull(this.companyApi);
        Assertions.assertNotNull(this.componentRegistry.findComponent(CompanySystemApi.class, null));
        this.companyRepository = this.componentRegistry.findComponent(CompanyRepository.class, null);
        Assertions.assertNotNull(this.companyRepository);
    }

    /**
     * Testing simple save and version increment
     */
    @Test
    @Order(2)
    void saveOk() {
        Company entity = createCompany(0);
        entity = this.companyApi.save(entity);
        Assertions.assertEquals(1, entity.getEntityVersion());
        Assertions.assertTrue(entity.getId() > 0);
        Assertions.assertEquals("exampleName0", entity.getBusinessName());
    }

    @Test
    @Order(30)
    void virtualHostIsNormalizedAndResolvesCompany() {
        Company entity = createCompany(1000);
        entity.setVirtualHost("HTTPS://Tenant.Example.TEST:443/");

        Company saved = companyApi.save(entity);
        Company resolved = componentRegistry.findComponent(CompanySystemApi.class, null)
                .findByVirtualHost("tenant.example.test");

        Assertions.assertEquals("tenant.example.test", saved.getVirtualHost());
        Assertions.assertNotNull(resolved);
        Assertions.assertEquals(saved.getId(), resolved.getId());
    }

    @Test
    @Order(31)
    void duplicatedNormalizedVirtualHostIsRejected() {
        Company duplicated = createCompany(1001);
        duplicated.setVirtualHost("tenant.example.test");

        Assertions.assertThrows(DuplicateEntityException.class, () -> companyApi.save(duplicated));
    }

    /**
     * Testing update logic, basic test
     */
    @Test
    @Order(3)
    void updateShouldWork() {
        Query q = this.companyRepository.getQueryBuilderInstance().createQueryFilter("businessName=exampleName0");
        Company entity = this.companyApi.find(q);
        Assertions.assertNotNull(entity);
        String newBusinessName = entity.getBusinessName() + "Updated";
        entity.setBusinessName(newBusinessName);
        entity = this.companyApi.update(entity);
        Assertions.assertEquals(newBusinessName, entity.getBusinessName());
        Assertions.assertEquals(2, entity.getEntityVersion());
    }

    /**
     * Testing update logic, basic test
     */
    @Test
    @Order(4)
    void updateShouldFailWithWrongVersion() {
        Query q = this.companyRepository.getQueryBuilderInstance().createQueryFilter("businessName=exampleName0Updated");
        Company errorEntity = this.companyApi.find(q);
        Assertions.assertEquals("exampleName0Updated", errorEntity.getBusinessName());
        Assertions.assertEquals(2, errorEntity.getEntityVersion());
        errorEntity.setEntityVersion(1);
        Assertions.assertThrows(WaterRuntimeException.class, () -> this.companyApi.update(errorEntity));
    }

    /**
     * Testing finding all entries with no pagination
     */
    @Test
    @Order(5)
    void findAllShouldWork() {
        PaginableResult<Company> all = this.companyApi.findAll(null, -1, -1, null);
        Assertions.assertEquals(1,all.getResults().size());
    }

    /**
     * Testing finding all entries with settings related to pagination.
     * Searching with 5 items per page starting from page 1.
     */
    @Test
    @Order(6)
    void findAllPaginatedShouldWork() {
        for (int i = 2; i < 11; i++) {
            Company u = createCompany(i);
            this.companyApi.save(u);
        }
        PaginableResult<Company> paginated = this.companyApi.findAll(null, 7, 1, null);
        Assertions.assertEquals(7, paginated.getResults().size());
        Assertions.assertEquals(1, paginated.getCurrentPage());
        Assertions.assertEquals(2, paginated.getNextPage());
        paginated = this.companyApi.findAll(null, 7, 2, null);
        Assertions.assertEquals(3, paginated.getResults().size());
        Assertions.assertEquals(2, paginated.getCurrentPage());
        Assertions.assertEquals(1, paginated.getNextPage());
    }

    /**
     * Testing removing all entities using findAll method.
     */
    @Test
    @Order(7)
    void removeAllShouldWork() {
        PaginableResult<Company> paginated = this.companyApi.findAll(null, -1, -1, null);
        paginated.getResults().forEach(entity -> {
            this.companyApi.remove(entity.getId());
        });
        Assertions.assertEquals(0,this.companyApi.countAll(null));
    }

    /**
     * Testing failure on duplicated entity
     */
    @Test
    @Order(8)
    void saveShouldFailOnDuplicatedEntity() {
        Company entity = createCompany(1);
        this.companyApi.save(entity);
        Company duplicated = this.createCompany(1);
        //cannot insert new entity wich breaks unique constraint
        Assertions.assertThrows(DuplicateEntityException.class, () -> this.companyApi.save(duplicated));
    }

    /**
     * Testing failure on validation failure for example code injection
     */
    @Test
    @Order(9)
    void updateShouldFailOnValidationFailure() {
        Company newEntity = createCompany(3);
        newEntity.setBusinessName("<script>function(){alert('ciao')!}</script>");
        Assertions.assertThrows(ValidationException.class, () -> this.companyApi.save(newEntity));
    }

    /**
     * Testing Crud operations on manager role
     */
    @Order(10)
    @Test
    void managerCanDoEverything() {
        TestRuntimeInitializer.getInstance().impersonate(companyManagerUser, runtime);
        final Company entity = createCompany(101);
        Company savedEntity = Assertions.assertDoesNotThrow(() -> this.companyApi.save(entity));
        savedEntity.setBusinessName("newSavedEntity");
        Assertions.assertDoesNotThrow(() -> this.companyApi.update(entity));
        Assertions.assertDoesNotThrow(() -> this.companyApi.find(savedEntity.getId()));
        Assertions.assertDoesNotThrow(() -> this.companyApi.remove(savedEntity.getId()));

    }

    @Order(11)
    @Test
    void viewerCannotSaveOrUpdateOrRemove() {
        TestRuntimeInitializer.getInstance().impersonate(companyViewerUser, runtime);
        final Company entity = createCompany(201);
        Assertions.assertThrows(UnauthorizedException.class, () -> this.companyApi.save(entity));
        //viewer can search
        Assertions.assertEquals(0, this.companyApi.findAll(null, -1, -1, null).getResults().size());
    }

    @Order(12)
    @Test
    void editorCannotRemove() {
        TestRuntimeInitializer.getInstance().impersonate(companyEditorUser, runtime);
        final Company entity = createCompany(301);
        Company savedEntity = Assertions.assertDoesNotThrow(() -> this.companyApi.save(entity));
        savedEntity.setBusinessName("editorNewSavedEntity");
        Assertions.assertDoesNotThrow(() -> this.companyApi.update(entity));
        Assertions.assertDoesNotThrow(() -> this.companyApi.find(savedEntity.getId()));
        long savedEntityId = savedEntity.getId();
        Assertions.assertThrows(UnauthorizedException.class, () -> this.companyApi.remove(savedEntityId));
    }

    @Order(13)
    @Test
    void ownedResourceShouldBeAccessedOnlyByOwner() {
        TestRuntimeInitializer.getInstance().impersonate(companyEditorUser, runtime);
        final Company entity = createCompany(401);
        //saving as editor
        Company savedEntity = Assertions.assertDoesNotThrow(() -> this.companyApi.save(entity));
        Assertions.assertDoesNotThrow(() -> this.companyApi.find(savedEntity.getId()));
        TestRuntimeInitializer.getInstance().impersonate(companyManagerUser, runtime);
        //find an owned entity with different user from the creator should raise an unauthorized exception
        long savedEntityId = savedEntity.getId();
        Assertions.assertThrows(NoResultException.class, () -> this.companyApi.find(savedEntityId));
    }

    private Company createCompany(int seed) {
        Company entity = new Company("exampleName" + seed, "invoice Address" + seed, "City" + seed, "postalCode" + seed, "nation" + seed, "vatNumber" + seed, (long) seed);
        //todo add more fields here...
        return entity;
    }
}
