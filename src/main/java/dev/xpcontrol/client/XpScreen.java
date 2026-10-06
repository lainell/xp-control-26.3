package dev.xpcontrol.client;

import dev.xpcontrol.XpConfig;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Paged list of mobs. Each row has a text field:
 *   "2" or "x2"  -> double the XP,   "0" -> no XP,   "=10" -> exactly 10 XP.
 * Changes are applied immediately and written to config/xpcontrol.json when the menu closes.
 */
public class XpScreen extends Screen {
    private static final int ROW_H = 24;
    private static final int LIST_TOP = 78;

    private record Row(EntityType<?> type, String id, String name) {}

    private final List<Row> all = new ArrayList<>();
    private final List<Row> filtered = new ArrayList<>();
    private final List<AbstractWidget> rowWidgets = new ArrayList<>();

    private String searchText = "";
    private int page = 0;
    private int perPage = 1;

    private EditBox search;
    private StringWidget pageLabel;

    public XpScreen() {
        super(Component.translatable("xpcontrol.title"));
    }

    @Override
    protected void init() {
        all.clear();
        for (EntityType<?> type : BuiltInRegistries.ENTITY_TYPE) {
            if (type.getCategory() == MobCategory.MISC) continue; // players, items, projectiles, etc.
            String id = BuiltInRegistries.ENTITY_TYPE.getKey(type).toString();
            all.add(new Row(type, id, type.getDescription().getString()));
        }
        all.sort(Comparator.comparing(Row::name, String.CASE_INSENSITIVE_ORDER));

        int cx = this.width / 2;

        addRenderableWidget(new StringWidget(cx - 150, 10, 300, 12, this.title, this.font));

        // Default rule for all mobs
        addRenderableWidget(new StringWidget(cx - 150, 34, 170, 12,
                Component.translatable("xpcontrol.global"), this.font));
        EditBox global = new EditBox(this.font, cx + 30, 30, 120, 18, Component.empty());
        global.setHint(Component.literal("x1"));
        global.setValue(XpConfig.get(XpConfig.GLOBAL));
        global.setResponder(v -> XpConfig.set(XpConfig.GLOBAL, v));
        addRenderableWidget(global);

        // Search
        search = new EditBox(this.font, cx - 150, 54, 300, 18, Component.empty());
        search.setHint(Component.translatable("xpcontrol.search"));
        search.setValue(searchText);
        search.setResponder(v -> {
            searchText = v;
            page = 0;
            applyFilter();
            refreshRows();
        });
        addRenderableWidget(search);

        // Bottom bar
        int bottom = this.height - 26;
        addRenderableWidget(Button.builder(Component.literal("<"), b -> changePage(-1))
                .bounds(cx - 150, bottom, 40, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("xpcontrol.done"), b -> onClose())
                .bounds(cx - 50, bottom, 100, 20).build());
        addRenderableWidget(Button.builder(Component.literal(">"), b -> changePage(1))
                .bounds(cx + 110, bottom, 40, 20).build());

        pageLabel = new StringWidget(cx - 40, bottom - 14, 120, 12, Component.empty(), this.font);
        addRenderableWidget(pageLabel);

        perPage = Math.max(1, (this.height - LIST_TOP - 44) / ROW_H);

        applyFilter();
        refreshRows();
    }

    private void applyFilter() {
        filtered.clear();
        String q = searchText.toLowerCase(Locale.ROOT).trim();
        for (Row row : all) {
            if (q.isEmpty()
                    || row.name().toLowerCase(Locale.ROOT).contains(q)
                    || row.id().toLowerCase(Locale.ROOT).contains(q)) {
                filtered.add(row);
            }
        }
    }

    private int pageCount() {
        return Math.max(1, (filtered.size() + perPage - 1) / perPage);
    }

    private void changePage(int delta) {
        page = Math.max(0, Math.min(pageCount() - 1, page + delta));
        refreshRows();
    }

    private void refreshRows() {
        for (AbstractWidget w : rowWidgets) {
            removeWidget(w);
        }
        rowWidgets.clear();

        page = Math.max(0, Math.min(pageCount() - 1, page));
        int cx = this.width / 2;
        int start = page * perPage;
        int end = Math.min(filtered.size(), start + perPage);

        for (int i = start; i < end; i++) {
            Row row = filtered.get(i);
            int y = LIST_TOP + (i - start) * ROW_H;

            StringWidget label = new StringWidget(cx - 150, y + 5, 175, 12, Component.literal(row.name()), this.font);
            EditBox box = new EditBox(this.font, cx + 30, y, 120, 18, Component.literal(row.name()));
            box.setHint(Component.literal("x1"));
            box.setValue(XpConfig.get(row.id()));
            box.setResponder(v -> XpConfig.set(row.id(), v));

            rowWidgets.add(addRenderableWidget(label));
            rowWidgets.add(addRenderableWidget(box));
        }

        pageLabel.setMessage(Component.translatable("xpcontrol.page", page + 1, pageCount()));
    }

    @Override
    public void onClose() {
        XpConfig.save();
        super.onClose();
    }
}
