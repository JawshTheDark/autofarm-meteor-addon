package com.jawsh.autofarm;

import com.jawsh.autofarm.modules.AutoFarm;
import com.jawsh.autofarm.modules.AutoPlanter;

import meteordevelopment.meteorclient.addons.MeteorAddon;
import meteordevelopment.meteorclient.systems.modules.Modules;

public class AutoFarmAddon extends MeteorAddon {
    @Override
    public void onInitialize() {
        // Register the AutoFarm module so it appears in Meteor
        Modules.get().add(new AutoFarm());
        Modules.get().add(new AutoPlanter());
    }

    @Override
    public String getPackage() {
        return "com.jawsh.autofarm";
    }
}
