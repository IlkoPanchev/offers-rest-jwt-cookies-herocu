package cars.entities.users;

import cars.constants.GlobalConstants;
import cars.entities.roles.RoleRepository;
import cars.entities.roles.model.RoleEntity;
import cars.entities.roles.model.RoleName;
import cars.entities.users.model.UserAddBindingModel;
import cars.entities.users.model.UserEditBindingModel;
import cars.entities.users.model.UserEntity;
import cars.entities.users.model.UserLoginBindingModel;
import cars.entities.users.repository.UserRepository;
import cars.entities.users.service.UserService;
import cars.events.logout.OnUserLogoutSuccessEventPublisher;
import cars.jwt.JwtProvider;
import cars.jwt.LoggedOutJwtTokenCache;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.minidev.json.parser.JSONParser;
import org.json.JSONObject;
import org.junit.jupiter.api.*;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.EmbeddedDatabaseConnection;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import javax.servlet.http.Cookie;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@AutoConfigureTestDatabase(connection = EmbeddedDatabaseConnection.H2)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class AuthenticationControllerIntegrationTests {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    private final String USERNAME = "username";
    private final String EXISTING_USERNAME = "existing_username";
    private final String PASSWORD = "password";
    private final String EMAIL = "email@abv.bg";
    private final String EXISTING_EMAIL = "existing_email@abv.bg";
    private final String PHONE = "+359887586666";
    private final String EXISTING_PHONE = "+359887586666";
    private final String ADDRESS = "ul.Tintayava 17";
    private final String EXISTING_ADDRESS = "ul.Tintayava 17";
    private final String NEW_ADDRESS = "ul.Vitosha 177";

    private final String ROLE = "ROLE_USER";

    private UserAddBindingModel userAddBindingModel;
    private UserLoginBindingModel userLoginBindingModel;
    private UserEditBindingModel userEditBindingModel;

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private RoleRepository roleRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    public void setUp() {
        this.userAddBindingModel = this.createUserAddBindingModel();
        this.userLoginBindingModel = this.createUserLoginBindingModel();
        this.userEditBindingModel = this.createUserEditBindingModel();
        this.userRepository.deleteAll();
        this.initUserRepository();
    }

    @Test
    @Order(1)
    public void testRegisterUserValidUserData() throws Exception {

        mockMvc.perform(MockMvcRequestBuilders.post("/api/users/register")
                .content(objectMapper.writeValueAsString(this.userAddBindingModel))
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.username").value(this.USERNAME))
                .andExpect(jsonPath("$.email").value(this.EMAIL))
                .andExpect(jsonPath("$.address").value(this.ADDRESS))
                .andExpect(jsonPath("$.phone").value(this.PHONE))
                .andExpect(jsonPath("$.roles").value(this.ROLE));
    }


    @Test
    @Order(2)
    public void testRegisterUserInvalidUsername() throws Exception {

        this.userAddBindingModel.setUsername("u");

        mockMvc.perform(MockMvcRequestBuilders.post("/api/users/register")
                .content(objectMapper.writeValueAsString(this.userAddBindingModel))
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON))
                .andExpect(status().is(400));
    }

    @Test
    @Order(3)
    public void testRegisterUserInvalidPassword() throws Exception {

        this.userAddBindingModel.setPassword("p");

        mockMvc.perform(MockMvcRequestBuilders.post("/api/users/register")
                .content(objectMapper.writeValueAsString(this.userAddBindingModel))
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON))
                .andExpect(status().is(400));
    }

    @Test
    @Order(4)
    public void testRegisterUserInvalidEmail() throws Exception {

        this.userAddBindingModel.setEmail("email.email.com");

        mockMvc.perform(MockMvcRequestBuilders.post("/api/users/register")
                .content(objectMapper.writeValueAsString(this.userAddBindingModel))
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON))
                .andExpect(status().is(400));
    }

    @Test
    @Order(5)
    public void testRegisterUserExistingEmail() throws Exception {

        this.userAddBindingModel.setEmail(this.EXISTING_EMAIL);

        mockMvc.perform(MockMvcRequestBuilders.post("/api/users/register")
                .content(objectMapper.writeValueAsString(this.userAddBindingModel))
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON))
                .andExpect(status().is(400))
                .andExpect(jsonPath("$.message").value("Email is already in use!"));
        ;
    }

    @Test
    @Order(6)
    public void testRegisterUserExistingUsername() throws Exception {

        this.userAddBindingModel.setUsername(this.EXISTING_USERNAME);

        mockMvc.perform(MockMvcRequestBuilders.post("/api/users/register")
                .content(objectMapper.writeValueAsString(this.userAddBindingModel))
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON))
                .andExpect(status().is(400))
                .andExpect(jsonPath("$.message").value("Username is already taken!"));
        ;
    }


    @Test
    @Order(7)
    public void testLoginUserValidUserData() throws Exception {

        mockMvc.perform(MockMvcRequestBuilders.post("/api/users/login")
                .content(objectMapper.writeValueAsString(this.userLoginBindingModel))
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.username").value(this.EXISTING_USERNAME))
                .andExpect(jsonPath("$.email").value(this.EXISTING_EMAIL))
                .andExpect(jsonPath("$.phone").value(this.EXISTING_PHONE))
                .andExpect(jsonPath("$.address").value(this.EXISTING_ADDRESS))
                .andExpect(jsonPath("$.roles").value(this.ROLE));
    }



    @Test
    @Order(8)
    public void testGetProfile() throws Exception {

        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.post("/api/users/login")
                        .content(objectMapper.writeValueAsString(this.userLoginBindingModel))
                        .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        Cookie authCookie = result.getResponse().getCookie("authToken");

        mockMvc.perform(MockMvcRequestBuilders.get("/api/users/profile")
                        .cookie(authCookie))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.username").value(this.EXISTING_USERNAME))
                .andExpect(jsonPath("$.email").value(this.EXISTING_EMAIL))
                .andExpect(jsonPath("$.phone").value(this.EXISTING_PHONE))
                .andExpect(jsonPath("$.address").value(this.EXISTING_ADDRESS))
                .andExpect(jsonPath("$.roles").value(this.ROLE));



    }

    @Test
    @Order(9)
    public void testEditProfile() throws Exception {

        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.post("/api/users/login")
                        .content(objectMapper.writeValueAsString(this.userLoginBindingModel))
                        .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        Cookie authCookie = result.getResponse().getCookie("authToken");

        mockMvc.perform(MockMvcRequestBuilders.put("/api/users/profile")
                        .content(objectMapper.writeValueAsString(this.userEditBindingModel))
                        .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON)
                        .cookie(authCookie))

                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.username").value(this.EXISTING_USERNAME))
                .andExpect(jsonPath("$.email").value(this.EXISTING_EMAIL))
                .andExpect(jsonPath("$.phone").value(this.EXISTING_PHONE))
                .andExpect(jsonPath("$.address").value(this.NEW_ADDRESS))
                .andExpect(jsonPath("$.roles").value(this.ROLE));

    }

    @Test
    @Order(10)
    public void testLogoutUser() throws Exception {

        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.post("/api/users/login")
                        .content(objectMapper.writeValueAsString(this.userLoginBindingModel))
                        .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        Cookie authCookie = result.getResponse().getCookie("authToken");

        mockMvc.perform(MockMvcRequestBuilders.post("/api/users/logout")
                        .cookie(authCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("User has successfully logged out"));
        ;
        ;

    }

//    @Test
//    @Order(11)
//    public void testLoginUserWithTokenOfAlreadyLoggedOutUser() throws Exception {
//
//        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.post("/api/users/login")
//                        .content(objectMapper.writeValueAsString(this.userLoginBindingModel))
//                        .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON))
//                .andExpect(status().isOk())
//                .andReturn();
//
//        Cookie authCookie = result.getResponse().getCookie("authToken");
//
//        mockMvc.perform(MockMvcRequestBuilders.post("/api/users/logout")
//                        .cookie(authCookie))
//                .andExpect(status().isOk());
//
//        mockMvc.perform(MockMvcRequestBuilders.post("/api/users/login")
//                        .content(objectMapper.writeValueAsString(this.userAddBindingModel))
//                        .cookie(authCookie))
//                .andExpect(status().isUnauthorized());
//
//    }


    private UserEditBindingModel createUserEditBindingModel() {

        UserEditBindingModel userEditBindingModel = new UserEditBindingModel();
        userEditBindingModel.setEmail(this.EXISTING_EMAIL);
        userEditBindingModel.setAddress(this.NEW_ADDRESS);
        userEditBindingModel.setPhone(this.EXISTING_PHONE);

        return userEditBindingModel;
    }

    private UserLoginBindingModel createUserLoginBindingModel() {

        UserLoginBindingModel userLoginBindingModel = new UserLoginBindingModel();
        userLoginBindingModel.setUsername(this.EXISTING_USERNAME);
        userLoginBindingModel.setPassword(this.PASSWORD);

        return userLoginBindingModel;
    }

    private void initUserRepository() {

        UserEntity userEntity = new UserEntity();
        userEntity.setUsername(this.EXISTING_USERNAME);
        userEntity.setPassword(this.passwordEncoder.encode(this.PASSWORD));
        userEntity.setEmail(this.EXISTING_EMAIL);
        userEntity.setAddress(this.EXISTING_ADDRESS);
        userEntity.setPhone(this.EXISTING_PHONE);

        RoleEntity userRole = roleRepository.findByName(RoleName.ROLE_USER)
                .orElseThrow(() -> new RuntimeException(GlobalConstants.ROLE_NOT_FOUND + RoleName.ROLE_USER));
        userEntity.getRoles().add(userRole);

        this.userRepository.saveAndFlush(userEntity);
    }

    private UserAddBindingModel createUserAddBindingModel() {

        UserAddBindingModel userAddBindingModel = new UserAddBindingModel();
        userAddBindingModel.setUsername(this.USERNAME);
        userAddBindingModel.setPassword(this.PASSWORD);
        userAddBindingModel.setEmail(this.EMAIL);
        userAddBindingModel.setAddress(this.ADDRESS);
        userAddBindingModel.setPhone(this.PHONE);
        userAddBindingModel.setRoles(Set.of(this.ROLE));

        return userAddBindingModel;
    }


}
