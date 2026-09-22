package it.water.company.service.integration;

import it.water.company.api.CompanyApi;
import it.water.company.api.CompanySystemApi;
import it.water.company.model.Company;
import it.water.core.api.model.WaterCompany;
import it.water.repository.entity.model.exceptions.NoResultException;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CompanyIntegrationLocalClientTest {

    @Test
    void reportsWhetherTheCompanyExists() {
        CompanyApi companyApi = mock(CompanyApi.class);
        CompanySystemApi companySystemApi = mock(CompanySystemApi.class);
        CompanyIntegrationLocalClient client = client(companyApi, companySystemApi);

        when(companySystemApi.find(7L)).thenReturn(mock(Company.class));
        doThrow(new NoResultException()).when(companySystemApi).find(8L);

        assertTrue(client.existsCompany(7L));
        assertFalse(client.existsCompany(8L));
    }

    @Test
    void removesCompanyOnlyThroughTheCompanyOwnedSystemApi() {
        CompanyApi companyApi = mock(CompanyApi.class);
        CompanySystemApi companySystemApi = mock(CompanySystemApi.class);
        CompanyIntegrationLocalClient client = client(companyApi, companySystemApi);

        client.removeCompany(7L);

        verify(companySystemApi).remove(7L);
    }

    @Test
    void readsCompanyThroughThePermissionAwareCompanyApi() {
        CompanyApi companyApi = mock(CompanyApi.class);
        CompanySystemApi companySystemApi = mock(CompanySystemApi.class);
        CompanyIntegrationLocalClient client = client(companyApi, companySystemApi);
        Company company = mock(Company.class);
        when(companyApi.find(7L)).thenReturn(company);

        WaterCompany result = client.findCompany(7L);

        assertSame(company, result);
        verify(companyApi).find(7L);
    }

    @Test
    void createsCompanyThroughThePermissionAwareCompanyApi() {
        CompanyApi companyApi = mock(CompanyApi.class);
        CompanySystemApi companySystemApi = mock(CompanySystemApi.class);
        CompanyIntegrationLocalClient client = client(companyApi, companySystemApi);
        Company saved = mock(Company.class);
        when(companyApi.save(org.mockito.ArgumentMatchers.any(Company.class))).thenReturn(saved);

        WaterCompany result = client.createCompany(
                "Acme", "Via Roma 1", "Roma", "00100", "IT", "IT12345678901", "acme.localhost");

        ArgumentCaptor<Company> created = ArgumentCaptor.forClass(Company.class);
        verify(companyApi).save(created.capture());
        assertSame(saved, result);
        assertEquals("Acme", created.getValue().getBusinessName());
        assertEquals("Via Roma 1", created.getValue().getInvoiceAddress());
        assertEquals("Roma", created.getValue().getCity());
        assertEquals("00100", created.getValue().getPostalCode());
        assertEquals("IT", created.getValue().getNation());
        assertEquals("IT12345678901", created.getValue().getVatNumber());
        assertEquals("acme.localhost", created.getValue().getVirtualHost());
    }

    private CompanyIntegrationLocalClient client(CompanyApi companyApi, CompanySystemApi companySystemApi) {
        CompanyIntegrationLocalClient client = new CompanyIntegrationLocalClient();
        client.setCompanyApi(companyApi);
        client.setCompanySystemApi(companySystemApi);
        return client;
    }
}
