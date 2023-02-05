package cn.nukkit.inventory;

import cn.nukkit.Player;
import cn.nukkit.Server;
import cn.nukkit.block.BlockAir;
import cn.nukkit.entity.EntityHuman;
import cn.nukkit.entity.EntityHumanType;
import cn.nukkit.event.entity.EntityArmorChangeEvent;
import cn.nukkit.event.entity.EntityInventoryChangeEvent;
import cn.nukkit.event.player.PlayerItemHeldEvent;
import cn.nukkit.item.Item;
import cn.nukkit.item.ItemArmor;
import cn.nukkit.item.ItemBlock;
import cn.nukkit.nbt.tag.CompoundTag;
import cn.nukkit.nbt.tag.Tag;
import cn.nukkit.network.protocol.ContainerSetContentPacket;
import cn.nukkit.network.protocol.ContainerSetSlotPacket;
import cn.nukkit.network.protocol.MobArmorEquipmentPacket;
import cn.nukkit.network.protocol.MobEquipmentPacket;
import cn.nukkit.network.protocol.types.ContainerIds;
import org.apache.commons.lang3.ArrayUtils;

import java.util.Collection;
import java.util.Map;

/**
 * author: MagicDroidX
 * Nukkit Project
 */
public class PlayerInventory extends BaseInventory {

    protected int itemInHandIndex = 0;

    protected int[] hotbar;

    public PlayerInventory(EntityHumanType player, CompoundTag contents) {
        super(player, InventoryType.PLAYER);
        this.hotbar = new int[this.getHotbarSize()];
        for (int i = 0; i < this.hotbar.length; i++) {
            this.hotbar[i] = i;
        }
    }

    public PlayerInventory(EntityHumanType player) {
        this(player, null);
    }

    @Override
    public int getSize() {
        return super.getSize() - 4;
    }

    @Override
    public void setSize(int size) {
        super.setSize(size + 4);
        this.sendContents(this.getViewers());
    }

    public int getHotbarSlotIndex(int index) {
        return (index >= 0 && index < this.getHotbarSize()) ? this.hotbar[index] : -1;
    }

    public void setHotbarSlotIndex(int index, int slot) {
        if (index >= 0 && index < this.getHotbarSize() && slot >= -1 && slot < this.getSize()) {
            this.hotbar[index] = slot;
        }
    }

    public int getHeldItemIndex() {
        return itemInHandIndex;
    }

    //i love java....
    public void setHeldItemIndex(int index, boolean sendToHolder, int slotMapping) {
        if (slotMapping != -1000) {
            slotMapping -= this.getHotbarSize();
        }

        if (index >= 0 && index < this.getHotbarSize()) {
            this.itemInHandIndex = index;
        }
        if (index >= 0 && index < this.getHotbarSize()) {
            this.itemInHandIndex = index;
            if (slotMapping != -1000) {
                if (slotMapping < 0 || slotMapping >= this.getSize()) {
                    slotMapping -= 1;
                }

                Item item = this.getItem(slotMapping);
                if (this.getHolder() instanceof Player) {
                    PlayerItemHeldEvent event = new PlayerItemHeldEvent((Player) this.getHolder(), item, slotMapping, index);
                    Server.getInstance().getPluginManager().callEvent(event);
                    if (event.isCancelled()) {
                        this.sendHeldItem((Player) this.getHolder());
                        this.sendContents((Player) this.getHolder());
                    }
                }
            }

            int key = ArrayUtils.indexOf(this.hotbar, slotMapping);
            if (key != -1 && slotMapping != -1) {
                this.hotbar[key] = this.hotbar[this.itemInHandIndex];
            }
            this.hotbar[this.itemInHandIndex] = slotMapping;
        }
        this.sendHeldItem(this.getHolder().getViewers().values());
        if (sendToHolder) {
            this.sendHeldItem((Player) this.getHolder());
        }
    }

    public void setHeldItemIndex(int index, boolean sendToHolder) {
        this.setHeldItemIndex(index, sendToHolder, -1000);
    }

    public void setHeldItemIndex(int index) {
        this.setHeldItemIndex(index, true, -1000);
    }

    public Item getItemInHand() {
        Item item = this.getItem(this.getHeldItemSlot());
        if (item != null) {
            return item;
        } else {
            return new ItemBlock(new BlockAir(), 0, 0);
        }
    }

    public boolean setItemInHand(Item item) {
        return this.setItem(this.getHeldItemSlot(), item);
    }

    public int getHeldItemSlot() {
        return this.getHotbarSlotIndex(this.itemInHandIndex);
    }

    private boolean isHotbarSlot(int slot) {
        return slot >= 0 && slot <= this.getHotbarSize();
    }

    public void setHeldItemSlot(int slot) {
        if (!isHotbarSlot(slot)) {
            return;
        }

        this.itemInHandIndex = slot;

        if (this.getHolder() instanceof Player) {
            this.sendHeldItem((Player) this.getHolder());
        }

        this.sendHeldItem(this.getViewers());
    }

    public void sendHeldItem(Player player) {
        Item item = this.getItemInHand();

        MobEquipmentPacket pk = new MobEquipmentPacket();
        pk.eid = this.getHolder().getId();
        pk.item = item;
        pk.slot = (byte) this.getHeldItemSlot();
        pk.selectedSlot = (byte) this.getHeldItemIndex();
        pk.windowId = ContainerSetContentPacket.SPECIAL_INVENTORY;

        player.dataPacket(pk);
        if (this.getHeldItemSlot() != -1 && player == this.getHolder()) {
            this.sendSlot(this.getHeldItemSlot(), player);
        }
    }

    public void sendHeldItem(Player... players) {
        Item item = this.getItemInHand();

        MobEquipmentPacket pk = new MobEquipmentPacket();
        pk.item = item;
        pk.slot = pk.selectedSlot = this.getHeldItemIndex();

        for (Player player : players) {
            pk.eid = this.getHolder().getId();
            if (player.equals(this.getHolder())) {
                pk.eid = player.getId();
                this.sendSlot(this.getHeldItemIndex(), player);
            }

            player.dataPacket(pk);
        }
    }

    public void sendHeldItem(Map<Integer, Player> players) {
        this.sendHeldItem(players.values().toArray(Player.EMPTY_ARRAY));
    }

    public void sendHeldItem(Collection<Player> players) {
        this.sendHeldItem(players.toArray(Player.EMPTY_ARRAY));
    }

    @Override
    public void onSlotChange(int index, Item before, boolean send) {
        EntityHuman holder = this.getHolder();
        if (holder instanceof Player && !((Player) holder).spawned) {
            return;
        }

        if (index >= this.getSize()) {
            this.sendArmorSlot(index, this.getViewers());
            this.sendArmorSlot(index, this.getHolder().getViewers().values());
        } else {
            super.onSlotChange(index, before, send);
        }
    }

    public int getHotbarSize() {
        return 9;
    }

    public Item getArmorItem(int index) {
        return this.getItem(this.getSize() + index);
    }

    public boolean setArmorItem(int index, Item item) {
        return this.setItem(this.getSize() + index, item);
    }

    public Item getHelmet() {
        return this.getItem(this.getSize());
    }

    public Item getChestplate() {
        return this.getItem(this.getSize() + 1);
    }

    public Item getLeggings() {
        return this.getItem(this.getSize() + 2);
    }

    public Item getBoots() {
        return this.getItem(this.getSize() + 3);
    }

    public boolean setHelmet(Item helmet) {
        return this.setItem(this.getSize(), helmet);
    }

    public boolean setChestplate(Item chestplate) {
        return this.setItem(this.getSize() + 1, chestplate);
    }

    public boolean setLeggings(Item leggings) {
        return this.setItem(this.getSize() + 2, leggings);
    }

    public boolean setBoots(Item boots) {
        return this.setItem(this.getSize() + 3, boots);
    }

    @Override
    public boolean setItem(int index, Item item, boolean send) {
        if (index < 0 || index >= this.size) {
            return false;
        } else if (item.getId() == 0 || item.getCount() <= 0) {
            return this.clear(index, send);
        }

        //Armor change
        if (index >= this.getSize()) {
            EntityArmorChangeEvent ev = new EntityArmorChangeEvent(this.getHolder(), this.getItem(index), item, index);
            Server.getInstance().getPluginManager().callEvent(ev);
            if (ev.isCancelled() && this.getHolder() != null) {
                this.sendArmorSlot(index, this.getViewers());
                return false;
            }
            item = ev.getNewItem();
        } else {
            EntityInventoryChangeEvent ev = new EntityInventoryChangeEvent(this.getHolder(), this.getItem(index), item, index);
            Server.getInstance().getPluginManager().callEvent(ev);
            if (ev.isCancelled()) {
                this.sendSlot(index, this.getViewers());
                return false;
            }
            item = ev.getNewItem();
        }

        Item old = this.getItem(index);
        this.slots.put(index, item.clone());
        this.onSlotChange(index, old, send);

        return true;
    }

    @Override
    public boolean setItem(int index, Item item) {
        return this.setItem(index, item, true);
    }

    @Override
    public boolean clear(int index, boolean send) {
        if (this.slots.containsKey(index)) {
            Item item = new ItemBlock(new BlockAir(), null, 0);
            Item old = this.slots.get(index);
            if (index >= this.getSize() && index < this.size) {
                EntityArmorChangeEvent ev = new EntityArmorChangeEvent(this.getHolder(), old, item, index);
                Server.getInstance().getPluginManager().callEvent(ev);
                if (ev.isCancelled()) {
                    if (index >= this.size) {
                        this.sendArmorSlot(index, this.getViewers());
                    } else {
                        this.sendSlot(index, this.getViewers());
                    }
                    return false;
                }
                item = ev.getNewItem();
            } else {
                EntityInventoryChangeEvent ev = new EntityInventoryChangeEvent(this.getHolder(), old, item, index);
                Server.getInstance().getPluginManager().callEvent(ev);
                if (ev.isCancelled()) {
                    if (index >= this.size) {
                        this.sendArmorSlot(index, this.getViewers());
                    } else {
                        this.sendSlot(index, this.getViewers());
                    }
                    return false;
                }
                item = ev.getNewItem();
            }

            if (item.getId() != Item.AIR) {
                this.slots.put(index, item.clone());
            } else {
                this.slots.remove(index);
            }

            this.onSlotChange(index, old, send);
        }

        return true;
    }

    @Override
    public boolean clear(int index) {
        return this.clear(index, true);
    }

    public Item[] getArmorContents() {
        Item[] armor = new Item[4];
        for (int i = 0; i < 4; i++) {
            armor[i] = this.getItem(this.getSize() + i);
        }

        return armor;
    }

    @Override
    public void clearAll(boolean send) {
        int limit = this.getSize() + 4;
        for (int index = 0; index < limit; ++index) {
            this.clear(index, send);
        }
        this.hotbar = new int[this.getHotbarSize()];
        for (int i = 0; i < this.hotbar.length; i++) {
            this.hotbar[i] = i;
        }
        this.sendContents(this.getViewers());
    }

    @Override
    public void clearAll() {
        this.clearAll(true);
    }

    public void sendArmorContents(Player player) {
        this.sendArmorContents(new Player[]{player});
    }

    public void sendArmorContents(Player[] players) {
        Item[] armor = this.getArmorContents();

        MobArmorEquipmentPacket pk = new MobArmorEquipmentPacket();
        pk.eid = this.getHolder().getId();
        pk.slots = armor;
        pk.encode();
        pk.isEncoded = true;

        for (Player player : players) {
            if (player.equals(this.getHolder())) {
                ContainerSetContentPacket pk2 = new ContainerSetContentPacket();
                pk2.windowid = ContainerSetContentPacket.SPECIAL_ARMOR;
                pk2.eid = player.getId();
                pk2.slots = armor;
                player.dataPacket(pk2);
            } else {
                player.dataPacket(pk);
            }
        }
    }

    public void setArmorContents(Item[] items) {
        for (int i = 0; i < 4; ++i) {
            if (items[i] == null) {
                items[i] = new ItemBlock(new BlockAir(), null, 0);
            }

            if (items[i].getId() == Item.AIR) {
                this.clear(this.getSize() + i);
            } else {
                this.setItem(this.getSize() + i, items[i]);
            }
        }
    }

    public void sendArmorContents(Collection<Player> players) {
        this.sendArmorContents(players.toArray(Player.EMPTY_ARRAY));
    }

    public void sendArmorSlot(int index, Player player) {
        this.sendArmorSlot(index, new Player[]{player});
    }

    public void sendArmorSlot(int index, Player[] players) {
        Item[] armor = this.getArmorContents();

        MobArmorEquipmentPacket pk = new MobArmorEquipmentPacket();
        pk.eid = this.getHolder().getId();
        pk.slots = armor;
        pk.encode();
        pk.isEncoded = true;

        for (Player player : players) {
            if (player.equals(this.getHolder())) {
                ContainerSetSlotPacket pk2 = new ContainerSetSlotPacket();
                pk2.windowid = ContainerSetContentPacket.SPECIAL_ARMOR;
                pk2.slot = index - this.getSize();
                pk2.item = this.getItem(index);
                player.dataPacket(pk2);
            } else {
                player.dataPacket(pk);
            }
        }
    }

    public void sendArmorSlot(int index, Collection<Player> players) {
        this.sendArmorSlot(index, players.toArray(Player.EMPTY_ARRAY));
    }

    @Override
    public void sendContents(Player player) {
        this.sendContents(new Player[]{player});
    }

    @Override
    public void sendContents(Collection<Player> players) {
        this.sendContents(players.toArray(Player.EMPTY_ARRAY));
    }
    @Override
    public void sendContents(Player[] players) {
        ContainerSetContentPacket pk = new ContainerSetContentPacket();
        pk.slots = new Item[this.getSize() +  + this.getHotbarSize()];
        for (int i = 0; i < this.getSize(); ++i) {
            pk.slots[i] = this.getItem(i);
        }

        //Because PE is stupid and shows 9 less slots than you send it, give it 9 dummy slots so it shows all the REAL slots.
        for(int i = this.getSize(); i < this.getSize() + this.getHotbarSize(); ++i){
            pk.slots[i] = new ItemBlock(new BlockAir());
        }

        for (Player player : players) {
            if (player.equals(this.getHolder())) {
                pk.hotbar = new int[this.getHotbarSize()];
                for (int i = 0; i < this.getHotbarSize(); ++i) {
                    int index = this.getHotbarSlotIndex(i);
                    pk.hotbar[i] = index <= -1 ? -1 : index + 9;
                }
            }
            int id = player.getWindowId(this);
            if (id == -1 || !player.spawned) {
                this.close(player);
                continue;
            }
            pk.eid = player.getId();
            pk.windowid = (byte) id;
            player.dataPacket(pk.clone());
        }
    }

    @Override
    public void sendSlot(int index, Player player) {
        this.sendSlot(index, new Player[]{player});
    }

    @Override
    public void sendSlot(int index, Collection<Player> players) {
        this.sendSlot(index, players.toArray(Player.EMPTY_ARRAY));
    }

    @Override
    public void sendSlot(int index, Player... players) {
        ContainerSetSlotPacket pk = new ContainerSetSlotPacket();
        pk.slot = index;
        pk.item = this.getItem(index).clone();

        for (Player player : players) {
            if (player.equals(this.getHolder())) {
                pk.windowid = ContainerIds.INVENTORY;
                player.dataPacket(pk);
            } else {
                int id = player.getWindowId(this);
                if (id == -1) {
                    this.close(player);
                    continue;
                }
                pk.windowid = id;
                player.dataPacket(pk.clone());
            }
        }
    }

    @Override
    public EntityHuman getHolder() {
        return (EntityHuman) super.getHolder();
    }
}

