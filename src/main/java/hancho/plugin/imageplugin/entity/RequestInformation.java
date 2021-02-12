package hancho.plugin.imageplugin.entity;

import cn.nukkit.level.Location;
import cn.nukkit.math.BlockFace;

import java.util.HashMap;
import java.util.Map;

public class RequestInformation {
    public Location pos1, pos2;
    public BlockFace face;
    public String filePath;
    public Map<String, String> data = new HashMap<>();

    public void putData(String key, String data){
        this.data.put(key, data);
    }

    public void putData(String key, int data){
        this.data.put(key, Integer.toString(data));
    }
}
