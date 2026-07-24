package it.water.company.service.integration;

import it.water.company.api.CompanySystemApi;
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
    private CompanySystemApi companySystemApi;

    @Override
    public Long findCompanyIdByVirtualHost(String virtualHost) {
        var company = companySystemApi.findByVirtualHost(virtualHost);
        return company == null ? null : company.getId();
    }
}
