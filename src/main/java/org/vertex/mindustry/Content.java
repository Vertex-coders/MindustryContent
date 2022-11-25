package org.vertex.mindustry;

import arc.files.Fi;
import arc.files.ZipFi;
import arc.graphics.Pixmap;
import arc.graphics.Texture;
import arc.graphics.g2d.TextureAtlas;
import arc.struct.ObjectMap;
import arc.util.Http;
import arc.util.UnsafeRunnable;
import mindustry.core.ContentLoader;
import mindustry.core.GameState;
import mindustry.core.World;
import mindustry.type.Item;
import mindustry.world.Tile;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;

import static arc.graphics.g2d.TextureAtlas.TextureAtlasData;
import static arc.graphics.g2d.TextureAtlas.TextureAtlasData.AtlasPage;
import static arc.util.serialization.Jval.read;
import static mindustry.Vars.content;

public class Content {
    public static final Fi data = Fi.get(".mindustry");
    protected static final Fi resources = data.child("resources");
    protected static final Fi sprites = data.child("sprites");

    protected static final ObjectMap<String, BufferedImage> regions = new ObjectMap<>();
    protected static final ObjectMap<Item, Long> emojis = new ObjectMap<>();

    protected static BufferedImage currentImage;
    protected static Graphics2D currentGraphics;

    public static void init() {
        ContentLoader content = new ContentLoader();
        GameState state = new GameState();

        content.createBaseContent();

        loadIgnoreErrors(content::init);
        loadTextureData();
        loadIgnoreErrors(content::load);

        loadBlockColors();

        World world = new World() {
            public Tile tile(int x, int y) {
                return new Tile(x, y);
            }
        };

        boolean useLegacyLine = true;
        float scl = 1f / 4f;
    }

    private static void downloadResources(String fileName) {
        var file = resources.child(fileName);

        Http.get("https://api.github.com/repos/Anuken/Mindustry/releases/latest").timeout(0).block(release -> {
            var assets = read(release.getResultAsString()).get("assets").asArray();

            Http.get(assets.get(0).getString("browser_download_url")).timeout(0).block(response -> {
                file.writeBytes(response.getResult());

                new ZipFi(file).child("sprites").walk(fi -> {
                    if (fi.isDirectory()) fi.copyFilesTo(sprites);
                    else fi.copyTo(sprites);
                });
            });
        });
    }

    private static void loadTextureData() {
        var data = new TextureAtlasData(Content.data.child("sprites.aatls"), sprites, false);
        var images = new ObjectMap<AtlasPage, BufferedImage>();

        TextureAtlas atlas = new TextureAtlas();

        data.getPages().each(page -> loadIgnoreErrors(() -> {
            page.texture = Texture.createEmpty(null);
            images.put(page, ImageIO.read(page.textureFile.file()));
        }));

        data.getRegions().each(region -> atlas.addRegion(region.name, new ImageRegion(region, images.get(region.page))));

        atlas.setErrorRegion("error");
        SchematicBatch batch = new SchematicBatch();
    }

    private static void loadBlockColors() {
        var pixmap = new Pixmap(sprites.child("block_colors.png"));
        for (int i = 0; i < pixmap.width; i++) {
            var block = content.block(i);
            if (block.itemDrop != null) block.mapColor.set(block.itemDrop.color);
            else block.mapColor.rgba8888(pixmap.get(i, 0)).a(1f);
        }

        pixmap.dispose();
    }

    private static void loadIgnoreErrors(UnsafeRunnable runnable) {
        try {
            runnable.run();
        } catch (Throwable ignored) {
        }
    }
}