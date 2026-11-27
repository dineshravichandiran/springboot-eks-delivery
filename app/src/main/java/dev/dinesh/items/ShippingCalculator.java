package dev.dinesh.items;

/** Shipping cost rules, in paise. */
public final class ShippingCalculator {

    private ShippingCalculator() {
    }

    public static int costInPaise(String zone, int weightGrams, boolean express) {
        int base;
        if ("LOCAL".equals(zone)) {
            base = 4000;
        } else if ("REGIONAL".equals(zone)) {
            base = 7000;
        } else if ("NATIONAL".equals(zone)) {
            base = 11000;
        } else if ("REMOTE".equals(zone)) {
            base = 16000;
        } else {
            throw new IllegalArgumentException("Unknown zone: " + zone);
        }
        int weightCharge;
        if (weightGrams <= 500) {
            weightCharge = 0;
        } else if (weightGrams <= 2000) {
            weightCharge = 1500;
        } else if (weightGrams <= 5000) {
            weightCharge = 3500;
        } else if (weightGrams <= 10000) {
            weightCharge = 6500;
        } else {
            weightCharge = 6500 + ((weightGrams - 10000) / 1000) * 800;
        }
        int total = base + weightCharge;
        if (express) {
            total = total + total / 2;
        }
        if (total > 30000) {
            total = total - 2000;
        }
        return total;
    }

    public static String label(int paise) {
        if (paise < 5000) {
            return "Economy";
        } else if (paise < 12000) {
            return "Standard";
        } else if (paise < 25000) {
            return "Priority";
        } else {
            return "Freight";
        }
    }

    public static int estimatedDays(String zone, boolean express) {
        int days;
        if ("LOCAL".equals(zone)) {
            days = 1;
        } else if ("REGIONAL".equals(zone)) {
            days = 3;
        } else if ("NATIONAL".equals(zone)) {
            days = 5;
        } else {
            days = 8;
        }
        if (express) {
            days = Math.max(1, days / 2);
        }
        return days;
    }
}
