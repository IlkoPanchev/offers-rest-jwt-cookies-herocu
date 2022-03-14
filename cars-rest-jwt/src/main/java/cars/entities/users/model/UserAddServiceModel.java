package cars.entities.users.model;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import java.util.Set;

public class UserAddServiceModel {

    private String username;
    private String password;
    private String email;
    private String phone;
    private String address;
    private Set<String> roles;

    public String getUsername() {
        return username;
    }

    public UserAddServiceModel setUsername(String username) {
        this.username = username;
        return this;
    }

    public String getPassword() {
        return password;
    }

    public UserAddServiceModel setPassword(String password) {
        this.password = password;
        return this;
    }

    public String getEmail() {
        return email;
    }

    public UserAddServiceModel setEmail(String email) {
        this.email = email;
        return this;
    }

    @NotBlank
    @Size(max = 20)
    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    @NotBlank
    @Size(max = 50)
    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public Set<String> getRoles() {
        return roles;
    }

    public UserAddServiceModel setRoles(Set<String> roles) {
        this.roles = roles;
        return this;
    }
}
