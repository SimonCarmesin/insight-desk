package de.adesso.testing.userservice.model;

import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(exclude = "password")
@Entity(name = "Users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Enumerated(EnumType.STRING)
    private Role role;

    private String name;

    private String password;

    @Version
    private Long version;

    public User(String name, String password) {
        this.role = Role.USER;
        this.name = name;
        this.password = password;
    }
}
