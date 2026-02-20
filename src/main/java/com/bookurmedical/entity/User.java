
package com.bookurmedical.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Document(collection = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {
    @Id
    private String id;
    private String username;
    private String email;
    private String password;
    private String role;
    private String firstName;
    private String lastName;
    private String phoneNumber;
    private boolean isProfileCompleted = false;
<<<<<<< HEAD
    private String resetToken;
    private java.time.LocalDateTime resetTokenExpiry;
=======
>>>>>>> 96d0f91b3637f55db93cce76dd31b9df811f1d68
}
