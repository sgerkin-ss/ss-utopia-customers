package com.ss.utopia.customer.client;

import com.ss.utopia.customer.client.authentication.AuthenticationRequest;
import com.ss.utopia.customer.controller.EndpointConstants;
import com.ss.utopia.customer.dto.CreateUserAccountDto;
import com.ss.utopia.customer.dto.DeleteAccountDto;
import java.util.UUID;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient("utopia-auth-service")
public interface AccountsClient {

  @PostMapping(value = "/login")
  ResponseEntity<String> login(@RequestBody AuthenticationRequest authenticationRequest);

  @PutMapping(value = EndpointConstants.API_V_0_1_ACCOUNTS + "/customer/{customerId}",
      consumes = MediaType.TEXT_PLAIN_VALUE)
  ResponseEntity<Void> updateCustomerEmail(@RequestHeader(value = "Authorization")
                                        String authorizationHeader,
                                        @PathVariable UUID customerId,
                                        @RequestBody String newEmail);

  @PostMapping(EndpointConstants.API_V_0_1_ACCOUNTS)
  ResponseEntity<UUID> createNewAccount(@RequestBody CreateUserAccountDto dto);

  @DeleteMapping(EndpointConstants.API_V_0_1_ACCOUNTS + "/customer")
  ResponseEntity<Void> initiateCustomerDeletion(@RequestHeader(value = "Authorization")
                                             String authorizationHeader,
                                             @RequestBody DeleteAccountDto deleteAccountDto);

  @DeleteMapping(EndpointConstants.API_V_0_1_ACCOUNTS + "/customer/{confirmationToken}")
  ResponseEntity<UUID> completeCustomerDeletion(@RequestHeader(value = "Authorization")
                                                String authorizationHeader,
                                                @PathVariable UUID confirmationToken);
}
