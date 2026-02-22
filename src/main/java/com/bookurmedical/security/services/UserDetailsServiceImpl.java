
package com.bookurmedical.security.services;

import com.bookurmedical.entity.User;
import com.bookurmedical.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    @Autowired
    UserRepository userRepository;

    @Override
    @Transactional
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User Not Found with username: " + username));

        // ── Backward-compatibility migration ──────────────────────────────────
        // Users created BEFORE the email-verification system was added will have
        // emailVerified=false but no emailVerificationToken in the DB.
        // Treat them as already verified so they are not suddenly locked out.
        if (!user.isEmailVerified() && user.getEmailVerificationToken() == null) {
            user.setEmailVerified(true);
            userRepository.save(user);
        }

        return UserDetailsImpl.build(user);
    }
}
