package com.example.controllers;

import com.example.dao.LocationDao;
import com.example.dto.response.WeatherCardDto;
import com.example.dto.response.LocationResponseDto;
import com.example.dto.response.WeatherResponseDto;
import com.example.models.Locations;
import com.example.services.LocationService;
import com.example.services.WeatherService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.*;

@Controller
@RequestMapping("/weather")
@RequiredArgsConstructor

public class WeatherController {

    private final WeatherService weatherService;
    private final LocationService locationService;
    private final LocationDao locationDao;

    /**
     * Контроллер отвечает за вывод главной сраницы и полученным из сессии логином который был сохранен при аутентификации
     *
     * @return возвращаем основную страницу с карточками
     */
    @GetMapping
    public String mainScreenPage(HttpServletRequest request, Model model, HttpSession httpSession) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return "redirect:/auth/sign-in";
        }

        String login = (String) session.getAttribute("login");
        Integer userId = (Integer) httpSession.getAttribute("id");
        if (userId == null) {
            return "redirect:/auth/sign-in";
        }

        model.addAttribute("login", login);

        List<Locations> locations = locationDao.findLocationsByUserId(userId);
        List<WeatherCardDto> weatherCards = new ArrayList<>();

        for (Locations location : locations) {
            double lat = location.getLatitude();
            double lon = location.getLongitude();
            try {
                LocationResponseDto weather = locationService.searchWeather(lat, lon);
                weatherCards.add(new WeatherCardDto(location.getId(), location.getName(), weather));
            } catch (Exception e) {
                System.err.println("Ошибка получения погоды для " + location.getName() + ": " + e.getMessage());
            }
        }

        model.addAttribute("weatherCards", weatherCards);
        return "pages/index";
    }


    @DeleteMapping("/delete-card/{id}")
    public String deleteCard(@PathVariable("id") int locationId, HttpSession session) {
        Integer userId = (Integer) session.getAttribute("id");
        if (userId != null) {
            List<Locations> locations = locationDao.findLocationsByUserId(userId);
            if (locations.stream().anyMatch(location -> location.getId() == locationId)) {
                locationDao.deleteLocationById(locationId);
            }
        }

        return "redirect:/weather";
    }

    /**
     * Выполняет поиск города по названию и назначает уникальный ключ каждой найденной локации пользователя.
     * Результат поиска сохраняется в HashMap с уникальным ключом как ключом и данными о погоде как значением.
     * Ключ и дополнительные данные о городе передаются на фронтенд для отображения.
     *
     * @param nameCity           название города, введенное пользователем и полученное из формы
     * @param request            объект HttpServletRequest для доступа к данным сессии
     * @param redirectAttributes объект RedirectAttributes для передачи всплывающих атрибутов при редиректе
     * @param session            объект HttpSession для хранения ожидающих локаций с уникальными ключами
     * @return имя представления ("pages/search-results") для отображения результатов поиска
     * @throws IllegalArgumentException если город не найден или входные данные недействительны
     */
    @PostMapping("/search-results")
    public String search(@RequestParam("nameCity") String nameCity,
                         Model model,
                         HttpServletRequest request,
                         RedirectAttributes redirectAttributes,
                         HttpSession session) {

        if (nameCity == null || nameCity.trim().isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Город не может быть пустым");
            return "redirect:/weather";
        }

        HttpSession sessionFalse = request.getSession(false);

        if (sessionFalse != null) {
            String login = (String) sessionFalse.getAttribute("login");
            model.addAttribute("login", login);
        }

        WeatherResponseDto search = weatherService.searchCity(nameCity);

        if (search == null) {
            throw new IllegalArgumentException("Не удалось найти город");
        }

        // уникальность попадания в БД
        if (locationDao.uniqueLocationDate(search.getCoord().getLat(), search.getCoord().getLon(), (Integer) session.getAttribute("id"))) {
            redirectAttributes.addFlashAttribute("successfulMessage", "Вы уже добавли этот город");
            return "redirect:/weather";
        }

        String locationKey = UUID.randomUUID().toString(); // ключ для каждоый локации
        Map<String, WeatherResponseDto> pendingLocations = (Map<String, WeatherResponseDto>) session.getAttribute("pendingLocations");
        if (pendingLocations == null) {
            pendingLocations = new HashMap<>();
            session.setAttribute("pendingLocations", pendingLocations);
        }

        pendingLocations.put(locationKey, search);
        model.addAttribute("locationKey", locationKey);
        model.addAttribute("nameCity", search.getName());
        model.addAttribute("coord", search.getCoord());
        model.addAttribute("sys", search.getSys());

        return "pages/search-results";
    }

    @PostMapping("/add-location")
    public String addLocation(@RequestParam("locationKey") String locationKey,
                              HttpSession session) {

        Map<String, WeatherResponseDto> pendingLocations = (Map<String, WeatherResponseDto>) session.getAttribute("pendingLocations");
        if (pendingLocations != null) {
            WeatherResponseDto responseDto = pendingLocations.get(locationKey);
            if (responseDto != null) {
                Integer id = (Integer) session.getAttribute("id"); // получаем id который был сохранен при аутентификации пользователя
                locationService.saveLocation(
                        id,
                        responseDto.getName(),
                        responseDto.getCoord().getLat(),
                        responseDto.getCoord().getLon()
                );
                pendingLocations.remove(locationKey); // сразу же удаляем из hashmap данные о локации после добавляение ее в БД
            }
        }

        return "redirect:/weather";
    }


}




