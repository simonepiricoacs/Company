/*
 * Copyright 2024 Aristide Cittadino
 *
 * Licensed under the Apache License, Version 2.0 (the "License")
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package it.water.company.service.rest.spring;

import com.fasterxml.jackson.annotation.JsonView;
import it.water.company.api.rest.CompanyRestApi;
import it.water.company.model.Company;
import it.water.core.api.model.PaginableResult;
import it.water.core.api.service.rest.FrameworkRestApi;
import it.water.core.api.service.rest.WaterJsonView;
import it.water.service.rest.api.security.LoggedIn;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

/**
 * @Author Aristide Cittadino
 * Interface exposing same methods of its parent CompanyRestApi but adding Spring annotations.
 * Swagger annotation should be found because they have been defined in the parent CompanyRestApi.
 */
@RequestMapping("/companies")
@FrameworkRestApi
public interface CompanySpringRestApi extends CompanyRestApi {
    @LoggedIn
    @GetMapping("/current")
    @JsonView(WaterJsonView.Public.class)
    Company findCurrent();

    @LoggedIn
    @PostMapping
    @JsonView(WaterJsonView.Public.class)
    Company save(@RequestBody Company company);

    @LoggedIn
    @PutMapping
    @JsonView(WaterJsonView.Public.class)
    Company update(@RequestBody Company company);

    @LoggedIn
    @GetMapping("/{id}")
    @JsonView(WaterJsonView.Public.class)
    Company find(@PathVariable("id") long id);

    @LoggedIn
    @GetMapping
    @JsonView(WaterJsonView.Public.class)
    PaginableResult<Company> findAll();

    @LoggedIn
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @JsonView(WaterJsonView.Public.class)
    void remove(@PathVariable("id") long id);
}
