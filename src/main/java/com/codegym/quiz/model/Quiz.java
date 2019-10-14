package com.codegym.quiz.model;

import lombok.Data;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import java.io.Serializable;

@Entity
@Data
public class Quiz implements Serializable {
    private static final long serialVersionUID = 5926468583005150707L;

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    private String question;

    private String answerA;

    private String answerB;

    private String answerC;

    private String answerD;

    private String correctAnswer;

    private boolean correct;
}
