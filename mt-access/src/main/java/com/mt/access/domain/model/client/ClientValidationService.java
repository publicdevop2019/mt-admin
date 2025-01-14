package com.mt.access.domain.model.client;

import com.mt.access.domain.DomainRegistry;
import com.mt.common.domain.model.restful.query.QueryUtility;
import com.mt.common.domain.model.validate.Checker;
import com.mt.common.domain.model.validate.ValidationNotificationHandler;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class ClientValidationService {
    public void validate(Client client, ValidationNotificationHandler handler) {
        validateResource(client, handler);
        validateExternalResource(client, handler);
    }

    private void validateResource(Client client, ValidationNotificationHandler handler) {
        if (!client.getResources().isEmpty()) {
            if (client.getResources().contains(client.getClientId())) {
                handler.handleError("client cannot have itself as resource");
            }
            Set<Client> allByQuery = QueryUtility.getAllByQuery(
                (query) -> DomainRegistry.getClientRepository().query(query),
                new ClientQuery(client.getResources()));
            if (allByQuery.size() != client.getResources().size()) {
                handler.handleError("unable to find all resource(s)");
            }
            boolean b = allByQuery.stream().anyMatch(e -> !e.getAccessible());
            if (b) {
                handler.handleError("resource(s) not accessible");
            }
            if (allByQuery.stream().map(Client::getProjectId)
                .anyMatch(e -> !client.getProjectId().equals(e))) {
                handler.handleError("client belongs to another project");
            }
        }
    }

    private void validateExternalResource(Client client, ValidationNotificationHandler handler) {
        if (Checker.notNull(client.getExternalResources()) &&
            Checker.notEmpty(client.getExternalResources())) {
            Set<Client> allByQuery = QueryUtility.getAllByQuery(
                (query) -> DomainRegistry.getClientRepository().query(query),
                new ClientQuery(client.getExternalResources()));
            if (allByQuery.size() != client.getExternalResources().size()) {
                handler.handleError("unable to find all external resource(s)");
            }
            boolean b = allByQuery.stream().anyMatch(e -> !e.getAccessible());
            if (b) {
                handler.handleError("resource(s) not accessible");
            }
        }
    }
}
