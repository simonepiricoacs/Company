# Company Module — Organization / Business Entity Management

## Purpose
Manages company and organization records within the Water Framework. Each company belongs to a user (`OwnedResource`) and can be shared with others (`SharedEntity`). The VAT number is a global unique identifier across all companies. Does NOT manage users, authentication, or billing logic — it is purely a data management module for company records.

## Sub-modules

| Sub-module | Runtime | Key Classes |
|---|---|---|
| `Company-api` | All | `CompanyApi`, `CompanySystemApi`, `CompanyRestApi`, `CompanyRepository` |
| `Company-model` | All | `Company` (extends `AbstractJpaExpandableEntity`) |
| `Company-service` | Water/OSGi | Service impl, repository, REST controller |
| `Company-service-spring` | Spring Boot | Spring MVC REST controllers |

## Company Entity

```java
@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"vatNumber"}))
@AccessControl(
    availableActions = {CrudActions.SAVE, CrudActions.UPDATE, CrudActions.FIND, CrudActions.FIND_ALL, CrudActions.REMOVE},
    rolesPermissions = {
        @DefaultRoleAccess(roleName = "companyManager", actions = {CrudActions.class}),
        @DefaultRoleAccess(roleName = "companyViewer",  actions = {CrudActions.FIND, CrudActions.FIND_ALL}),
        @DefaultRoleAccess(roleName = "companyEditor",  actions = {CrudActions.SAVE, CrudActions.UPDATE, CrudActions.FIND, CrudActions.FIND_ALL})
    }
)
public class Company extends AbstractJpaExpandableEntity implements ProtectedEntity, SharedEntity {

    @NotNull @NotEmpty @NoMalitiusCode @Column(length = 255)
    private String businessName;    // Legal company name

    @NotNull @NotEmpty @NoMalitiusCode @Column(length = 255)
    private String invoiceAddress;  // Billing address

    @NotNull @NotEmpty @NoMalitiusCode @Column(length = 255)
    private String city;            // City

    @NotNull @NotEmpty @NoMalitiusCode @Column(length = 255)
    private String postalCode;      // ZIP / postal code

    @NotNull @NotEmpty @NoMalitiusCode @Column(length = 255)
    private String nation;          // Country

    @NotNull @NotEmpty @NoMalitiusCode
    @Column(unique = true, length = 255)
    private String vatNumber;       // VAT/Tax ID (globally unique)

    @JsonIgnore
    private Long ownerUserId;       // Owner user ID (hidden from REST responses)
}
```

## Key Operations

### CompanyApi (permission-checked)
```java
// Inherits from BaseEntityApi<Company>:
Company save(Company entity);
Company update(Company entity);
Company find(long id);
PaginableResult<Company> findAll(int delta, int page, Query filter, QueryOrder order);
void remove(long id);
```

### CompanySystemApi (bypasses permissions)
Same CRUD methods, callable without a logged-in user context. Used internally when other modules need to resolve company data.

## Key Flow

```
Client
  └─► CompanyRestControllerImpl (@FrameworkRestController)
       └─► CompanyServiceImpl (@FrameworkComponent)
            └─► CompanySystemServiceImpl
                 └─► CompanyRepository (JPA)
                      └─► Company table
```

## REST Endpoints

| Method | Path | Permission | Description |
|---|---|---|---|
| `POST` | `/companies` | companyManager | Create company |
| `PUT` | `/companies` | companyManager / companyEditor | Update company |
| `GET` | `/companies/{id}` | companyViewer | Find by ID |
| `GET` | `/companies` | companyViewer | Find all (paginated) |
| `DELETE` | `/companies/{id}` | companyManager | Delete company |

All endpoints require `@LoggedIn`. Responses use `@JsonView(WaterJsonView.Public.class)`.

## Default Roles

| Role | Allowed Actions |
|---|---|
| `companyManager` | SAVE, UPDATE, FIND, FIND_ALL, REMOVE |
| `companyViewer` | FIND, FIND_ALL |
| `companyEditor` | SAVE, UPDATE, FIND, FIND_ALL |

## Dependencies
- `it.water.repository.jpa:JpaRepository-api` — `AbstractJpaExpandableEntity`
- `it.water.core:Core-permission` — `@AccessControl`, `CrudActions`, `SharedEntity`
- `it.water.rest:Rest-api` — `RestApi`, `@LoggedIn`
- `jakarta.persistence:jakarta.persistence-api` — JPA 3.0 annotations

## Testing
- Unit tests: `WaterTestExtension` — test CRUD + VAT uniqueness + permission scenarios
- REST tests: **Karate only** (never JUnit direct calls to `CompanyRestController`)
- Test VAT uniqueness: saving two companies with same `vatNumber` must fail with `ValidationException`
- Impersonate admin: `TestRuntimeUtils.impersonateAdmin(componentRegistry)`

## Code Generation Rules
- `vatNumber` is globally unique — always validate at REST boundary before persistence
- `ownerUserId` is `@JsonIgnore` — never include it in REST responses or API examples
- `SharedEntity` impl: `ownerUserId` is set automatically from logged-in user context on save
- All string fields use `@NoMalitiusCode` — prevents XSS/SQL injection on all inputs
- `AbstractJpaExpandableEntity` supports dynamic field extension — if custom fields are needed, use the expansion API rather than adding new columns
- REST controllers tested **exclusively via Karate**
