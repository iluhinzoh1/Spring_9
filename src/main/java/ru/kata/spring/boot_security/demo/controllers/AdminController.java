package ru.kata.spring.boot_security.demo.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import ru.kata.spring.boot_security.demo.models.User;
import ru.kata.spring.boot_security.demo.services.RoleServiceImp;
import ru.kata.spring.boot_security.demo.services.UserServiceImp;

import java.security.Principal;
import java.util.List;

@Controller
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {
    private final UserServiceImp userServiceImp;
    private final RoleServiceImp roleServiceImp;


    @Autowired
    public AdminController(UserServiceImp userServiceImp, RoleServiceImp roleServiceImp) {
        this.userServiceImp = userServiceImp;
        this.roleServiceImp = roleServiceImp;
    }

    @GetMapping()
    public String Users(Model model, Principal principal, @RequestParam(value = "username", required = false) String username) {
        List<User> user = userServiceImp.findAllUsers();
        model.addAttribute("allUsers", user);
        User user2 = userServiceImp.findByUserName(principal.getName());
        if (user2 == null) {
            return "redirect:/login";
        }
        User updateUser;
        if (username != null && !username.isEmpty()) {
            updateUser = userServiceImp.findByUserName(username);
            if (updateUser == null) {
                updateUser = new User();
            }
        } else {
            updateUser = new User();
        }
        model.addAttribute("updateUserId", updateUser);
        model.addAttribute("roles", roleServiceImp.getAllRoles());
        model.addAttribute("userShow", user2);
        model.addAttribute("save", new User());
        return "admin";
    }

    @PostMapping("/saveUsers")
    public String saveUser(@ModelAttribute("save") User user) {
        userServiceImp.saveUser(user);
        return "redirect:/admin";
    }

    @PostMapping("/updateUserById")
    public String updateUserById(
            @ModelAttribute("updateUserId") User user,
            @RequestParam(value = "newPassword", required = false) String newPassword,
            Principal principal) {

        User existingUser = userServiceImp.findById(user.getId());
        existingUser.setUsername(user.getUsername());
        existingUser.setFirstName(user.getFirstName());
        existingUser.setLastName(user.getLastName());
        existingUser.setAge(user.getAge());
        existingUser.setRoles(user.getRoles());

        if (newPassword != null && !newPassword.isEmpty()) {
            existingUser.setPassword(newPassword);
        }
        if (user.getRoles() == null || user.getRoles().isEmpty()) {
            user.setRoles(existingUser.getRoles());
        }
        userServiceImp.updateUser(user, newPassword);
        if (principal.getName().equals(existingUser.getUsername())) {
            Authentication authentication = new UsernamePasswordAuthenticationToken(
                    existingUser, existingUser.getPassword(), existingUser.getAuthorities());
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }
        if (!principal.getName().equals(existingUser.getUsername())) {
            return "redirect:/admin";
        }
        if (existingUser.getRoles().stream().anyMatch(role -> role.getName().equals("ROLE_ADMIN"))) {
            return "redirect:/admin";
        } else if (existingUser.getRoles().stream().anyMatch(role -> role.getName().equals("ROLE_USER"))) {
            return "redirect:/user";
        } else {
            return "redirect:/login";
        }
    }

    @PostMapping("deleteUser")
    public String deleteUserById(@RequestParam(value = "id") Long id) {
        userServiceImp.deleteUser(id);
        return "redirect:/admin";
    }
}
