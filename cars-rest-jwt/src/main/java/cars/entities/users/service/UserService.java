package cars.entities.users.service;

import cars.entities.users.model.*;

import javax.validation.Valid;

public interface UserService {

    UserViewServiceModel addUser(@Valid UserAddServiceModel userAddServiceModel);

    boolean existByUsername(String username);

    boolean existsByEmail(String email);

    UserViewServiceModel getUserById(Long id);

    UserViewServiceModel getUserByUsername(String username);

    UserViewServiceModel updateUser(String username, UserEditServiceModel userAddBindingModel);
}
