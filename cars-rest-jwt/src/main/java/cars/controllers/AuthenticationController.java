package cars.controllers;

import cars.constants.GlobalConstants;
import cars.entities.cars.model.CarAddBindingModel;
import cars.entities.roles.model.RoleEntity;
import cars.entities.users.model.*;
import cars.entities.users.service.UserService;
import cars.events.logout.OnUserLogoutSuccessEventPublisher;
import cars.jwt.JwtProvider;
import cars.jwt.JwtResponse;
import cars.utils.message.MessageResponse;
import cars.entities.users.details.UserDetailsImpl;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import javax.validation.constraints.Min;
import java.util.*;
import java.util.stream.Collectors;

@CrossOrigin(origins = {"http://localhost:4200", "https://offers-jwt-ngrx.herokuapp.com"}, maxAge = 3600)
@RestController
@RequestMapping("/api/users")
public class AuthenticationController {

    private final AuthenticationManager authenticationManager;

    private final JwtProvider jwtProvider;

    private final UserService userService;

    private final ModelMapper modelMapper;

    private final OnUserLogoutSuccessEventPublisher onUserLogoutSuccessEventPublisher;

    @Autowired
    public AuthenticationController(AuthenticationManager authenticationManager, JwtProvider jwtProvider, UserService userService, ModelMapper modelMapper, OnUserLogoutSuccessEventPublisher onUserLogoutSuccessEventPublisher) {
        this.authenticationManager = authenticationManager;
        this.jwtProvider = jwtProvider;
        this.userService = userService;
        this.modelMapper = modelMapper;
        this.onUserLogoutSuccessEventPublisher = onUserLogoutSuccessEventPublisher;
    }


    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@Valid @RequestBody UserAddBindingModel userAddBindingModel, HttpServletResponse httpServletResponse) {

        if (this.userService.existByUsername(userAddBindingModel.getUsername())) {
            return ResponseEntity
                    .badRequest()
                    .body(new MessageResponse(GlobalConstants.USERNAME_EXISTS));
        }

        if (this.userService.existsByEmail(userAddBindingModel.getEmail())) {
            return ResponseEntity
                    .badRequest()
                    .body(new MessageResponse(GlobalConstants.EMAIL_EXISTS));
        }

        UserAddServiceModel userAddServiceModel = this.modelMapper
                .map(userAddBindingModel, UserAddServiceModel.class);

        UserViewServiceModel userViewServiceModel = this.userService.addUser(userAddServiceModel);

        ResponseEntity<?> response = getResponseEntity(httpServletResponse, userAddServiceModel.getUsername(), userAddServiceModel.getPassword());

        return response;
    }



    @PostMapping("/login")
    public ResponseEntity<?> loginUser(@Valid @RequestBody UserLoginBindingModel userLoginBindingModel, HttpServletResponse httpServletResponse) {

        ResponseEntity<?> response = getResponseEntity(httpServletResponse, userLoginBindingModel.getUsername(), userLoginBindingModel.getPassword());
        return response;
    }


    @PostMapping("/logout")
    public ResponseEntity<?> logoutUser(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) {

        String authToken = readRequestCookie(httpServletRequest, "authToken").orElse(null);

        this.onUserLogoutSuccessEventPublisher.publishLogout(authToken);

        Cookie deleteCookie = createDeleteCookie("authToken", "");
        httpServletResponse.addCookie(deleteCookie);
        addSameSiteCookieAttribute(httpServletResponse);

        return ResponseEntity.ok(new MessageResponse(GlobalConstants.USER_LOGOUT));


    }

    @GetMapping("/profile")
    public ResponseEntity<?> getProfile(HttpServletRequest httpServletRequest) {

        String authToken = readRequestCookie(httpServletRequest, "authToken").orElse(null);
        String username = this.jwtProvider.getUserNameFromJwtToken(authToken);

        UserViewServiceModel userViewServiceModel = this.userService.getUserByUsername(username);
        UserViewBindingModel userViewBindingModel = this.modelMapper.map(userViewServiceModel, UserViewBindingModel.class);

        List<String> roles = getUserRolesAsString(userViewServiceModel);

        userViewBindingModel.setRoles(roles);

        ResponseEntity<?> response = ResponseEntity.ok().body(userViewBindingModel);

        return response;
    }

    @PutMapping("/profile")
    public ResponseEntity<?> editProfile(HttpServletRequest httpServletRequest,
                                        @Valid @RequestBody UserEditBindingModel userEditBindingModel){

        String authToken = readRequestCookie(httpServletRequest, "authToken").orElse(null);
        String username = this.jwtProvider.getUserNameFromJwtToken(authToken);
        UserViewServiceModel userViewServiceModel = this.userService.getUserByUsername(username);

        if (this.userService.existsByEmail(userEditBindingModel.getEmail()) && (!(userEditBindingModel.getEmail()).equals(userViewServiceModel.getEmail()))){
            return ResponseEntity
                    .badRequest()
                    .body(new MessageResponse(GlobalConstants.EMAIL_EXISTS));
        }

        UserEditServiceModel userEditServiceModel = this.modelMapper.map(userEditBindingModel, UserEditServiceModel.class);

       userViewServiceModel = this.userService.updateUser(username, userEditServiceModel);
        UserViewBindingModel userViewBindingModel = this.modelMapper.map(userViewServiceModel, UserViewBindingModel.class);

        List<String> roles = getUserRolesAsString(userViewServiceModel);
        userViewBindingModel.setRoles(roles);

        ResponseEntity<?> response = ResponseEntity.ok().body(userViewBindingModel);
        return response;

    }

    private List<String> getUserRolesAsString(UserViewServiceModel userViewServiceModel){

        Set<RoleEntity> userRoles =  userViewServiceModel.getRoles();

        List<String> userRolesAsString = userRoles.stream()
                .map(roleEntity -> roleEntity.getName().name()).collect(Collectors.toList());

        return userRolesAsString;
    }

    private ResponseEntity<?> getResponseEntity(HttpServletResponse response, String username, String password) {

        Authentication authentication = authenticateUser(username, password);
        String jwt = jwtProvider.generateJwtToken(authentication);

        Cookie authCookie = createAuthCookie("authToken", jwt);
        response.addCookie(authCookie);

        addSameSiteCookieAttribute(response);

        UserViewBindingModel userViewBindingModel = getUserDetails(authentication);

        return ResponseEntity
                .ok()
                .body(userViewBindingModel);
    }

    private void addSameSiteCookieAttribute(HttpServletResponse response) {
        Collection<String> headers = response.getHeaders(HttpHeaders.SET_COOKIE);
        boolean firstHeader = true;
        for (String header : headers) {
            if (firstHeader) {
                response.setHeader(HttpHeaders.SET_COOKIE, String.format("%s; %s", header, "SameSite=Strict"));
                firstHeader = false;
                continue;
            }
            response.addHeader(HttpHeaders.SET_COOKIE, String.format("%s; %s", header, "SameSite=Strict"));
        }
    }

    private Authentication authenticateUser(String username, String password) {

        UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken(username, password);
        Authentication authentication = authenticationManager.authenticate(token);

        SecurityContextHolder.getContext().setAuthentication(authentication);

        return authentication;


    }

    private UserViewBindingModel getUserDetails(Authentication authentication) {

        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();

        UserViewServiceModel userViewServiceModel = this.userService.getUserById(userDetails.getId());

        UserViewBindingModel userViewBindingModel = this.modelMapper.map(userViewServiceModel, UserViewBindingModel.class);

        Set<RoleEntity> userRoles =  userViewServiceModel.getRoles();
        List<String> roles = userRoles.stream().map(roleEntity -> roleEntity.getName().name()).collect(Collectors.toList());

        userViewBindingModel.setRoles(roles);

        return userViewBindingModel;

    }

    private Cookie createAuthCookie(String name, String value) {

        Cookie cookie = new Cookie(name, value);
        cookie.setHttpOnly(true);
        cookie.setSecure(true);
        cookie.setPath("/");
        cookie.setMaxAge(86400);//24h in sec
//        cookie.setDomain("http://localhost:8080");
//        cookie.setDomain("offers-rest-jwt-cookies.herokuapp.com");

        return cookie;
    }

    private Cookie createDeleteCookie(String name, String value) {

        Cookie cookie = new Cookie(name, value);
        cookie.setSecure(true);
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        cookie.setMaxAge(0);

        return cookie;
    }

    private Optional<String> readRequestCookie(HttpServletRequest request, String name) {
        return Arrays.stream(request.getCookies())
                .filter(cookie -> name.equals(cookie.getName()))
                .map(Cookie::getValue)
                .findAny();
    }
}
