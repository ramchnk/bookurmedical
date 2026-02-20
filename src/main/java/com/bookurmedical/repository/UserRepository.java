
package com.bookurmedical.repository;

import com.bookurmedical.entity.User;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends MongoRepository<User, String> {
    Optional<User> findByUsername(String username);

    Boolean existsByUsername(String username);

    Boolean existsByEmail(String email);
<<<<<<< HEAD

    Optional<User> findByEmail(String email);

    Optional<User> findByResetToken(String resetToken);
=======
>>>>>>> 96d0f91b3637f55db93cce76dd31b9df811f1d68
}
