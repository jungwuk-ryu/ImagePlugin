package hancho.plugin.imageplugin;

import cn.nukkit.Player;
import cn.nukkit.Server;
import cn.nukkit.block.*;
import cn.nukkit.blockentity.BlockEntity;
import cn.nukkit.blockentity.BlockEntityItemFrame;
import cn.nukkit.command.Command;
import cn.nukkit.command.CommandSender;
import cn.nukkit.event.EventHandler;
import cn.nukkit.event.Listener;
import cn.nukkit.event.block.BlockBreakEvent;
import cn.nukkit.event.block.ItemFrameDropItemEvent;
import cn.nukkit.event.player.PlayerInteractEvent;
import cn.nukkit.item.Item;
import cn.nukkit.item.ItemID;
import cn.nukkit.item.ItemItemFrame;
import cn.nukkit.item.ItemMap;
import cn.nukkit.level.Level;
import cn.nukkit.math.Vector3;
import cn.nukkit.nbt.tag.CompoundTag;
import cn.nukkit.nbt.tag.Tag;
import cn.nukkit.plugin.PluginBase;
import hancho.plugin.imageplugin.entity.RequestInformation;

import javax.imageio.ImageIO;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

public class ImagePlugin extends PluginBase implements Listener {
    public static final String PREFIX = "§7§o[ ! ] ";

    public HashMap<String, RequestInformation> placeQueue = new HashMap<>();
    public HashSet<String> removeQueue = new HashSet<>();

    @Override
    public void onEnable() {
        this.getServer().getPluginManager().registerEvents(this, this);
        File file = new File(this.getDataFolder().getAbsolutePath() + "/img");
        if(!file.exists()) file.mkdirs();
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent ev){
        if(ev.getAction() != PlayerInteractEvent.Action.RIGHT_CLICK_BLOCK) return;
        Player player = ev.getPlayer();

        if(ev.getBlock().getId() == BlockID.ITEM_FRAME_BLOCK){
            Block block = ev.getBlock();
            BlockEntityItemFrame frame = (BlockEntityItemFrame) block.getLevel().getBlockEntity(block.getLocation());
            CompoundTag tag = frame.getItem().getNamedTag();

            if(tag != null && tag.exist("ip_sx")){
                ev.setCancelled();
            }
        }

        if(placeQueue.containsKey(player.getName())){
            RequestInformation info = placeQueue.get(player.getName());

            ev.setCancelled();

            if(ev.getFace().getIndex() < 2) {
                player.sendMessage(PREFIX + "현재 옆을 바라보고 있는 아이템 액자만 지원합니다.");
                return;
            }

            if(info.pos1 == null){
                info.pos1 = ev.getBlock().getLocation();
                player.sendMessage(PREFIX + "두번째 지점을 선택하여주세요.");
            }else{
                info.pos2 = ev.getBlock().getLocation();
                info.face = ev.getFace();

                if((info.pos1.x != info.pos2.x) && (info.pos1.z != info.pos2.z)){
                    player.sendMessage(PREFIX + "대각선 방향은 지원하지 않습니다.");
                    player.sendMessage(String.format(PREFIX + "pos1 (§c%d§7, %d, §c%d§7), pos2 (§c%d§7, %d, §c%d§7)",
                            (int) info.pos1.x, (int) info.pos1.y, (int) info.pos1.z,
                            (int) info.pos2.x, (int) info.pos2.y, (int) info.pos2.z));
                    return;
                }

                player.sendMessage(PREFIX + "작업 시작 중 . . .");

                Thread thread = new Thread(() -> {
                    try{
                        placeImage(info);
                    }catch (FileNotFoundException e) {
                        player.sendMessage(PREFIX + "§c파일을 찾을 수 없습니다!");
                    } catch (IOException e) {
                        Server.getInstance().getLogger().error("이미지 처리 중 오류 발생", e);
                        player.sendMessage(PREFIX + "이미지를 처리할 수 없습니다. 손상된 이미지일 수 있습니다.");
                        return;
                    }
                    player.sendMessage(PREFIX + "작업이 완료되었습니다.");
                });

                this.placeQueue.remove(player.getName());
                thread.start();
            }
        }else if(removeQueue.contains(player.getName())){
            Block block = ev.getBlock();
            BlockEntityItemFrame entityItemFrame;
            Item item;

            if(block.getId() != BlockID.ITEM_FRAME_BLOCK) return;
            ev.setCancelled();

            entityItemFrame = (BlockEntityItemFrame) block.getLevel().getBlockEntity(block.getLocation());
            item = entityItemFrame.getItem();
            if(item.getNamedTag().contains("ip_sx")){
                try {
                    breakImage(entityItemFrame);
                    this.removeQueue.remove(player.getName());
                    player.sendMessage(PREFIX + "제거되었습니다.");
                } catch (IllegalArgumentException e) {
                    this.getServer().getLogger().error("", e);
                }
            }
        }
    }

    @EventHandler
    public void onItemFrameDropItem(ItemFrameDropItemEvent ev){
        Item item = ev.getItem();
        CompoundTag tag = item.getNamedTag();
        if(tag.contains("ip_sx") && ! ev.getPlayer().getName().equals(tag.getString("img_owner"))) ev.setCancelled();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // command : /imageplugin, ip
        if(args.length == 0){
            sender.sendMessage(PREFIX + "/ip place <img name>" +
                    "\n" + PREFIX + "/ip remove");
            return true;
        }
        if(args[0].equals("place")){
            if(args.length < 2){
                sender.sendMessage(PREFIX + "사용법 : /ip place <img name>");
            }else{
                String path = this.getDataFolder().getAbsolutePath() + "/img/" + args[1];

                RequestInformation info = new RequestInformation();
                info.filePath = path;

                this.placeQueue.put(sender.getName(), info);
                sender.sendMessage(PREFIX + "첫번째 지점을 터치하여주세요.");
            }
        }else if(args[0].equals("remove")){
            this.removeQueue.add(sender.getName());
            sender.sendMessage(PREFIX + "제거할 이미지를 터치하세요.");
        }
        return true;
    }

    public static void breakImage(BlockEntityItemFrame blockEntity) throws IllegalArgumentException {
        Item item = blockEntity.getItem();
        if(item.getId() != ItemID.MAP
                || item.getNamedTag() == null
                || !item.getNamedTag().contains("ip_sx")
        ) throw new IllegalArgumentException();

        CompoundTag tag = item.getNamedTag();
        final int startX = Integer.parseInt(tag.getString("ip_sx"));
        final int startY = Integer.parseInt(tag.getString("ip_sy"));
        final int startZ = Integer.parseInt(tag.getString("ip_sz"));
        final int endX = Integer.parseInt(tag.getString("ip_ex"));
        final int endY = Integer.parseInt(tag.getString("ip_ey"));
        final int endZ = Integer.parseInt(tag.getString("ip_ez"));
        final Level level = blockEntity.level;
        final BlockAir blockAir = new BlockAir();

        for (int x = startX; x >= endX; x--) {
            for (int z = startZ; z >= endZ; z--) {
                for (int y = startY; y >= endY; y--) {
                    level.setBlock(new Vector3(x, y, z), blockAir, false, false);
                }
            }
        }
    }

    public static void placeImage(RequestInformation info) throws IOException, FileNotFoundException {
        final Level level = info.pos1.getLevel();
        final ItemItemFrame itemItemFrame = new ItemItemFrame();
        final BlockItemFrame blockItemFrame = new BlockItemFrame();
        final BlockStone blockStone = new BlockStone();
        final int[] FACING = new int[]{4, 5, 3, 2, 1, 0};   // TODO CHECK UPDATE (BlockItemFrame.FANCING)
        final int horizontalFaceIndex = info.face.getHorizontalIndex();

        blockItemFrame.level = level;
        blockStone.level = level;

        // start >= end
        int startX = (int) Math.max(info.pos1.x, info.pos2.x);
        int startY = (int) Math.max(info.pos1.y, info.pos2.y);
        int startZ = (int) Math.max(info.pos1.z, info.pos2.z);
        int endX = (int) Math.min(info.pos1.x, info.pos2.x);
        int endY = (int) Math.min(info.pos1.y, info.pos2.y);
        int endZ = (int) Math.min(info.pos1.z, info.pos2.z);
        info.putData("ip_sx", startX);
        info.putData("ip_sy", startY);
        info.putData("ip_sz", startZ);
        info.putData("ip_ex", endX);
        info.putData("ip_ey", endY);
        info.putData("ip_ez", endZ);
        final boolean mode = startX == endX; // true == z is diff

        // Image Processing
        File imageFile = new File(info.filePath);
        ArrayList<ArrayList<ItemMap>> itemMapList;
        if(!imageFile.exists()) {
            throw new FileNotFoundException(info.filePath);
        }

        int diffX =
                mode ? (startZ - endZ + 1) : (startX - endX + 1);
        itemMapList = ImageUtils.getSplitImageMap(diffX, startY - endY + 1, ImageIO.read(imageFile), info.data);

        for (int x = startX; x >= endX; x--){
            for(int z = startZ; z >= endZ; z--){
                ArrayList<ItemMap> currentList;
                if(horizontalFaceIndex < 2){
                    currentList =
                            mode ? itemMapList.get(z - endZ) : itemMapList.get(x - endX);
                } else {
                    currentList =
                            mode ? itemMapList.get(startZ - z) : itemMapList.get(startX - x);
                }

                for(int y = startY; y >= endY; y --){
                    BlockAir blockAir = new BlockAir();
                    blockAir.level = level;
                    blockAir.x = x;
                    blockAir.y = y;
                    blockAir.z = z;


                    //blockItemFrame.place(itemItemFrame, blockAir, blockStone, info.face, blockAir.x, blockAir.y, blockAir.z, null);
                    //BlockEntityItemFrame entity = (BlockEntityItemFrame) level.getBlockEntity(blockAir.getLocation());

                    blockItemFrame.setDamage(FACING[info.face.getIndex()]);
                    blockItemFrame.getLevel().setBlock(blockAir, blockItemFrame, true, false);

                    CompoundTag nbt = new CompoundTag()
                            .putString("id", BlockEntity.ITEM_FRAME)
                            .putInt("x", (int) blockAir.x)
                            .putInt("y", (int) blockAir.y)
                            .putInt("z", (int) blockAir.z)
                            .putByte("ItemRotation", 0)
                            .putFloat("ItemDropChance", 1.0f);
                    BlockEntityItemFrame frame = (BlockEntityItemFrame) BlockEntity.createBlockEntity(BlockEntity.ITEM_FRAME, blockItemFrame.getLevel().getChunk((int) blockAir.x >> 4, (int) blockAir.z >> 4), nbt);

                    frame.setItem(currentList.get(startY - y));
                }
            }
        }
    }
}
