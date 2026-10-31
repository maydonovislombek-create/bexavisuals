package com.bexa.visuals.setting;

import com.google.gson.JsonElement;

import java.util.function.BooleanSupplier;

/** Barcha sozlamalar uchun asosiy sinf. */
public abstract class Setting {
    public final String name;
    private BooleanSupplier visibility = () -> true;

    protected Setting(String name) {
        this.name = name;
    }

    /** Sozlama faqat shart bajarilganda ko'rinadi (masalan, rejimga qarab). */
    @SuppressWarnings("unchecked")
    public <T extends Setting> T visibleWhen(BooleanSupplier supplier) {
        this.visibility = supplier;
        return (T) this;
    }

    public boolean isVisible() {
        return visibility.getAsBoolean();
    }

    public abstract JsonElement serialize();

    public abstract void deserialize(JsonElement element);
}
