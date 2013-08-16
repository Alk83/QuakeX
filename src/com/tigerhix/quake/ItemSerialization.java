package com.tigerhix.quake;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;

import org.yaml.snakeyaml.external.biz.base64Coder.Base64Coder;

import net.minecraft.server.v1_6_R2.NBTBase;
import net.minecraft.server.v1_6_R2.NBTTagCompound;
import net.minecraft.server.v1_6_R2.NBTTagList;

import org.bukkit.craftbukkit.v1_6_R2.inventory.CraftInventoryCustom;
import org.bukkit.craftbukkit.v1_6_R2.inventory.CraftItemStack;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class ItemSerialization {
    public static String toBase64(Inventory inventory) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        DataOutputStream dataOutput = new DataOutputStream(outputStream);
        NBTTagList itemList = new NBTTagList();

        // Save every element in the list
        for (int i = 0; i < inventory.getSize(); i++) {
            NBTTagCompound outputObject = new NBTTagCompound();
            CraftItemStack craft = getCraftVersion(inventory.getItem(i));

            // Convert the item stack to a NBT compound
            if (craft != null)
                CraftItemStack.asNMSCopy(craft).save(outputObject);
            itemList.add(outputObject);
        }

        // Now save the list
        NBTBase.a(itemList, dataOutput);

        // Serialize that array
        return Base64Coder.encodeLines(outputStream.toByteArray());
    }

    public static Inventory fromBase64(String data) {
        ByteArrayInputStream inputStream = new ByteArrayInputStream(Base64Coder.decodeLines(data));
        NBTTagList itemList = (NBTTagList) NBTBase.a(new DataInputStream(inputStream));
        Inventory inventory = new CraftInventoryCustom(null, itemList.size());

        for (int i = 0; i < itemList.size(); i++) {
            NBTTagCompound inputObject = (NBTTagCompound) itemList.get(i);

            if (!inputObject.isEmpty()) {
                inventory.setItem(i, CraftItemStack.asCraftMirror(
                        net.minecraft.server.v1_6_R2.ItemStack.createStack(inputObject)));
            }
        }

        // Serialize that array
        return inventory;
    }

    public static Inventory getInventoryFromArray(ItemStack[] items) {
        CraftInventoryCustom custom = new CraftInventoryCustom(null, items.length);

        for (int i = 0; i < items.length; i++) {
            if (items[i] != null) {
                custom.setItem(i, items[i]);
            }
        }
        return custom;
    }

    private static CraftItemStack getCraftVersion(ItemStack stack) {
        if (stack instanceof CraftItemStack)
            return (CraftItemStack) stack;
        else if (stack != null)
            return CraftItemStack.asCraftCopy(stack);
        else
            return null;
    }
}