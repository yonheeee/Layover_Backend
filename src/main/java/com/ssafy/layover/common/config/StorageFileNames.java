package com.ssafy.layover.common.config;

import java.util.Locale;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class StorageFileNames {

    private static final Pattern SAFE_EXTENSION = Pattern.compile("\\.([A-Za-z0-9]{1,10})$");

    private StorageFileNames() {
    }

    static String randomName(String originalFilename) {
        String extension = "";
        if (originalFilename != null) {
            Matcher matcher = SAFE_EXTENSION.matcher(originalFilename);
            if (matcher.find()) {
                extension = "." + matcher.group(1).toLowerCase(Locale.ROOT);
            }
        }
        return UUID.randomUUID() + extension;
    }
}
