package com.codegym.quiz.controller;

import com.codegym.quiz.model.*;
import com.codegym.quiz.service.*;
import org.apache.http.client.ClientProtocolException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.io.IOException;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

@Controller
public class UserController {

    private static final String USER_REGISTER = "user/register";

    private static final String ERROR_404 = "error-404";
    private static final String MESSAGE = "message";
    private static final String DEFAULT_ROLE = "ROLE_USER";
    private static final String TEXT = "To confirm your account, please click here : "
            + "https://springbootlibrary.herokuapp.com/confirm-account?token=";

    private static final String SUBJECT = "Complete Registration!";

    @Autowired
    private UserService userService;

    @Autowired
    private RoleService roleService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private EmailService emailService;

    @Autowired
    private VerificationTokenService verificationTokenService;

    @Autowired
    private RestFB restFB;

    @Autowired
    private GoogleUtils googleUtils;

    @GetMapping("/register")
    public ModelAndView showRegisterForm() {
        ModelAndView modelAndView = new ModelAndView(USER_REGISTER);
        modelAndView.addObject("participant", new Participant());
        return modelAndView;
    }

    @PostMapping("/register")
    public ModelAndView registerNewUser(@Valid @ModelAttribute Participant participant, BindingResult bindingResult) {
        if (bindingResult.hasFieldErrors()) {
            return new ModelAndView(USER_REGISTER);
        }
        if (userService.isRegister(participant)) {
            ModelAndView modelAndView = new ModelAndView(USER_REGISTER);
            modelAndView.addObject(MESSAGE, "username or email is already registered");
            return modelAndView;
        } else if (!userService.isCorrectConfirmPassword(participant)) {
            ModelAndView modelAndView = new ModelAndView(USER_REGISTER);
            modelAndView.addObject(MESSAGE, "Confirm Password is incorrect");
            return modelAndView;
        } else {
            ModelAndView modelAndView = new ModelAndView("user/successfulRegister");
            Role role = roleService.findRoleByName(DEFAULT_ROLE);
            Set<Role> roles = new HashSet<>();
            roles.add(role);
            Participant currentParticipant = new Participant();
            currentParticipant.setUsername(participant.getUsername());
            currentParticipant.setPassword(passwordEncoder.encode(participant.getPassword()));
            currentParticipant.setConfirmPassword(passwordEncoder.encode(participant.getConfirmPassword()));
            currentParticipant.setEmail(participant.getEmail());
            currentParticipant.setPhoneNumber(participant.getPhoneNumber());
            currentParticipant.setRoles(roles);
            userService.save(currentParticipant);
            VerificationToken token = new VerificationToken(currentParticipant);
            token.setExpiryDate(1);
            verificationTokenService.save(token);
            SimpleMailMessage mailMessage = new SimpleMailMessage();
            mailMessage.setTo(participant.getEmail());
            mailMessage.setSubject(SUBJECT);
            mailMessage.setText(TEXT+ token.getToken());

            emailService.sendEmail(participant.getEmail(), SUBJECT, TEXT + token.getToken());
            modelAndView.addObject("participant", currentParticipant);
            modelAndView.addObject("email", currentParticipant.getEmail());
            return modelAndView;
        }
    }

    @RequestMapping(value = "/confirm-account", method = {RequestMethod.GET, RequestMethod.POST})
    public ModelAndView confirmUserAccount(@RequestParam("token") String confirmationToken) {
        ModelAndView modelAndView;
        VerificationToken token = verificationTokenService.findByToken(confirmationToken);
        if (token != null) {
            boolean isExpired = token.isExpired();
            if (!isExpired) {
                Participant participant = userService.findByEmail(token.getParticipant().getEmail());
                participant.setEnabled(true);
                userService.save(participant);
                modelAndView = new ModelAndView("user/accountVerified");
                return modelAndView;
            }
        }
        modelAndView = new ModelAndView("user/error");
        modelAndView.addObject(MESSAGE, "The link is invalid or broken!");
        return modelAndView;
    }

    @GetMapping(value = {"/homepage","/"})
    public ModelAndView home() {
        ModelAndView modelAndView = new ModelAndView("user/homepage");
        modelAndView.addObject("user", userService.getCurrentUser());
        return modelAndView;
    }

    @GetMapping("/newPassword/{id}")
    public ModelAndView showEditForm(@PathVariable Long id, @RequestParam("token") String confirmationToken) {
        VerificationToken token = verificationTokenService.findByToken(confirmationToken);
        if (token != null) {
            boolean isExpired = token.isExpired();
            if (!isExpired) {
                Optional<Participant> user = userService.findById(id);
                if (user.isPresent()) {
                    ModelAndView modelAndView = new ModelAndView("user/newPassword");
                    modelAndView.addObject("user", user);
                    return modelAndView;
                }
            }
        } else {
            ModelAndView modelAndView = new ModelAndView("user/error");
            modelAndView.addObject(MESSAGE, "The link is invalid or broken!");
            return modelAndView;
        }
        return new ModelAndView(ERROR_404);
    }

    @RequestMapping(value = "/newPassword", method = {RequestMethod.GET, RequestMethod.POST})
    public ModelAndView editUser(@ModelAttribute Participant participant) {
        ModelAndView modelAndView = new ModelAndView("user/newPassword");
        if (!userService.isCorrectConfirmPassword(participant)) {
            modelAndView.addObject(MESSAGE, "your confirm password is incorrect");
        } else {
            String newPassword = passwordEncoder.encode(participant.getPassword());
            Optional<Participant> currentUser = userService.findById(participant.getId());
            if(currentUser.isPresent()){
                currentUser.get().setPassword(newPassword);
                userService.save(currentUser.get());
                modelAndView.addObject("user", currentUser);
                modelAndView.addObject(MESSAGE, "Your password is changed");
            }
        }
        return modelAndView;
    }

    @GetMapping("/view/{id}")
    public ModelAndView viewUser(@PathVariable Long id) {
        Optional<Participant> user = userService.findById(id);
        if (!user.isPresent()) {
            return new ModelAndView(ERROR_404);
        }

        ModelAndView modelAndView = new ModelAndView("user/view");
        modelAndView.addObject("user", user);
        return modelAndView;
    }

    @GetMapping("/login")
    public ModelAndView loginForm() {
        return new ModelAndView("login");
    }

    @PostMapping("/login")
    public ModelAndView login(Participant participant) {
        ModelAndView modelAndView;
        if (userService.checkLogin(participant)) {
            modelAndView = new ModelAndView("user/homepage");
            modelAndView.addObject("user", participant);
            return modelAndView;
        }
        modelAndView = new ModelAndView("user/login");
        modelAndView.addObject(MESSAGE, "username or password incorrect");
        return modelAndView;
    }

    @PostMapping("/login-facebook")
    public String loginFacebook(HttpServletRequest request) {
        String code = request.getParameter("code");
        String accessToken = "";
        try {
            accessToken = restFB.getToken(code);
        } catch (IOException e) {
            return "login?facebook=error";
        }
        com.restfb.types.User user = restFB.getUserInfo(accessToken);
        UserDetails userDetail = restFB.buildUser(user);
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(userDetail, null,
                userDetail.getAuthorities());
        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authentication);
        return "redirect:homepage";
    }

    @PostMapping("/login-google")
    public String loginGoogle(HttpServletRequest request) throws ClientProtocolException, IOException {
        String code = request.getParameter("code");

        if (code == null || code.isEmpty()) {
            return "redirect:login?google=error";
        }
        String accessToken = googleUtils.getToken(code);

        GooglePojo googlePojo = googleUtils.getUserInfo(accessToken);
        UserDetails userDetail = googleUtils.buildUser(googlePojo);
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(userDetail, null,
                userDetail.getAuthorities());
        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authentication);
        return "redirect:homepage";
    }
}
