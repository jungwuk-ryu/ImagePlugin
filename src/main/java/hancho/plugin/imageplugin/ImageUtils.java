package hancho.plugin.imageplugin;

import cn.nukkit.item.ItemMap;
import cn.nukkit.nbt.tag.CompoundTag;

import javax.annotation.Nullable;
import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;

public class ImageUtils {
    public static ItemMap getItemMapWithImage(String path, @Nullable Map<String, String> data) throws IOException {
        return getItemMapWithImage(new File(path), data);
    }

    public static ItemMap getItemMapWithImage(File file, @Nullable Map<String, String> data) throws IOException {
        return getItemMapWithImage(ImageIO.read(file), data);
    }

    public static ItemMap getItemMapWithImage(BufferedImage image, @Nullable Map<String, String> data) {
        ItemMap itemMap = new ItemMap();
        itemMap.setImage(image);
        itemMap.setCount(1);

        if(data != null) {
            CompoundTag tag = itemMap.getNamedTag();

            data.forEach(tag::putString);
            itemMap.setNamedTag(tag);
        }

        return itemMap;
    }

    public static ArrayList<ArrayList<BufferedImage>> getSplitImage(int lx, int ly, BufferedImage image){
        ArrayList<ArrayList<BufferedImage>> list = new ArrayList<>();
        int originDx2 = image.getWidth();
        int originDy2 = image.getHeight();

        final int dx2 = originDx2 / lx;
        final int dy2 = originDy2 / ly;
        int sx1, sy1, sx2, sy2;

        for(int x = 0; x < lx; x++) {
            ArrayList<BufferedImage> listY = new ArrayList<>();
            sx1 = x * dx2;
            sx2 = sx1 + dx2;
            for(int y = 0; y < ly; y++) {
                sy1 = y * dy2;
                sy2 = sy1 + dy2;

                BufferedImage frame = new BufferedImage(dx2, dy2, image.getType());
                Graphics2D graphic = frame.createGraphics();
                //System.out.println("0, 0, " + dx2 + ", " +dy2 + " | " + sx1 + ", " + sy1 + ", " + sx2 + ", " + sy2);
                graphic.drawImage(image, 0, 0, dx2, dy2, sx1, sy1, sx2, sy2, null);
                graphic.dispose();

                listY.add(frame);
            }
            list.add(listY);
        }
        return list;
    }

    public static ArrayList<ArrayList<ItemMap>> getSplitImageMap(int lx, int ly, File file) throws IOException {
        return getSplitImageMap(lx, ly, ImageIO.read(file));
    }

    public static ArrayList<ArrayList<ItemMap>> getSplitImageMap(int lx, int ly, BufferedImage image){
        return getSplitImageMap(lx, ly, image);
    }

    public static ArrayList<ArrayList<ItemMap>> getSplitImageMap(int lx, int ly, BufferedImage image, @Nullable Map<String, String> data){
        ArrayList<ArrayList<BufferedImage>> splitImageList = getSplitImage(lx, ly, image);
        ArrayList<ArrayList<ItemMap>> result = new ArrayList<>();

        for (ArrayList<BufferedImage> listY : splitImageList) {
            ArrayList<ItemMap> resultListY = new ArrayList<>();

            for (BufferedImage splitImage : listY) {
                resultListY.add(getItemMapWithImage(splitImage, data));
            }
            result.add(resultListY);
        }

        return result;
    }

}
