package com.bexa.visuals.module.visuals;

import com.bexa.visuals.module.Category;
import com.bexa.visuals.module.Module;
import com.bexa.visuals.setting.ModeSetting;
import com.bexa.visuals.setting.NumberSetting;

/** Ekran proyeksiyasining aspekt nisbatini o'zgartiradi (GameRendererMixin orqali). */
public class AspectRatio extends Module {
    private static final String[] PRESETS = {"Custom", "4:3", "16:9", "21:9", "5:4", "Stretched 32:9"};
    public static volatile float target;

    private final ModeSetting preset = add(new ModeSetting("Preset", "Custom", PRESETS));
    private final NumberSetting custom = add(new NumberSetting("Ratio", 1.78, 0.5, 3.0, 0.01));

    public AspectRatio() {
        super("Aspect Ratio", "Overrides the rendering aspect ratio", Category.VISUALS);
        custom.visibleWhen(() -> preset.is("Custom"));
    }

    private float presetValue() {
        return switch (preset.get()) {
            case "4:3" -> 4f / 3f;
            case "16:9" -> 16f / 9f;
            case "21:9" -> 21f / 9f;
            case "5:4" -> 5f / 4f;
            case "Stretched 32:9" -> 32f / 9f;
            default -> custom.getF();
        };
    }

    @Override
    public void onTick() { target = presetValue(); }

    @Override
    protected void onEnable() { target = presetValue(); }

    @Override
    protected void onDisable() { target = 0f; }
}
