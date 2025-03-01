package com.mt.access.resource;

import static com.mt.common.CommonConstant.HTTP_HEADER_AUTHORIZATION;

import com.mt.access.application.ApplicationServiceRegistry;
import com.mt.access.domain.DomainRegistry;
import com.mt.access.domain.model.ticket.SignedTicket;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(produces = "application/json", path = "tickets")
public class TicketResource {
    @PostMapping("{clientId}")
    public ResponseEntity<Void> create(
        @RequestHeader(HTTP_HEADER_AUTHORIZATION) String jwt,
        @PathVariable String clientId
    ) {
        DomainRegistry.getCurrentUserService().setUserJwt(jwt);
        SignedTicket encryptedTicket =
            ApplicationServiceRegistry.getTicketApplicationService().create(clientId);
        return ResponseEntity.ok().header("Location", encryptedTicket.getValue()).build();
    }

}
