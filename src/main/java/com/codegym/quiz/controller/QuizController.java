package com.codegym.quiz.controller;

import com.codegym.quiz.model.Quiz;
import com.codegym.quiz.service.QuizService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import java.util.Optional;

@Controller
public class QuizController {
    @Autowired
    private QuizService quizService;

    @GetMapping("/questions")
    public ModelAndView showAllQuiz(){
        Iterable<Quiz> quizzes = quizService.findAll();
        ModelAndView modelAndView = new ModelAndView("quiz/list");
        modelAndView.addObject("quizzes",quizzes);
        return modelAndView;
    }

    @GetMapping(name = "/question/{id}")
    public ModelAndView showQuestion(@RequestParam("id") Long id){
        Optional<Quiz> quiz = quizService.findById(id);
        ModelAndView modelAndView = new ModelAndView("quiz/question");
        modelAndView.addObject("quiz",quiz);
        return modelAndView;
    }

    @GetMapping(name = "/newQuestion")
    public ModelAndView showCreateForm(){
        ModelAndView modelAndView = new ModelAndView("quiz/newQuestion");
        modelAndView.addObject("quiz",new Quiz());
        return modelAndView;
    }

    @PostMapping(name = "/newQuestion")
    public ModelAndView newQuestion(@ModelAttribute Quiz quiz){
        quizService.save(quiz);
        ModelAndView modelAndView = new ModelAndView("quiz/newQuestion");
        modelAndView.addObject("quiz",quiz);
        modelAndView.addObject("messenger","Created!");
        return modelAndView;
    }

}
