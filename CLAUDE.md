# Company Module — Business Entity (Multi-Tenant)

## Purpose
Models the **Company** business entity in the Water Framework. A Company is a multi-tenant organizational unit that implements two framework interfaces: `ProtectedEntity` (access control) and `SharedEntity` (resource sharing). Reference implementation for entities that combine permission-based access with sharing across users.

## Sub-modules

| Sub-module | Runtime | Key Classes |
|---|---|---|
| `Company-api` | All | `CompanyApi`, `CompanySystemApi`, `CompanyRestApi`, `CompanyRepository` |
| `Company-model` | All | `Company` entity |
| `Company-service` | Water/OSGi | `CompanyServiceImpl`, `CompanySystemServiceImpl`, `CompanyRepositoryImpl`, `CompanyRestControllerImpl` |
| `Company-service-spring` | Spring Boot | Spring MVC REST controllers, Spring Boot app config |

## Company Entity

```java
@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"vatNumber"}))
@Access(AccessType.FIELD)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@RequiredArgsConstructor
@Getter
@Setter(AccessLevel.PROTECTED)
@ToString
@EqualsAndHashCode(callSuper = true, of = {"vatNumber"})
@AccessControl(
    availableActions = {CrudActions.SAVE, CrudActions.UPDATE, CrudActions.FIND, CrudActions.FIND_ALL, CrudActions.REMOVE},
    rolesPermissions = {
        @DefaultRoleAccess(roleName = Company.DEFAULT_MANAGER_ROLE,
                           actions = {CrudActions.SAVE, CrudActions.UPDATE, CrudActions.FIND, CrudActions.FIND_ALL, CrudActions.REMOVE}),
        @DefaultRoleAccess(roleName = Company.DEFAULT_VIEWER_ROLE,
                           actions = {CrudActions.FIND, CrudActions.FIND_ALL}),
        @DefaultRoleAccess(roleName = Company.DEFAULT_EDITOR_ROLE,
                           actions = {CrudActions.SAVE, CrudActions.UPDATE, CrudActions.FIND, CrudActions.FIND_ALL})
    }
)
public class Company extends AbstractJpaExpandableEntity
    implements ProtectedEntity, SharedEntity {

    public static final String DEFAULT_MANAGER_ROLE = "companyManager";
    public static final String DEFAULT_VIEWER_ROLE  = "companyViewer";
    public static final String DEFAULT_EDITOR_ROLE  = "companyEditor";

    @JsonView(WaterJsonView.Extended.class)
    @NotNullOnPersist @NotEmpty @NoMalitiusCode @Size(max = 255) @NonNull
    private String businessName;

    @JsonView(WaterJsonView.Extended.class)
    @NotNullOnPersist @NotEmpty @NoMalitiusCode @Size(max = 255) @NonNull
    private String invoiceAddress;

    @JsonView(WaterJsonView.Extended.class)
    @NotNullOnPersist @NotEmpty @NoMalitiusCode @Size(max = 255) @NonNull
    private String city;

    @JsonView(WaterJsonView.Extended.class)
    @NotNullOnPersist @NotEmpty @NoMalitiusCode @Size(max = 255) @NonNull
    private String postalCode;

    @JsonView(WaterJsonView.Extended.class)
    @NotNullOnPersist @NotEmpty @NoMalitiusCode @Size(max = 255) @NonNull
    private String nation;

    @JsonView(WaterJsonView.Extended.class)
    @NotNullOnPersist @NotEmpty @NoMalitiusCode @Size(max = 255) @NonNull
    private String vatNumber;        // unique business identifier

    @JsonView(WaterJsonView.Extended.class)
    @JsonIgnore @NonNull @Setter
    private Long ownerUserId;        // hidden from REST responses
}
```

### Key entity choices
- Extends `AbstractJpaExpandableEntity` (supports dynamic field extensions, not just `AbstractJpaEntity`)
- Implements `ProtectedEntity` + `SharedEntity` (NOT `OwnedResource` — ownership is tracked via `ownerUserId` field but the entity does not opt into the framework's automatic ownership filtering)
- All textual fields use the full validation stack: `@NotNullOnPersist + @NotEmpty + @NoMalitiusCode + @Size(max = 255)`
- `vatNumber` carries `@NoMalitiusCode` like every other String field, plus a JPA `@UniqueConstraint`
- Lombok defines a protected no-args constructor and a public required-args constructor over the `@NonNull` fields
- Default role constants live as `public static final` on the entity itself

## Implemented Interfaces — Behavior Implications

### ProtectedEntity
- Enables `@AllowPermissions` interceptors on all `CompanyApi` methods through `BaseEntityServiceImpl`
- Roles (`companyManager`, `companyViewer`, `companyEditor`) defined via `@DefaultRoleAccess`

### SharedEntity
- Allows the `SharedEntity` module to grant access to a Company instance to another user via `WaterSharedEntity`
- A user without ownership can still operate on the Company if a `WaterSharedEntity` record exists for them, subject to the permissions granted by their role

> Note: `Company` does NOT implement `OwnedResource`, so `BaseEntityServiceImpl` does not automatically filter `findAll()` by `ownerUserId`. The `ownerUserId` field is informational and must be enforced manually if the module needs ownership-based filtering.

## Key Interfaces

`CompanyApi` and `CompanySystemApi` are currently **inherited-only**: they expose just the standard CRUD coming from `BaseEntityApi<Company>` and `BaseEntitySystemApi<Company>` (save, update, remove, find, findAll, countAll). No domain-specific methods (e.g. `findByVatNumber`) are defined yet — add them here when needed.

```java
public interface CompanyApi extends BaseEntityApi<Company> { }
public interface CompanySystemApi extends BaseEntitySystemApi<Company> { }
```

### CompanyServiceImpl (Api layer)
```java
@FrameworkComponent
public class CompanyServiceImpl extends BaseEntityServiceImpl<Company> implements CompanyApi {
    @Inject @Getter @Setter private CompanySystemApi systemService;
    @Inject @Getter @Setter private ComponentRegistry componentRegistry;
}
```
Pure delegation to `BaseEntityServiceImpl`. No `save()` override, no automatic `ownerUserId` injection — the caller is responsible for setting it.

### CompanySystemServiceImpl (SystemApi layer)
```java
@FrameworkComponent
public class CompanySystemServiceImpl extends BaseEntitySystemServiceImpl<Company> implements CompanySystemApi {
    @Inject @Getter @Setter private CompanyRepository repository;
    @Inject @Setter private ComponentFilterBuilder componentFilterBuilder;
}
```

## REST Endpoints

Defined in `CompanyRestApi` under `@Path("/companies")`. The framework prefixes the rest root context (`/water` by default) automatically.

| Method | Path | Permission |
|---|---|---|
| `POST`   | `/water/companies`      | companyManager / companyEditor (SAVE) |
| `PUT`    | `/water/companies`      | companyManager / companyEditor (UPDATE) |
| `GET`    | `/water/companies/{id}` | any role with FIND |
| `GET`    | `/water/companies`      | any role with FIND_ALL |
| `DELETE` | `/water/companies/{id}` | companyManager (REMOVE) |

All endpoints carry `@LoggedIn`. Responses use `@JsonView(WaterJsonView.Public.class)`; entity fields tagged `Extended` are included.

## Default Roles

| Role | Allowed Actions |
|---|---|
| `companyManager` | SAVE, UPDATE, FIND, FIND_ALL, REMOVE |
| `companyViewer`  | FIND, FIND_ALL |
| `companyEditor`  | SAVE, UPDATE, FIND, FIND_ALL |

## Multi-Tenant Pattern Example

```java
// Caller sets the owner explicitly (no automatic injection)
Company company = new Company("Acme Corp", "123 Main St", "NYC", "10001", "US", "US123456789");
company.setOwnerUserId(userId);
companySystemApi.save(company);

// Grant access to another user via the SharedEntity module
WaterSharedEntity share = new WaterSharedEntity(Company.class.getName(), company.getId(), targetUserId);
sharedEntityApi.save(share);

// Target user can now access the company through CompanyApi
TestRuntimeInitializer.getInstance().impersonate(targetUser, runtime);
Company found = companyApi.find(company.getId());
```

## Dependencies
- `it.water.repository.jpa:JpaRepository-api` — `AbstractJpaExpandableEntity`
- `it.water.core:Core-permission` — `@AccessControl`, `CrudActions`, `ProtectedEntity`
- `it.water.core:Core-validation` — `@NoMalitiusCode`, `@NotNullOnPersist`
- `it.water.core:Core-api` — `SharedEntity` interface (and the `SharedEntity` module to use it)
- `it.water.service.rest:Rest-persistence` — `BaseEntityRestApi`

## Testing
- Unit tests: `WaterTestExtension`
  - Test CRUD under different roles (companyManager, companyViewer, companyEditor)
  - Test sharing semantics: a user can access a Company shared with them via `WaterSharedEntity`
- REST tests: **Karate only** — never JUnit direct calls to `CompanyRestController`
- Switch users with `TestRuntimeInitializer.getInstance().impersonate(user, runtime)`
- After permission tests, restore admin: `TestRuntimeUtils.impersonateAdmin(componentRegistry)`

## Code Generation Rules
- When generating a new sharable entity: follow `Company` as the canonical reference (`ProtectedEntity` + `SharedEntity`, role constants on the entity, full validation stack on String fields)
- If you need automatic ownership filtering on `findAll()`, also implement `OwnedResource` — `Company` intentionally does NOT, so add it explicitly only when required
- `ownerUserId` is `Long` (wrapper) and annotated with `@JsonIgnore` to keep it out of REST responses
- Apply `@NoMalitiusCode` to ALL String fields exposed through REST, including identifiers like `vatNumber`
- `CompanyRestController` is exercised **exclusively through Karate**
