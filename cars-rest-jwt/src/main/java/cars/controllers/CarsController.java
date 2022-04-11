package cars.controllers;

import cars.entities.cars.model.*;
import cars.entities.cars.service.CarService;
import cars.jwt.JwtProvider;
import cars.utils.validation.ValidationUtil;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.validation.ConstraintViolationException;
import javax.validation.Valid;
import javax.validation.constraints.Min;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@CrossOrigin(origins = {"http://localhost:4200", "https://offers-jwt-ngrx.herokuapp.com"}, maxAge = 3600)
@RequestMapping("/api/offers")
public class CarsController {


    private final ModelMapper modelMapper;
    private final CarService carService;
    private final ValidationUtil validationUtil;
    private final JwtProvider jwtProvider;

    @Autowired
    public CarsController(ModelMapper modelMapper, CarService carService, ValidationUtil validationUtil, JwtProvider jwtProvider) {
        this.modelMapper = modelMapper;
        this.carService = carService;
        this.validationUtil = validationUtil;
        this.jwtProvider = jwtProvider;
    }



    @GetMapping()
    @ResponseBody
    public Page<CarViewBindingModel> getAllCars(@RequestParam(name = "keyword", defaultValue = "") String keyword,
                                                @RequestParam(name = "ownerId", defaultValue = "0") Long ownerId,
                                                @RequestParam(name = "page", defaultValue = "0") Integer page,
                                                @RequestParam(name = "pageSize", defaultValue = "6") Integer pageSize) {

        Pageable pageable = PageRequest.of(page, pageSize);
        Page<CarViewServiceModel> carViewServiceModelPage;

        if (!keyword.equals("")) {
            carViewServiceModelPage = this.carService.search(keyword, pageable);
        } else if(ownerId != 0){
            carViewServiceModelPage = this.carService.getAllCarsByOwnerId(ownerId, pageable);
        }else {
            carViewServiceModelPage = this.carService.getAllCars(pageable);
        }

        List<CarViewBindingModel> carViewBindingModels = carViewServiceModelPage
                .stream()
                .map(carViewServiceModel -> this.modelMapper.map(carViewServiceModel, CarViewBindingModel.class))
                .collect(Collectors.toList());

        Page<CarViewBindingModel> carViewBindingModelPage = new PageImpl<>(carViewBindingModels, pageable, carViewServiceModelPage.getTotalElements());
        return carViewBindingModelPage;
    }



    @PostMapping()
    @ResponseStatus(HttpStatus.CREATED)
    @ResponseBody
    public CarViewBindingModel createCar( HttpServletRequest httpServletRequest,
                                         @Valid @ModelAttribute CarAddBindingModel carAddBindingModel,
                                         @RequestParam (name = "imageUrl", required = false)MultipartFile multipartFile) throws IOException {

        CarAddServiceModel carAddServiceModel = this.modelMapper
                .map(carAddBindingModel, CarAddServiceModel.class);
        ImageAddBindingModel imageAddBindingModel = new ImageAddBindingModel(multipartFile);

        String authToken = readRequestCookie(httpServletRequest, "authToken").orElse(null);
        String username = this.jwtProvider.getUserNameFromJwtToken(authToken);

        if(this.validationUtil.isValid(imageAddBindingModel)){

            ImageAddServiceModel imageAddServiceModel = this.modelMapper.map(imageAddBindingModel, ImageAddServiceModel.class);
            CarViewServiceModel carViewServiceModel = this.carService.createCar(carAddServiceModel, username, imageAddServiceModel);
            CarViewBindingModel carViewBindingModel = this.modelMapper.map(carViewServiceModel, CarViewBindingModel.class);

            return carViewBindingModel;

        }else {

            throw new ConstraintViolationException(this.validationUtil.
                    getViolations(imageAddBindingModel));
        }


    }

    @GetMapping("/{id}")
    @ResponseBody
    public CarViewBindingModel getCar(@PathVariable("id") @Min(1) Long id) {

        CarViewServiceModel carViewServiceModel = this.carService.getCarById(id);
        CarViewBindingModel carViewBindingModel = this.modelMapper.map(carViewServiceModel, CarViewBindingModel.class);

        return carViewBindingModel;

    }



    @PutMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    public CarViewBindingModel updateCar(@PathVariable("id") @Min(1) Long id,
                          @Valid @ModelAttribute CarAddBindingModel carAddBindingModel,
                          @RequestParam (name = "imageUrl", required = false)MultipartFile multipartFile) throws IOException {

        CarAddServiceModel carAddServiceModel = this.modelMapper
                .map(carAddBindingModel, CarAddServiceModel.class);
        ImageAddBindingModel imageAddBindingModel = new ImageAddBindingModel(multipartFile);

        CarViewBindingModel carViewBindingModel;

        if(this.validationUtil.isValid(imageAddBindingModel)){
            ImageAddServiceModel imageAddServiceModel = this.modelMapper.map(imageAddBindingModel, ImageAddServiceModel.class);
            CarViewServiceModel carViewServiceModel =  this.carService.updateCar(carAddServiceModel, id, imageAddServiceModel);
            carViewBindingModel = this.modelMapper.map(carViewServiceModel, CarViewBindingModel.class);

        }else {

            throw new ConstraintViolationException(this.validationUtil.
                    getViolations(imageAddBindingModel));
        }

        return carViewBindingModel;

    }


    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    public void deleteCar(@PathVariable("id") @Min(1) Long id) {

        this.carService.deleteCar(id);
    }


    @GetMapping("/count")
    @ResponseStatus(HttpStatus.OK)
    public Long getCarsCount(@RequestParam(name = "ownerId", defaultValue = "0") Long ownerId,
                             @RequestParam(name = "keyword", defaultValue = "") String keyword) {

        Long carsCount;
        if(ownerId != 0){
            carsCount = this.carService.getCarsRepositoryCountByOwnerId(ownerId);
        }else if(!keyword.equals("")){
            carsCount = this.carService.getCarsRepositoryCountByKeyword(keyword);
        }else {
            carsCount = this.carService.getCarsRepositoryCount();
        }

        return carsCount;
    }

    private Optional<String> readRequestCookie(HttpServletRequest request, String name) {
        return Arrays.stream(request.getCookies())
                .filter(cookie -> name.equals(cookie.getName()))
                .map(Cookie::getValue)
                .findAny();
    }





}
