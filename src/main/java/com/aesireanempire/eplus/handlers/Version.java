package com.aesireanempire.eplus.handlers;

import com.aesireanempire.eplus.EnchantingPlus;
import com.aesireanempire.eplus.lib.References;

import net.minecraftforge.common.Property;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Properties;

import cpw.mods.fml.common.Loader;

/**
 * @author Freyja Lesser GNU Public License v3
 *         (http://www.gnu.org/licenses/lgpl.html)
 */
public class Version implements Runnable
{
    private static final String REMOTE_VERSION_FILE = "https://dl.dropboxusercontent.com/u/21347544/EnchantingPlus/eplusvers.txt";// "https://raw.github.com/odininon/EnchantingPlus/1.4.6/resources/version";
    private static final String REMOTE_CHANGELOG = "https://dl.dropboxusercontent.com/u/21347544/EnchantingPlus/changelogs/";
    public static Version instance = new Version();
    public static EnumUpdateState currentVersion = EnumUpdateState.CURRENT;
    private static boolean updated;
    private static boolean versionCheckCompleted;
    private static ModVersion recommendedVersion;
    private static ModVersion currentModVersion;
    private static String[] cachedChangelog;
    private static String recommendedDownload;

    public static void check()
    {
        new Thread(instance).start();
    }

    public static String[] getChangelog()
    {
        if (cachedChangelog == null)
        {
            cachedChangelog = grabChangelog(recommendedVersion);
        }
        return cachedChangelog;
    }

    public static ModVersion getCurrentModVersion()
    {
        return currentModVersion;
    }

    public static String getMinecraftVersion()
    {
        return Loader.instance().getMinecraftModContainer().getVersion();
    }

    public static String getRecommendedDownload()
    {
        return recommendedDownload;
    }

    public static ModVersion getRecommendedVersion()
    {
        return recommendedVersion;
    }

    public static String[] grabChangelog(ModVersion recommendedVersion)
    {

        String recommendedVersionFile = recommendedVersion.getAsFileExtension() + ".txt";

        final String changelogURL = REMOTE_CHANGELOG + recommendedVersionFile;

        try
        {
            final URL url = new URL(changelogURL);
            final BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()));

            String line = null;
            final ArrayList<String> changelog = new ArrayList<String>();
            while ((line = reader.readLine()) != null)
            {

                if (line.startsWith("#"))
                {
                    continue;
                }

                if (line.isEmpty())
                {
                    continue;
                }

                changelog.add(line);
            }
            return changelog.toArray(new String[changelog.size()]);
        }
        catch (final Exception ex)
        {

        }
        return new String[]
                { "Failed to grab changelog." };
    }

    public static boolean hasUpdated()
    {
        return updated;
    }

    public static void init(Properties versionProperties)
    {
        if (versionProperties == null)
        {
            currentModVersion = new ModVersion("0.0.0");
            return;
        }

        final String major = versionProperties.getProperty("eplus.major.number");
        final String minor = versionProperties.getProperty("eplus.minor.number");
        versionProperties.getProperty("eplus.revision.number");
        currentModVersion = new ModVersion(major + "." + minor);
    }

    public static boolean isVersionCheckComplete()
    {
        return versionCheckCompleted;
    }

    public static void versionCheck()
    {
        final Properties props = new Properties();
        try
        {
            final URL url = new URL(REMOTE_VERSION_FILE);

            final InputStreamReader inputStreamReader = new InputStreamReader(url.openStream());
            final BufferedReader reader = new BufferedReader(inputStreamReader);

            String line = null;
            final String mcVersion = getMinecraftVersion();
            while ((line = reader.readLine()) != null)
            {
                if (line.startsWith(mcVersion))
                {
                    if (line.contains(References.MODID))
                    {

                        final String[] tokens = line.split(":");
                        recommendedVersion = new ModVersion(tokens[2]);

                        if (line.endsWith(currentModVersion.toString()))
                        {
                            EnchantingPlus.log.info("Using the latest version [" + getCurrentModVersion() + "] for Minecraft " + mcVersion);
                            currentVersion = EnumUpdateState.CURRENT;
                            return;
                        }
                    }
                }
            }

        }
        catch (final Exception ex)
        {
            EnchantingPlus.log.warning("Unable to read from remote version authority.");
            ex.printStackTrace();
            currentVersion = EnumUpdateState.CONNECTION_ERROR;
            recommendedVersion = new ModVersion("0.0.0");
            return;
        }

        int compared = currentModVersion.compareTo(recommendedVersion);

        if (compared == 0)
        {
            EnchantingPlus.log.info("Using the latest version for Minecraft " + getMinecraftVersion());
            currentVersion = EnumUpdateState.CURRENT;
            updated = false;
        }
        else if (compared > 0)
        {
            EnchantingPlus.log.info("Using a Beta version of Enchanting Plus: " + getCurrentModVersion());
            currentVersion = EnumUpdateState.BETA;
            updated = false;
        }
        else if (compared < 0)
        {
            EnchantingPlus.log.info("An updated version of Enchanting Plus is available: " + getRecommendedVersion());
            currentVersion = EnumUpdateState.OUTDATED;
            updated = true;
        }
        props.clear();
    }

    public static boolean versionSeen()
    {
        if (!(currentVersion == EnumUpdateState.OUTDATED) || (currentVersion == EnumUpdateState.BETA))
        {
            return false;
        }

        final Property property = ConfigurationHandler.configuration.get("version", "SeenVersion", currentModVersion.toString());
        final String seenVersion = property.getString();

        if (recommendedVersion == null || recommendedVersion.equals( new ModVersion(seenVersion)))
        {
            return false;
        }

        property.set(getRecommendedVersion().toString());
        ConfigurationHandler.configuration.save();
        return true;
    }

    @Override
    public void run()
    {
        int count = 0;
        currentVersion = null;

        EnchantingPlus.log.info("Starting version check thread");

        while (count < 3 && (currentVersion == null || currentVersion == EnumUpdateState.CONNECTION_ERROR))
        {
            versionCheck();
            count++;
        }

        EnchantingPlus.log.info("Version check complete with " + currentVersion);
        versionCheckCompleted = true;
    }

    public static enum EnumUpdateState
    {
        CURRENT, OUTDATED, CONNECTION_ERROR, BETA
    }
}
