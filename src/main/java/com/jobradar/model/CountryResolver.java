package com.jobradar.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

// Turns free-text locations ("Tel Aviv", "Petah Tikva, IL", "Remote - Israel") into a country name.
public final class CountryResolver {

    private record Keyword(String text, String country) {
    }

    private static final List<Keyword> KEYWORDS = new ArrayList<>();

    static {
        // Israel comes first, so a location like "London or Tel Aviv" resolves to Israel.
        add("Israel",
                "israel", "tel aviv", "jerusalem", "haifa", "herzliya", "petah tikva", "petach tikva",
                "ramat gan", "givatayim", "bnei brak", "ra'anana", "raanana", "netanya", "kfar saba",
                "hod hasharon", "rehovot", "ness ziona", "rishon lezion", "rishon le zion", "holon",
                "bat yam", "or yehuda", "airport city", "modiin", "rosh haayin", "rosh ha'ayin",
                "yokneam", "caesarea", "beer sheva", "be'er sheva", "beersheba", "ashdod", "ashkelon",
                "lod", "ramla", "kiryat gat", "nazareth");
        add("United States",
                "united states", "usa", "us", "new york", "san francisco", "boston", "seattle",
                "austin", "chicago", "los angeles", "denver", "atlanta", "miami");
        add("United Kingdom", "united kingdom", "uk", "england", "london", "manchester");
        add("Germany", "germany", "berlin", "munich", "hamburg");
        add("France", "france", "paris");
        add("Netherlands", "netherlands", "amsterdam");
        add("Ireland", "ireland", "dublin");
        add("Spain", "spain", "madrid", "barcelona");
        add("Portugal", "portugal", "lisbon");
        add("Poland", "poland", "warsaw", "krakow");
        add("Bulgaria", "bulgaria", "sofia");
        add("Romania", "romania", "bucharest");
        add("Ukraine", "ukraine", "kyiv", "kiev");
        add("India", "india", "bangalore", "bengaluru", "hyderabad", "pune");
        add("Singapore", "singapore");
        add("Japan", "japan", "tokyo");
        add("Australia", "australia", "sydney", "melbourne");
        add("Canada", "canada", "toronto", "vancouver");
    }

    private CountryResolver() {
    }

    public static String resolve(String location) {
        if (location == null || location.isBlank()) {
            return null;
        }

        String normalized = normalize(location);

        for (Keyword keyword : KEYWORDS) {
            if (normalized.contains(keyword.text())) {
                return keyword.country();
            }
        }
        return null;
    }

    private static void add(String country, String... words) {
        for (String word : words) {
            KEYWORDS.add(new Keyword(normalize(word), country));
        }
    }

    // Lowercase, turn everything that is not a letter into a space, and pad with spaces,
    // so "Tel-Aviv" matches "tel aviv", and "us" matches only as a whole word (not inside "Austin").
    private static String normalize(String text) {
        String lettersOnly = text.toLowerCase(Locale.ROOT).replaceAll("[^a-z]+", " ").strip();
        return " " + lettersOnly + " ";
    }
}