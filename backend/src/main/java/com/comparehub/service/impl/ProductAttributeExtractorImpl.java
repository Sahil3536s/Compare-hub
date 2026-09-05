package com.comparehub.service.impl;

import com.comparehub.dto.ProductAttributesDto;
import com.comparehub.service.ProductAttributeExtractor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
public class ProductAttributeExtractorImpl implements ProductAttributeExtractor {

    // Known Brands
    private static final Map<String, String> BRAND_DICTIONARY = Map.ofEntries(
            Map.entry("apple", "Apple"),
            Map.entry("iphone", "Apple"),
            Map.entry("ipad", "Apple"),
            Map.entry("macbook", "Apple"),
            Map.entry("airpods", "Apple"),
            Map.entry("samsung", "Samsung"),
            Map.entry("galaxy", "Samsung"),
            Map.entry("sony", "Sony"),
            Map.entry("bravia", "Sony"),
            Map.entry("playstation", "Sony"),
            Map.entry("dell", "Dell"),
            Map.entry("alienware", "Dell"),
            Map.entry("asus", "Asus"),
            Map.entry("rog", "Asus"),
            Map.entry("hp", "HP"),
            Map.entry("lenovo", "Lenovo"),
            Map.entry("thinkpad", "Lenovo"),
            Map.entry("oneplus", "OnePlus"),
            Map.entry("xiaomi", "Xiaomi"),
            Map.entry("redmi", "Xiaomi"),
            Map.entry("realme", "Realme"),
            Map.entry("google", "Google"),
            Map.entry("pixel", "Google"),
            Map.entry("nothing", "Nothing"),
            Map.entry("bose", "Bose"),
            Map.entry("boat", "boAt"),
            Map.entry("noise", "Noise"),
            Map.entry("lg", "LG")
    );

    // Color Patterns
    private static final String[] COLOR_LIST = {
            "Natural Titanium", "Desert Titanium", "Black Titanium", "White Titanium",
            "Phantom Black", "Space Black", "Space Gray", "Space Grey", "Midnight",
            "Starlight", "Silver", "Graphite", "Gold", "Rose Gold", "Titanium",
            "Black", "White", "Blue", "Green", "Red", "Yellow", "Purple", "Cream", "Gray", "Grey"
    };

    // Variant Patterns
    private static final String[] VARIANT_LIST = {
            "Pro Max", "Pro", "Ultra", "Plus", "Max", "Mini", "5G", "4G", "LTE", "Wi-Fi", "Slim", "OLED"
    };

    // Regex for Storage: e.g. "256 GB", "256GB", "1 TB", "1TB", "12/256GB", "(Black, 256 GB)"
    private static final Pattern STORAGE_SLASH_PATTERN = Pattern.compile(
            "\\b\\d+\\s*/\\s*(\\d{2,4})\\s*(?:gb)?\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern STORAGE_EXPLICIT_PATTERN = Pattern.compile(
            "\\b(64|128|256|512)\\s*(?:gb|gigabytes?)\\b|\\b(1|2)\\s*(?:tb|terabytes?)\\b", Pattern.CASE_INSENSITIVE);

    // Regex for RAM: e.g. "12GB RAM", "12GB", "12/256GB", "16 GB"
    private static final Pattern RAM_SLASH_PATTERN = Pattern.compile(
            "\\b(4|6|8|12|16|24|32|64)\\s*/\\s*\\d{2,4}", Pattern.CASE_INSENSITIVE);
    private static final Pattern RAM_EXPLICIT_PATTERN = Pattern.compile(
            "\\b(4|6|8|12|16|24|32|64)\\s*(?:gb|gigabytes?)\\s*(?:ram)\\b|\\b(4|6|8|12|16|24|32|64)\\s*gb\\b", Pattern.CASE_INSENSITIVE);

    // Model Number Pattern: e.g. "SM-S928B", "A3106", "WH-1000XM5", "MU793HN/A"
    private static final Pattern MODEL_NUMBER_PATTERN = Pattern.compile(
            "\\b([A-Z]{1,3}-[A-Z0-9]{3,8}|[A-Z][0-9]{4}[A-Z]*/[A-Z]|WH-[0-9]{4}XM[0-9]|SM-[A-Z0-9]{4,6})\\b");

    @Override
    public ProductAttributesDto extractAttributes(String title, String brandFallback, String categoryFallback) {
        if (title == null || title.isBlank()) {
            return ProductAttributesDto.builder()
                    .brand(brandFallback != null ? brandFallback : "Generic")
                    .model("Unknown")
                    .canonicalKey("unknown")
                    .build();
        }

        String raw = title.trim();
        String lower = raw.toLowerCase(Locale.ROOT);

        // 1. Extract Brand
        String brand = extractBrand(lower, brandFallback);

        // 2. Extract Storage
        String storage = extractStorage(raw);

        // 3. Extract RAM
        String ram = extractRam(raw, storage);

        // 4. Extract Color
        String color = extractColor(raw);

        // 5. Extract Variant
        String variant = extractVariant(raw);

        // 6. Extract Model
        String model = extractModel(raw, brand);

        // 7. Extract Model Number
        String modelNumber = extractModelNumber(raw);

        // 8. Generate Canonical Key
        String canonicalKey = generateCanonicalKey(brand, model, variant, ram, storage, color);

        return ProductAttributesDto.builder()
                .brand(brand)
                .model(model)
                .variant(variant)
                .ram(ram)
                .storage(storage)
                .color(color)
                .modelNumber(modelNumber)
                .canonicalKey(canonicalKey)
                .build();
    }

    private String extractBrand(String lower, String fallback) {
        boolean isNoiseCancellingFeature = lower.contains("noise cancelling") || lower.contains("noise cancellation");

        for (Map.Entry<String, String> entry : BRAND_DICTIONARY.entrySet()) {
            if (entry.getKey().equals("noise") && isNoiseCancellingFeature) {
                continue;
            }
            Pattern p = Pattern.compile("(?i)\\b" + Pattern.quote(entry.getKey()) + "\\b");
            if (p.matcher(lower).find()) {
                return entry.getValue();
            }
        }
        if (fallback != null && !fallback.isBlank() && !"all".equalsIgnoreCase(fallback)) {
            return fallback.trim();
        }
        return "Generic";
    }

    private String extractStorage(String raw) {
        Matcher slashMatcher = STORAGE_SLASH_PATTERN.matcher(raw);
        if (slashMatcher.find()) {
            return slashMatcher.group(1).toUpperCase() + "GB";
        }

        Matcher explicitMatcher = STORAGE_EXPLICIT_PATTERN.matcher(raw);
        if (explicitMatcher.find()) {
            String gb = explicitMatcher.group(1);
            String tb = explicitMatcher.group(2);
            if (gb != null) return gb.toUpperCase() + "GB";
            if (tb != null) return tb.toUpperCase() + "TB";
        }
        return null;
    }

    private String extractRam(String raw, String storage) {
        Matcher slashMatcher = RAM_SLASH_PATTERN.matcher(raw);
        if (slashMatcher.find()) {
            return slashMatcher.group(1).toUpperCase() + "GB";
        }

        Matcher explicitMatcher = RAM_EXPLICIT_PATTERN.matcher(raw);
        while (explicitMatcher.find()) {
            String match = explicitMatcher.group(1) != null ? explicitMatcher.group(1) : explicitMatcher.group(2);
            if (match != null) {
                String candidate = match.toUpperCase() + "GB";
                // Ensure candidate RAM is not identical to the storage extracted
                if (storage == null || !candidate.equalsIgnoreCase(storage)) {
                    int val = Integer.parseInt(match);
                    if (val <= 64 && (storage == null || val < parseSize(storage))) {
                        return candidate;
                    }
                }
            }
        }
        return null;
    }

    private String extractColor(String raw) {
        for (String col : COLOR_LIST) {
            Pattern p = Pattern.compile("(?i)\\b" + Pattern.quote(col) + "\\b");
            if (p.matcher(raw).find()) {
                return col;
            }
        }
        return null;
    }

    private String extractVariant(String raw) {
        for (String v : VARIANT_LIST) {
            Pattern p = Pattern.compile("(?i)\\b" + Pattern.quote(v) + "\\b");
            if (p.matcher(raw).find()) {
                return v;
            }
        }
        return null;
    }

    private String extractModel(String raw, String brand) {
        String cleaned = raw.replaceAll("(?i)\\b(samsung|apple|sony|dell|asus|hp|lenovo|oneplus|xiaomi|realme|google|nothing)\\b", "")
                .replaceAll("(?i)\\b(gb|tb|ram|rom|5g|4g|lte|black|white|blue|silver|titanium|gray|grey)\\b", "")
                .replaceAll("(?i)\\b(with|for|and|in|at)\\b", "")
                .replaceAll("(?i)[(),/\\-]", " ")
                .replaceAll("\\s{2,}", " ")
                .trim();

        Pattern galaxyPattern = Pattern.compile("(?i)\\b(galaxy\\s+[A-Z0-9]+(?:\\s+Ultra|\\s+Pro|\\s+Plus|\\s+FE)?|s[0-9]{2}(?:\\s+ultra|\\s+plus|\\s+fe)?)\\b");
        Matcher gm = galaxyPattern.matcher(raw);
        if (gm.find()) {
            String m = gm.group(1).trim();
            if (m.toLowerCase().startsWith("s") && Character.isDigit(m.charAt(1))) {
                return "Galaxy " + m.toUpperCase();
            }
            return capitalizeWords(m);
        }

        Pattern iphonePattern = Pattern.compile("(?i)\\b(iphone\\s+[0-9]{1,2}(?:\\s+Pro\\s+Max|\\s+Pro|\\s+Plus|\\s+Mini)?)\\b");
        Matcher im = iphonePattern.matcher(raw);
        if (im.find()) {
            return capitalizeWords(im.group(1).trim());
        }

        Pattern macbookPattern = Pattern.compile("(?i)\\b(macbook\\s+(?:air|pro)(?:\\s+[0-9]{1,2}\\s*inch)?)\\b");
        Matcher mm = macbookPattern.matcher(raw);
        if (mm.find()) {
            return capitalizeWords(mm.group(1).trim());
        }

        Pattern sonyPattern = Pattern.compile("(?i)\\b(wh-[0-9]{4}xm[0-9]|wf-[0-9]{4}xm[0-9])\\b");
        Matcher sm = sonyPattern.matcher(raw);
        if (sm.find()) {
            return sm.group(1).toUpperCase();
        }

        String[] words = cleaned.split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < Math.min(3, words.length); i++) {
            if (!words[i].isBlank() && words[i].length() > 1) {
                sb.append(Character.toUpperCase(words[i].charAt(0)))
                  .append(words[i].substring(1).toLowerCase())
                  .append(" ");
            }
        }
        return sb.toString().trim().isEmpty() ? raw.split("\\s+")[0] : sb.toString().trim();
    }

    private String extractModelNumber(String raw) {
        Matcher m = MODEL_NUMBER_PATTERN.matcher(raw);
        if (m.find()) {
            return m.group(1).toUpperCase();
        }
        return null;
    }

    private String generateCanonicalKey(String brand, String model, String variant, String ram, String storage, String color) {
        StringBuilder key = new StringBuilder();
        key.append(slugify(brand)).append("_");
        key.append(slugify(model));
        if (variant != null) key.append("_").append(slugify(variant));
        if (ram != null) key.append("_").append(slugify(ram));
        if (storage != null) key.append("_").append(slugify(storage));
        if (color != null) key.append("_").append(slugify(color));
        return key.toString();
    }

    private String slugify(String str) {
        if (str == null) return "";
        return str.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "-").replaceAll("-{2,}", "-").replaceAll("^-|-$", "");
    }

    private int parseSize(String sizeStr) {
        try {
            if (sizeStr.toUpperCase().endsWith("TB")) {
                return Integer.parseInt(sizeStr.toUpperCase().replace("TB", "")) * 1024;
            }
            return Integer.parseInt(sizeStr.toUpperCase().replace("GB", ""));
        } catch (Exception e) {
            return 0;
        }
    }

    private String capitalizeWords(String str) {
        if (str == null || str.isBlank()) return str;
        String[] words = str.split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (String w : words) {
            if (!w.isBlank()) {
                sb.append(Character.toUpperCase(w.charAt(0)))
                  .append(w.substring(1).toLowerCase())
                  .append(" ");
            }
        }
        return sb.toString().trim();
    }
}
