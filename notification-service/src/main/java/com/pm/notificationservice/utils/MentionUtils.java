package com.pm.notificationservice.utils;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MentionUtils {
    /**
     * Extracts all @username mentions from the given text. Mentions are case-sensitive.
     * @param text The comment text
     * @return Set of mentioned usernames (without the @)
     */
    public static Set<String> extractMentions(String text) {
        Set<String> mentions = new HashSet<>();
        if (text == null) return mentions;
        Pattern mentionPattern = Pattern.compile("@([A-Za-z0-9_]+)");
        Matcher matcher = mentionPattern.matcher(text);
        while (matcher.find()) {
            mentions.add(matcher.group(1));
        }
        return mentions;
    }
}
