package com.example.lab3_clean;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Controller
public class SqlController {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // --- НОВИЙ МЕТОД ДЛЯ ЛОГІНУ ---
    @GetMapping("/login")
    public String loginPage() {
        return "login";
    }
    // -----------------------------

    @GetMapping("/")
    public String home(Model model) {
        loadTreeData(model);
        return "main";
    }

    @PostMapping("/execute")
    public String executeCommand(@RequestParam(value = "command", required = false) String command,
                                 Model model,
                                 Authentication authentication) {

        loadTreeData(model);

        if (command == null || command.trim().isEmpty()) {
            model.addAttribute("error", "Команда не може бути порожньою");
            return "main";
        }

        String sql = command.trim();
        String lowerSql = sql.toLowerCase();
        boolean isModification = !lowerSql.startsWith("select");

        // Перевірка прав адміністратора
        boolean isAdmin = authentication != null && authentication.getAuthorities()
                .contains(new SimpleGrantedAuthority("ROLE_ADMIN"));

        if (isModification && !isAdmin) {
            model.addAttribute("error", "ПОМИЛКА ДОСТУПУ: Тільки адміністратор може змінювати дані!");
            return "main";
        }

        try {
            if (tryHandleCustomCommand(sql, model)) {
                loadTreeData(model);
                return "main";
            }

            if (lowerSql.startsWith("select")) {
                List<Map<String, Object>> result = jdbcTemplate.queryForList(sql);
                model.addAttribute("queryResult", result);
                model.addAttribute("message", "SQL виконано! Знайдено рядків: " + result.size());
            } else {
                int rowsAffected = jdbcTemplate.update(sql);
                loadTreeData(model);
                model.addAttribute("message", "SQL успішно виконано! Змінено рядків: " + rowsAffected);
            }
        } catch (Exception e) {
            model.addAttribute("error", "Помилка виконання: " + e.getMessage());
        }

        return "main";
    }

    private void loadTreeData(Model model) {
        try {
            // Варіант 2: Country -> Resort -> Hotel
            String sql = "SELECT c.name AS Country, r.name AS Resort, h.name AS Hotel, " +
                    "c.id AS C_ID, r.id AS R_ID, h.id AS H_ID " +
                    "FROM country c " +
                    "LEFT JOIN resort r ON c.id = r.country_id " +
                    "LEFT JOIN hotel h ON r.id = h.resort_id " +
                    "ORDER BY c.name, r.name, h.name";
            List<Map<String, Object>> tree = jdbcTemplate.queryForList(sql);
            model.addAttribute("treeData", tree);
        } catch (Exception e) {
            // Ігноруємо помилки
        }
    }

    private boolean tryHandleCustomCommand(String command, Model model) {
        String lowerCmd = command.toLowerCase();

        if (lowerCmd.startsWith("insert country")) {
            String name = extractValue(command, "name='([^']*)'");
            if (name != null) {
                jdbcTemplate.update("INSERT INTO country (name) VALUES (?)", name);
                model.addAttribute("message", "Успішно додано країну: " + name);
                return true;
            }
        }
        else if (lowerCmd.startsWith("insert resort")) {
            String name = extractValue(command, "name='([^']*)'");
            String countryId = extractValue(command, "country_id='(\\d+)'");
            if (name != null && countryId != null) {
                jdbcTemplate.update("INSERT INTO resort (name, country_id) VALUES (?, ?)", name, Integer.parseInt(countryId));
                model.addAttribute("message", "Успішно додано курорт: " + name);
                return true;
            }
        }
        else if (lowerCmd.startsWith("insert hotel")) {
            String name = extractValue(command, "name='([^']*)'");
            String resortId = extractValue(command, "resort_id='(\\d+)'");
            if (name != null && resortId != null) {
                jdbcTemplate.update("INSERT INTO hotel (name, resort_id) VALUES (?, ?)", name, Integer.parseInt(resortId));
                model.addAttribute("message", "Успішно додано готель: " + name);
                return true;
            }
        }
        else if (lowerCmd.startsWith("delete")) {
            String table = null;
            if (lowerCmd.contains("country")) table = "country";
            else if (lowerCmd.contains("resort")) table = "resort";
            else if (lowerCmd.contains("hotel")) table = "hotel";

            String idStr = extractValue(command, "id='(\\d+)'");
            if (table != null && idStr != null) {
                jdbcTemplate.update("DELETE FROM " + table + " WHERE id = ?", Integer.parseInt(idStr));
                model.addAttribute("message", "Успішно видалено запис ID=" + idStr + " з таблиці " + table);
                return true;
            }
        }
        return false;
    }

    private String extractValue(String text, String regex) {
        Pattern p = Pattern.compile(regex);
        Matcher m = p.matcher(text);
        if (m.find()) {
            return m.group(1);
        }
        return null;
    }
}