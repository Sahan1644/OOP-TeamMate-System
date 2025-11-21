package com.teamMate.util;

import com.teamMate.model.Participant;

import java.util.List;
import java.util.regex.Pattern;

public class ValidationUtil {
    private static final Pattern EMAIL = Pattern.compile("^[\\w._%+-]+@[\\w.-]+\\.[A-Za-z]{2,}$");

    public static boolean isValidEmail(String e) {
        if (e == null) return false;
        return EMAIL.matcher(e).matches();
    }

    public static boolean isValidSkill(int s) {
        return s >= 1 && s <= 10;
    }

    public static boolean idExists(List<Participant> list, String id) {
        return list.stream().anyMatch(p -> p.getId().equalsIgnoreCase(id));
    }

    public static boolean emailExists(List<Participant> list, String email) {
        return list.stream().anyMatch(p -> p.getEmail().equalsIgnoreCase(email));
    }
}
