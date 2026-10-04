package dev.zeli.mallardguard.client;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.resources.ResourceLocation;

/** Reloaded coverage of the active shield textures. */
final class GuardShieldArtwork {
    private record Span(int x, int y, int end) {}
    private record Artwork(java.util.List<Span> purple, java.util.List<Span> red, java.util.List<Span> outline) {}
    private static volatile Artwork artwork = new Artwork(java.util.List.of(), java.util.List.of(), java.util.List.of());
    private GuardShieldArtwork(){}
    static void reload(ResourceManager resources){
        int[][] next={read(resources,"shield.png"),read(resources,"shield_red.png")};
        artwork = new Artwork(spans(next[0], false), spans(next[1], false), spans(next[0], true));
    }
    private static int[] read(ResourceManager resources,String name){
        int[] pixels=new int[16];
        ResourceLocation texture=ResourceLocation.fromNamespaceAndPath("mallardguard","textures/gui/"+name);
        try(var input=resources.open(texture);NativeImage image=NativeImage.read(input)){
            for(int y=0;y<image.getHeight();y++)for(int x=0;x<image.getWidth();x++)
                if((image.getPixelRGBA(x,y)>>>24)!=0)pixels[Math.min(15,y*16/image.getHeight())]|=1<<Math.min(15,x*16/image.getWidth());
        }catch(java.io.IOException error){org.slf4j.LoggerFactory.getLogger("Mallard Guard").warn("Could not read shield texture for reaction outlines: {}",name,error);}
        return pixels;
    }
    private static boolean pixel(int[] coverage, int x, int y) {
        return x>=0&&x<16&&y>=0&&y<16&&(coverage[y]&(1<<x))!=0;
    }
    private static java.util.List<Span> spans(int[] coverage, boolean outline) {
        java.util.List<Span> result = new java.util.ArrayList<>();
        int min = outline ? -1 : 0, max = outline ? 17 : 16;
        for (int y = min; y < max; y++) {
            int start = max;
            for (int x = min; x <= max; x++) {
                boolean covered = x < max && pixel(coverage, x, y);
                if (outline && x < max && !covered) {
                    for (int dy = -1; dy <= 1 && !covered; dy++)
                        for (int dx = -1; dx <= 1; dx++) covered |= pixel(coverage, x + dx, y + dy);
                } else if (outline) covered = false;
                if (covered && start == max) start = x;
                if (!covered && start != max) { result.add(new Span(start, y, x)); start = max; }
            }
        }
        return java.util.List.copyOf(result);
    }
    static void drawMask(net.minecraft.client.gui.GuiGraphics graphics, int color, boolean red) {
        for (Span span : red ? artwork.red() : artwork.purple())
            graphics.fill(span.x(), span.y(), span.end(), span.y() + 1, color);
    }
    static void drawOutline(net.minecraft.client.gui.GuiGraphics graphics, int color) {
        for (Span span : artwork.outline())
            graphics.fill(span.x(), span.y(), span.end(), span.y() + 1, color);
    }
}
