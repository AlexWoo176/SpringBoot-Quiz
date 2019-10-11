package com.codegym.quiz.service;

import com.codegym.quiz.model.VerificationToken;

public interface VerificationTokenService {
    VerificationToken findByToken(String token);

    void save(VerificationToken token);
}
