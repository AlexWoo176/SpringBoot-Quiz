package com.codegym.quiz.service;

import com.codegym.quiz.model.User;
import org.springframework.security.core.userdetails.UserDetailsService;

import java.util.Optional;


public interface UserService extends UserDetailsService {
    void save(User user);

    Iterable<User> findAll();

    User findByUsername(String username);

    boolean checkLogin(User user);

    boolean isRegister(User user);

    User findByEmail(String email);

    boolean isCorrectConfirmPassword(User user);

    Optional<User> findById(Long id);

    User getCurrentUser();
}
