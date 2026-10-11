package local.badlioncosmeticport;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import java.util.*;
import local.badlioncosmeticport.MeshCosmetics.*;

/** Visual cosmetic browser; hovering previews without changing saved equipment. */
public final class MeshSelectionScreen extends Screen {
    private final Screen parent;
    private final List<Card> cards = new ArrayList<>();
    private final ThumbnailCache thumbnails = new ThumbnailCache();
    private final EnumMap<Category, ButtonWidget> categories = new EnumMap<>(Category.class);
    private TextFieldWidget search;
    private Category category = Category.CAPE;
    private boolean equippedOnly;
    private int page, pages = 1, resultCount, gridX, gridY, gridWidth, gridBottom, panelX, panelWidth;
    private Pack focusedPack, previewPack;
    private Loaded preview;
    private String status = "";
    private float previewYaw = 25;
    private ButtonWidget pose, hatDown, hatUp, previous, next, filter, all, shieldVisibility;
    public MeshSelectionScreen(Screen parent) {
        super(Text.literal("Cosmetic Port " + MeshCosmetics.VERSION)); this.parent = parent;
    }
    private static String label(Category type) {
        return switch (type) {
            case CAPE -> "Cloaks"; case HAT -> "Hats"; case BANDANA -> "Bandanas";
            case SHIELD -> "Shields"; case PET -> "Pets"; case WINGS -> "Wings"; case SWORD -> "Swords"; case BOW -> "Bows";
        };
    }
    @Override protected void init() {
        int side = width < 600 ? 80 : 104;
        panelWidth = width < 600 ? 112 : 170;
        panelX = width - panelWidth - 10;
        gridX = side + 16; gridY = 63; gridWidth = panelX - gridX - 10; gridBottom = height - 38;
        addDrawableChild(ButtonWidget.builder(Text.literal("↶"), b -> previewYaw -= 45).dimensions(panelX, 36, 25, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("↷"), b -> previewYaw += 45).dimensions(panelX + panelWidth - 25, 36, 25, 20).build());
        all = addDrawableChild(ButtonWidget.builder(Text.literal("All cosmetics"), b -> changeCategory(null)).dimensions(10, 49, side, 20).build());
        for (Category type : Category.values()) categories.put(type, addDrawableChild(ButtonWidget.builder(Text.literal(label(type)),
                b -> changeCategory(type)).dimensions(10, 74 + type.ordinal() * Math.min(24, Math.max(18, (height - 90) / Category.values().length)), side, 20).build()));
        search = new TextFieldWidget(textRenderer, gridX, 36, Math.max(40, gridWidth - 73), 20, Text.literal("Search cosmetics"));
        search.setPlaceholder(Text.literal("Search name or ID"));
        search.setChangedListener(value -> { page = 0; refresh(); }); addDrawableChild(search);
        filter = addDrawableChild(ButtonWidget.builder(Text.literal("All items"), b -> {
            equippedOnly = !equippedOnly; page = 0; refresh();
        }).dimensions(gridX + gridWidth - 68, 36, 68, 20).build());
        previous = addDrawableChild(ButtonWidget.builder(Text.literal("<"), b -> { page = Math.max(0, page - 1); refresh(); })
                .dimensions(gridX, height - 29, 28, 20).build());
        next = addDrawableChild(ButtonWidget.builder(Text.literal(">"), b -> { page = Math.min(pages - 1, page + 1); refresh(); })
                .dimensions(gridX + gridWidth - 28, height - 29, 28, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Done"), b -> close()).dimensions(panelX, height - 29, panelWidth, 20).build());
        pose = addDrawableChild(ButtonWidget.builder(Text.empty(), b -> {
            if (category == Category.CAPE) MeshCosmetics.capeMotion = CapeMotion.values()[(MeshCosmetics.capeMotion.ordinal() + 1) % 3];
            if (category == Category.PET) MeshCosmetics.petPose = switch (MeshCosmetics.petPose) { case "shoulder" -> "head"; case "head" -> "air"; default -> "shoulder"; };
            if (category == Category.SHIELD) MeshCosmetics.shieldOnArm = !MeshCosmetics.shieldOnArm;
            MeshCosmetics.save(); refresh();
        }).dimensions(panelX, height - 78, panelWidth, 20).build());
        shieldVisibility = addDrawableChild(ButtonWidget.builder(Text.empty(), b -> {
            MeshCosmetics.shieldWithoutItem = !MeshCosmetics.shieldWithoutItem;
            MeshCosmetics.save(); refresh();
        }).dimensions(panelX, height - 102, panelWidth, 20).build());
        hatDown = addDrawableChild(ButtonWidget.builder(Text.literal("−"), b -> adjustHat(-1)).dimensions(panelX, height - 78, 25, 20).build());
        hatUp = addDrawableChild(ButtonWidget.builder(Text.literal("+"), b -> adjustHat(1)).dimensions(panelX + panelWidth - 25, height - 78, 25, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Unequip"), b -> {
            Category type = focusedPack == null ? category : focusedPack.type;
            if (type != null) MeshCosmetics.selectId(type, -1, true);
            focusedPack = null; previewPack = null; refresh();
        }).dimensions(panelX, height - 54, panelWidth, 20).build());
        refresh();
    }
    private void changeCategory(Category value) {
        category = value; page = 0; focusedPack = null; status = "";
        previewYaw = value == null || value == Category.CAPE || value == Category.WINGS ? 25 : 180;
        search.setText(""); refresh();
    }
    private void adjustHat(int amount) {
        MeshCosmetics.hatHeight = Math.max(-8, Math.min(8, MeshCosmetics.hatHeight + amount));
        MeshCosmetics.save(); refresh();
    }
    private void refresh() {
        if (search == null) return;
        for (Card card : cards) remove(card); cards.clear();
        String query = search.getText().toLowerCase(Locale.ROOT);
        List<Pack> matching = Arrays.stream(Category.values()).filter(type -> category == null || type == category)
                .flatMap(type -> MeshCosmetics.packs(type).stream())
                .filter(pack -> !equippedOnly || MeshCosmetics.selected(pack))
                .filter(pack -> (pack.name + " " + pack.id).toLowerCase(Locale.ROOT).contains(query)).toList();
        int columns = Math.max(1, gridWidth / 82), cardWidth = gridWidth / columns;
        int rows = Math.max(1, (gridBottom - gridY) / 82), cardHeight = (gridBottom - gridY) / rows, size = columns * rows;
        resultCount = matching.size(); pages = Math.max(1, (resultCount + size - 1) / size); page = Math.min(page, pages - 1);
        for (int i = page * size; i < Math.min(resultCount, (page + 1) * size); i++) {
            int cell = i % size;
            Card card = new Card(matching.get(i), gridX + cell % columns * cardWidth, gridY + cell / columns * cardHeight, cardWidth - 5, cardHeight - 5);
            cards.add(card); addDrawableChild(card);
        }
        previous.active = page > 0; next.active = page < pages - 1;
        filter.setMessage(Text.literal(equippedOnly ? "Equipped" : "All items"));
        all.active = category != null;
        for (var entry : categories.entrySet()) entry.getValue().active = category != entry.getKey();
        pose.visible = category == Category.CAPE || category == Category.PET || category == Category.SHIELD || category == Category.HAT;
        pose.active = category != Category.HAT;
        hatDown.visible = hatUp.visible = category == Category.HAT;
        shieldVisibility.visible = category == Category.SHIELD;
        shieldVisibility.setMessage(Text.literal("Without vanilla shield: " + (MeshCosmetics.shieldWithoutItem ? "show" : "hide")));
        pose.setX(panelX + (category == Category.HAT ? 28 : 0)); pose.setWidth(panelWidth - (category == Category.HAT ? 56 : 0));
        pose.setMessage(Text.literal(category == null ? "" : switch (category) {
            case CAPE -> "Motion: " + MeshCosmetics.capeMotion; case PET -> "Pose: " + MeshCosmetics.petPose;
            case SHIELD -> MeshCosmetics.shieldOnArm ? "Shield: arm / item" : "Shield: back";
            case HAT -> MeshCosmetics.hatHeight + " px"; default -> "";
        }));
    }
    private void preview(Pack pack) {
        if (pack == previewPack) return;
        if (preview != null) preview.close();
        preview = null; previewPack = pack;
        if (pack != null && !MeshCosmetics.selected(pack)) {
            try { preview = MeshCosmetics.loadPreview(pack); }
            catch (Exception failure) { status = "Preview unavailable"; }
        }
    }
    @Override public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        Pack hovered = cards.stream().filter(card -> card.isMouseOver(mouseX, mouseY)).map(card -> card.pack).findFirst().orElse(null);
        Pack candidate = hovered != null ? hovered : focusedPack;
        if (candidate == null && category != null && MeshCosmetics.active(category) != null) candidate = MeshCosmetics.active(category).pack;
        preview(candidate);
        super.render(context, mouseX, mouseY, delta);
        context.drawTextWithShadow(textRenderer, title, 10, 12, 0xFFFFFF);
        context.drawTextWithShadow(textRenderer, Text.literal("YOUR COSMETICS"), gridX, 25, 0x36C9F5);
        if (client != null && client.player != null) MeshCosmetics.withPreview(preview, () -> drawPreview(context));
        if (candidate != null) context.drawTextWrapped(textRenderer, Text.literal(candidate.name), panelX + 4, height - (category == Category.SHIELD ? 137 : 111), panelWidth - 8, 0xFFFFFF);
        context.drawCenteredTextWithShadow(textRenderer, Text.literal((page + 1) + " / " + pages + " · " + resultCount),
                gridX + gridWidth / 2, height - 23, 0xFFFFFF);
        if (resultCount == 0) context.drawCenteredTextWithShadow(textRenderer, Text.literal("No matching cosmetics"), gridX + gridWidth / 2, gridY + 30, 0xFFFFFF);
        if (!status.isEmpty()) context.drawTextWithShadow(textRenderer, Text.literal(textRenderer.trimToWidth(status, gridWidth)), gridX, height - 39, 0xEEEEAA);
        if (hovered != null) context.drawTooltip(textRenderer, List.of(Text.literal(hovered.name),
                Text.literal(label(hovered.type) + " · " + hovered.id), Text.literal("Click to equip")), mouseX, mouseY);
    }
    @Override public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
        context.fill(0, 0, width, height, 0xF0101722);
        context.fill(panelX, 34, panelX + panelWidth, height - 86, 0xFF182433);
    }
    private void drawPreview(DrawContext context) {
        if(category==Category.SWORD||category==Category.BOW) {
            var weapon=MeshCosmetics.active(category);
            if(weapon!=null)try {
                var texture=weapon.weaponTexture(category==Category.SWORD?"diamond-gui":"bow-gui");
                int size=Math.min(panelWidth-12,Math.max(40,height-190));
                context.drawTexture(texture,panelX+(panelWidth-size)/2,70,0,0,size,size,size,size);
            }catch(Exception failure){status="Weapon preview unavailable";}
            return;
        }
        var player = client.player;
        float body = player.bodyYaw, yaw = player.getYaw(), pitch = player.getPitch();
        float head = player.headYaw, previousHead = player.prevHeadYaw;
        int previewBottom = height - (category == Category.SHIELD ? 141 : 115);
        context.enableScissor(panelX, 60, panelX + panelWidth, previewBottom);
        try {
            player.bodyYaw = previewYaw; player.setYaw(previewYaw); player.setPitch(0);
            player.headYaw = player.prevHeadYaw = previewYaw;
            float size = Math.max(22, Math.min((previewBottom - 60) / 2f, panelWidth * .48f));
            InventoryScreen.drawEntity(context, panelX + panelWidth / 2f, (60 + previewBottom) / 2f, size,
                    new org.joml.Vector3f(0, player.getHeight() / 2 + .0625f, 0),
                    new org.joml.Quaternionf().rotateZ((float)Math.PI).rotateX(.10f),
                    new org.joml.Quaternionf().rotateX(.10f), player);
        } finally {
            player.bodyYaw = body; player.setYaw(yaw); player.setPitch(pitch);
            player.headYaw = head; player.prevHeadYaw = previousHead; context.disableScissor();
        }
    }
    @Override public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (button == 0 && mouseX >= panelX && mouseX <= panelX + panelWidth && mouseY >= 60 && mouseY < height - 115) {
            previewYaw += (float)deltaX; return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }
    @Override public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
        if (mouseX >= gridX && mouseX < panelX && mouseY >= gridY && mouseY <= gridBottom && vertical != 0) {
            page = Math.max(0, Math.min(pages - 1, page + (vertical < 0 ? 1 : -1))); refresh(); return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontal, vertical);
    }
    @Override public boolean shouldPause() { return false; }
    @Override public void removed() {
        if (preview != null) preview.close(); preview = null; previewPack = null; thumbnails.close();
    }
    @Override public void close() { if (client != null) client.setScreen(parent); }
    private final class Card extends ButtonWidget {
        private final Pack pack;
        Card(Pack pack, int x, int y, int w, int h) {
            super(x, y, w, h, Text.literal(pack.name), b -> {
                focusedPack = pack;
                status = MeshCosmetics.selectId(pack.type, pack.id, true) ? "Equipped " + pack.name : "Could not load this cosmetic";
                previewPack = null; refresh();
            }, DEFAULT_NARRATION_SUPPLIER);
            this.pack = pack;
        }
        @Override protected void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
            boolean selected = MeshCosmetics.selected(pack);
            context.fill(getX(), getY(), getX() + getWidth(), getY() + getHeight(), isHovered() ? 0xEE25465B : 0xD51B2B3B);
            context.drawBorder(getX(), getY(), getWidth(), getHeight(), selected ? 0xFF36C9F5 : isFocused() ? 0xFFFFFFFF : 0xFF445469);
            var thumbnail = thumbnails.get(pack);
            int size = Math.max(20, Math.min(getWidth() - 12, getHeight() - 28));
            if (thumbnail != null) context.drawTexture(thumbnail, getX() + (getWidth() - size) / 2, getY() + 3, size, size, 0, 0, 128, 128, 128, 128);
            else context.drawCenteredTextWithShadow(textRenderer, Text.literal("?"), getX() + getWidth() / 2, getY() + size / 2, 0xFFFFFF);
            context.drawCenteredTextWithShadow(textRenderer, Text.literal(textRenderer.trimToWidth(pack.name, getWidth() - 10)), getX() + getWidth() / 2,
                    getY() + getHeight() - 16, 0xFFFFFF);
            if (selected) context.drawTextWithShadow(textRenderer, Text.literal("✓"), getX() + getWidth() - 12, getY() + 4, 0x36C9F5);
        }
    }
}
