package com.codegym.quiz.repository;

import com.codegym.quiz.model.Participant;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<Participant, Long> {
    Participant findByUsername(String username);

    Participant findByEmail(String email);
}
