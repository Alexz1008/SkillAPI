/*
 * SkillAPI
 * com.sucy.skill.api.util.ParticleHelper
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2014 Steven Sucy
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software") to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.sucy.skill.api.util;

import com.destroystokyo.paper.ParticleBuilder;
import com.google.common.collect.ImmutableSet;
import com.rit.sucy.version.VersionManager;
import com.sucy.skill.api.Settings;
import com.sucy.skill.api.enums.Direction;
import org.bukkit.*;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Wolf;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.HashMap;
import java.util.Random;
import java.util.Set;

/**
 * Helper class for playing particles via config strings in various ways.
 */
public class ParticleHelper {
    /**
     * Settings key for the arrangement type of particles
     */
    public static final String ARRANGEMENT_KEY = "arrangement";

    /**
     * Number of particles
     */
    public static final String PARTICLES_KEY = "particles";

    /**
     * The level to use for scaling values
     */
    public static final String LEVEL = "level";

    /**
     * Settings key for the type of particle
     */
    public static final String PARTICLE_KEY = "particle";

    /**
     * Settings key for the material used by the particle (for block crack, icon crack, and block dust)
     */
    public static final String MATERIAL_KEY = "material";

    /**
     * Settings key for the material data used by the particle (for block crack, icon crack, and block dust)
     */
    public static final String TYPE_KEY = "type";

    /**
     * Settings key for the radius of the particle arrangement
     */
    public static final String RADIUS_KEY = "radius";

    /**
     * Settings key for the amount of particles to play
     */
    public static final String AMOUNT_KEY = "amount";

    /**
     * Settings key for the particle arrangement direction (circles only)
     */
    public static final String DIRECTION_KEY = "direction";

    /**
     * Settings key for the Bukkit effects' data (default 0)
     */
    public static final String DATA_KEY = "data";

    /**
     * Settings key for the reflection particles' visible radius (default 25)
     */
    public static final String VISIBLE_RADIUS_KEY = "visible-radius";

    /**
     * Settings key for the reflection particles' X-offset (default 0)
     */
    public static final String DX_KEY = "dx";

    /**
     * Settings key for the reflection particles' Y-offset (default 0)
     */
    public static final String DY_KEY = "dy";

    /**
     * Settings key for the reflection particles' Z-offset (default 0)
     */
    public static final String DZ_KEY = "dz";

    /**
     * Settings key for the reflection particles' "speed" value (default 1)
     */
    public static final String SPEED_KEY = "speed";

    private static final Random random = new Random();

    /**
     * Plays an entity effect at the given location
     *
     * @param loc    location to play the effect
     * @param effect entity effect to play
     */
    public static void play(Location loc, EntityEffect effect) {
        Wolf wolf = (Wolf) loc.getWorld().spawnEntity(loc, EntityType.WOLF);
        wolf.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 100, 100));
        wolf.playEffect(effect);
        wolf.remove();
    }

    /**
     * Plays particles about the given location using the given settings
     *
     * @param loc      location to center the effect around
     * @param settings data to play the particles with
     */
    public static void play(LivingEntity caster, Location loc, Settings settings) {
        String particle = settings.getString(PARTICLE_KEY, "invalid");
        if (settings.has(ARRANGEMENT_KEY)) {
            int level = settings.getInt(LEVEL, 1);
            double radius = settings.getAttr(RADIUS_KEY, level, 3.0);
            int amount = (int) settings.getAttr(PARTICLES_KEY, level, 10);

            String arrangement = settings.getString(ARRANGEMENT_KEY).toLowerCase();
            switch (arrangement) {
                case "circle":
                    Direction dir = null;
                    if (settings.has(DIRECTION_KEY)) {
                        try {
                            dir = Direction.valueOf(settings.getString(DIRECTION_KEY));
                        } catch (Exception ex) { /* Use default value */ }
                    }
                    if (dir == null) {
                        dir = Direction.XZ;
                    }

                    fillCircle(caster, loc, particle, settings, radius, amount, dir);
                    break;
                case "sphere":
                    fillSphere(caster, loc, particle, settings, radius, amount);
                    break;
                case "hemisphere":
                    fillHemisphere(caster, loc, particle, settings, radius, amount);
                    break;
            }
        } else {
            play(caster, loc, particle, settings);
        }
    }

    /**
     * Plays a particle at the given location based on the string
     *
     * @param loc      location to play the effect
     * @param particle particle to play
     * @param settings data to play the particle with
     */
    public static void play(LivingEntity caster, Location loc, final String particle, Settings settings) {
        int rad = settings.getInt(VISIBLE_RADIUS_KEY, 25);

        final boolean onlyCaster = settings.getBool("onlycaster", true);

        final float dx = (float) settings.getDouble(DX_KEY, 0.0);
        final float dy = (float) settings.getDouble(DY_KEY, 0.0);
        final float dz = (float) settings.getDouble(DZ_KEY, 0.0);
        final int amount = settings.getInt(AMOUNT_KEY, 1);
        final float speed = (float) settings.getDouble(SPEED_KEY, 1.0);
        final Material mat = Material.valueOf(settings.getString(MATERIAL_KEY, "DIRT").toUpperCase().replace(" ", "_"));

        try {
            // Normal bukkit effects
            if (BUKKIT_EFFECTS.containsKey(particle)) {
                loc.getWorld().playEffect(loc, BUKKIT_EFFECTS.get(particle), settings.getInt(DATA_KEY, 0));
            }

            // Entity effects
            else if (ENTITY_EFFECTS.contains(particle)) {
                play(loc, EntityEffect.valueOf(particle.toUpperCase().replace(' ', '_')));
            }

            // v1.13 particles
            else if (VersionManager.isVersionAtLeast(11300)) {

                ParticleBuilder builder = new ParticleBuilder(org.bukkit.Particle.valueOf(particle))
                        .location(loc)
                        .extra(speed)
                        .offset(dx, dy, dz)
                        .count(amount);

                if (particle.toLowerCase().startsWith("block")) {
                    builder.data(mat.createBlockData());
                }

                if (particle.toLowerCase().startsWith("icon")) {
                    builder.data(new ItemStack(mat));
                }

                if (particle.equalsIgnoreCase("redstone")) {
                    final Color color = Color.fromRGB((int) (255 * dx), (int) (255 * dy), (int) (255 * dz));
                    builder.color(color);
                }

                if (onlyCaster) {
                    builder.receivers((Player) caster);
                } else {
                    builder.receivers(rad);
                }

                builder.spawn();
            }
        } catch (Exception ex) {
            System.out.println("ERROR: " + caster.getName());
            settings.dumpToConsole();
        }
    }

    public static ParticleBuilder configureParticle(LivingEntity caster, Settings settings) {
        String particle = settings.getString(PARTICLE_KEY, "invalid");
        final boolean onlyCaster = settings.getBool("onlycaster", true);

        int rad = settings.getInt(VISIBLE_RADIUS_KEY, 25);
        final float dx = (float) settings.getDouble(DX_KEY, 0.0);
        final float dy = (float) settings.getDouble(DY_KEY, 0.0);
        final float dz = (float) settings.getDouble(DZ_KEY, 0.0);
        final int amount = settings.getInt(AMOUNT_KEY, 1);
        final float speed = (float) settings.getDouble(SPEED_KEY, 1.0);
        final Material mat = Material.valueOf(settings.getString(MATERIAL_KEY, "DIRT").toUpperCase().replace(" ", "_"));

        try {

            ParticleBuilder builder = new ParticleBuilder(org.bukkit.Particle.valueOf(particle))
                    .extra(speed)
                    .offset(dx, dy, dz)
                    .count(amount);

            if (particle.toLowerCase().startsWith("block")) {
                builder.data(mat.createBlockData());
            }

            if (particle.toLowerCase().startsWith("icon")) {
                builder.data(new ItemStack(mat));
            }

            if (particle.equalsIgnoreCase("redstone")) {
                final Color color = Color.fromRGB((int) (255 * dx), (int) (255 * dy), (int) (255 * dz));
                builder.color(color);
            }

            if (onlyCaster) {
                builder.receivers((Player) caster);
            } else {
                builder.receivers(rad);
            }

            return builder;

        } catch (Exception ex) {
            System.out.println("ERROR: " + caster.getName());
            settings.dumpToConsole();
            return null;
        }
    }

    /**
     * Plays several of a particle type randomly within a circle
     *
     * @param loc      center location of the circle
     * @param particle particle to play
     * @param settings data to play the particle with
     * @param radius   radius of the circle
     * @param amount   amount of particles to play
     */
    public static void fillCircle(
            LivingEntity caster,
            Location loc,
            String particle,
            Settings settings,
            double radius,
            int amount,
            Direction direction) {
        Location temp = loc.clone();
        double rSquared = radius * radius;
        double twoRadius = radius * 2;
        int index = 0;

        // Play the particles
        while (index < amount) {
            if (direction == Direction.XY || direction == Direction.XZ) {
                temp.setX(loc.getX() + random.nextDouble() * twoRadius - radius);
            }
            if (direction == Direction.XY || direction == Direction.YZ) {
                temp.setY(loc.getY() + random.nextDouble() * twoRadius - radius);
            }
            if (direction == Direction.XZ || direction == Direction.YZ) {
                temp.setZ(loc.getZ() + random.nextDouble() * twoRadius - radius);
            }

            if (temp.distanceSquared(loc) > rSquared) {
                continue;
            }

            play(caster, temp, particle, settings);
            index++;
        }
    }

    /**
     * Randomly plays particle effects within the sphere
     *
     * @param loc      location to center the effect around
     * @param particle the string value for the particle
     * @param settings data to play the particle with
     * @param radius   radius of the sphere
     * @param amount   amount of particles to use
     */
    public static void fillSphere(LivingEntity caster, Location loc, String particle, Settings settings, double radius, int amount) {
        Location temp = loc.clone();
        double rSquared = radius * radius;
        double twoRadius = radius * 2;
        int index = 0;

        // Play the particles
        while (index < amount) {
            temp.setX(loc.getX() + random.nextDouble() * twoRadius - radius);
            temp.setY(loc.getY() + random.nextDouble() * twoRadius - radius);
            temp.setZ(loc.getZ() + random.nextDouble() * twoRadius - radius);

            if (temp.distanceSquared(loc) > rSquared) {
                continue;
            }

            play(caster, temp, particle, settings);
            index++;
        }
    }

    /**
     * Randomly plays particle effects within the hemisphere
     *
     * @param loc      location to center the effect around
     * @param particle the string value for the particle
     * @param settings data to play the particle with
     * @param radius   radius of the sphere
     * @param amount   amount of particles to use
     */
    public static void fillHemisphere(LivingEntity caster, Location loc, String particle, Settings settings, double radius, int amount) {
        Location temp = loc.clone();
        double rSquared = radius * radius;
        double twoRadius = radius * 2;
        int index = 0;

        // Play the particles
        while (index < amount) {
            temp.setX(loc.getX() + random.nextDouble() * twoRadius - radius);
            temp.setY(loc.getY() + random.nextDouble() * radius);
            temp.setZ(loc.getZ() + random.nextDouble() * twoRadius - radius);

            if (temp.distanceSquared(loc) > rSquared) {
                continue;
            }

            play(caster, temp, particle, settings);
            index++;
        }
    }

    private static final HashMap<String, Effect> BUKKIT_EFFECTS = new HashMap<String, Effect>() {{
        put("smoke", Effect.SMOKE);
        put("ender signal", Effect.ENDER_SIGNAL);
        put("mobspawner flames", Effect.MOBSPAWNER_FLAMES);
        put("potion break", Effect.POTION_BREAK);
    }};

    private static final Set<String> ENTITY_EFFECTS = ImmutableSet.of(
            "death", "hurt", "sheep eat", "wolf hearts", "wolf shake", "wolf smoke");

}
