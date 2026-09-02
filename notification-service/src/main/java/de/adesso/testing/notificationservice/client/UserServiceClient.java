package de.adesso.testing.notificationservice.client;

import de.adesso.testing.notificationservice.exception.UserNotFoundException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

@Component
public class UserServiceClient {

    private final RestClient restClient;

    public UserServiceClient(@Value("${user-service.base-url}") String baseUrl) {
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .build();
    }

    public UserDto getUserById(Long id) {
        try {
            return restClient.get()
                    .uri("/users/{id}", id)
                    .retrieve()
                    .body(UserDto.class);
        } catch (HttpClientErrorException.NotFound e) {
            throw new UserNotFoundException(id);
        }
    }
}