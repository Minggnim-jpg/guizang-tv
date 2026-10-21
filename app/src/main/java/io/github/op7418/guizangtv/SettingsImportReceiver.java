package io.github.op7418.guizangtv;

import android.content.*;
import org.json.*;
import java.util.*;

/** Explicit ADB import, guarded by android.permission.DUMP in the manifest.
 * Imports appearance only into a fresh fork install; never imports startup grants or settings. */
public final class SettingsImportReceiver extends BroadcastReceiver {
    private static final Set<String> KEYS = new HashSet<>(Arrays.asList(
            "layout", "focus", "dim", "apps", "background", "controls", "font", "size", "material"));
    @Override public void onReceive(Context c, Intent intent) {
        try {
            if (c.getSharedPreferences("migration", 0).getBoolean("imported", false))
                throw new IllegalArgumentException("Already imported; refusing to replace settings");
            if (!c.getSharedPreferences("home-appearance", 0).getAll().isEmpty()
                    || !c.getSharedPreferences("home-items", 0).getAll().isEmpty()
                    || !c.getSharedPreferences("frame-wallpapers", 0).getAll().isEmpty())
                throw new IllegalArgumentException("Import before first opening the new app");
            String raw = intent.getStringExtra("json");
            if (raw == null || raw.length() > 16_384) throw new IllegalArgumentException("Invalid size");
            JSONObject data = new JSONObject(raw);
            if (data.getInt("schema") != 1) throw new IllegalArgumentException("Unknown schema");
            JSONObject appearance = data.getJSONObject("appearance");
            Map<String, Integer> validated = new HashMap<>();
            Iterator<String> keys = appearance.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                int value = appearance.getInt(key);
                if (!KEYS.contains(key) || AppearanceValues.normalize(key, value) != value)
                    throw new IllegalArgumentException("Invalid appearance");
                validated.put(key, value);
            }
            JSONArray order = data.getJSONArray("items");
            Set<String> seen = new HashSet<>();
            if (order.length() < 1 || order.length() > 9) throw new IllegalArgumentException("Invalid item count");
            for (int i = 0; i < order.length(); i++) {
                String id = order.getString(i);
                if (!HomeItems.validId(id) || !seen.add(id)) throw new IllegalArgumentException("Invalid item");
            }
            JSONObject wallpaper = data.getJSONObject("wallpaper");
            String selected = wallpaper.getString("selected");
            java.io.ByteArrayOutputStream bytes = new java.io.ByteArrayOutputStream();
            try (java.io.InputStream stream = c.getAssets().open("wallpapers/catalog.json")) {
                byte[] buffer = new byte[4096]; int n;
                while ((n = stream.read(buffer)) != -1) bytes.write(buffer, 0, n);
            }
            JSONArray catalog = new JSONArray(bytes.toString("UTF-8"));
            boolean found = false;
            for (int i = 0; i < catalog.length(); i++) if (selected.equals(catalog.getJSONObject(i).getString("id"))) found = true;
            if (!found) throw new IllegalArgumentException("Wallpaper not bundled");
            int minutes = wallpaper.getInt("minutes");
            if (WallpaperCycle.minutes(minutes) != minutes) throw new IllegalArgumentException("Invalid interval");
            boolean rotating = wallpaper.getBoolean("rotating");
            // Validate all inputs before writing any preference file.
            SharedPreferences.Editor editor = c.getSharedPreferences("home-appearance", 0).edit();
            for (Map.Entry<String, Integer> entry : validated.entrySet()) editor.putInt(entry.getKey(), entry.getValue());
            if (!editor.commit()
                    || !c.getSharedPreferences("home-items", 0).edit().putString("order", order.toString()).commit()
                    || !c.getSharedPreferences("frame-wallpapers", 0).edit().putString("selected", selected)
                        .putBoolean("rotating", rotating).putInt("minutes", minutes).commit())
                throw new IllegalStateException("Write failed; original app is untouched");
            c.getSharedPreferences("migration", 0).edit().putBoolean("imported", true).commit();
            setResultCode(-1); setResultData("Imported appearance, home items and wallpaper; startup remains off");
        } catch (Exception error) {
            setResultCode(0); setResultData("Import refused: " + error.getMessage());
        }
    }
}
