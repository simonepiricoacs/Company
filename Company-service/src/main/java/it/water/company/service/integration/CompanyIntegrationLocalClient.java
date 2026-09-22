package it.water.company.service.integration;

import it.water.company.api.CompanyApi;
import it.water.company.api.CompanySystemApi;
import it.water.company.model.Company;
import it.water.core.api.model.WaterCompany;
import it.water.core.api.service.integration.CompanyIntegrationClient;
import it.water.core.interceptors.annotations.FrameworkComponent;
import it.water.core.interceptors.annotations.Inject;
import lombok.Setter;

/**
 * Local adapter used when Company and its caller share the same Water runtime.
 */
@FrameworkComponent(priority = 1, services = CompanyIntegrationClient.class)
public class CompanyIntegrationLocalClient implements CompanyIntegrationClient {

    @Inject
    @Setter
    private CompanyApi companyApi;

    @Inject
    @Setter
    private CompanySystemApi companySystemApi;

    @Override
    public WaterCompany findCompany(long companyId) {
        return companyApi.find(companyId);
    }

    @Override
    public WaterCompany createCompany(String businessName, String invoiceAddress, String city, String postalCode,
                                      String nation, String vatNumber, String virtualHost) {
        Company company = new Company(
                businessName,
                invoiceAddress,
                city,
                postalCode,
                nation,
                vatNumber,
                0L);
        company.setVirtualHost(virtualHost);
        return companyApi.save(company);
    }

    @Override
    public Long findCompanyIdByVirtualHost(String virtualHost) {
        var company = companySystemApi.findByVirtualHost(virtualHost);
        return company == null ? null : company.getId();
    }

    @Override
    public boolean existsCompany(long companyId) {
        try {
            return companySystemApi.find(companyId) != null;
        } catch (it.water.repository.entity.model.exceptions.NoResultException e) {
            return false;
        }
    }

    @Override
    public void removeCompany(long companyId) {
        companySystemApi.remove(companyId);
    }
}
