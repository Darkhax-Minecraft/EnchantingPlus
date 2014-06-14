package com.aesireanempire.eplus;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.event.ForgeSubscribe;
import net.minecraftforge.event.entity.EntityEvent;

/**
 * Created by freyja
 */
public class Events
{
    @ForgeSubscribe
    public void extendedProperties(EntityEvent.EntityConstructing event)
    {
        if(event.entity instanceof EntityPlayer)
        {
            event.entity.registerExtendedProperties("eplus", new Properties((EntityPlayer) event.entity));
        }
    }
}
